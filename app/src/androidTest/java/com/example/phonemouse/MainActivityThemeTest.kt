package com.example.phonemouse

import android.Manifest
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.anything
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityThemeTest {

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
    fun testThemeSwitching_LightToDarkColorChange() {
        // 1. Open Settings
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.settingsBtn)).perform(click())
        Thread.sleep(500)

        // 2. Select Light Theme (Position 2 in [Auto, Dark, Light])
        onView(withId(R.id.themeDropdown)).perform(click())
        Thread.sleep(500)
        onData(anything()).inRoot(isPlatformPopup()).atPosition(2).perform(click())
        Thread.sleep(1500)

        // 3. Verify Light Theme Colors (Black text)
        onView(withId(R.id.settingsTitleText)).check(matches(withTextColor(Color.BLACK)))
        
        // 4. Select Dark Theme (Position 1 in [Auto, Dark, Light])
        onView(withId(R.id.themeDropdown)).perform(click())
        Thread.sleep(500)
        onData(anything()).inRoot(isPlatformPopup()).atPosition(1).perform(click())
        Thread.sleep(1500)

        // 5. Verify Dark Theme Colors (White text)
        onView(withId(R.id.settingsTitleText)).check(matches(withTextColor(Color.WHITE)))

        // 6. Check Home Screen Elements Colors
        onView(withId(R.id.settingsBackBtn)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.profilesBtn)).check(matches(withTextColor(Color.WHITE)))
    }

    private fun withTextColor(expectedColor: Int): Matcher<View> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with text color: ")
                description.appendValue(expectedColor)
            }

            override fun matchesSafely(textView: TextView): Boolean {
                val color = textView.currentTextColor
                return (color and 0xFFFFFF) == (expectedColor and 0xFFFFFF)
            }
        }
    }
}
