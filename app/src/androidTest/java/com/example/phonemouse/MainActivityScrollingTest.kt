package com.example.phonemouse

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
     * Before State: 25 profiles are added to the repository.
     * During Test: Opens Profiles panel, scrolls to the end using UI Automator.
     * After State: Item 25 is found on screen.
     */
    @Test
    fun testProfileListScrolling() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                for (i in 1..25) {
                    viewModel.addConfig("Scroll Profile $i", 100, 300, 50, 150, 3000, 60000, 500)
                }
            }

            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(1000)

            val scrollable = UiScrollable(UiSelector().resourceId("com.example.phonemouse:id/configsRecyclerView"))
            scrollable.setAsVerticalList()
            scrollable.scrollToEnd(5)
            Thread.sleep(1000)

            val lastItem = device.findObject(UiSelector().text("Scroll Profile 25"))
            assert(lastItem.exists()) { "Last item 'Scroll Profile 25' should be found after scrolling to end" }
        }
    }

    /**
     * Purpose: Verify that the Recordings list can be scrolled when it contains many entries.
     * Before State: 20 recordings are added to the repository.
     * During Test: Opens Recordings panel, scrolls to the end using UI Automator.
     * After State: Recording 20 is found on screen.
     */
    @Test
    fun testRecordingListScrolling() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
                for (i in 1..20) {
                    viewModel.addDummyRecording("Scroll Recording $i")
                }
            }

            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.recordingsBtn)).perform(click())
            Thread.sleep(1000)

            val scrollable = UiScrollable(UiSelector().resourceId("com.example.phonemouse:id/recordingsRecyclerView"))
            scrollable.setAsVerticalList()
            scrollable.scrollToEnd(5)
            Thread.sleep(1000)

            val lastRec = device.findObject(UiSelector().text("Scroll Recording 20"))
            assert(lastRec.exists()) { "Last item 'Scroll Recording 20' should be found after scrolling to end" }
        }
    }
}
