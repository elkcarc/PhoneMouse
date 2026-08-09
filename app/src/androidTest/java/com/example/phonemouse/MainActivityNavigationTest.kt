package com.example.phonemouse

import android.Manifest
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
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
class MainActivityNavigationTest {

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
        MainViewModel.testingHidManager = FakeHidManager(fakeService)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
    }

    @After
    fun teardown() {
        MainViewModel.testingHidManager = null
    }

    @Test
    fun testNavigationToSettings() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withContentDescription("Open drawer")).perform(click())
            onView(withId(R.id.settingsBtn)).perform(click())
            onView(withId(R.id.settingsDrawerPanel)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testBackButtonNavigationSequence() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(200)
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(200)
            onView(withId(R.id.profilesPanel)).check(matches(isDisplayed()))

            pressBack()
            Thread.sleep(200)
            onView(withId(R.id.mainNavPanel)).check(matches(isDisplayed()))

            pressBack()
            Thread.sleep(500)
            onView(withId(R.id.leftClickBtn)).check(matches(isDisplayed()))
        }
    }
}
