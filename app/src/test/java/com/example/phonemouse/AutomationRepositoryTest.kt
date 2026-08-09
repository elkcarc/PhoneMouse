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
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        
        // Initial values for constructor
        every { prefs.getString("configs", "") } returns ""
        every { prefs.getInt("selected_config_index", 0) } returns 0
        every { prefs.getString("recordings", "") } returns ""
        every { prefs.getInt("selected_recording_index", 0) } returns 0
        
        repo = AutomationRepository(context)
    }

    @Test
    fun `saveSelectedIndex updates flow and prefs`() {
        repo.saveSelectedIndex(5)
        assertEquals(5, repo.selectedIndex.value)
        verify { editor.putInt("selected_config_index", 5) }
    }

    @Test
    fun `saveTrackpadSensitivity clamps values`() {
        repo.saveTrackpadSensitivity(10.0f)
        assertEquals(8.0f, repo.trackpadSensitivity.value) // Max 8.0
        verify { editor.putFloat("trackpad_sensitivity", 8.0f) }

        repo.saveTrackpadSensitivity(0.0f)
        assertEquals(0.1f, repo.trackpadSensitivity.value) // Min 0.1
        verify { editor.putFloat("trackpad_sensitivity", 0.1f) }
    }

    @Test
    fun `saveTrackpadAcceleration updates flow and prefs`() {
        repo.saveTrackpadAcceleration(1.5f)
        assertEquals(1.5f, repo.trackpadAcceleration.value)
        verify { editor.putFloat("trackpad_acceleration", 1.5f) }
    }

    @Test
    fun `saveTrackpointCurve updates flow and prefs`() {
        repo.saveTrackpointCurve("Cubic")
        assertEquals("Cubic", repo.trackpointCurve.value)
        verify { editor.putString("trackpoint_curve", "Cubic") }
    }

    @Test
    fun `saveThemeMode updates flow and prefs`() {
        repo.saveThemeMode("Dark")
        assertEquals("Dark", repo.themeMode.value)
        verify { editor.putString("theme_mode", "Dark") }
    }

    @Test
    fun `saveLanguage updates flow and prefs`() {
        repo.saveLanguage("ja")
        assertEquals("ja", repo.appLanguage.value)
        verify { editor.putString("app_language", "ja") }
    }

    @Test
    fun `saveTrailEnabled updates flow and prefs`() {
        repo.saveTrailEnabled(false)
        assertEquals(false, repo.isTrailEnabled.value)
        verify { editor.putBoolean("is_trail_enabled", false) }
    }
}
