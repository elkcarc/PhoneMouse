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
class MainActivityInteractionTest {

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
    fun testProfileDeletionUI() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.profilesBtn)).perform(click())
        Thread.sleep(500)

        // Lock drawer
        activityRule.scenario.onActivity { activity ->
            activity.findViewById<DrawerLayout>(R.id.drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
        }

        // Create one
        onView(withId(R.id.addVariationBtn)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.editName)).perform(replaceText("To Delete"), closeSoftKeyboard())
        onView(withId(android.R.id.button1)).perform(click())
        Thread.sleep(1000)

        // Swipe Left deliberately
        onView(withId(R.id.configsRecyclerView)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("To Delete")),
                GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER_RIGHT, GeneralLocation.CENTER_LEFT, Press.FINGER)
            )
        )
        Thread.sleep(1000)

        // Verify Dialog
        onView(withText(R.string.confirm_delete_title)).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).perform(click())
        Thread.sleep(1000)

        onView(withText("To Delete")).check(doesNotExist())
        
        // Unlock
        activityRule.scenario.onActivity { activity ->
            activity.findViewById<DrawerLayout>(R.id.drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    @Test
    fun testDragAndDropProfile() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.profilesBtn)).perform(click())
        Thread.sleep(500)

        // Ensure at least two items
        onView(withId(R.id.addVariationBtn)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.editName)).perform(replaceText("Draggable Item"), closeSoftKeyboard())
        onView(withText(R.string.save)).perform(click())
        Thread.sleep(500)

        // Perform drag on the handle (index 1 is our new item)
        onView(withId(R.id.configsRecyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1,
                GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER, GeneralLocation.TOP_CENTER, Press.FINGER)
            )
        )
        Thread.sleep(1000)

        onView(withText("Draggable Item")).check(matches(isDisplayed()))
    }
}
