package com.example.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
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

/**
 * Instrumented test class focusing on scrolling behavior in long lists.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityScrollingTest {

    private val fakeService = MouseHidService(ApplicationProvider.getApplicationContext<Application>()).apply {
        isTestMode = true
    }

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
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
     * Purpose: Verify that the Profiles list can be scrolled when it contains many entries.
     * Before State: 20 profiles are added to the repository.
     * During Test: Opens Profiles panel, scrolls to the last item ("Profile 20").
     * After State: The last item is visible and correctly rendered.
     */
    @Test
    fun testProfileListScrolling() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                for (i in 1..20) {
                    viewModel.addConfig("Scroll Profile $i", 100, 300, 50, 150, 3000, 60000, 500)
                }
            }

            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(500)

            // Scroll to the last item
            onView(withId(R.id.configsRecyclerView)).perform(
                RecyclerViewActions.scrollTo<androidx.recyclerview.widget.RecyclerView.ViewHolder>(
                    hasDescendant(withText("Scroll Profile 20"))
                )
            )

            onView(withText("Scroll Profile 20")).check(matches(isDisplayed()))
        }
    }

    /**
     * Purpose: Verify that the Recordings list can be scrolled when it contains many entries.
     * Before State: 15 recordings are added to the repository.
     * During Test: Opens Recordings panel, scrolls to the last item ("Recording 15").
     * After State: The last item is visible and correctly rendered.
     */
    @Test
    fun testRecordingListScrolling() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                for (i in 1..15) {
                    viewModel.addDummyRecording("Scroll Recording $i")
                }
            }

            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.recordingsBtn)).perform(click())
            Thread.sleep(500)

            // Scroll to the last item
            onView(withId(R.id.recordingsRecyclerView)).perform(
                RecyclerViewActions.scrollTo<androidx.recyclerview.widget.RecyclerView.ViewHolder>(
                    hasDescendant(withText("Scroll Recording 15"))
                )
            )

            onView(withText("Scroll Recording 15")).check(matches(isDisplayed()))
        }
    }
}
