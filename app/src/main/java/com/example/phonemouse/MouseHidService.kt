package com.example.phonemouse

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Service that manages Bluetooth HID communication, acting as a virtual mouse.
 * Handles profile registration, manual reports, and an automated randomization click loop.
 */
class MouseHidService(private val context: Context) {
    companion object {
        private const val TAG = "MouseHidService"
    }

    private var hidDevice: BluetoothHidDevice? = null
    private var isRegistered = false
    private var connectedHost: BluetoothDevice? = null
    private var currentButtonState: Byte = 0

    @Suppress("DEPRECATION")
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private var currentConfig: AutomationConfig? = null

    private val _isConnected = MutableStateFlow(false)
    /** Emits true when a Bluetooth host is actively connected. */
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    /** Emits the name of the currently connected Bluetooth host. */
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _isAutomationRunning = MutableStateFlow(false)
    /** Emits true when the automated click loop is active. */
    val isAutomationRunning: StateFlow<Boolean> = _isAutomationRunning.asStateFlow()

    /**
     * Updates the automation parameters used by the click loop.
     */
    fun setConfig(config: AutomationConfig?) {
        currentConfig = config
    }

    /** 
     * Standard HID Mouse Descriptor (4-byte version: Buttons, X, Y, Wheel)
     * Provides full compatibility with Windows and Android system settings.
     */
    private val hidMouseDescriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // USAGE (Mouse)
        0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x09.toByte(), 0x01.toByte(), //   USAGE (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   COLLECTION (Physical)
        0x05.toByte(), 0x09.toByte(), //     USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(), //     USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x03.toByte(), //     USAGE_MAXIMUM (Button 3)
        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //     LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //     REPORT_SIZE (1)
        0x95.toByte(), 0x03.toByte(), //     REPORT_COUNT (3)
        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
        0x75.toByte(), 0x05.toByte(), //     REPORT_SIZE (5)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x81.toByte(), 0x03.toByte(), //     INPUT (Cnst,Var,Abs)
        0x05.toByte(), 0x01.toByte(), //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //     USAGE (X)
        0x09.toByte(), 0x31.toByte(), //     USAGE (Y)
        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(), //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
        0x95.toByte(), 0x02.toByte(), //     REPORT_COUNT (2)
        0x81.toByte(), 0x06.toByte(), //     INPUT (Data,Var,Rel)
        0x09.toByte(), 0x38.toByte(), //     USAGE (Wheel)
        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7F.toByte(), //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x81.toByte(), 0x06.toByte(), //     INPUT (Data,Var,Rel)
        0xC0.toByte(),               //   END_COLLECTION
        0xC0.toByte()                // END_COLLECTION
    )

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(device: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(device, registered)
            Log.d(TAG, "onAppStatusChanged: registered=$registered")
            isRegistered = registered
            if (registered) {
                connectToPairedDevices()
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            val stateStr = when(state) {
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
                else -> "STATE_$state"
            }
            Log.d(TAG, "onConnectionStateChanged: $stateStr for device $device")
            
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedHost = device
                _connectedDeviceName.value = try { device.name ?: "Unknown PC" } catch (_: SecurityException) { "Paired PC" }
                _isConnected.value = true
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                connectedHost = null
                _connectedDeviceName.value = null
                stopClickLoop()
                _isConnected.value = false
            }
        }

        @android.annotation.SuppressLint("MissingPermission")
        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            super.onGetReport(device, type, id, bufferSize)
            Log.d(TAG, "onGetReport: type=$type, id=$id")
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) {
                // Reply with 4 bytes to match our standard descriptor
                hidDevice?.replyReport(device, type, id, byteArrayOf(0, 0, 0, 0))
            }
        }
    }

    /**
     * Registers the Android device as a Bluetooth HID device.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun registerProfile() {
        if (isRegistered) return
        Log.d(TAG, "Registering HID Device profile")
        bluetoothAdapter?.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        Log.d(TAG, "HID Proxy Connected")
                        hidDevice = proxy as BluetoothHidDevice
                        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                            "USB Optical Mouse", "Optical Mouse", "Logitech",
                            BluetoothHidDevice.SUBCLASS1_MOUSE, hidMouseDescriptor
                        )
                        val success = hidDevice?.registerApp(sdpSettings, null, null, Executors.newSingleThreadExecutor(), hidCallback)
                        Log.d(TAG, "registerApp success: $success")
                    }
                }
                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        Log.d(TAG, "HID Proxy Disconnected")
                        hidDevice = null
                        isRegistered = false
                    }
                }
            },
            BluetoothProfile.HID_DEVICE,
        )
    }

    /**
     * Attempts to initiate connection to already paired devices.
     * Helps fix the "Disconnected" status in phone settings.
     */
    @android.annotation.SuppressLint("MissingPermission")
    private fun connectToPairedDevices() {
        val paired = bluetoothAdapter?.bondedDevices ?: return
        for (device in paired) {
            Log.d(TAG, "Attempting to connect to paired device: ${device.name}")
            hidDevice?.connect(device)
        }
    }

    /**
     * Unregisters the HID application and closes the proxy connection.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun unregisterProfile() {
        Log.d(TAG, "Unregistering Profile")
        hidDevice?.unregisterApp()
        if (hidDevice != null) {
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            hidDevice = null
        }
    }

    /**
     * Starts or stops the automated click loop.
     */
    fun toggleAutomation() {
        if (_isAutomationRunning.value) stopClickLoop() else startClickLoop()
    }

    /**
     * Updates the state of a mouse button and sends the report.
     * @param buttonMask The bitmask for the button (e.g., 0x01 for left).
     * @param isPressed True if the button is down, false if released.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun setButtonState(buttonMask: Byte, isPressed: Boolean) {
        val host = connectedHost ?: return
        val device = hidDevice ?: return
        
        currentButtonState = if (isPressed) {
            (currentButtonState.toInt() or buttonMask.toInt()).toByte()
        } else {
            (currentButtonState.toInt() and buttonMask.toInt().inv()).toByte()
        }
        
        try {
            device.sendReport(host, 0, byteArrayOf(currentButtonState, 0, 0, 0))
        } catch (_: Exception) {}
    }

    /**
     * Sends a manual mouse click report (press and quick release).
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun sendManualClick(buttonMask: Byte) {
        setButtonState(buttonMask, true)
        handler.postDelayed({ setButtonState(buttonMask, false) }, 50)
    }

    /**
     * Sends a manual mouse movement report, preserving the current button state.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun sendManualMove(dx: Int, dy: Int) {
        val host = connectedHost ?: return
        val device = hidDevice ?: return
        try {
            // Send 4-byte report (Buttons, X, Y, Wheel)
            val report = byteArrayOf(
                currentButtonState, 
                dx.coerceIn(-127, 127).toByte(), 
                dy.coerceIn(-127, 127).toByte(), 
                0x00
            )
            device.sendReport(host, 0, report)
        } catch (_: Exception) {}
    }

    /**
     * Sends a manual scroll wheel report, preserving the current button state.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun sendManualScroll(scrollDelta: Int) {
        val host = connectedHost ?: return
        val device = hidDevice ?: return
        try {
            // Send 4-byte report with current buttons and scroll delta
            device.sendReport(host, 0, byteArrayOf(currentButtonState, 0, 0, scrollDelta.toByte()))
            // Send clear report for scroll delta only, still preserving buttons
            handler.postDelayed({
                try { device.sendReport(host, 0, byteArrayOf(currentButtonState, 0, 0, 0)) } catch (_: Exception) {}
            }, 50)
        } catch (_: Exception) {}
    }

    private fun generateGaussian(mean: Float, stdDev: Float): Float {
        var u1: Float
        do { u1 = random.nextFloat() } while (u1 == 0.0f)
        val u2 = random.nextFloat()
        val z0 = sqrt((-2.0 * ln(u1.toDouble()))).toFloat() * cos(2.0 * Math.PI * u2).toFloat()
        return mean + (z0 * stdDev)
    }

    private val clickRunnable = object : Runnable {
        @android.annotation.SuppressLint("MissingPermission")
        override fun run() {
            if (!_isAutomationRunning.value || connectedHost == null || hidDevice == null) return
            try {
                // Press Left Button via shared state
                setButtonState(0x01.toByte(), true)

                val config = currentConfig
                val holdTime = if (config != null) {
                    random.nextInt(config.maxPressDuration - config.minPressDuration + 1) + config.minPressDuration
                } else {
                    var h = generateGaussian(95.0f, 17.0f).toInt()
                    if (h < 10) h = 10
                    h
                }

                handler.postDelayed({
                    if (!_isAutomationRunning.value || connectedHost == null || hidDevice == null) return@postDelayed
                    // Release Left Button via shared state
                    setButtonState(0x01.toByte(), false)

                    val gapTime: Long = if (config != null) {
                        if (random.nextInt(config.delayFrequency) == 0) {
                            (random.nextInt(config.maxBreakDelay - config.minBreakDelay + 1) + config.minBreakDelay).toLong()
                        } else {
                            (random.nextInt(config.maxInterval - config.minInterval + 1) + config.minInterval).toLong()
                        }
                    } else {
                        if (random.nextInt(500) == 0) {
                            (random.nextInt(120001 - 3000) + 3000).toLong()
                        } else {
                            var standardGap = generateGaussian(153.0f, 48.0f).toInt()
                            if (standardGap < 10) standardGap = 10
                            standardGap.toLong()
                        }
                    }
                    if (_isAutomationRunning.value) handler.postDelayed(this, gapTime)
                }, holdTime.toLong())
            } catch (_: Exception) {}
        }
    }


    private fun startClickLoop() {
        if (connectedHost == null) return
        _isAutomationRunning.value = true
        handler.post(clickRunnable)
    }

    private fun stopClickLoop() {
        _isAutomationRunning.value = false
        handler.removeCallbacks(clickRunnable)
    }
}