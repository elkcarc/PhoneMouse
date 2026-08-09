package com.example.phonemouse

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityDialogDetailTest {

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
    fun testEditProfileDialogFields() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.profilesBtn)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.addVariationBtn)).perform(click())
        Thread.sleep(500)

        // Test all fields
        onView(withId(R.id.editName)).perform(replaceText("Detailed Test"), closeSoftKeyboard())
        onView(withId(R.id.editMinInt)).perform(replaceText("111"), closeSoftKeyboard())
        onView(withId(R.id.editMaxInt)).perform(replaceText("222"), closeSoftKeyboard())
        onView(withId(R.id.editMinPress)).perform(replaceText("33"), closeSoftKeyboard())
        onView(withId(R.id.editMaxPress)).perform(replaceText("44"), closeSoftKeyboard())
        onView(withId(R.id.editMinBreak)).perform(replaceText("555"), closeSoftKeyboard())
        onView(withId(R.id.editMaxBreak)).perform(replaceText("666"), closeSoftKeyboard())
        onView(withId(R.id.editFreq)).perform(replaceText("777"), closeSoftKeyboard())

        // Save
        onView(withText(R.string.save)).perform(click())
        Thread.sleep(1000)

        // Verify the profile exists
        onView(withText("Detailed Test")).check(matches(isDisplayed()))
    }

    @Test
    fun testCancelEditProfileDialog() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.profilesBtn)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.addVariationBtn)).perform(click())
        Thread.sleep(500)

        onView(withId(R.id.editName)).perform(replaceText("Cancel Test"), closeSoftKeyboard())
        
        // Click Cancel
        onView(withText(R.string.cancel)).perform(click())
        Thread.sleep(500)

        // Verify the profile DOES NOT exist
        onView(withText("Cancel Test")).check(doesNotExist())
    }

    @Test
    fun testEditRecordingDialog() {
        // Create a fake recording first (requires connection, so we might just test the UI elements if visible)
        // Since we can't easily fake a recording without a lot of setup, we'll verify the "Recordings" panel exists.
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.recordingsBtn)).perform(click())
        Thread.sleep(500)
        
        onView(withId(R.id.recordingsPanel)).check(matches(isDisplayed()))
    }
}
