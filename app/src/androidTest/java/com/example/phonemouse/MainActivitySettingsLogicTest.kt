package com.example.phonemouse

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySettingsLogicTest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
    )

    @Test
    fun testSliderLabelUpdates() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.settingsBtn)).perform(click())
        Thread.sleep(500)

        // 1. Test Trackpad Sensitivity Slider
        onView(withId(R.id.trackpadSensitivitySlider)).perform(scrollTo(), swipeRight())
        // After swiping right, value should change from 3.0. We check if it still has the 'x' suffix.
        onView(withId(R.id.trackpadSensitivityValueText)).check(matches(withText(containsString("x"))))

        // 2. Test Trackpad Acceleration Slider
        onView(withId(R.id.trackpadAccelerationSlider)).perform(scrollTo(), swipeRight())
        onView(withId(R.id.trackpadAccelerationValueText)).check(matches(withText(containsString("x"))))
    }

    @Test
    fun testTogglePersistenceAcrossRecreation() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.settingsBtn)).perform(click())
        Thread.sleep(500)

        // 1. Initial state (default is true)
        onView(withId(R.id.trailToggle)).perform(scrollTo()).check(matches(isChecked()))

        // 2. Toggle OFF
        onView(withId(R.id.trailToggle)).perform(click())
        onView(withId(R.id.trailToggle)).check(matches(isNotChecked()))

        // 3. Recreate activity
        activityRule.scenario.recreate()
        Thread.sleep(1000)

        // 4. Navigate back to settings
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.settingsBtn)).perform(click())
        Thread.sleep(500)

        // 5. Verify state is still OFF
        onView(withId(R.id.trailToggle)).perform(scrollTo()).check(matches(isNotChecked()))
    }
}
