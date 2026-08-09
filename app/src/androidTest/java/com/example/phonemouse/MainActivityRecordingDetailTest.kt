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

    @Test
    fun testEditAndRemoveRecordingUI() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val dummyName = "UI Recording Test"
            
            scenario.onActivity { activity ->
                val viewModel = androidx.lifecycle.ViewModelProvider(activity)[MainViewModel::class.java]
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
}
