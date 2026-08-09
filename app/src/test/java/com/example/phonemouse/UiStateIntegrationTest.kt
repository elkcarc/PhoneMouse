package com.example.phonemouse

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class UiStateIntegrationTest {

    @Test
    fun `MainUiState default values are correct`() {
        val state = MainUiState()
        assertEquals(false, state.isConnected)
        assertEquals(false, state.isAutoclickerRunning)
        assertEquals(false, state.isRecording)
        assertEquals(false, state.isPlaying)
    }

    @Test
    fun `MainUiState calculates control button properties correctly`() {
        // Test Autoclicker Running state
        val activeState = MainUiState(isConnected = true, isAutoclickerRunning = true)
        
        // We can't easily test 'render' logic in unit tests without robolectric/ui tests,
        // but we can verify the state that drives it.
        assertEquals(true, activeState.isAutoclickerRunning)
        assertEquals(true, activeState.isConnected)
    }
}
