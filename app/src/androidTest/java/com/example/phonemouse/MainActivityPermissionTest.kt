package com.example.phonemouse

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityPermissionTest {

    private val fakeService = MouseHidService(ApplicationProvider.getApplicationContext<Application>()).apply {
        isTestMode = true
    }

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
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
     * Purpose: Verify that denying requested permissions results in a graceful UI state.
     * Before State: App launched with permissions not yet granted.
     * During Test: Clicks the status button, then selects "Deny" on the system permission dialog.
     * After State: The status button text correctly updates to reflect the missing permissions.
     */
    @Test
    fun testPermissionDenialGracefulFailure() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Trigger permission request
            onView(withId(R.id.statusBtn)).perform(click())

            // Wait for dialog
            val permissionDialog = device.wait(Until.hasObject(By.textContains("Allow")), 3000)
            
            if (permissionDialog) {
                val denyButton = device.findObject(By.textContains("Don't allow")) 
                    ?: device.findObject(By.textContains("Deny"))
                    ?: device.findObject(By.text("DENY"))
                
                denyButton?.click()
                Thread.sleep(500)
                
                onView(withId(R.id.statusBtn)).check(matches(isDisplayed()))
                onView(withId(R.id.statusBtn)).check(matches(withText(R.string.permissions_required_tap_to_grant)))
            }
        }
    }
}
