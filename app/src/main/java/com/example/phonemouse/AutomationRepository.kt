package com.example.phonemouse

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.*

/** Manages persistent storage for all user settings and automation configurations. */
class AutomationRepository(context: Context) {
    private val p = context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow(value = emptyList<String>())
    /** List of all saved automation variation strings. */
    val configs = _configs.asStateFlow()

    private val _selectedIndex = MutableStateFlow(value = 0)
    /** Index of the variation currently selected for use. */
    val selectedIndex = _selectedIndex.asStateFlow()

    private val _isTrailEnabled = MutableStateFlow(value = true)
    /** User preference for relative trackpad trail visibility. */
    val isTrailEnabled = _isTrailEnabled.asStateFlow()

    private val _trackpadSensitivity = MutableStateFlow(value = 3.0f)
    /** Multiplier for relative trackpad movement speed. */
    val trackpadSensitivity = _trackpadSensitivity.asStateFlow()

    private val _trackpointSensitivity = MutableStateFlow(value = 1.5f)
    /** Multiplier for absolute trackpoint movement speed. */
    val trackpointSensitivity = _trackpointSensitivity.asStateFlow()

    private val _isTrackpointAnimationEnabled = MutableStateFlow(value = true)
    /** User preference for absolute trackpoint icon animation. */
    val isTrackpointAnimationEnabled = _isTrackpointAnimationEnabled.asStateFlow()

    private val _trackpadMode = MutableStateFlow(value = "Trackpad")
    /** Current operating mode: "Trackpad" or "Trackpoint". */
    val trackpadMode = _trackpadMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(value = "en")
    /** ISO language code preferred by the user. */
    val appLanguage = _appLanguage.asStateFlow()

    private val _themeMode = MutableStateFlow(value = "Auto")
    /** Theme preference: "Auto", "Light", or "Dark". */
    val themeMode = _themeMode.asStateFlow()

    init {
        val s = p.getString("configs", "100,300,50,150,3000,60000,500") ?: ""
        _configs.value = if (s.isEmpty()) emptyList() else s.split("|")
        _selectedIndex.value = p.getInt("selected_config_index", 0)
        _isTrailEnabled.value = p.getBoolean("is_trail_enabled", true)
        _trackpadSensitivity.value = p.getFloat("trackpad_sensitivity", 3.0f)
        _trackpointSensitivity.value = p.getFloat("trackpoint_sensitivity", 1.5f)
        _isTrackpointAnimationEnabled.value = p.getBoolean("is_trackpoint_animation_enabled", true)
        _trackpadMode.value = p.getString("trackpad_mode", "Trackpad") ?: "Trackpad"
        _appLanguage.value = p.getString("app_language", "en") ?: "en"
        _themeMode.value = p.getString("theme_mode", "Auto") ?: "Auto"
    }

    /** Setters for persisting and emitting updated user preferences. */
    fun saveConfigs(l: List<String>) { _configs.value = l; p.edit { putString("configs", l.joinToString("|")) } }
    fun saveSelectedIndex(i: Int) { _selectedIndex.value = i; p.edit { putInt("selected_config_index", i) } }
    fun saveTrailEnabled(e: Boolean) { _isTrailEnabled.value = e; p.edit { putBoolean("is_trail_enabled", e) } }
    fun saveTrackpadSensitivity(v: Float) { val c = v.coerceIn(0.1f, 8.0f); _trackpadSensitivity.value = c; p.edit { putFloat("trackpad_sensitivity", c) } }
    fun saveTrackpointSensitivity(v: Float) { val c = v.coerceIn(0.1f, 8.0f); _trackpointSensitivity.value = c; p.edit { putFloat("trackpoint_sensitivity", c) } }
    fun saveTrackpointAnimationEnabled(e: Boolean) { _isTrackpointAnimationEnabled.value = e; p.edit { putBoolean("is_trackpoint_animation_enabled", e) } }
    fun saveTrackpadMode(m: String) { _trackpadMode.value = m; p.edit { putString("trackpad_mode", m) } }
    fun saveLanguage(l: String) { _appLanguage.value = l; p.edit { putString("app_language", l) } }
    fun saveThemeMode(m: String) { _themeMode.value = m; p.edit { putString("theme_mode", m) } }
    /** Retrieves the currently active [AutomationConfig] object. */
    fun getActiveConfig() = _configs.value.getOrNull(_selectedIndex.value)?.let { AutomationConfig.fromString(it) }
}