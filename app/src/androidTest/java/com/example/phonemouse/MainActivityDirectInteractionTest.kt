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
class MainActivityDirectInteractionTest {

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
    fun testMouseButtonVisualFeedback() {
        // Verify that clicking mouse buttons doesn't crash and buttons are enabled
        onView(withId(R.id.leftClickBtn)).perform(click()).check(matches(isEnabled()))
        onView(withId(R.id.rightClickBtn)).perform(click()).check(matches(isEnabled()))
        onView(withId(R.id.middleClickBtn)).perform(click()).check(matches(isEnabled()))
    }

    @Test
    fun testScrollButtons() {
        onView(withId(R.id.scrollUpBtn)).perform(click()).check(matches(isDisplayed()))
        onView(withId(R.id.scrollDownBtn)).perform(click()).check(matches(isDisplayed()))
    }

    @Test
    fun testTrackpadInteraction() {
        // Swipe on trackpad area
        onView(withId(R.id.trackpad)).perform(swipeRight())
        onView(withId(R.id.trackpad)).perform(swipeDown())
        onView(withId(R.id.trackpad)).check(matches(isDisplayed()))
    }
}
