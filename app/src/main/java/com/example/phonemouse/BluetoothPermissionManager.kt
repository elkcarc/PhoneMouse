package com.example.phonemouse

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Handles Bluetooth permissions and system state checks for different Android versions.
 * Simplifies the boilerplate required for discovery and connection.
 */
class BluetoothPermissionManager(private val activity: Activity) {

    companion object {
        const val REQUEST_CODE_BLUETOOTH_PERMISSIONS = 101
    }

    /**
     * Set of permissions required based on API level.
     * Android 12+ (S) requires SCAN/CONNECT/ADVERTISE.
     * Legacy versions require generic Bluetooth and Location permissions.
     */
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    /**
     * Checks if all required Bluetooth permissions have been granted.
     */
    fun hasPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Triggers the system permission request dialog.
     */
    fun requestPermissions() {
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_BLUETOOTH_PERMISSIONS)
    }

    /**
     * Checks if the Bluetooth adapter is currently enabled.
     */
    fun isBluetoothEnabled(): Boolean {
        val btManager = activity.getSystemService(BluetoothManager::class.java)
        return btManager?.adapter?.isEnabled == true
    }

    /**
     * Triggers a system dialog asking the user to turn on Bluetooth.
     */
    fun requestBluetoothEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivity(enableBtIntent)
    }
}