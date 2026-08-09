package com.example.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
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
class MainActivityInteractionTest {

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
     * Purpose: Verify that adding a profile and then tapping it only selects it (doesn't trigger edit).
     * Before State: App launched in English, profiles list empty or default.
     * During Test: Adds a profile "UI Selection Test", then taps it in the list.
     * After State: The profile panel remains visible and no edit dialog appears on simple tap.
     */
    @Test
    fun testAddProfileAndSelectionBehavior() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.addVariationBtn)).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.editName)).perform(replaceText("UI Selection Test"), closeSoftKeyboard())
            onView(withId(android.R.id.button1)).perform(click())
            Thread.sleep(1000)

            onView(withText("UI Selection Test")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.profilesPanel)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()))
        }
    }

    /**
     * Purpose: Verify the standard profile deletion flow including the confirmation dialog.
     * Before State: App launched, Drawer locked open for consistent interaction.
     * During Test: Adds "To Delete", swipes left slow to trigger dialog, clicks Positive button.
     * After State: The profile is successfully removed from the RecyclerView.
     */
    @Test
    fun testProfileDeletionUI() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.addVariationBtn)).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.editName)).perform(replaceText("To Delete"), closeSoftKeyboard())
            onView(withId(android.R.id.button1)).perform(click())
            Thread.sleep(1000)

            scenario.onActivity { it.findViewById<DrawerLayout>(R.id.drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN) }

            onView(withId(R.id.configsRecyclerView)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("To Delete")),
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

            onView(withText("To Delete")).check(doesNotExist())
        }
    }
}
