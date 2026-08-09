package com.example.phonemouse

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityFunctionalTest {

    private val fakeService = MouseHidService(ApplicationProvider.getApplicationContext<Application>()).apply {
        isTestMode = true
        try {
            setTestHost(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("00:11:22:33:44:55"))
        } catch (_: Exception) {}
    }

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
    )

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Reset environment
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE).edit().clear().commit()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        
        MainViewModel.testingHidManager = FakeHidManager(fakeService)
        fakeService.reportInterceptor = null
    }

    @After
    fun teardown() {
        MainViewModel.testingHidManager = null
    }

    private fun forceClick() = object : ViewAction {
        override fun getConstraints() = isAssignableFrom(View::class.java)
        override fun getDescription() = "force click"
        override fun perform(uiController: UiController, view: View) { view.performClick() }
    }

    private fun swipeLeftSlow(): ViewAction {
        return androidx.test.espresso.action.GeneralSwipeAction(
            Swipe.SLOW,
            GeneralLocation.CENTER_RIGHT,
            GeneralLocation.CENTER_LEFT,
            Press.FINGER
        )
    }

    @Test
    fun testNoDeleteConfirmationWhenDisabled() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.settingsBtn)).perform(click())
            Thread.sleep(500)
            
            // Toggle confirmation OFF
            val scrollable = UiScrollable(UiSelector().scrollable(true))
            scrollable.scrollIntoView(UiSelector().resourceId("com.example.phonemouse:id/confirmDeleteToggle"))
            val toggle = device.findObject(UiSelector().resourceId("com.example.phonemouse:id/confirmDeleteToggle"))
            if (toggle.isChecked) toggle.click()
            Thread.sleep(200)

            onView(withId(R.id.settingsBackBtn)).perform(forceClick())
            Thread.sleep(500)

            // Add profile
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.addVariationBtn)).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.editName)).perform(replaceText("Fast Delete"), closeSoftKeyboard())
            onView(withId(android.R.id.button1)).perform(click())
            Thread.sleep(1000)

            // Lock drawer
            scenario.onActivity { it.findViewById<DrawerLayout>(R.id.drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN) }

            // Perform slow swipe
            onView(withId(R.id.configsRecyclerView)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Fast Delete")),
                    swipeLeftSlow()
                )
            )
            Thread.sleep(2000)

            onView(withText("Fast Delete")).check(doesNotExist())
        }
    }

    @Test
    fun testSensitivitySettingsFunctionalEffect() {
        var totalDx = 0
        fakeService.reportInterceptor = { report -> totalDx += kotlin.math.abs(report[1].toInt()) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { ViewModelProvider(it)[MainViewModel::class.java].startService() }
            Thread.sleep(1000)
            onView(withId(R.id.drawerLayout)).perform(DrawerActions.close())
            Thread.sleep(500)

            val microSwipe = androidx.test.espresso.action.GeneralSwipeAction(
                Swipe.FAST,
                GeneralLocation.CENTER,
                { view -> floatArrayOf(view.width / 2f + 5f, view.height / 2f) },
                Press.FINGER
            )
            
            totalDx = 0
            onView(withId(R.id.trackpad)).perform(microSwipe)
            val dxAtDefault = totalDx

            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.settingsBtn)).perform(click())
            Thread.sleep(500)
            val slider = device.findObject(UiSelector().resourceId("com.example.phonemouse:id/trackpadSensitivitySlider"))
            slider.swipeRight(100)
            Thread.sleep(500)
            onView(withId(R.id.settingsBackBtn)).perform(forceClick())
            onView(withId(R.id.drawerLayout)).perform(DrawerActions.close())
            Thread.sleep(500)

            totalDx = 0
            onView(withId(R.id.trackpad)).perform(microSwipe)
            val dxAtHigh = totalDx

            assert(dxAtHigh > dxAtDefault) { "High sensitivity ($dxAtHigh) must produce more total movement than default ($dxAtDefault)" }
        }
    }
}
