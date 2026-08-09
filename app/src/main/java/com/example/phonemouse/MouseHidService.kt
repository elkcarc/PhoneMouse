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
class MouseHidService(
    private val context: Context,
    private var hid: BluetoothHidDevice? = null,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    private var isRegistered = false
    private var host: BluetoothDevice? = null

    /** Internal for testing. Simulates a host connection. */
    fun setTestHost(device: BluetoothDevice?) {
        host = device
        _isConnected.value = device != null
    }

    private var btnState: Byte = 0
    private val random = Random()
    private var config: AutomationConfig? = null

    private val _isConnected = MutableStateFlow(value = false)
    val isConnected = _isConnected.asStateFlow()
    private val _deviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName = _deviceName.asStateFlow()
    private val _isAutoRunning = MutableStateFlow(value = false)
    val isAutomationRunning = _isAutoRunning.asStateFlow()

    // Recording & Playback
    private val _isRecording = MutableStateFlow(value = false)
    val isRecording = _isRecording.asStateFlow()
    private val _isPlaying = MutableStateFlow(value = false)
    val isPlaying = _isPlaying.asStateFlow()
    
    private var currentRecording = mutableListOf<Pair<Long, ByteArray>>()
    private var recordStartTime: Long = 0
    /** Total duration of the most recently finished recording in ms. */
    var lastRecordingDuration: Long = 0
        private set
    /** Total number of clicks in the most recently finished recording. */
    var lastRecordingClicks: Int = 0
        private set
    private var onRecordingFinished: ((String) -> Unit)? = null

    fun setConfig(c: AutomationConfig?) { config = c }
    fun setOnRecordingFinishedListener(l: (String) -> Unit) { onRecordingFinished = l }

    private val descriptor = byteArrayOf(
        0x05, 0x01, 0x09, 0x02, 0xA1.toByte(), 0x01, 0x09, 0x01, 0xA1.toByte(), 0x00,
        0x05, 0x09, 0x19, 0x01, 0x29, 0x03, 0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x03, 0x81.toByte(), 0x02,
        0x75, 0x05, 0x95.toByte(), 0x01, 0x81.toByte(), 0x03, 0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x15, 0x81.toByte(), 0x25, 0x7F,
        0x75, 0x08, 0x95.toByte(), 0x02, 0x81.toByte(), 0x06, 0x09, 0x38, 0x15, 0x81.toByte(), 0x25, 0x7F, 0x75, 0x08, 0x95.toByte(), 0x01, 0x81.toByte(), 0x06,
        0xC0.toByte(), 0xC0.toByte(),
    )

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(d: BluetoothDevice?, r: Boolean) { isRegistered = r; if (r) connectPaired() }
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(d: BluetoothDevice, s: Int) {
            if (s == BluetoothProfile.STATE_CONNECTED) { 
                host = d
                _deviceName.value = try { d.name } catch (_: SecurityException) { null } ?: "PC"
                _isConnected.value = true 
            }
            else if (s == BluetoothProfile.STATE_DISCONNECTED) { host = null; _deviceName.value = null; stopAuto(); stopPlayback(); _isConnected.value = false }
        }
        @SuppressLint("MissingPermission")
        override fun onGetReport(d: BluetoothDevice?, t: Byte, i: Byte, b: Int) { if (t == BluetoothHidDevice.REPORT_TYPE_INPUT) hid?.replyReport(d, t, i, byteArrayOf(0,0,0,0)) }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun registerProfile() {
        if (isRegistered) return
        BluetoothAdapter.getDefaultAdapter()?.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                    hid = proxy as BluetoothHidDevice
                    hid?.registerApp(BluetoothHidDeviceAppSdpSettings("Optical Mouse", "Mouse", "Logitech", BluetoothHidDevice.SUBCLASS1_MOUSE, descriptor), null, null, Executors.newSingleThreadExecutor(), callback)
                }
                override fun onServiceDisconnected(p: Int) { hid = null; isRegistered = false }
            },
            BluetoothProfile.HID_DEVICE,
        )
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun connectPaired() { BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.forEach { hid?.connect(it) } }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun unregisterProfile() {
        hid?.unregisterApp()
        if (hid != null) {
            BluetoothAdapter.getDefaultAdapter()?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
            hid = null
        }
    }

    fun toggleAutomation() { if (_isAutoRunning.value) stopAuto() else startAuto() }

    fun toggleRecording() {
        if (_isRecording.value) {
            _isRecording.value = false
            lastRecordingDuration = SystemClock.elapsedRealtime() - recordStartTime
            lastRecordingClicks = currentRecording.count { it.second[0].toInt() != 0 }
            val data = currentRecording.joinToString(";") { "${it.first}:${it.second.joinToString(",")}" }
            onRecordingFinished?.invoke(data)
        } else {
            stopAuto()
            stopPlayback()
            currentRecording.clear()
            lastRecordingClicks = 0
            lastRecordingDuration = 0
            recordStartTime = SystemClock.elapsedRealtime()
            _isRecording.value = true
        }
    }

    fun togglePlayback(data: String?, loop: Boolean = false) {
        if (_isPlaying.value) stopPlayback() else startPlayback(data, loop)
    }

    private fun startPlayback(data: String?, loop: Boolean) {
        if (data.isNullOrEmpty() || (host == null)) return
        val events = data.split(";").mapNotNull {
            try {
                val parts = it.split(":")
                val delay = parts[0].toLong()
                val report = parts[1].split(",").map { b -> b.toByte() }.toByteArray()
                delay to report
            } catch (_: Exception) { null }
        }
        if (events.isEmpty()) return
        _isPlaying.value = true
        stopAuto()
        
        fun runEvents() {
            events.forEach { event ->
                handler.postDelayed(
                    {
                        if (_isPlaying.value) sendReportInternal(event.second)
                    },
                    event.first
                )
            }
            val totalDuration = events.last().first
            handler.postDelayed(
                {
                    if (_isPlaying.value) {
                        if (loop) runEvents() else _isPlaying.value = false
                    }
                },
                totalDuration + 100,
            )
        }
        runEvents()
    }

    private fun stopPlayback() {
        _isPlaying.value = false
        handler.removeCallbacksAndMessages(null)
    }

    @SuppressLint("MissingPermission")
    fun setButtonState(mask: Byte, pressed: Boolean) {
        btnState = if (pressed) (btnState.toInt() or mask.toInt()).toByte() else (btnState.toInt() and mask.toInt().inv()).toByte()
        sendReportInternal(byteArrayOf(btnState, 0, 0, 0))
    }

    @SuppressLint("MissingPermission")
    fun sendManualMove(dx: Int, dy: Int) {
        sendReportInternal(byteArrayOf(btnState, dx.coerceIn(-127, 127).toByte(), dy.coerceIn(-127, 127).toByte(), 0))
    }

    @SuppressLint("MissingPermission")
    fun sendManualScroll(delta: Int) {
        sendReportInternal(byteArrayOf(btnState, 0, 0, delta.toByte()))
        handler.postDelayed({ sendReportInternal(byteArrayOf(btnState, 0, 0, 0)) }, 50)
    }

    @SuppressLint("MissingPermission")
    private fun sendReportInternal(report: ByteArray) {
        val h = host ?: return
        try {
            hid?.sendReport(h, 0, report)
            if (_isRecording.value) {
                currentRecording.add((SystemClock.elapsedRealtime() - recordStartTime) to report)
            }
        } catch (_: Exception) {}
    }

    private fun gaussian(m: Float, s: Float): Int {
        var u1: Float; do { u1 = random.nextFloat() } while (u1 == 0f)
        return (m + (sqrt(-2.0 * ln(u1.toDouble())).toFloat() * cos(2.0 * Math.PI * random.nextFloat()).toFloat() * s)).toInt().coerceAtLeast(10)
    }

    private val autoRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            if ((!_isAutoRunning.value) || (host == null) || (hid == null)) return
            setButtonState(mask = 0x01, pressed = true)
            val hold = config?.let { random.nextInt((it.maxPressDuration - it.minPressDuration) + 1) + it.minPressDuration } ?: gaussian(95f, 17f)
            handler.postDelayed(
                {
                    setButtonState(mask = 0x01, pressed = false)
                    val gap = config?.let { if (random.nextInt(it.delayFrequency) == 0) (random.nextInt((it.maxBreakDelay - it.minBreakDelay) + 1) + it.minBreakDelay) else (random.nextInt((it.maxInterval - it.minInterval) + 1) + it.minInterval) } ?: (if (random.nextInt(500) == 0) random.nextInt(117001) + 3000 else gaussian(153f, 48f))
                    if (_isAutoRunning.value) handler.postDelayed(this, gap.toLong())
                },
                hold.toLong()
            )
        }
    }

    private fun startAuto() { if (host != null) { _isAutoRunning.value = true; handler.post(autoRunnable) } }
    private fun stopAuto() { _isAutoRunning.value = false; handler.removeCallbacks(autoRunnable) }
}