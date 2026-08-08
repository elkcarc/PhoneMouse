package com.example.phonemouse

/** Immutable snapshot of the entire UI state for the Main screen. */
data class MainUiState(
    val isConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val isAutomationRunning: Boolean = false,
    val configs: List<String> = emptyList(),
    val selectedConfigIndex: Int = 0,
    val appLanguage: String = "en",
    val themeMode: String = "Auto",
    val isSettingsVisible: Boolean = false,
    val trackpadMode: String = "Trackpad",
    val isTrailEnabled: Boolean = true,
    val trackpadSensitivity: Float = 3.0f,
    val trackpointSensitivity: Float = 1.5f,
    val isTrackpointAnimationEnabled: Boolean = true,
) {
    /** Resource ID for the connection status text. */
    val statusTextRes = if (isConnected) R.string.connected else R.string.disconnected_tap_to_open_bluetooth_settings
    /** True if the relative trackpad mode is active. */
    val isTrackpadMode = trackpadMode == "Trackpad"
    
    /** Flags for enabling/disabling mode-specific settings controls. */
    val isTrackpadTrailControlEnabled = isTrackpadMode
    val isTrackpadSensitivityControlEnabled = isTrackpadMode
    val isTrackpointAnimationControlEnabled = !isTrackpadMode
    val isTrackpointSensitivityControlEnabled = !isTrackpadMode
    
    /** Alpha values for visual feedback on disabled setting cards. */
    val trackpadSettingsAlpha = if (isTrackpadMode) 1.0f else 0.5f
    val trackpointSettingsAlpha = if (!isTrackpadMode) 1.0f else 0.5f
}