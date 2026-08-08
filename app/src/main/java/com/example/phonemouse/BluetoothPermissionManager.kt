package com.example.phonemouse

import android.annotation.SuppressLint
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** Handles Bluetooth and Notification permission requests across different Android versions. */
class BluetoothPermissionManager(private val activity: Activity) {
    companion object { const val REQUEST_CODE_BLUETOOTH_PERMISSIONS = 101 }
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        mutableListOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    } else arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)

    /** Returns true if all required permissions have been granted by the user. */
    fun hasPermissions() = permissions.all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }
    /** Triggers the system permission request dialog. */
    fun requestPermissions() = ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_BLUETOOTH_PERMISSIONS)
    /** Returns true if the hardware Bluetooth adapter is currently active. */
    fun isBluetoothEnabled() = activity.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
    /** Triggers a system dialog asking the user to enable Bluetooth. */
    @SuppressLint("MissingPermission")
    fun requestBluetoothEnable() {
        try {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivity(enableBtIntent)
        } catch (_: SecurityException) {}
    }
}