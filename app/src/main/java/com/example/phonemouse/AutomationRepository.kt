package com.example.phonemouse

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Manages persistent storage for all user profiles and input recordings. */
class AutomationRepository(context: Context) {
    private val p = context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow(value = emptyList<AutomationConfig>())
    /** List of all saved autoclicker profiles. */
    val configs = _configs.asStateFlow()

    private val _selectedIndex = MutableStateFlow(value = 0)
    /** Index of the profile currently selected for use. */
    val selectedIndex = _selectedIndex.asStateFlow()

    private val _recordings = MutableStateFlow(value = emptyList<InputRecording>())
    /** List of all saved input recordings. */
    val recordings = _recordings.asStateFlow()

    private val _selectedRecordingIndex = MutableStateFlow(value = 0)
    /** Index of the recording currently selected for playback. */
    val selectedRecordingIndex = _selectedRecordingIndex.asStateFlow()

    init {
        val s = p.getString("configs", "") ?: ""
        _configs.value = if (s.isEmpty()) {
            listOf(AutomationConfig("Profile 1", 100, 300, 50, 150, 3000, 60000, 500))
        } else s.split("|||").mapNotNull { AutomationConfig.fromJson(it) }
        _selectedIndex.value = p.getInt("selected_config_index", 0)
        
        val r = p.getString("recordings", "") ?: ""
        _recordings.value = if (r.isEmpty()) emptyList() else r.split("|||").mapNotNull { InputRecording.fromJson(it) }
        _selectedRecordingIndex.value = p.getInt("selected_recording_index", 0)
    }

    /** Serializes and persists the list of autoclicker profiles. */
    fun saveConfigs(l: List<AutomationConfig>) { 
        _configs.value = l
        p.edit { putString("configs", l.joinToString("|||") { it.toJson() }) } 
    }
    fun saveSelectedIndex(i: Int) { _selectedIndex.value = i; p.edit { putInt("selected_config_index", i) } }
    
    /** Serializes and persists the list of input recordings. */
    fun saveRecordings(l: List<InputRecording>) { 
        _recordings.value = l
        p.edit { putString("recordings", l.joinToString("|||") { it.toJson() }) } 
    }
    fun saveSelectedRecordingIndex(i: Int) { _selectedRecordingIndex.value = i; p.edit { putInt("selected_recording_index", i) } }

    /** Retrieves the currently active profile. */
    fun getActiveConfig() = _configs.value.getOrNull(_selectedIndex.value)
}