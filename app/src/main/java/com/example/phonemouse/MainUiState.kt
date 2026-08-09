package com.example.phonemouse

/** Immutable snapshot of the entire UI state for the Main screen. */
data class MainUiState(
    val isConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val isAutoclickerRunning: Boolean = false,
    val isRecording: Boolean = false,
    val isPlaying: Boolean = false,
    val hasRecording: Boolean = false,
    val configs: List<AutomationConfig> = emptyList(),
    val selectedConfigIndex: Int = 0,
    val recordings: List<InputRecording> = emptyList(),
    val selectedRecordingIndex: Int = 0,
    val confirmDelete: Boolean = true,
    val activePanel: String = "Main", // "Main", "Profiles", "Recordings"
    val appLanguage: String = "en",
    val themeMode: String = "Auto",
    val isSettingsVisible: Boolean = false,
    val trackpadMode: String = "Trackpad",
    val isTrailEnabled: Boolean = true,
    val trackpadSensitivity: Float = 3.0f,
    val trackpadAcceleration: Float = 1.0f,
    val trackpointSensitivity: Float = 1.5f,
    val trackpointCurve: String = "Linear",
    val isTrackpointAnimationEnabled: Boolean = true,
    val hasPermissions: Boolean = true,
    val isBluetoothEnabled: Boolean = true,
    val isTestMode: Boolean = false,
    val isTwoFingerScrollEnabled: Boolean = true,
    val isTapToClickEnabled: Boolean = true,
    val isDoubleTapToRightClickEnabled: Boolean = true,
) {
    val statusTextRes = when {
        !hasPermissions -> R.string.permissions_required_tap_to_grant
        !isBluetoothEnabled -> R.string.bluetooth_disabled_tap_to_enable
        isConnected -> R.string.connected
        else -> R.string.disconnected_tap_to_open_bluetooth_settings
    }
    val isTrackpadMode = trackpadMode == "Trackpad"
    val isTrackpadTrailControlEnabled = isTrackpadMode
    val isTrackpadSensitivityControlEnabled = isTrackpadMode
    val isTrackpointAnimationControlEnabled = !isTrackpadMode
    val isTrackpointSensitivityControlEnabled = !isTrackpadMode
    val trackpadSettingsAlpha = if (isTrackpadMode) 1.0f else 0.5f
    val trackpointSettingsAlpha = if (!isTrackpadMode) 1.0f else 0.5f
}