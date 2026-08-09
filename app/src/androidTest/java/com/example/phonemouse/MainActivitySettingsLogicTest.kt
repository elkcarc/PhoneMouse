package com.example.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySettingsLogicTest {

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
        context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE).edit().clear().commit()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        
        MainViewModel.testingHidManager = FakeHidManager(fakeService)
    }

    @After
    fun teardown() {
        MainViewModel.testingHidManager = null
    }

    /**
     * Purpose: Verify that moving the sensitivity/acceleration sliders updates their text labels in real-time.
     * Before State: Settings panel open, sliders at default values.
     * During Test: Performs horizontal swipes on the Material sliders.
     * After State: Verification that the associated TextViews now contain updated multiplier text (e.g., "x").
     */
    @Test
    fun testSliderLabelUpdates() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.settingsBtn)).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.trackpadSensitivitySlider)).perform(scrollTo(), swipeRight())
            onView(withId(R.id.trackpadSensitivityValueText)).check(matches(withText(containsString("x"))))

            onView(withId(R.id.trackpadAccelerationSlider)).perform(scrollTo(), swipeRight())
            onView(withId(R.id.trackpadAccelerationValueText)).check(matches(withText(containsString("x"))))
        }
    }

    /**
     * Purpose: Verify that UI toggle states (like Trackpad Trail) survive an activity recreation.
     * Before State: Trail toggle is in its default (checked) state.
     * During Test: Toggles the setting to OFF, then manually triggers activity recreation.
     * After State: After restart, the toggle remains in the OFF (unchecked) state.
     */
    @Test
    fun testTogglePersistenceAcrossRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.settingsBtn)).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.trailToggle)).perform(scrollTo()).check(matches(isChecked()))

            onView(withId(R.id.trailToggle)).perform(click())
            onView(withId(R.id.trailToggle)).check(matches(isNotChecked()))

            scenario.recreate()
            Thread.sleep(1000)

            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.settingsBtn)).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.trailToggle)).perform(scrollTo()).check(matches(isNotChecked()))
        }
    }
}
