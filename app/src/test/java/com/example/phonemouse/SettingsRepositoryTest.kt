package com.example.phonemouse

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {
    private val context = mockk<Context>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private lateinit var repo: SettingsRepository

    @Before
    fun setup() {
        every { context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        
        repo = SettingsRepository(context)
    }

    /**
     * Purpose: Verify that sensitivity values are clamped to the allowed range (0.1 - 8.0).
     * Before State: SettingsRepository initialized.
     * During Test: Attempts to save sensitivity values of 10.0 and 0.0.
     * After State: The repository clamps the values to 8.0 and 0.1 respectively.
     */
    @Test
    fun `saveTrackpadSensitivity clamps values`() {
        repo.saveTrackpadSensitivity(10.0f)
        assertEquals(8.0f, repo.trackpadSensitivity.value)
        verify { editor.putFloat("trackpad_sensitivity", 8.0f) }

        repo.saveTrackpadSensitivity(0.0f)
        assertEquals(0.1f, repo.trackpadSensitivity.value)
        verify { editor.putFloat("trackpad_sensitivity", 0.1f) }
    }

    @Test
    fun `saveThemeMode updates flow and prefs`() {
        repo.saveThemeMode("Dark")
        assertEquals("Dark", repo.themeMode.value)
        verify { editor.putString("theme_mode", "Dark") }
    }
}
