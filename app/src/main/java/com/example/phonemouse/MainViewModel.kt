package com.example.phonemouse

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** UI State owner. Orchestrates repository data and service events into a unified state stream. */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AutomationRepository(app)
    private val hid = HidServiceManager(app)
    private val _isSettingsVisible = MutableStateFlow(value = false)
    /** Reactive link to the low-level HID reporting service. */
    val mouseHidService = hid.mouseHidService

    /** Consolidated UI state flow derived from multiple internal and external data sources. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        mouseHidService.flatMapLatest { it?.isConnected ?: flowOf(false) },
        mouseHidService.flatMapLatest { it?.connectedDeviceName ?: flowOf(null) },
        mouseHidService.flatMapLatest { it?.isAutomationRunning ?: flowOf(false) },
        repo.configs, repo.selectedIndex, repo.appLanguage, repo.themeMode, _isSettingsVisible,
        repo.trackpadMode, repo.isTrailEnabled, repo.trackpadSensitivity, repo.trackpointSensitivity, repo.isTrackpointAnimationEnabled
    ) { p ->
        @Suppress("UNCHECKED_CAST")
        MainUiState(
            isConnected = p[0] as Boolean,
            connectedDeviceName = p[1] as? String,
            isAutomationRunning = p[2] as Boolean,
            configs = p[3] as List<String>,
            selectedConfigIndex = p[4] as Int,
            appLanguage = p[5] as String,
            themeMode = p[6] as String,
            isSettingsVisible = p[7] as Boolean,
            trackpadMode = p[8] as String,
            isTrailEnabled = p[9] as Boolean,
            trackpadSensitivity = p[10] as Float,
            trackpointSensitivity = p[11] as Float,
            isTrackpointAnimationEnabled = p[12] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    init {
        hid.startAndBind()
        viewModelScope.launch { mouseHidService.collectLatest { it?.setConfig(repo.getActiveConfig()) } }
    }

    /** Persistence setters for user preferences and trackpad parameters. */
    fun setLanguage(l: String) = repo.saveLanguage(l)
    fun setThemeMode(m: String) = repo.saveThemeMode(m)
    fun setTrackpadMode(m: String) = repo.saveTrackpadMode(m)
    fun setTrailEnabled(e: Boolean) = repo.saveTrailEnabled(e)
    fun setTrackpadSensitivity(v: Float) = repo.saveTrackpadSensitivity(v)
    fun setTrackpointSensitivity(v: Float) = repo.saveTrackpointSensitivity(v)
    fun setTrackpointAnimationEnabled(e: Boolean) = repo.saveTrackpointAnimationEnabled(e)
    /** Toggles the navigation drawer panel visibility state. */
    fun setSettingsVisible(v: Boolean) { _isSettingsVisible.value = v }

    /** Logic for managing the automation variation list and selection state. */
    fun selectConfig(i: Int) { repo.saveSelectedIndex(i); mouseHidService.value?.setConfig(repo.getActiveConfig()) }
    fun addConfig(c: AutomationConfig) { repo.saveConfigs(repo.configs.value.toMutableList().apply { add(c.toString()) }) }
    fun deleteConfig(i: Int) {
        val l = repo.configs.value.toMutableList().apply { removeAt(i) }
        val s = repo.selectedIndex.value.let { if (l.isEmpty()) 0 else if (it >= l.size) l.size - 1 else if (i < it) it - 1 else it }
        repo.saveSelectedIndex(s); repo.saveConfigs(l); mouseHidService.value?.setConfig(repo.getActiveConfig())
    }
    fun moveConfig(f: Int, t: Int) {
        val l = repo.configs.value.toMutableList().apply { add(t, removeAt(f)) }
        val s = repo.selectedIndex.value.let { when (it) { f -> t; in (f+1)..t -> it - 1; in t until f -> it + 1; else -> it } }
        repo.saveSelectedIndex(s); repo.saveConfigs(l); mouseHidService.value?.setConfig(repo.getActiveConfig())
    }
    /** Triggers the execution state of the automation clicker. */
    fun toggleAutomation() = mouseHidService.value?.toggleAutomation()
    /** Cleans up the background service binding. */
    override fun onCleared() { super.onCleared(); hid.unbind() }
}