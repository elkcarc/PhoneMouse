package com.example.phonemouse

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository responsible for persisting and managing automation configurations.
 * Uses SharedPreferences as the backing storage.
 */
class AutomationRepository(context: Context) {
    private val prefs = context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow<List<String>>(emptyList())
    /** Flow emitting the current list of saved configurations as strings. */
    val configs: StateFlow<List<String>> = _configs.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    /** Flow emitting the index of the currently selected configuration. */
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    private val _isTrailEnabled = MutableStateFlow(true)
    /** Flow emitting whether the trackpad trail animation is enabled. */
    val isTrailEnabled: StateFlow<Boolean> = _isTrailEnabled.asStateFlow()

    private val _trackpadSensitivity = MutableStateFlow(3.0f)
    /** Flow emitting the current trackpad sensitivity multiplier. */
    val trackpadSensitivity: StateFlow<Float> = _trackpadSensitivity.asStateFlow()

    private val _trackpointSensitivity = MutableStateFlow(1.5f)
    /** Flow emitting the current trackpoint sensitivity multiplier. */
    val trackpointSensitivity: StateFlow<Float> = _trackpointSensitivity.asStateFlow()

    private val _isTrackpointAnimationEnabled = MutableStateFlow(true)
    /** Flow emitting whether the trackpoint icon animation is enabled. */
    val isTrackpointAnimationEnabled: StateFlow<Boolean> = _isTrackpointAnimationEnabled.asStateFlow()

    private val _trackpadMode = MutableStateFlow("Trackpad")
    /** Flow emitting the current trackpad mode ("Trackpad" or "Trackpoint"). */
    val trackpadMode: StateFlow<String> = _trackpadMode.asStateFlow()

    private val _appLanguage = MutableStateFlow("en")
    /** Flow emitting the current application language code. */
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    init {
        loadFromPrefs()
    }

    /**
     * Loads configurations and selection state from SharedPreferences.
     */
    private fun loadFromPrefs() {
        val saved = prefs.getString("configs", "5000,6000,1000,1500,15000,20000,10") ?: ""
        _configs.value = if (saved.isEmpty()) emptyList() else saved.split("|")
        _selectedIndex.value = prefs.getInt("selected_config_index", 0)
        _isTrailEnabled.value = prefs.getBoolean("is_trail_enabled", true)
        _trackpadSensitivity.value = prefs.getFloat("trackpad_sensitivity", 3.0f)
        _trackpointSensitivity.value = prefs.getFloat("trackpoint_sensitivity", 1.5f)
        _isTrackpointAnimationEnabled.value = prefs.getBoolean("is_trackpoint_animation_enabled", true)
        _trackpadMode.value = prefs.getString("trackpad_mode", "Trackpad") ?: "Trackpad"
        _appLanguage.value = prefs.getString("app_language", "en") ?: "en"
    }

    /**
     * Saves the current list of configurations to SharedPreferences.
     */
    fun saveConfigs(list: List<String>) {
        _configs.value = list
        prefs.edit { putString("configs", list.joinToString("|")) }
    }

    /**
     * Saves the currently selected index to SharedPreferences.
     */
    fun saveSelectedIndex(index: Int) {
        _selectedIndex.value = index
        prefs.edit { putInt("selected_config_index", index) }
    }

    /**
     * Saves whether the trail animation is enabled to SharedPreferences.
     */
    fun saveTrailEnabled(enabled: Boolean) {
        _isTrailEnabled.value = enabled
        prefs.edit { putBoolean("is_trail_enabled", enabled) }
    }

    /**
     * Saves the trackpad sensitivity multiplier to SharedPreferences.
     */
    fun saveTrackpadSensitivity(value: Float) {
        val capped = value.coerceIn(0.1f, 8.0f)
        _trackpadSensitivity.value = capped
        prefs.edit { putFloat("trackpad_sensitivity", capped) }
    }

    /**
     * Saves the trackpoint sensitivity multiplier to SharedPreferences.
     */
    fun saveTrackpointSensitivity(value: Float) {
        val capped = value.coerceIn(0.1f, 8.0f)
        _trackpointSensitivity.value = capped
        prefs.edit { putFloat("trackpoint_sensitivity", capped) }
    }

    /**
     * Saves whether the trackpoint animation is enabled to SharedPreferences.
     */
    fun saveTrackpointAnimationEnabled(enabled: Boolean) {
        _isTrackpointAnimationEnabled.value = enabled
        prefs.edit { putBoolean("is_trackpoint_animation_enabled", enabled) }
    }

    /**
     * Saves the trackpad operation mode ("Trackpad" or "Trackpoint") to SharedPreferences.
     */
    fun saveTrackpadMode(mode: String) {
        _trackpadMode.value = mode
        prefs.edit { putString("trackpad_mode", mode) }
    }

    /**
     * Saves the application language code to SharedPreferences.
     */
    fun saveLanguage(languageCode: String) {
        _appLanguage.value = languageCode
        prefs.edit { putString("app_language", languageCode) }
    }

    /**
     * Utility to get the active AutomationConfig object based on the current selection.
     */
    fun getActiveConfig(): AutomationConfig? {
        val index = _selectedIndex.value
        return _configs.value.getOrNull(index)?.let { AutomationConfig.fromString(it) }
    }
}