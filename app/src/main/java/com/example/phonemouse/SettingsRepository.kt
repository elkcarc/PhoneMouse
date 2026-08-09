package com.example.phonemouse

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Manages persistent storage for all user preferences and UI settings. */
class SettingsRepository(context: Context) {
    private val p = context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE)

    private val _confirmDelete = MutableStateFlow(value = p.getBoolean("confirm_delete", true))
    val confirmDelete = _confirmDelete.asStateFlow()

    private val _isTrailEnabled = MutableStateFlow(value = p.getBoolean("is_trail_enabled", true))
    val isTrailEnabled = _isTrailEnabled.asStateFlow()

    private val _trackpadSensitivity = MutableStateFlow(value = p.getFloat("trackpad_sensitivity", 3.0f))
    val trackpadSensitivity = _trackpadSensitivity.asStateFlow()

    private val _trackpointSensitivity = MutableStateFlow(value = p.getFloat("trackpoint_sensitivity", 1.5f))
    val trackpointSensitivity = _trackpointSensitivity.asStateFlow()

    private val _isTrackpointAnimationEnabled = MutableStateFlow(value = p.getBoolean("is_trackpoint_animation_enabled", true))
    val isTrackpointAnimationEnabled = _isTrackpointAnimationEnabled.asStateFlow()

    private val _trackpadMode = MutableStateFlow(value = p.getString("trackpad_mode", "Trackpad") ?: "Trackpad")
    val trackpadMode = _trackpadMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(value = p.getString("app_language", "en") ?: "en")
    val appLanguage = _appLanguage.asStateFlow()

    private val _themeMode = MutableStateFlow(value = p.getString("theme_mode", "Auto") ?: "Auto")
    val themeMode = _themeMode.asStateFlow()

    private val _trackpadAcceleration = MutableStateFlow(value = p.getFloat("trackpad_acceleration", 1.0f))
    val trackpadAcceleration = _trackpadAcceleration.asStateFlow()

    private val _trackpointCurve = MutableStateFlow(value = p.getString("trackpoint_curve", "Linear") ?: "Linear")
    val trackpointCurve = _trackpointCurve.asStateFlow()

    fun saveConfirmDelete(enabled: Boolean) { _confirmDelete.value = enabled; p.edit(commit = true) { putBoolean("confirm_delete", enabled) } }
    fun saveTrailEnabled(e: Boolean) { _isTrailEnabled.value = e; p.edit(commit = true) { putBoolean("is_trail_enabled", e) } }
    fun saveTrackpadSensitivity(v: Float) { val c = v.coerceIn(0.1f, 8.0f); _trackpadSensitivity.value = c; p.edit(commit = true) { putFloat("trackpad_sensitivity", c) } }
    fun saveTrackpointSensitivity(v: Float) { val c = v.coerceIn(0.1f, 8.0f); _trackpointSensitivity.value = c; p.edit(commit = true) { putFloat("trackpoint_sensitivity", c) } }
    fun saveTrackpointAnimationEnabled(e: Boolean) { _isTrackpointAnimationEnabled.value = e; p.edit(commit = true) { putBoolean("is_trackpoint_animation_enabled", e) } }
    fun saveTrackpadMode(m: String) { _trackpadMode.value = m; p.edit(commit = true) { putString("trackpad_mode", m) } }
    fun saveLanguage(l: String) { _appLanguage.value = l; p.edit(commit = true) { putString("app_language", l) } }
    fun saveThemeMode(m: String) { _themeMode.value = m; p.edit(commit = true) { putString("theme_mode", m) } }
    fun saveTrackpadAcceleration(v: Float) { _trackpadAcceleration.value = v; p.edit(commit = true) { putFloat("trackpad_acceleration", v) } }
    fun saveTrackpointCurve(c: String) { _trackpointCurve.value = c; p.edit(commit = true) { putString("trackpoint_curve", c) } }
}