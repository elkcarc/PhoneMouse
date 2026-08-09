package com.example.phonemouse

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AutomationRepositoryTest {
    private val context = mockk<Context>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private lateinit var repo: AutomationRepository

    @Before
    fun setup() {
        every { context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        
        // Initial values for constructor
        every { prefs.getString("configs", "") } returns ""
        every { prefs.getInt("selected_config_index", 0) } returns 0
        every { prefs.getString("recordings", "") } returns ""
        every { prefs.getInt("selected_recording_index", 0) } returns 0
        
        repo = AutomationRepository(context)
    }

    /**
     * Purpose: Verify that saving the selected index updates both the StateFlow and SharedPreferences.
     * Before State: Repository initialized with mocked context and empty preferences.
     * During Test: Calls saveSelectedIndex(5).
     * After State: The selectedIndex flow emits 5, and the editor putInt() is verified.
     */
    @Test
    fun `saveSelectedIndex updates flow and prefs`() {
        repo.saveSelectedIndex(5)
        assertEquals(5, repo.selectedIndex.value)
        verify { editor.putInt("selected_config_index", 5) }
    }
}
