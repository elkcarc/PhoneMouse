package com.example.phonemouse

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySettingsDetailTest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
    )

    @Test
    fun testSettingsMenuVisibility() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.settingsBtn)).perform(click())
        Thread.sleep(500)

        // Verify all key settings exist in the UI
        onView(withId(R.id.trackpadModeDropdown)).check(matches(isDisplayed()))
        onView(withId(R.id.themeDropdown)).check(matches(isDisplayed()))
        onView(withId(R.id.languageDropdown)).check(matches(isDisplayed()))

        onView(withId(R.id.trackpadSensitivitySlider)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.trackpadAccelerationSlider)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.trailToggle)).perform(scrollTo()).check(matches(isDisplayed()))
        
        onView(withId(R.id.trackpointSensitivitySlider)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.trackpointCurveDropdown)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.trackpointAnimationToggle)).perform(scrollTo()).check(matches(isDisplayed()))
        
        onView(withId(R.id.confirmDeleteToggle)).perform(scrollTo()).check(matches(isDisplayed()))
    }
}
