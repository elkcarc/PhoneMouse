package com.example.phonemouse

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityControlDetailTest {

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
    fun testMainControlButtons() {
        // Verify primary mouse buttons are visible and enabled
        onView(withId(R.id.leftClickBtn)).check(matches(isDisplayed()))
        onView(withId(R.id.rightClickBtn)).check(matches(isDisplayed()))
        onView(withId(R.id.middleClickBtn)).check(matches(isDisplayed()))
        onView(withId(R.id.scrollUpBtn)).check(matches(isDisplayed()))
        onView(withId(R.id.scrollDownBtn)).check(matches(isDisplayed()))

        // Verify automation buttons exist
        onView(withId(R.id.autoclickerBtn)).check(matches(isDisplayed()))
        onView(withId(R.id.recordBtn)).check(matches(isDisplayed()))
        onView(withId(R.id.playbackBtn)).check(matches(isDisplayed()))
    }
}
