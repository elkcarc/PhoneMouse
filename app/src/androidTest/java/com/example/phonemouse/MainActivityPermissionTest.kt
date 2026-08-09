package com.example.phonemouse

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityPermissionTest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun testPermissionDenialGracefulFailure() {
        // Trigger permission request via the connection button
        onView(withId(R.id.statusBtn)).perform(click())

        // Wait for permission dialog to appear
        val permissionDialog = device.wait(Until.hasObject(By.textContains("Allow")), 5000)
        
        if (permissionDialog) {
            // Find "Don't allow" or "Deny" button and click it
            val denyButton = device.findObject(By.textContains("Don't allow")) 
                ?: device.findObject(By.textContains("Deny"))
                ?: device.findObject(By.text("DENY"))
            
            denyButton?.click()
            
            // Verify that the status button clearly indicates permissions are required
            onView(withId(R.id.statusBtn)).check(matches(isDisplayed()))
            onView(withId(R.id.statusBtn)).check(matches(withText(R.string.permissions_required_tap_to_grant)))
        }
    }
}
