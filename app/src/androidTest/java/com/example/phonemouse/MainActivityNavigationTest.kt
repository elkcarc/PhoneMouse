package com.example.phonemouse

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {

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
    fun testNavigationToProfiles() {
        onView(withContentDescription("Open drawer")).perform(click())
        onView(withId(R.id.profilesBtn)).perform(click())
        onView(withId(R.id.profilesPanel)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToRecordings() {
        onView(withContentDescription("Open drawer")).perform(click())
        onView(withId(R.id.recordingsBtn)).perform(click())
        onView(withId(R.id.recordingsPanel)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToSettings() {
        onView(withContentDescription("Open drawer")).perform(click())
        onView(withId(R.id.settingsBtn)).perform(click())
        onView(withId(R.id.settingsDrawerPanel)).check(matches(isDisplayed()))
    }
}
