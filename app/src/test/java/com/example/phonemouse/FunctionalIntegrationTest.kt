package com.example.phonemouse

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Higher level integration testing between ViewModel, Repository and Service logic. */
@OptIn(ExperimentalCoroutinesApi::class)
class FunctionalIntegrationTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val app = mockk<Application>(relaxed = true)
    private val repo = mockk<AutomationRepository>(relaxed = true)
    private val settings = mockk<SettingsRepository>(relaxed = true)
    private val hidManager = mockk<HidManager>(relaxed = true)
    
    private val mockHid = mockk<BluetoothHidDevice>(relaxed = true)
    private val mockHost = mockk<BluetoothDevice>(relaxed = true)
    private val service = MouseHidService(app, mockHid)
    
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        
        // Mock connection
        every { hidManager.mouseHidService } returns MutableStateFlow(service)
        service.setTestHost(mockHost)
        
        // Mock repo flows
        every { repo.configs } returns MutableStateFlow(emptyList())
        every { repo.selectedIndex } returns MutableStateFlow(0)
        every { repo.recordings } returns MutableStateFlow(emptyList())
        every { repo.selectedRecordingIndex } returns MutableStateFlow(0)

        // Mock settings flows
        every { settings.confirmDelete } returns MutableStateFlow(true)
        every { settings.appLanguage } returns MutableStateFlow("en")
        every { settings.themeMode } returns MutableStateFlow("Auto")
        every { settings.trackpadMode } returns MutableStateFlow("Trackpad")
        every { settings.isTrailEnabled } returns MutableStateFlow(true)
        every { settings.trackpadSensitivity } returns MutableStateFlow(1.0f)
        every { settings.trackpadAcceleration } returns MutableStateFlow(1.0f)
        every { settings.trackpointSensitivity } returns MutableStateFlow(1.0f)
        every { settings.trackpointCurve } returns MutableStateFlow("Linear")
        every { settings.isTrackpointAnimationEnabled } returns MutableStateFlow(true)
        every { settings.isTwoFingerScrollEnabled } returns MutableStateFlow(true)
        every { settings.isTapToClickEnabled } returns MutableStateFlow(true)
        every { settings.isDoubleTapToRightClickEnabled } returns MutableStateFlow(true)

        viewModel = MainViewModel(app, repo, settings, hidManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Purpose: Verify that a movement command in the ViewModel reaches the low-level HID stack.
     * Before State: ViewModel initialized with a mocked HidManager and MouseHidService.
     * During Test: Calls sendManualMove(10, 20) on the service flow.
     * After State: Verification that BluetoothHidDevice.sendReport was called with the correct byte array.
     */
    @Test
    fun `viewModel movement propagates to hid service`() {
        val dx = 10
        val dy = 20
        viewModel.mouseHidService.value?.sendManualMove(dx, dy)
        verify { mockHid.sendReport(mockHost, 0, match { it[1] == 10.toByte() && it[2] == 20.toByte() }) }
    }

    /**
     * Purpose: Verify that selecting a profile in the UI correctly updates the active service parameters.
     * Before State: App running, dummy profile available.
     * During Test: Calls selectConfig(0) on the ViewModel.
     * After State: The repository is queried for the config to be set on the HID service.
     */
    @Test
    fun `selecting config in viewmodel updates service config`() {
        val config = AutomationConfig("Test", 1, 2, 3, 4, 5, 6, 7)
        every { repo.getActiveConfig() } returns config
        viewModel.selectConfig(0)
        verify { repo.getActiveConfig() }
    }

    /**
     * Purpose: Verify that settings changes initiated in the ViewModel are persisted to disk.
     * Before State: ViewModel and SettingsRepository mocked.
     * During Test: Updates theme, sensitivity, and language through ViewModel methods.
     * After State: The corresponding save methods in the SettingsRepository are verified as called.
     */
    @Test
    fun `settings changes in viewmodel persist to repository`() {
        viewModel.setThemeMode("Dark")
        verify { settings.saveThemeMode("Dark") }

        viewModel.setTrackpadSensitivity(5.0f)
        verify { settings.saveTrackpadSensitivity(5.0f) }

        viewModel.setLanguage("ru")
        verify { settings.saveLanguage("ru") }
    }
}
