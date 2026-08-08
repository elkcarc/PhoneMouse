package com.example.phonemouse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Main screen.
 * Orchestrates communication between the UI, the HID service, and the data repository.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    /** The low-level Bluetooth HID service instance, provided via binding. */
    private val _mouseHidService = MutableStateFlow<MouseHidService?>(null)
    val mouseHidService: StateFlow<MouseHidService?> = _mouseHidService.asStateFlow()
    
    /** Repository handling data persistence for configurations. */
    private val repository = AutomationRepository(application)

    /** Observable stream of all automation configurations. */
    val configs: StateFlow<List<String>> = repository.configs
    
    /** Observable stream of the currently active configuration index. */
    val selectedConfigIndex: StateFlow<Int> = repository.selectedIndex

    /** Observable stream of whether the trackpad trail is enabled. */
    val isTrailEnabled: StateFlow<Boolean> = repository.isTrailEnabled

    /** Observable stream of the trackpad sensitivity multiplier. */
    val sensitivity: StateFlow<Float> = repository.sensitivity

    /** Observable stream of the current application language code. */
    val appLanguage: StateFlow<String> = repository.appLanguage

    /**
     * Injects the HID service instance and initializes its config.
     */
    fun setMouseHidService(service: MouseHidService) {
        _mouseHidService.value = service
        service.setConfig(repository.getActiveConfig())
    }

    /**
     * Updates the application language code.
     * @param languageCode The ISO 639-1 language code (e.g., "en", "es").
     */
    fun setLanguage(languageCode: String) {
        repository.saveLanguage(languageCode)
    }

    /**
     * Updates whether the trackpad trail animation is enabled.
     * @param enabled True to show the trail, false to hide it.
     */
    fun setTrailEnabled(enabled: Boolean) {
        repository.saveTrailEnabled(enabled)
    }

    /**
     * Updates the trackpad sensitivity multiplier.
     * @param value The multiplier (e.g., 0.5 to 2.0).
     */
    fun setSensitivity(value: Float) {
        repository.saveSensitivity(value)
    }

    /**
     * Updates the active configuration index and notifies the HID service.
     * @param index The new selection index in the variation list.
     */
    fun selectConfig(index: Int) {
        repository.saveSelectedIndex(index)
        mouseHidService.value?.setConfig(repository.getActiveConfig())
    }

    /**
     * Appends a new configuration to the list and persists it.
     * @param config The configuration to add.
     */
    fun addConfig(config: AutomationConfig) {
        val newList = configs.value.toMutableList()
        newList.add(config.toString())
        repository.saveConfigs(newList)
    }

    /**
     * Removes a configuration at the specified index and adjusts selection state.
     * @param index The index of the item to delete.
     */
    fun deleteConfig(index: Int) {
        val newList = configs.value.toMutableList()
        if (index in newList.indices) {
            newList.removeAt(index)
            
            var newSelected = selectedConfigIndex.value
            if (newSelected >= newList.size) {
                newSelected = (newList.size - 1).coerceAtLeast(0)
            } else if (index < newSelected) {
                newSelected--
            }
            
            repository.saveSelectedIndex(newSelected)
            repository.saveConfigs(newList)
            mouseHidService.value?.setConfig(repository.getActiveConfig())
        }
    }

    /**
     * Moves a configuration from one position to another, preserving selection state.
     * @param from The original position in the list.
     * @param to The new destination position.
     */
    fun moveConfig(from: Int, to: Int) {
        val newList = configs.value.toMutableList()
        if (from in newList.indices && to in newList.indices) {
            val item = newList.removeAt(from)
            newList.add(to, item)
            
            var newSelected = selectedConfigIndex.value
            when (selectedConfigIndex.value) {
                from -> newSelected = to
                in (from + 1)..to -> newSelected--
                in to until from -> newSelected++
            }
            
            repository.saveSelectedIndex(newSelected)
            repository.saveConfigs(newList)
            mouseHidService.value?.setConfig(repository.getActiveConfig())
        }
    }

    /**
     * Toggles the automated click loop in the HID service.
     */
    fun toggleAutomation() {
        mouseHidService.value?.toggleAutomation()
    }
}