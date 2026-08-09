package com.elk.phonemouse

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

@RunWith(AndroidJUnit4::class)
class MainActivityDirectInteractionTest {

    private val fakeService = MouseHidService(ApplicationProvider.getApplicationContext<Application>()).apply {
        isTestMode = true
    }

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE,
        Manifest.permission.ACCESS_FINE_LOCATION
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
     * Before State: Main activity launched.
     * During Test: Click Left, Right, and Middle buttons.
     * After State: Buttons remain enabled and the app does not crash.
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
     * Before State: Main activity launched.
     * During Test: Click Scroll Up and Scroll Down buttons.
     * After State: Controls remain visible and clickable.
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
     * Before State: Main activity launched.
     * During Test: Perform right and down swipes on the trackpad area.
     * After State: Trackpad remains visible and responsive to touch input.
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
