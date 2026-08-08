package com.example.phonemouse

import android.bluetooth.*
import android.content.Context
import android.os.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.*
import android.annotation.SuppressLint

/** Core logic for Bluetooth HID communication. Acts as a virtual mouse peripheral. */
class MouseHidService(private val context: Context) {
    private var hid: BluetoothHidDevice? = null
    private var isRegistered = false
    private var host: BluetoothDevice? = null
    private var btnState: Byte = 0
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()
    private var config: AutomationConfig? = null

    private val _isConnected = MutableStateFlow(false)
    /** True when a PC is actively connected via Bluetooth HID. */
    val isConnected = _isConnected.asStateFlow()
    private val _deviceName = MutableStateFlow<String?>(null)
    /** Name of the connected PC (e.g. "DESKTOP-X"). */
    val connectedDeviceName = _deviceName.asStateFlow()
    private val _isAutoRunning = MutableStateFlow(false)
    /** True when the randomized automation clicker is active. */
    val isAutomationRunning = _isAutoRunning.asStateFlow()

    /** Updates the parameters used for randomized timing in automation mode. */
    fun setConfig(c: AutomationConfig?) { config = c }

    /** Standard HID Mouse Descriptor defining the data packet format (Buttons, X, Y, Wheel). */
    private val descriptor = byteArrayOf(
        0x05, 0x01, // USAGE_PAGE (Generic Desktop)
        0x09, 0x02, // USAGE (Mouse)
        0xA1.toByte(), 0x01, // COLLECTION (Application)
        0x09, 0x01, //   USAGE (Pointer)
        0xA1.toByte(), 0x00, //   COLLECTION (Physical)
        0x05, 0x09, //     USAGE_PAGE (Button)
        0x19, 0x01, //     USAGE_MINIMUM (Button 1)
        0x29, 0x03, //     USAGE_MAXIMUM (Button 3)
        0x15, 0x00, //     LOGICAL_MINIMUM (0)
        0x25, 0x01, //     LOGICAL_MAXIMUM (1)
        0x75, 0x01, //     REPORT_SIZE (1)
        0x95.toByte(), 0x03, //     REPORT_COUNT (3)
        0x81.toByte(), 0x02, //     INPUT (Data,Var,Abs)
        0x75, 0x05, //     REPORT_SIZE (5)
        0x95.toByte(), 0x01, //     REPORT_COUNT (1)
        0x81.toByte(), 0x03, //     INPUT (Const,Var,Abs)
        0x05, 0x01, //     USAGE_PAGE (Generic Desktop)
        0x09, 0x30, //     USAGE (X)
        0x09, 0x31, //     USAGE (Y)
        0x15, 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25, 0x7F, //     LOGICAL_MAXIMUM (127)
        0x75, 0x08, //     REPORT_SIZE (8)
        0x95.toByte(), 0x02, //     REPORT_COUNT (2)
        0x81.toByte(), 0x06, //     INPUT (Data,Var,Rel)
        0x09, 0x38, //     USAGE (Wheel)
        0x15, 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25, 0x7F, //     LOGICAL_MAXIMUM (127)
        0x75, 0x08, //     REPORT_SIZE (8)
        0x95.toByte(), 0x01, //     REPORT_COUNT (1)
        0x81.toByte(), 0x06, //     INPUT (Data,Var,Rel)
        0xC0.toByte(), //   END_COLLECTION
        0xC0.toByte(), // END_COLLECTION
    )

    /** Bluetooth system callback to track HID profile status and connection events. */
    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(d: BluetoothDevice?, r: Boolean) { isRegistered = r; if (r) connectPaired() }
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(d: BluetoothDevice, s: Int) {
            if (s == BluetoothProfile.STATE_CONNECTED) { 
                host = d
                _deviceName.value = try { d.name } catch (_: SecurityException) { null } ?: "PC"
                _isConnected.value = true 
            }
            else if (s == BluetoothProfile.STATE_DISCONNECTED) { host = null; _deviceName.value = null; stopAuto(); _isConnected.value = false }
        }
        @SuppressLint("MissingPermission")
        override fun onGetReport(d: BluetoothDevice?, t: Byte, i: Byte, b: Int) { if (t == BluetoothHidDevice.REPORT_TYPE_INPUT) hid?.replyReport(d, t, i, byteArrayOf(0,0,0,0)) }
    }

    /** Requests the Android Bluetooth HID Device profile proxy. */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun registerProfile() {
        if (isRegistered) return
        BluetoothAdapter.getDefaultAdapter()?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                hid = proxy as BluetoothHidDevice
                hid?.registerApp(BluetoothHidDeviceAppSdpSettings("Optical Mouse", "Mouse", "Logitech", BluetoothHidDevice.SUBCLASS1_MOUSE, descriptor), null, null, Executors.newSingleThreadExecutor(), callback)
            }
            override fun onServiceDisconnected(p: Int) { hid = null; isRegistered = false }
        }, BluetoothProfile.HID_DEVICE)
    }

    /** Proactively attempts to reconnect to devices already bonded with the phone. */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun connectPaired() { BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.forEach { hid?.connect(it) } }

    /** Shuts down the HID profile and releases Bluetooth system resources. */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun unregisterProfile() {
        hid?.unregisterApp()
        if (hid != null) {
            BluetoothAdapter.getDefaultAdapter()?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
            hid = null
        }
    }

    /** Toggles the automated click state. */
    fun toggleAutomation() { if (_isAutoRunning.value) stopAuto() else startAuto() }

    /** Sets the physical state (down/up) of a mouse button and sends the HID report. */
    @SuppressLint("MissingPermission")
    fun setButtonState(mask: Byte, pressed: Boolean) {
        val h = host ?: return
        btnState = if (pressed) (btnState.toInt() or mask.toInt()).toByte() else (btnState.toInt() and mask.toInt().inv()).toByte()
        try { hid?.sendReport(h, 0, byteArrayOf(btnState, 0, 0, 0)) } catch (_: Exception) {}
    }

    /** Sends a delta movement report, preserving current button hold states. */
    @SuppressLint("MissingPermission")
    fun sendManualMove(dx: Int, dy: Int) {
        val h = host ?: return
        try { hid?.sendReport(h, 0, byteArrayOf(btnState, dx.coerceIn(-127, 127).toByte(), dy.coerceIn(-127, 127).toByte(), 0)) } catch (_: Exception) {}
    }

    /** Sends a vertical scroll delta report. */
    @SuppressLint("MissingPermission")
    fun sendManualScroll(delta: Int) {
        val h = host ?: return
        try {
            hid?.sendReport(h, 0, byteArrayOf(btnState, 0, 0, delta.toByte()))
            handler.postDelayed({ try { hid?.sendReport(h, 0, byteArrayOf(btnState, 0, 0, 0)) } catch (_: Exception) {} }, 50)
        } catch (_: Exception) {}
    }

    /** Generates a Box-Muller Gaussian random integer clipped to a minimum of 10ms. */
    private fun gaussian(m: Float, s: Float): Int {
        var u1: Float; do { u1 = random.nextFloat() } while (u1 == 0f)
        return (m + sqrt(-2.0 * ln(u1.toDouble())).toFloat() * cos(2.0 * Math.PI * random.nextFloat()).toFloat() * s).toInt().coerceAtLeast(10)
    }

    /** Continuous loop for executing randomized automated clicks. */
    private val autoRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            if (!_isAutoRunning.value || host == null || hid == null) return
            setButtonState(mask = 0x01, pressed = true)
            val hold = config?.let { random.nextInt(it.maxPressDuration - it.minPressDuration + 1) + it.minPressDuration } ?: gaussian(m = 95f, s = 17f)
            handler.postDelayed({
                setButtonState(mask = 0x01, pressed = false)
                val gap = config?.let { if (random.nextInt(it.delayFrequency) == 0) (random.nextInt(it.maxBreakDelay - it.minBreakDelay + 1) + it.minBreakDelay) else (random.nextInt(it.maxInterval - it.minInterval + 1) + it.minInterval) } ?: (if (random.nextInt(500) == 0) random.nextInt(117001) + 3000 else gaussian(m = 153f, s = 48f))
                if (_isAutoRunning.value) handler.postDelayed(this, gap.toLong())
            }, hold.toLong())
        }
    }

    /** Activates the automation click sequence. */
    private fun startAuto() { if (host != null) { _isAutoRunning.value = true; handler.post(autoRunnable) } }
    /** Halts the automation click sequence. */
    private fun stopAuto() { _isAutoRunning.value = false; handler.removeCallbacks(autoRunnable) }
}