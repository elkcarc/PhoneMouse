package com.example.phonemouse

import android.app.Application
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val app = mockk<Application>(relaxed = true)
    private val repo = mockk<AutomationRepository>(relaxed = true)
    private val settings = mockk<SettingsRepository>(relaxed = true)
    private val hid = mockk<HidManager>(relaxed = true)
    private val serviceFlow = MutableStateFlow<MouseHidService?>(null)
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        
        // Mock repo flows
        every { repo.configs } returns MutableStateFlow(emptyList())
        every { repo.selectedIndex } returns MutableStateFlow(0)
        every { repo.recordings } returns MutableStateFlow(emptyList())
        every { repo.selectedRecordingIndex } returns MutableStateFlow(0)

        // Mock settings flows
        every { settings.confirmDelete } returns MutableStateFlow(value = true)
        every { settings.appLanguage } returns MutableStateFlow("en")
        every { settings.themeMode } returns MutableStateFlow("Auto")
        every { settings.trackpadMode } returns MutableStateFlow("Trackpad")
        every { settings.isTrailEnabled } returns MutableStateFlow(value = true)
        every { settings.trackpadSensitivity } returns MutableStateFlow(3.0f)
        every { settings.trackpadAcceleration } returns MutableStateFlow(1.0f)
        every { settings.trackpointSensitivity } returns MutableStateFlow(1.5f)
        every { settings.trackpointCurve } returns MutableStateFlow("Linear")
        every { settings.isTrackpointAnimationEnabled } returns MutableStateFlow(value = true)
        
        every { hid.mouseHidService } returns serviceFlow
        
        viewModel = MainViewModel(app, repo, settings, hid)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    /**
     * Purpose: Verify that toggling settings visibility updates the corresponding UI state stream.
     * Before State: uiState flow collected via Turbine.
     * During Test: Calls setSettingsVisible(true).
     * After State: The flow emits an updated state with isSettingsVisible = true.
     */
    @Test
    fun `toggle settings visibility updates UI state`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(false, initialState.isSettingsVisible)
            
            viewModel.setSettingsVisible(true)
            val updatedState = awaitItem()
            assertEquals(true, updatedState.isSettingsVisible)
        }
    }

    /**
     * Purpose: Verify that changing the active panel (e.g. to Profiles) is reflected in the UI state.
     * Before State: Initial UI state active.
     * During Test: Calls setActivePanel("Profiles").
     * After State: uiState emits a new state with the activePanel set to "Profiles".
     */
    @Test
    fun `setActivePanel updates UI state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial
            
            viewModel.setActivePanel("Profiles")
            val state = awaitItem()
            assertEquals("Profiles", state.activePanel)
        }
    }

    @Test
    fun `generateNextProfileName handles empty list`() {
        every { repo.configs.value } returns emptyList()
        assertEquals("Profile 1", viewModel.generateNextProfileName())
    }

    @Test
    fun `addConfig updates repository`() {
        viewModel.addConfig("New Profile", 100, 200, 50, 100, 1000, 2000, 50)
        verify { repo.saveConfigs(any()) }
    }

    @Test
    fun `deleteConfig updates repository and adjusts selection`() {
        val configs = listOf(AutomationConfig("P1", 0,0,0,0,0,0,0))
        every { repo.configs.value } returns configs
        every { repo.selectedIndex.value } returns 0
        
        viewModel.deleteConfig(0)
        
        verify { repo.saveConfigs(emptyList()) }
        verify { repo.saveSelectedIndex(0) }
    }

    @Test
    fun `updateConfig updates repository and service`() {
        val configs = listOf(AutomationConfig("Old", 0,0,0,0,0,0,0))
        every { repo.configs.value } returns configs
        val mockService = mockk<MouseHidService>(relaxed = true)
        serviceFlow.value = mockService
        
        viewModel.updateConfig(0, "New", 1, 2, 3, 4, 5, 6, 7)
        
        verify { repo.saveConfigs(any()) }
        verify { mockService.setConfig(any()) }
    }

    @Test
    fun `toggleAutoclicker calls service`() {
        val mockService = mockk<MouseHidService>(relaxed = true)
        serviceFlow.value = mockService
        
        viewModel.toggleAutoclicker()
        verify { mockService.toggleAutomation() }
    }

    @Test
    fun `toggleRecording calls service`() {
        val mockService = mockk<MouseHidService>(relaxed = true)
        serviceFlow.value = mockService
        
        viewModel.toggleRecording()
        verify { mockService.toggleRecording() }
    }

    @Test
    fun `togglePlayback calls service with current recording`() {
        val recording = InputRecording("R1", 0, 0, 0, "data", true)
        every { repo.recordings.value } returns listOf(recording)
        every { repo.selectedRecordingIndex.value } returns 0
        val mockService = mockk<MouseHidService>(relaxed = true)
        serviceFlow.value = mockService
        
        viewModel.togglePlayback()
        verify { mockService.togglePlayback("data", true) }
    }

    @Test
    fun `moveConfig reorders list and preserves selection`() {
        val configs = listOf(
            AutomationConfig("P1", 0,0,0,0,0,0,0),
            AutomationConfig("P2", 0,0,0,0,0,0,0),
            AutomationConfig("P3", 0,0,0,0,0,0,0),
        )
        every { repo.configs.value } returns configs
        every { repo.selectedIndex.value } returns 1 // P2 is selected
        
        // Move P1 (index 0) to index 1.
        viewModel.moveConfig(0, 1)
        
        // New list order: P2, P1, P3
        // Selected index should move from 1 to 0 to keep P2 selected.
        verify { repo.saveConfigs(any()) }
        verify { repo.saveSelectedIndex(0) }
    }

    @Test
    fun `moveRecording reorders list and preserves selection`() {
        val recordings = listOf(
            InputRecording("R1", 0,0,0,"",false),
            InputRecording("R2", 0,0,0,"",false)
        )
        every { repo.recordings.value } returns recordings
        every { repo.selectedRecordingIndex.value } returns 1 // R2 selected
        
        viewModel.moveRecording(1, 0)
        
        // New list: R2, R1. Selected index moves to 0.
        verify { repo.saveRecordings(any()) }
        verify { repo.saveSelectedRecordingIndex(0) }
    }

    @Test
    fun `deleteRecording updates repository`() {
        val recordings = listOf(InputRecording("R1", 0,0,0,"",false))
        every { repo.recordings.value } returns recordings
        every { repo.selectedRecordingIndex.value } returns 0
        
        viewModel.deleteRecording(0)
        
        verify { repo.saveRecordings(emptyList()) }
        verify { repo.saveSelectedRecordingIndex(0) }
    }

    @Test
    fun `generateNextProfileName with gaps`() {
        every { repo.configs.value } returns listOf(
            AutomationConfig("Profile 1", 0,0,0,0,0,0,0),
            AutomationConfig("Profile 3", 0,0,0,0,0,0,0)
        )
        // Should fill the gap and pick "Profile 2"
        assertEquals("Profile 2", viewModel.generateNextProfileName())
    }

    @Test
    fun `renameRecording updates repository`() {
        val recordings = listOf(InputRecording("Old", 0,0,0,"",false))
        every { repo.recordings.value } returns recordings
        
        viewModel.renameRecording(0, "New Name")
        
        verify { repo.saveRecordings(match { it[0].name == "New Name" }) }
    }

    @Test
    fun `updateRecordingLoop updates repository`() {
        val recordings = listOf(InputRecording("R1", 0,0,0,"",false))
        every { repo.recordings.value } returns recordings
        
        viewModel.updateRecordingLoop(0, true)
        
        verify { repo.saveRecordings(match { it[0].loopPlayback }) }
    }

    @Test
    fun `settings updates call repository`() {
        viewModel.setThemeMode("Dark")
        verify { settings.saveThemeMode("Dark") }
        
        viewModel.setLanguage("es")
        verify { settings.saveLanguage("es") }
        
        viewModel.setTrackpadSensitivity(5.0f)
        verify { settings.saveTrackpadSensitivity(5.0f) }
        
        viewModel.setConfirmDelete(false)
        verify { settings.saveConfirmDelete(false) }
    }

    @Test
    fun `updatePermissionState updates UI state`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(true, initialState.hasPermissions)
            
            viewModel.updatePermissionState(false)
            val updatedState = awaitItem()
            assertEquals(false, updatedState.hasPermissions)
            assertEquals(R.string.permissions_required_tap_to_grant, updatedState.statusTextRes)
        }
    }

    @Test
    fun `updateBluetoothState updates UI state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial
            
            viewModel.updateBluetoothState(false)
            val updatedState = awaitItem()
            assertEquals(false, updatedState.isBluetoothEnabled)
            assertEquals(R.string.bluetooth_disabled_tap_to_enable, updatedState.statusTextRes)
        }
    }
}
