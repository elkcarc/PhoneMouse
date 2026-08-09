package com.example.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test class for direct UI interactions on the main trackpad screen.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityDirectInteractionTest {

    private val fakeService = MouseHidService(ApplicationProvider.getApplicationContext<Application>()).apply {
        isTestMode = true
    }

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
    )

    @Before
    fun setup() {
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
     * Purpose: Verify that the primary mouse button views are interactive.
     * Expected Before State: Main activity launched.
     * Actions During Test: Click Left, Right, and Middle buttons.
     * Expected After State: Buttons remain enabled and the app does not crash.
     */
    /**
     * Purpose: Verify that mouse button UI elements are responsive and enabled on start.
     * Before State: App launched, default settings active.
     * During Test: Clicks Left, Right, and Middle mouse buttons.
     * After State: No crashes occur and buttons remain in an enabled state.
     */
    @Test
    fun testMouseButtonVisualFeedback() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.leftClickBtn)).perform(click()).check(matches(isEnabled()))
            onView(withId(R.id.rightClickBtn)).perform(click()).check(matches(isEnabled()))
            onView(withId(R.id.middleClickBtn)).perform(click()).check(matches(isEnabled()))
        }
    }

    /**
     * Purpose: Verify that the scroll control buttons are displayed.
     * Expected Before State: Main activity launched.
     * Actions During Test: Click Scroll Up and Scroll Down buttons.
     * Expected After State: Controls remain visible.
     */
    /**
     * Purpose: Verify that scroll buttons are visible and clickable.
     * Before State: App launched.
     * During Test: Clicks the Scroll Up and Scroll Down buttons.
     * After State: UI remains responsive and buttons are verified as displayed.
     */
    @Test
    fun testScrollButtons() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.scrollUpBtn)).perform(click()).check(matches(isDisplayed()))
            onView(withId(R.id.scrollDownBtn)).perform(click()).check(matches(isDisplayed()))
        }
    }

    /**
     * Purpose: Verify basic gesture responsiveness of the TrackpadView.
     * Expected Before State: Main activity launched.
     * Actions During Test: Perform right and down swipes on the trackpad area.
     * Expected After State: Trackpad remains visible.
     */
    /**
     * Purpose: Verify that the trackpad surface handles touch gestures.
     * Before State: App launched.
     * During Test: Performs horizontal and vertical swipes on the trackpad area.
     * After State: Trackpad remains visible and no interaction errors are reported.
     */
    @Test
    fun testTrackpadInteraction() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.trackpad)).perform(swipeRight())
            onView(withId(R.id.trackpad)).perform(swipeDown())
            onView(withId(R.id.trackpad)).check(matches(isDisplayed()))
        }
    }
}
