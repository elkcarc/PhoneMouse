package com.example.phonemouse

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps the Bluetooth HID profile registered when the app is backgrounded.
 * Prevents the system from unregistering the virtual mouse during pairing or multitasking.
 */
class BluetoothHidService : Service() {

    companion object {
        const val STOP_ACTION = "com.example.phonemouse.STOP_SERVICE"
        private const val CHANNEL_ID = "BluetoothHidServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /** The actual logic handler for HID reports. */
    lateinit var mouseHidService: MouseHidService
        private set

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BluetoothHidService", "Service Created")
        mouseHidService = MouseHidService(this)
        startForegroundService()
        
        // Auto-register profile on creation
        mouseHidService.registerProfile()
        
        // Observe connection state to update notification text
        serviceScope.launch {
            mouseHidService.connectedDeviceName.collectLatest { name ->
                updateNotification(name)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_ACTION) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.foreground_service_notification_title),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        startForeground(NOTIFICATION_ID, buildNotification(null))
    }

    private fun updateNotification(deviceName: String?) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(deviceName))
    }

    private fun buildNotification(deviceName: String?): Notification {
        val stopIntent = Intent(this, BluetoothHidService::class.java).apply {
            action = STOP_ACTION
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (deviceName != null) {
            getString(R.string.connected_to, deviceName)
        } else {
            getString(R.string.foreground_service_notification_description)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_service_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop_mouse), stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mouseHidService.unregisterProfile()
        super.onDestroy()
    }
}