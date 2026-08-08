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
    private val _activePanel = MutableStateFlow(value = "Main")
    /** Reactive link to the low-level HID reporting service. */
    val mouseHidService = hid.mouseHidService

    /** Consolidated UI state flow derived from multiple internal and external data sources. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        mouseHidService.flatMapLatest { it?.isConnected ?: flowOf(false) },
        mouseHidService.flatMapLatest { it?.connectedDeviceName ?: flowOf(null) },
        mouseHidService.flatMapLatest { it?.isAutomationRunning ?: flowOf(false) },
        mouseHidService.flatMapLatest { it?.isRecording ?: flowOf(false) },
        mouseHidService.flatMapLatest { it?.isPlaying ?: flowOf(false) },
        repo.recordings, repo.selectedRecordingIndex,
        repo.configs, repo.selectedIndex, repo.appLanguage, repo.themeMode, _isSettingsVisible, _activePanel,
        repo.trackpadMode, repo.isTrailEnabled, repo.trackpadSensitivity, repo.trackpointSensitivity, repo.isTrackpointAnimationEnabled
    ) { p ->
        @Suppress("UNCHECKED_CAST")
        MainUiState(
            isConnected = p[0] as Boolean,
            connectedDeviceName = p[1] as? String,
            isAutoclickerRunning = p[2] as Boolean,
            isRecording = p[3] as Boolean,
            isPlaying = p[4] as Boolean,
            hasRecording = (p[5] as List<InputRecording>).isNotEmpty(),
            recordings = p[5] as List<InputRecording>,
            selectedRecordingIndex = p[6] as Int,
            configs = p[7] as List<String>,
            selectedConfigIndex = p[8] as Int,
            appLanguage = p[9] as String,
            themeMode = p[10] as String,
            isSettingsVisible = p[11] as Boolean,
            activePanel = p[12] as String,
            trackpadMode = p[13] as String,
            isTrailEnabled = p[14] as Boolean,
            trackpadSensitivity = p[15] as Float,
            trackpointSensitivity = p[16] as Float,
            isTrackpointAnimationEnabled = p[17] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    init {
        hid.startAndBind()
        viewModelScope.launch { 
            mouseHidService.collectLatest { service ->
                service?.setConfig(repo.getActiveConfig())
                service?.setOnRecordingFinishedListener { data ->
                    val newList = repo.recordings.value.toMutableList()
                    val recording = InputRecording(
                        name = app.getString(R.string.recording_default_name, newList.size + 1),
                        timestamp = System.currentTimeMillis(),
                        durationMs = service.lastRecordingDuration,
                        clickCount = service.lastRecordingClicks,
                        data = data
                    )
                    newList.add(recording)
                    repo.saveRecordings(newList)
                    repo.saveSelectedRecordingIndex(newList.size - 1)
                }
            }
        }
    }

    /** Navigation and panel management. */
    fun setSettingsVisible(v: Boolean) { _isSettingsVisible.value = v }
    fun setActivePanel(panel: String) { _activePanel.value = panel }

    /** Autoclicker control. */
    fun toggleAutoclicker() = mouseHidService.value?.toggleAutomation()

    /** Input recording management. */
    fun toggleRecording() = mouseHidService.value?.toggleRecording()
    fun selectRecording(index: Int) = repo.saveSelectedRecordingIndex(index)
    fun togglePlayback() = mouseHidService.value?.togglePlayback(repo.recordings.value.getOrNull(repo.selectedRecordingIndex.value)?.data)

    fun renameRecording(index: Int, newName: String) {
        val newList = repo.recordings.value.toMutableList()
        if (index in newList.indices) {
            newList[index] = newList[index].copy(name = newName)
            repo.saveRecordings(newList)
        }
    }

    fun deleteRecording(index: Int) {
        val newList = repo.recordings.value.toMutableList()
        if (index in newList.indices) {
            newList.removeAt(index)
            repo.saveRecordings(newList)
            val currentSelected = repo.selectedRecordingIndex.value
            repo.saveSelectedRecordingIndex(if (newList.isEmpty()) 0 else if (currentSelected >= newList.size) newList.size - 1 else currentSelected)
        }
    }

    fun moveRecording(from: Int, to: Int) {
        val newList = repo.recordings.value.toMutableList()
        if (from in newList.indices && to in newList.indices) {
            val item = newList.removeAt(from)
            newList.add(to, item)
            val currentSelected = repo.selectedRecordingIndex.value
            val newSelected = when (currentSelected) {
                from -> to
                in (from + 1)..to -> currentSelected - 1
                in to until from -> currentSelected + 1
                else -> currentSelected
            }
            repo.saveSelectedRecordingIndex(newSelected)
            repo.saveRecordings(newList)
        }
    }

    /** Persistence setters for user preferences and trackpad parameters. */
    fun setLanguage(l: String) = repo.saveLanguage(l)
    fun setThemeMode(m: String) = repo.saveThemeMode(m)
    fun setTrackpadMode(m: String) = repo.saveTrackpadMode(m)
    fun setTrailEnabled(e: Boolean) = repo.saveTrailEnabled(e)
    fun setTrackpadSensitivity(v: Float) = repo.saveTrackpadSensitivity(v)
    fun setTrackpointSensitivity(v: Float) = repo.saveTrackpointSensitivity(v)
    fun setTrackpointAnimationEnabled(e: Boolean) = repo.saveTrackpointAnimationEnabled(e)

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

    /** Cleans up the background service binding. */
    override fun onCleared() { super.onCleared(); hid.unbind() }
}