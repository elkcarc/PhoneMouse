package com.example.phonemouse

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** UI State owner. Orchestrates repository data and service events into a unified state stream. */
class MainViewModel @JvmOverloads constructor(
    app: Application,
    private val repo: AutomationRepository = AutomationRepository(app),
    private val settings: SettingsRepository = SettingsRepository(app),
    private val hid: HidManager = BluetoothHidManager(app),
) : AndroidViewModel(app) {
    private val _isSettingsVisible = MutableStateFlow(value = false)
    private val _activePanel = MutableStateFlow(value = "Main")
    private val _hasPermissions = MutableStateFlow(value = true)
    private val _isBluetoothEnabled = MutableStateFlow(value = true)
    val mouseHidService = hid.mouseHidService

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        mouseHidService.flatMapLatest { it?.isConnected ?: flowOf(value = false) },
        mouseHidService.flatMapLatest { it?.connectedDeviceName ?: flowOf(value = null) },
        mouseHidService.flatMapLatest { it?.isAutomationRunning ?: flowOf(value = false) },
        mouseHidService.flatMapLatest { it?.isRecording ?: flowOf(value = false) },
        mouseHidService.flatMapLatest { it?.isPlaying ?: flowOf(value = false) },
        repo.recordings,
        repo.selectedRecordingIndex,
        repo.configs,
        repo.selectedIndex,
        settings.confirmDelete,
        settings.appLanguage,
        settings.themeMode,
        _isSettingsVisible,
        _activePanel,
        settings.trackpadMode,
        settings.isTrailEnabled,
        settings.trackpadSensitivity,
        settings.trackpadAcceleration,
        settings.trackpointSensitivity,
        settings.trackpointCurve,
        settings.isTrackpointAnimationEnabled,
        _hasPermissions,
        _isBluetoothEnabled,
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
            trackpadAcceleration = p[17] as Float,
            trackpointSensitivity = p[18] as Float,
            trackpointCurve = p[19] as String,
            isTrackpointAnimationEnabled = p[20] as Boolean,
            hasPermissions = p[21] as Boolean,
            isBluetoothEnabled = p[22] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    init {
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

    /** Starts the Bluetooth HID service. Should be called after permissions are granted. */
    fun startService() {
        hid.startAndBind()
    }

    /** Updates the internal permission state. */
    fun updatePermissionState(has: Boolean) { _hasPermissions.value = has }
    /** Updates the internal bluetooth hardware state. */
    fun updateBluetoothState(enabled: Boolean) { _isBluetoothEnabled.value = enabled }

    /** Navigation and panel management. */
    fun setSettingsVisible(v: Boolean) { _isSettingsVisible.value = v }
    fun setActivePanel(panel: String) { _activePanel.value = panel }

    /** Internal for testing: Adds a dummy recording to the list. */
    fun addDummyRecording(name: String) {
        val newList = repo.recordings.value.toMutableList()
        newList.add(InputRecording(name, System.currentTimeMillis(), 5000, 10, "data"))
        repo.saveRecordings(newList)
    }

    /** Autoclicker management. */
    fun toggleAutoclicker() = mouseHidService.value?.toggleAutomation()
    fun selectConfig(index: Int) { repo.saveSelectedIndex(index); mouseHidService.value?.setConfig(repo.getActiveConfig()) }

    /** Generates a unique default name for a new profile. */
    fun generateNextProfileName(): String {
        val existingNames = repo.configs.value.map { it.name }
        var i = 1
        while (existingNames.contains("Profile $i")) { i++ }
        return "Profile $i"
    }

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
            val next = when (val cur = repo.selectedIndex.value) {
                0 -> 0
                else -> if (cur >= newList.size) newList.size - 1 else cur
            }
            repo.saveSelectedIndex(next)
            mouseHidService.value?.setConfig(repo.getActiveConfig())
        }
    }
    fun moveConfig(from: Int, to: Int) {
        val newList = repo.configs.value.toMutableList()
        if ((from in newList.indices) && (to in newList.indices)) {
            newList.add(to, newList.removeAt(from))
            val next = when (val cur = repo.selectedIndex.value) {
                from -> to
                in (from + 1)..to -> cur - 1
                in to until from -> cur + 1
                else -> cur
            }
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
            val next = when (val cur = repo.selectedRecordingIndex.value) {
                0 -> 0
                else -> if (cur >= newList.size) newList.size - 1 else cur
            }
            repo.saveSelectedRecordingIndex(next)
        }
    }
    fun moveRecording(from: Int, to: Int) {
        val newList = repo.recordings.value.toMutableList()
        if ((from in newList.indices) && (to in newList.indices)) {
            newList.add(to, newList.removeAt(from))
            val cur = repo.selectedRecordingIndex.value
            val next = when (cur) { from -> to; in (from + 1)..to -> cur - 1; in to until from -> cur + 1; else -> cur }
            repo.saveSelectedRecordingIndex(next)
            repo.saveRecordings(newList)
        }
    }

    /** Persistence setters. */
    fun setConfirmDelete(enabled: Boolean) = settings.saveConfirmDelete(enabled)
    fun setLanguage(l: String) = settings.saveLanguage(l)
    fun setThemeMode(m: String) = settings.saveThemeMode(m)
    fun setTrackpadMode(m: String) = settings.saveTrackpadMode(m)
    fun setTrailEnabled(e: Boolean) = settings.saveTrailEnabled(e)
    fun setTrackpadSensitivity(v: Float) = settings.saveTrackpadSensitivity(v)
    fun setTrackpadAcceleration(v: Float) = settings.saveTrackpadAcceleration(v)
    fun setTrackpointSensitivity(v: Float) = settings.saveTrackpointSensitivity(v)
    fun setTrackpointCurve(c: String) = settings.saveTrackpointCurve(c)
    fun setTrackpointAnimationEnabled(e: Boolean) = settings.saveTrackpointAnimationEnabled(e)

    override fun onCleared() { super.onCleared(); hid.unbind() }
}