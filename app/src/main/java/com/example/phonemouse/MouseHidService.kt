package com.example.phonemouse

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
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
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        context.getSystemService(BluetoothManager::class.java)?.adapter
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private var currentConfig: AutomationConfig? = null

    private val _isConnected = MutableStateFlow(false)
    /** Emits true when a Bluetooth host is actively connected. */
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isAutomationRunning = MutableStateFlow(false)
    /** Emits true when the automated click loop is active. */
    val isAutomationRunning: StateFlow<Boolean> = _isAutomationRunning.asStateFlow()

    /**
     * Updates the automation parameters used by the click loop.
     */
    fun setConfig(config: AutomationConfig?) {
        currentConfig = config
    }

    /** Standard HID Mouse Descriptor (Report ID 0) */
    private val hidMouseDescriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x02.toByte(), 0xA1.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x01.toByte(), 0xA1.toByte(), 0x00.toByte(), 0x05.toByte(), 0x09.toByte(),
        0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x03.toByte(), 0x15.toByte(), 0x00.toByte(),
        0x25.toByte(), 0x01.toByte(), 0x75.toByte(), 0x01.toByte(), 0x95.toByte(), 0x03.toByte(),
        0x81.toByte(), 0x02.toByte(), 0x75.toByte(), 0x05.toByte(), 0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x01.toByte(), 0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x30.toByte(),
        0x09.toByte(), 0x31.toByte(), 0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x38.toByte(),
        0x15.toByte(), 0x81.toByte(), 0x25.toByte(), 0x7F.toByte(), 0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x03.toByte(), 0x81.toByte(), 0x06.toByte(),
        0xC0.toByte(), 0xC0.toByte(),
    )

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(device: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(device, registered)
            Log.d(TAG, "onAppStatusChanged: registered=$registered")
            isRegistered = registered
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            super.onConnectionStateChanged(device, state)
            val stateStr = when(state) {
                BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
                BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
                else -> "UNKNOWN ($state)"
            }
            Log.d(TAG, "onConnectionStateChanged: device=$device, state=$stateStr")
            
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedHost = device
                _isConnected.value = true
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                connectedHost = null
                stopClickLoop()
                _isConnected.value = false
            }
        }

        @android.annotation.SuppressLint("MissingPermission")
        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            super.onGetReport(device, type, id, bufferSize)
            // Windows handshake response: Reply with empty report to satisfy protocol check
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT && id == 0.toByte()) {
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
        bluetoothAdapter?.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidDevice = proxy as BluetoothHidDevice
                        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                            "Logitech", "USB Optical Mouse", "Logitech",
                            BluetoothHidDevice.SUBCLASS1_MOUSE, hidMouseDescriptor
                        )
                        hidDevice?.registerApp(sdpSettings, null, null, Executors.newSingleThreadExecutor(), hidCallback)
                    }
                }
                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidDevice = null
                        isRegistered = false
                    }
                }
            },
            BluetoothProfile.HID_DEVICE,
        )
    }

    /**
     * Unregisters the HID application and closes the proxy connection.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun unregisterProfile() {
        if (isRegistered) {
            hidDevice?.unregisterApp()
            isRegistered = false
        }
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
     * Sends a manual mouse click report.
     * @param buttonMask The HID bitmask for the button (0x01: Left, 0x02: Right, 0x04: Middle).
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun sendManualClick(buttonMask: Byte) {
        val host = connectedHost ?: return
        val device = hidDevice ?: return
        try {
            device.sendReport(host, 0, byteArrayOf(buttonMask, 0, 0, 0))
            handler.postDelayed({
                try { device.sendReport(host, 0, byteArrayOf(0, 0, 0, 0)) } catch (_: Exception) {}
            }, 50)
        } catch (_: Exception) {}
    }

    /**
     * Sends a manual scroll wheel report.
     * @param scrollDelta Vertical scroll amount (positive for up, negative for down).
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun sendManualScroll(scrollDelta: Int) {
        val host = connectedHost ?: return
        val device = hidDevice ?: return
        try {
            device.sendReport(host, 0, byteArrayOf(0, 0, 0, scrollDelta.toByte()))
            handler.postDelayed({
                try { device.sendReport(host, 0, byteArrayOf(0, 0, 0, 0)) } catch (_: Exception) {}
            }, 50)
        } catch (_: Exception) {}
    }

    /**
     * Sends a manual mouse movement report.
     * @param dx Relative X movement (-127 to 127).
     * @param dy Relative Y movement (-127 to 127).
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun sendManualMove(dx: Int, dy: Int) {
        val host = connectedHost ?: return
        val device = hidDevice ?: return
        try {
            val report = ByteArray(4)
            report[0] = 0x00 
            report[1] = dx.coerceIn(-127, 127).toByte()
            report[2] = dy.coerceIn(-127, 127).toByte()
            report[3] = 0x00 
            device.sendReport(host, 0, report)
        } catch (_: Exception) {}
    }

    /**
     * Generates a random value using a Gaussian distribution to simulate human behavior.
     */
    private fun generateGaussian(mean: Float, stdDev: Float): Float {
        var u1: Float
        do { u1 = random.nextFloat() } while (u1 == 0.0f)
        val u2 = random.nextFloat()
        val z0 = sqrt((-2.0 * ln(u1.toDouble()))).toFloat() * cos(2.0 * Math.PI * u2).toFloat()
        return mean + (z0 * stdDev)
    }

    /**
     * Runnable that implements the automated click logic with randomized timing.
     */
    private val clickRunnable = object : Runnable {
        @android.annotation.SuppressLint("MissingPermission")
        override fun run() {
            if ((!_isAutomationRunning.value) || (connectedHost == null) || (hidDevice == null)) return
            try {
                // Press Button
                hidDevice?.sendReport(connectedHost!!, 0, byteArrayOf(1, 0, 0, 0))
                
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
                    // Release Button
                    try { hidDevice?.sendReport(connectedHost!!, 0, byteArrayOf(0, 0, 0, 0)) } catch (_: Exception) {}

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