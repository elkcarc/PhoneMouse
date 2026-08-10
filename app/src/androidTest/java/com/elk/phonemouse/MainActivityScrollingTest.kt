package com.elk.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test class focusing on scrolling behavior in long lists.
 * Uses UI Automator to ensure that scrolling actually occurs and items are brought into view.
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
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
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
     * Before State: 50 profiles are added to the repository.
     * During Test: Opens Profiles panel, scrolls to the end using UI Automator.
     * After State: Item 50 is found on screen and Item 1 is scrolled out.
     */
    @Test
    fun testProfileListScrolling() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                // Add 50 items to ensure scrolling is required even on large screens/tablets
                for (i in 1..50) {
                    viewModel.addConfig("Scroll Profile $i", 100, 300, 50, 150, 3000, 60000, 500)
                }
            }

            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(1000)

            val scrollable = UiScrollable(UiSelector().resourceId("com.elk.phonemouse:id/configsRecyclerView"))
            scrollable.setAsVerticalList()
            
            // Verify first item exists before scroll
            val firstItem = device.findObject(UiSelector().text("Scroll Profile 1"))
            assert(firstItem.exists()) { "First item should be visible initially" }

            scrollable.scrollToEnd(10)
            Thread.sleep(1000)

            val lastItem = device.findObject(UiSelector().text("Scroll Profile 50"))
            assert(lastItem.exists()) { "Last item 'Scroll Profile 50' should be found after scrolling to end" }
            
            // Verify first item is gone (proving physical scroll happened)
            assert(!firstItem.exists()) { "First item should be scrolled out of view" }
        }
    }

    /**
     * Purpose: Verify that the Recordings list can be scrolled when it contains many entries.
     * Before State: 40 recordings are added to the repository.
     * During Test: Opens Recordings panel, scrolls to the end using UI Automator.
     * After State: Recording 40 is found on screen.
     */
    @Test
    fun testRecordingListScrolling() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                // Add 40 items
                for (i in 1..40) {
                    viewModel.addDummyRecording("Scroll Recording $i")
                }
            }

            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.recordingsBtn)).perform(click())
            Thread.sleep(1000)

            val scrollable = UiScrollable(UiSelector().resourceId("com.elk.phonemouse:id/recordingsRecyclerView"))
            scrollable.setAsVerticalList()
            
            val firstRec = device.findObject(UiSelector().text("Scroll Recording 1"))
            assert(firstRec.exists()) { "First recording should be visible initially" }

            scrollable.scrollToEnd(10)
            Thread.sleep(1000)

            val lastRec = device.findObject(UiSelector().text("Scroll Recording 40"))
            assert(lastRec.exists()) { "Last item 'Scroll Recording 40' should be found after scrolling to end" }
            assert(!firstRec.exists()) { "First recording should be scrolled out of view" }
        }
    }
}
