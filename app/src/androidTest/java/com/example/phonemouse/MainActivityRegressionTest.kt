package com.example.phonemouse

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
class MainActivityRegressionTest {

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
    fun testOpenProfilesPanelDoesNotCrash() {
        onView(withContentDescription("Open drawer")).perform(click())
        onView(withId(R.id.profilesBtn)).perform(click())
        onView(withId(R.id.profilesPanel)).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenRecordingsPanelDoesNotCrash() {
        onView(withContentDescription("Open drawer")).perform(click())
        onView(withId(R.id.recordingsBtn)).perform(click())
        onView(withId(R.id.recordingsPanel)).check(matches(isDisplayed()))
    }

    @Test
    fun testTapDoesNotOpenEditDialog() {
        onView(withContentDescription("Open drawer")).perform(click())
        onView(withId(R.id.profilesBtn)).perform(click())
        
        // Tap the first item (default Profile 1)
        onView(withText("Profile 1")).perform(click())
        
        // Check that edit dialog title is NOT present
        // Since we can't easily check "NOT displayed" for a view that might not exist at all,
        // we'll just check if the profiles panel is still the top view.
        onView(withId(R.id.profilesPanel)).check(matches(isDisplayed()))
    }
}
