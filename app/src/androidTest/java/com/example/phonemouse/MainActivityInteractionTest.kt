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
import org.hamcrest.Matchers.instanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInteractionTest {

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
    fun testAddAndEditProfile() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.profilesBtn)).perform(click())
        Thread.sleep(200)

        // Add
        onView(withId(R.id.addVariationBtn)).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.editName)).perform(replaceText("UI Test Profile"), closeSoftKeyboard())
        onView(withId(android.R.id.button1)).perform(click()) 
        Thread.sleep(200)

        // Verify
        onView(withId(R.id.configsRecyclerView)).check(matches(hasDescendant(withText("UI Test Profile"))))

        // Edit
        onView(withText("UI Test Profile")).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.editName)).perform(replaceText("UI Test Edited"), closeSoftKeyboard())
        onView(withId(android.R.id.button1)).perform(click())
        Thread.sleep(200)

        onView(withId(R.id.configsRecyclerView)).check(matches(hasDescendant(withText("UI Test Edited"))))
    }

    @Test
    fun testRecordingProcessUI() {
        // Connected device text usually contains "Connected" or the device name
        // We'll just click the record button
        onView(withId(R.id.recordBtn)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.recordBtn)).perform(click()) // Stop
        Thread.sleep(200)

        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.recordingsBtn)).perform(click())
        Thread.sleep(200)

        // Check if list contains a recording
        onView(withId(R.id.recordingsRecyclerView)).check(matches(hasDescendant(withText(containsString("Recording")))))
    }

    @Test
    fun testSettingsResponsiveness() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(200)
        onView(withId(R.id.settingsBtn)).perform(click())
        Thread.sleep(200)

        // Verify key settings exist and are interactable
        onView(withId(R.id.themeDropdown)).check(matches(isDisplayed()))
        onView(withId(R.id.languageDropdown)).check(matches(isDisplayed()))
        
        onView(withId(R.id.confirmDeleteToggle)).perform(scrollTo())
        onView(withId(R.id.confirmDeleteToggle)).check(matches(isDisplayed()))
    }
}
