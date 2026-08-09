package com.elk.phonemouse

import android.Manifest
import android.app.Application
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.anything
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityThemeTest {

    private val fakeService = MouseHidService(ApplicationProvider.getApplicationContext<Application>()).apply {
        isTestMode = true
    }

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE,
    )

    @Before
    fun setup() {
        MainViewModel.testingHidManager = FakeHidManager(fakeService)
        // Theme tests switch languages potentially, but they should start in English.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
    }

    @After
    fun teardown() {
        MainViewModel.testingHidManager = null
    }

    /**
     * Purpose: Verify that switching between Light and Dark themes actually updates the text color styles.
     * Before State: App launched in English, Settings panel open.
     * During Test: Selects "Light" theme, verifies BLACK text, then selects "Dark" theme, verifies WHITE text.
     * After State: The UI effectively re-renders with the chosen Material theme colors.
     */
    @Test
    fun testThemeSwitching_LightToDarkColorChange() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.settingsBtn)).perform(click())
            Thread.sleep(500)

            // Light
            onView(withId(R.id.themeDropdown)).perform(click())
            onData(anything()).inRoot(isPlatformPopup()).atPosition(2).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.settingsTitleText)).check(matches(withTextColor(Color.BLACK)))
            
            // Dark
            onView(withId(R.id.themeDropdown)).perform(click())
            onData(anything()).inRoot(isPlatformPopup()).atPosition(1).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.settingsTitleText)).check(matches(withTextColor(Color.WHITE)))
        }
    }

    private fun withTextColor(expectedColor: Int): Matcher<View> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) { description.appendText("with text color: "); description.appendValue(expectedColor) }
            override fun matchesSafely(textView: TextView) = (textView.currentTextColor and 0xFFFFFF) == (expectedColor and 0xFFFFFF)
        }
    }
}
