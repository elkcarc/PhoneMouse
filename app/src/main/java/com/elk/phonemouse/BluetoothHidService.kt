package com.elk.phonemouse

import android.app.*
import android.content.Intent
import android.os.*
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/** Foreground service that holds the Bluetooth HID connection alive. */
class BluetoothHidService : Service() {
    companion object {
        const val STOP_ACTION = "com.elk.phonemouse.STOP_SERVICE"
        private const val CHANNEL_ID = "BluetoothHidServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    /** The core logic handler for HID reports. */
    lateinit var mouseHidService: MouseHidService
        private set

    inner class LocalBinder : Binder() { fun getService() = this@BluetoothHidService }

    /** Initializes the HID service, notification channel, and starts the foreground task. */
    override fun onCreate() {
        super.onCreate()
        mouseHidService = MouseHidService(this)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.foreground_service_notification_title), NotificationManager.IMPORTANCE_LOW))
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(null), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(null))
        }
        
        mouseHidService.registerProfile()
        serviceScope.launch { mouseHidService.connectedDeviceName.collectLatest { updateNotification(it) } }
    }

    /** Handles the STOP action from the notification. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_ACTION) stopSelf()
        return START_STICKY
    }

    /** Returns the local binder for ViewModel interaction. */
    override fun onBind(intent: Intent?) = binder

    /** Updates the existing notification with the connected PC's name. */
    private fun updateNotification(name: String?) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(name))
    }

    /** Constructs the persistent notification with a stop button. */
    private fun buildNotification(name: String?): Notification {
        val stopIntent = PendingIntent.getService(this, 0, Intent(this, BluetoothHidService::class.java).apply { action = STOP_ACTION }, PendingIntent.FLAG_IMMUTABLE)
        val text = if (name != null) getString(R.string.connected_to, name) else getString(R.string.foreground_service_notification_description)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_service_notification_title))
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop_mouse), stopIntent)
            .build()
    }

    /** Cleans up coroutines and unregisters the Bluetooth profile. */
    override fun onDestroy() {
        serviceScope.cancel()
        mouseHidService.unregisterProfile()
        super.onDestroy()
    }
}