package com.elk.phonemouse

import android.content.*
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Production implementation of HidManager using a real Android Bluetooth Service. */
class BluetoothHidManager(private val context: Context) : HidManager {
    private val _mouseHidService = MutableStateFlow<MouseHidService?>(value = null)
    override val mouseHidService = _mouseHidService.asStateFlow()
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothHidService.LocalBinder
            _mouseHidService.value = binder.getService().mouseHidService
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            _mouseHidService.value = null
            isBound = false
        }
    }

    override fun startAndBind() {
        val intent = Intent(context, BluetoothHidService::class.java)
        context.startForegroundService(intent)
        if (!isBound) {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            isBound = true // bindService returns true if it initiated, but isBound should reflect if we NEED to unbind.
        }
    }

    override fun unbind() {
        if (isBound) {
            try {
                context.unbindService(connection)
            } catch (_: Exception) {}
            isBound = false
        }
        _mouseHidService.value = null
    }
}