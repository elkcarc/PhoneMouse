package com.example.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
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
     * Purpose: Verify basic navigation from the main screen to the settings panel.
     * Before State: App launched, drawer closed.
     * During Test: Opens drawer, clicks Settings button.
     * After State: The settings panel is verified as visible to the user.
     */
    @Test
    fun testNavigationToSettings() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withContentDescription("Open drawer")).perform(click())
            onView(withId(R.id.settingsBtn)).perform(click())
            onView(withId(R.id.settingsDrawerPanel)).check(matches(isDisplayed()))
        }
    }

    /**
     * Purpose: Verify that the system 'Back' button correctly pops the panel stack.
     * Before State: App launched in English, Profiles panel deep-linked or navigated to.
     * During Test: Clicks Back once (to main drawer) then twice (to close drawer).
     * After State: The drawer is closed and the main trackpad screen is the active focused view.
     */
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
