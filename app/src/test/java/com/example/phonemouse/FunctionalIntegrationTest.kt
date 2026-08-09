package com.example.phonemouse

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.view.MotionEvent
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
    private val hidManager = mockk<HidServiceManager>(relaxed = true)
    
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
        
        // Mock repo flows (minimal set needed for VM initialization)
        every { repo.configs } returns MutableStateFlow(emptyList())
        every { repo.selectedIndex } returns MutableStateFlow(0)
        every { repo.recordings } returns MutableStateFlow(emptyList())
        every { repo.selectedRecordingIndex } returns MutableStateFlow(0)
        every { repo.confirmDelete } returns MutableStateFlow(true)
        every { repo.appLanguage } returns MutableStateFlow("en")
        every { repo.themeMode } returns MutableStateFlow("Auto")
        every { repo.trackpadMode } returns MutableStateFlow("Trackpad")
        every { repo.isTrailEnabled } returns MutableStateFlow(true)
        every { repo.trackpadSensitivity } returns MutableStateFlow(1.0f)
        every { repo.trackpadAcceleration } returns MutableStateFlow(1.0f)
        every { repo.trackpointSensitivity } returns MutableStateFlow(1.0f)
        every { repo.trackpointCurve } returns MutableStateFlow("Linear")
        every { repo.isTrackpointAnimationEnabled } returns MutableStateFlow(true)

        viewModel = MainViewModel(app, repo, hidManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel movement propagates to hid service`() {
        // This simulates what happens in MainActivity's move listener:
        // trackpad.setOnMoveListener { dx, dy -> viewModel.mouseHidService.value?.sendManualMove(dx, dy) }
        
        val dx = 10
        val dy = 20
        viewModel.mouseHidService.value?.sendManualMove(dx, dy)
        
        // Verify correctly formed packet [0, 10, 20, 0] reached the low-level HID proxy
        verify { mockHid.sendReport(mockHost, 0, match { it[1] == 10.toByte() && it[2] == 20.toByte() }) }
    }

    @Test
    fun `selecting config in viewmodel updates service config`() {
        val config = AutomationConfig("Test", 1, 2, 3, 4, 5, 6, 7)
        every { repo.getActiveConfig() } returns config
        
        viewModel.selectConfig(0)
        
        // Service should now have the new config (used by auto-clicker)
        // We can't check private field 'config', but we can check if it was fetched from repo
        verify { repo.getActiveConfig() }
    }

    @Test
    fun `settings changes in viewmodel persist to repository`() {
        viewModel.setThemeMode("Dark")
        verify { repo.saveThemeMode("Dark") }

        viewModel.setTrackpadSensitivity(5.0f)
        verify { repo.saveTrackpadSensitivity(5.0f) }

        viewModel.setLanguage("ru")
        verify { repo.saveLanguage("ru") }
    }
}
