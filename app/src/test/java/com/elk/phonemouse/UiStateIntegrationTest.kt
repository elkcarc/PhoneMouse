package com.elk.phonemouse

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class UiStateIntegrationTest {

    @Test
    fun `MainUiState default values are correct`() {
        val state = MainUiState()
        assertEquals(false, state.isConnected)
        assertEquals(false, state.isAutoclickerRunning)
        assertEquals(false, state.isRecording)
        assertEquals(false, state.isPlaying)
    }

    /**
     * Purpose: Verify the mapping logic that converts boolean flags into user-facing status strings.
     * Before State: MainUiState instance with various flag combinations.
     * During Test: Checks resource IDs for No Permissions, No Bluetooth, and Disconnected states.
     * After State: Each combination correctly maps to the appropriate localized string resource.
     */
    @Test
    fun `MainUiState calculates status text correctly based on permissions`() {
        val noPerms = MainUiState(hasPermissions = false)
        assertEquals(R.string.permissions_required_tap_to_grant, noPerms.statusTextRes)

        val noBt = MainUiState(hasPermissions = true, isBluetoothEnabled = false)
        assertEquals(R.string.bluetooth_disabled_tap_to_enable, noBt.statusTextRes)

        val disconnected = MainUiState(hasPermissions = true, isBluetoothEnabled = true, isConnected = false)
        assertEquals(R.string.disconnected_tap_to_open_bluetooth_settings, disconnected.statusTextRes)

        val connected = MainUiState(hasPermissions = true, isBluetoothEnabled = true, isConnected = true)
        assertEquals(R.string.connected, connected.statusTextRes)
    }
}
