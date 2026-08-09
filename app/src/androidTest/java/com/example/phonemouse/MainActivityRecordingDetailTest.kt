package com.example.phonemouse

import android.Manifest
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityRecordingDetailTest {

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
    fun testEditAndRemoveRecordingUI() {
        val dummyName = "UI Recording Test"
        
        activityRule.scenario.onActivity { activity ->
            val viewModel = androidx.lifecycle.ViewModelProvider(activity)[MainViewModel::class.java]
            viewModel.addDummyRecording(dummyName)
        }
        
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.recordingsBtn)).perform(click())
        Thread.sleep(500)

        // Lock drawer
        activityRule.scenario.onActivity { activity ->
            activity.findViewById<DrawerLayout>(R.id.drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
        }

        // Swipe Right
        onView(withId(R.id.recordingsRecyclerView)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(dummyName)),
                GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER_LEFT, GeneralLocation.CENTER_RIGHT, Press.FINGER)
            )
        )
        Thread.sleep(1000)

        onView(withId(R.id.editRecName)).perform(replaceText("UI Updated Name"), closeSoftKeyboard())
        onView(withId(android.R.id.button1)).perform(click())
        Thread.sleep(1000)

        onView(withText("UI Updated Name")).check(matches(isDisplayed()))

        // Delete
        onView(withId(R.id.recordingsRecyclerView)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("UI Updated Name")),
                GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER_RIGHT, GeneralLocation.CENTER_LEFT, Press.FINGER)
            )
        )
        Thread.sleep(1000)

        onView(withId(android.R.id.button1)).perform(click())
        Thread.sleep(1000)

        onView(withText("UI Updated Name")).check(doesNotExist())
    }
}
