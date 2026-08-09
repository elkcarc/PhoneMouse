package com.elk.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityRecordingDetailTest {

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
        Manifest.permission.ACCESS_FINE_LOCATION,
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
     * Purpose: Verify the full lifecycle of editing and removing an input recording.
     * Before State: A dummy recording "UI Recording Test" is injected into the repository.
     * During Test: Navigates to recordings panel, swipes right to rename, verifies, then swipes left to delete.
     * After State: The recording is successfully removed from the UI and repository.
     */
    @Test
    fun testEditAndRemoveRecordingUI() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val dummyName = "UI Recording Test"
            
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                viewModel.addDummyRecording(dummyName)
            }
            
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.recordingsBtn)).perform(click())
            Thread.sleep(500)

            scenario.onActivity { it.findViewById<DrawerLayout>(R.id.drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN) }

            // Edit
            onView(withId(R.id.recordingsRecyclerView)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(dummyName)),
                    androidx.test.espresso.action.GeneralSwipeAction(
                        Swipe.SLOW,
                        GeneralLocation.CENTER_LEFT,
                        GeneralLocation.CENTER_RIGHT,
                        Press.FINGER
                    )
                )
            )
            Thread.sleep(1500)

            onView(withId(R.id.editRecName)).perform(replaceText("UI Updated Name"), closeSoftKeyboard())
            onView(withId(android.R.id.button1)).perform(click())
            Thread.sleep(1000)

            onView(withText("UI Updated Name")).check(matches(isDisplayed()))

            // Delete
            onView(withId(R.id.recordingsRecyclerView)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("UI Updated Name")),
                    androidx.test.espresso.action.GeneralSwipeAction(
                        Swipe.SLOW,
                        GeneralLocation.CENTER_RIGHT,
                        GeneralLocation.CENTER_LEFT,
                        Press.FINGER
                    )
                )
            )
            Thread.sleep(1500)

            onView(withId(android.R.id.button1)).perform(click())
            Thread.sleep(1000)

            onView(withText("UI Updated Name")).check(doesNotExist())
        }
    }

    /**
     * Purpose: Verify that canceling a recording rename discards changes.
     * Before State: A dummy recording "Cancel Rename Test" exists.
     * During Test: Opens rename dialog, changes name to "Should Not Save", clicks Cancel.
     * After State: The recording name remains "Cancel Rename Test".
     */
    @Test
    fun testCancelRecordingRename() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val dummyName = "Cancel Rename Test"
            scenario.onActivity { ViewModelProvider(it)[MainViewModel::class.java].addDummyRecording(dummyName) }
            
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.recordingsBtn)).perform(click())
            Thread.sleep(500)
            scenario.onActivity { it.findViewById<DrawerLayout>(R.id.drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN) }

            onView(withId(R.id.recordingsRecyclerView)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(dummyName)),
                    androidx.test.espresso.action.GeneralSwipeAction(
                        Swipe.SLOW,
                        GeneralLocation.CENTER_LEFT,
                        GeneralLocation.CENTER_RIGHT,
                        Press.FINGER
                    )
                )
            )
            Thread.sleep(1000)

            onView(withId(R.id.editRecName)).perform(replaceText("Should Not Save"), closeSoftKeyboard())
            onView(withId(android.R.id.button2)).perform(click()) // Cancel
            Thread.sleep(500)

            onView(withText(dummyName)).check(matches(isDisplayed()))
            onView(withText("Should Not Save")).check(doesNotExist())
        }
    }

    /**
     * Purpose: Verify that canceling a recording deletion keeps the item in the list.
     * Before State: A dummy recording "Cancel Delete Test" exists.
     * During Test: Swipes left to delete, clicks Cancel in the confirmation dialog.
     * After State: The recording remains in the list.
     */
    @Test
    fun testCancelRecordingDeletion() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val dummyName = "Cancel Delete Test"
            scenario.onActivity { ViewModelProvider(it)[MainViewModel::class.java].addDummyRecording(dummyName) }
            
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.recordingsBtn)).perform(click())
            Thread.sleep(500)
            scenario.onActivity { it.findViewById<DrawerLayout>(R.id.drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN) }

            onView(withId(R.id.recordingsRecyclerView)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(dummyName)),
                    androidx.test.espresso.action.GeneralSwipeAction(
                        Swipe.SLOW,
                        GeneralLocation.CENTER_RIGHT,
                        GeneralLocation.CENTER_LEFT,
                        Press.FINGER
                    )
                )
            )
            Thread.sleep(1000)

            onView(withId(android.R.id.button2)).perform(click()) // Cancel Delete
            Thread.sleep(500)

            onView(withText(dummyName)).check(matches(isDisplayed()))
        }
    }
}
