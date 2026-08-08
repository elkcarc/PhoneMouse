package com.example.phonemouse

import android.content.*
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.*

/** Manages the lifecycle and binding of the [BluetoothHidService]. */
class HidServiceManager(private val context: Context) {
    private val _mouseHidService = MutableStateFlow<MouseHidService?>(null)
    /** Emits the logic handler instance once the service is bound. */
    val mouseHidService = _mouseHidService.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            _mouseHidService.value = (service as BluetoothHidService.LocalBinder).getService().mouseHidService
        }
        override fun onServiceDisconnected(name: ComponentName?) { _mouseHidService.value = null }
    }

    /** Starts the foreground service and attaches this manager to it. */
    fun startAndBind() {
        val intent = Intent(context, BluetoothHidService::class.java)
        ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /** Detaches from the service and clears the instance reference. */
    fun unbind() {
        try { context.unbindService(connection) } catch (_: Exception) {}
        _mouseHidService.value = null
    }
}