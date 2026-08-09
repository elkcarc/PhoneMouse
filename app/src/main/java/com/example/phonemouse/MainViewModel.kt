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
    val mouseHidService = hid.mouseHidService

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        mouseHidService.flatMapLatest { it?.isConnected ?: flowOf(false) },
        mouseHidService.flatMapLatest { it?.connectedDeviceName ?: flowOf(null) },
        mouseHidService.flatMapLatest { it?.isAutomationRunning ?: flowOf(false) },
        mouseHidService.flatMapLatest { it?.isRecording ?: flowOf(false) },
        mouseHidService.flatMapLatest { it?.isPlaying ?: flowOf(false) },
        repo.recordings, repo.selectedRecordingIndex,
        repo.configs, repo.selectedIndex, repo.confirmDelete, repo.appLanguage, repo.themeMode, _isSettingsVisible, _activePanel,
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
            configs = p[7] as List<AutomationConfig>,
            selectedConfigIndex = p[8] as Int,
            confirmDelete = p[9] as Boolean,
            appLanguage = p[10] as String,
            themeMode = p[11] as String,
            isSettingsVisible = p[12] as Boolean,
            activePanel = p[13] as String,
            trackpadMode = p[14] as String,
            isTrailEnabled = p[15] as Boolean,
            trackpadSensitivity = p[16] as Float,
            trackpointSensitivity = p[17] as Float,
            isTrackpointAnimationEnabled = p[18] as Boolean
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

    /** Autoclicker management. */
    fun toggleAutoclicker() = mouseHidService.value?.toggleAutomation()
    fun selectConfig(index: Int) { repo.saveSelectedIndex(index); mouseHidService.value?.setConfig(repo.getActiveConfig()) }
    fun addConfig(name: String, minI: Int, maxI: Int, minP: Int, maxP: Int, minB: Int, maxB: Int, freq: Int) {
        val newList = repo.configs.value.toMutableList().apply { 
            add(AutomationConfig(name, minI, maxI, minP, maxP, minB, maxB, freq)) 
        }
        repo.saveConfigs(newList)
    }
    fun updateConfig(index: Int, name: String, minI: Int, maxI: Int, minP: Int, maxP: Int, minB: Int, maxB: Int, freq: Int) {
        val newList = repo.configs.value.toMutableList()
        if (index in newList.indices) {
            newList[index] = AutomationConfig(name, minI, maxI, minP, maxP, minB, maxB, freq)
            repo.saveConfigs(newList)
            mouseHidService.value?.setConfig(repo.getActiveConfig())
        }
    }
    fun deleteConfig(index: Int) {
        val newList = repo.configs.value.toMutableList()
        if (index in newList.indices) {
            newList.removeAt(index)
            repo.saveConfigs(newList)
            val currentSelected = repo.selectedIndex.value
            repo.saveSelectedIndex(if (newList.isEmpty()) 0 else if (currentSelected >= newList.size) newList.size - 1 else currentSelected)
            mouseHidService.value?.setConfig(repo.getActiveConfig())
        }
    }
    fun moveConfig(from: Int, to: Int) {
        val newList = repo.configs.value.toMutableList()
        if (from in newList.indices && to in newList.indices) {
            newList.add(to, newList.removeAt(from))
            val cur = repo.selectedIndex.value
            val next = when (cur) { from -> to; in (from + 1)..to -> cur - 1; in to until from -> cur + 1; else -> cur }
            repo.saveSelectedIndex(next)
            repo.saveConfigs(newList)
            mouseHidService.value?.setConfig(repo.getActiveConfig())
        }
    }

    /** Input recording management. */
    fun toggleRecording() = mouseHidService.value?.toggleRecording()
    fun selectRecording(index: Int) = repo.saveSelectedRecordingIndex(index)
    fun togglePlayback() {
        val recording = repo.recordings.value.getOrNull(repo.selectedRecordingIndex.value)
        mouseHidService.value?.togglePlayback(recording?.data, recording?.loopPlayback ?: true)
    }
    fun updateRecordingLoop(index: Int, loop: Boolean) {
        val newList = repo.recordings.value.toMutableList()
        if (index in newList.indices) {
            newList[index] = newList[index].copy(loopPlayback = loop)
            repo.saveRecordings(newList)
        }
    }
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
            newList.add(to, newList.removeAt(from))
            val cur = repo.selectedRecordingIndex.value
            val next = when (cur) { from -> to; in (from + 1)..to -> cur - 1; in to until from -> cur + 1; else -> cur }
            repo.saveSelectedRecordingIndex(next)
            repo.saveRecordings(newList)
        }
    }

    /** Persistence setters. */
    fun setConfirmDelete(enabled: Boolean) = repo.saveConfirmDelete(enabled)
    fun setLanguage(l: String) = repo.saveLanguage(l)
    fun setThemeMode(m: String) = repo.saveThemeMode(m)
    fun setTrackpadMode(m: String) = repo.saveTrackpadMode(m)
    fun setTrailEnabled(e: Boolean) = repo.saveTrailEnabled(e)
    fun setTrackpadSensitivity(v: Float) = repo.saveTrackpadSensitivity(v)
    fun setTrackpointSensitivity(v: Float) = repo.saveTrackpointSensitivity(v)
    fun setTrackpointAnimationEnabled(e: Boolean) = repo.saveTrackpointAnimationEnabled(e)

    override fun onCleared() { super.onCleared(); hid.unbind() }
}