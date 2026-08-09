package com.example.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test class focusing on the initial state and visual properties of the main controls.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityControlDetailTest {

    private val fakeService = MouseHidService(ApplicationProvider.getApplicationContext<Application>()).apply {
        isTestMode = true
    }

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        // Clear preferences to ensure default behavior (e.g. English, confirm delete on).
        context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE).edit().clear().commit()
        // Standardize locale to English for consistent string matching.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        
        MainViewModel.testingHidManager = FakeHidManager(fakeService)
    }

    @After
    fun teardown() {
        MainViewModel.testingHidManager = null
    }

    /**
     * Purpose: Verify that critical automation buttons are disabled when the app is not connected to a host.
     * Expected Before State: App is launched with no Bluetooth host connected.
     * Actions During Test: Observe the enabled state of Autoclicker, Record, and Playback buttons.
     * Expected After State: Buttons are in the disabled (un-clickable) state.
     */
    /**
     * Purpose: Verify that action buttons are disabled when the app is not connected to a host.
     * Before State: App launched, Bluetooth disconnected, zero permissions granted (mocked).
     * During Test: Checks the enabled state of Autoclicker, Record, and Playback buttons.
     * After State: Verification that all primary action buttons are effectively disabled.
     */
    @Test
    fun testInitialControlStates() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.autoclickerBtn)).check(matches(isNotEnabled()))
            onView(withId(R.id.recordBtn)).check(matches(isNotEnabled()))
            onView(withId(R.id.playbackBtn)).check(matches(isNotEnabled()))
        }
    }
}
