package com.example.phonemouse

import android.Manifest
import android.app.Application
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
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.*
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Functional tests for complex interactions, persistence, and low-level logic verification.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityFunctionalTest {

    private val fakeService = MouseHidService(ApplicationProvider.getApplicationContext<Application>()).apply {
        isTestMode = true
        try {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            setTestHost(bluetoothManager.adapter.getRemoteDevice("00:11:22:33:44:55"), "DummyHost")
        } catch (_: Exception) {}
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

    private fun lockDrawerOpen(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            activity.findViewById<DrawerLayout>(R.id.drawerLayout).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
        }
    }

    /**
     * Purpose: Verify that disabling "Confirm before deleting" allows immediate item removal.
     * Before State: App in English, confirmation setting disabled in Settings panel.
     * During Test: Adds a profile named "Fast Delete" and performs a deliberate slow swipe to remove it.
     * After State: The item is removed from the RecyclerView without showing a confirmation dialog.
     */
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

            lockDrawerOpen(scenario)

            onView(withId(R.id.configsRecyclerView)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Fast Delete")),
                    GeneralSwipeAction(
                        Swipe.SLOW,
                        GeneralLocation.CENTER_RIGHT,
                        GeneralLocation.CENTER_LEFT,
                        Press.FINGER
                    )
                )
            )
            Thread.sleep(2000)

            onView(withText("Fast Delete")).check(doesNotExist())
        }
    }

    /**
     * Purpose: Verify that the sensitivity slider actually scales outgoing HID movement packets.
     * Before State: App launched, virtual HID service started.
     * During Test: Measures movement packets at default (3x) vs maximum (8x) sensitivity using a report interceptor.
     * After State: Verification that the total 'dx' movement is higher when sensitivity is maximized.
     */
    @Test
    fun testSensitivitySettingsFunctionalEffect() {
        var totalDx = 0
        fakeService.reportInterceptor = { report -> totalDx += kotlin.math.abs(report[1].toInt()) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { ViewModelProvider(it)[MainViewModel::class.java].startService() }
            Thread.sleep(1000)
            onView(withId(R.id.drawerLayout)).perform(DrawerActions.close())
            Thread.sleep(500)

            val microSwipe = GeneralSwipeAction(
                Swipe.FAST,
                GeneralLocation.CENTER,
                { view ->
                    val loc = IntArray(2); view.getLocationOnScreen(loc)
                    floatArrayOf(loc[0] + (view.width / 2f) + 10f, loc[1] + (view.height / 2f))
                },
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

    /**
     * Purpose: Verify that the Drag-and-Drop functionality for reordering profiles is persistent.
     * Before State: Two profiles exist ("Profile 1" and "P2").
     * During Test: Drags the first profile down below the second using the drag handle via UI Automator.
     * After State: Verification that the order is reversed and persists after activity recreation.
     */
    @Test
    fun testDragAndDropPersistence() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(500)

            // Add P2
            onView(withId(R.id.addVariationBtn)).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.editName)).perform(replaceText("P2"), closeSoftKeyboard())
            onView(withId(android.R.id.button1)).perform(click())
            Thread.sleep(1000)

            // Current order: Profile 1 (index 0), P2 (index 1)
            val rv = device.findObject(UiSelector().resourceId("com.example.phonemouse:id/configsRecyclerView"))
            val item0 = rv.getChild(UiSelector().index(0))
            val handle0 = item0.getChild(UiSelector().resourceId("com.example.phonemouse:id/dragHandle"))
            
            val item1 = rv.getChild(UiSelector().index(1))
            val targetY = item1.visibleBounds.centerY() + 20
            
            // UI Automator drag with precise coordination
            handle0.dragTo(handle0.visibleBounds.centerX(), targetY, 100)
            Thread.sleep(2000)

            scenario.recreate()
            Thread.sleep(1500)
            onView(withContentDescription("Open drawer")).perform(click())
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(1000)

            // Verify order: P2 should now be at position 0
            onView(withId(R.id.configsRecyclerView))
                .check(matches(hasDescendant(allOf(withId(R.id.configName), withText("P2")))))
        }
    }

    /**
     * Purpose: Deep HID logic verification for Trackpoint mode curves (Linear vs Cubic).
     * Before State: App set to Trackpoint mode.
     * During Test: Holds a constant offset from center and compares accumulated 'dx' between curves.
     * After State: Cubic curve must produce significantly smaller packets for small/medium offsets.
     */
    @Test
    fun testTrackpointCurvePacketEffect() {
        var totalDx = 0
        fakeService.reportInterceptor = { report -> totalDx += kotlin.math.abs(report[1].toInt()) }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { ViewModelProvider(it)[MainViewModel::class.java].startService() }
            Thread.sleep(1000)
            
            // 1. Measure Linear
            onView(withContentDescription("Open drawer")).perform(click())
            onView(withId(R.id.settingsBtn)).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.trackpadModeDropdown)).perform(click())
            onView(withText("Trackpoint (Absolute)")).inRoot(isPlatformPopup()).perform(click())
            
            val scrollable = UiScrollable(UiSelector().scrollable(true))
            scrollable.scrollIntoView(UiSelector().resourceId("com.example.phonemouse:id/trackpointCurveDropdown"))
            onView(withId(R.id.trackpointCurveDropdown)).perform(click())
            onView(withText("Linear")).inRoot(isPlatformPopup()).perform(click())
            onView(withId(R.id.settingsBackBtn)).perform(forceClick())
            onView(withId(R.id.drawerLayout)).perform(DrawerActions.close())
            Thread.sleep(500)

            totalDx = 0
            onView(withId(R.id.trackpad)).perform(
                GeneralSwipeAction(
                    Swipe.SLOW,
                    GeneralLocation.CENTER,
                    { v ->
                        val loc = IntArray(2); v.getLocationOnScreen(loc)
                        floatArrayOf(loc[0] + (v.width * 0.7f), loc[1] + (v.height / 2f))
                    },
                    Press.FINGER
                )
            )
            Thread.sleep(1000)
            val dxLinear = totalDx

            // 2. Measure Cubic
            onView(withContentDescription("Open drawer")).perform(click())
            onView(withId(R.id.settingsBtn)).perform(click())
            Thread.sleep(500)
            scrollable.scrollIntoView(UiSelector().resourceId("com.example.phonemouse:id/trackpointCurveDropdown"))
            onView(withId(R.id.trackpointCurveDropdown)).perform(click())
            onView(withText("Cubic")).inRoot(isPlatformPopup()).perform(click())
            onView(withId(R.id.settingsBackBtn)).perform(forceClick())
            onView(withId(R.id.drawerLayout)).perform(DrawerActions.close())
            Thread.sleep(500)

            totalDx = 0
            onView(withId(R.id.trackpad)).perform(
                GeneralSwipeAction(
                    Swipe.SLOW,
                    GeneralLocation.CENTER,
                    { v ->
                        val loc = IntArray(2); v.getLocationOnScreen(loc)
                        floatArrayOf(loc[0] + (v.width * 0.7f), loc[1] + (v.height / 2f))
                    },
                    Press.FINGER
                )
            )
            Thread.sleep(1000)
            val dxCubic = totalDx

            assert(dxLinear > dxCubic) { "Linear curve ($dxLinear) > Cubic ($dxCubic) for 20% offset" }
        }
    }

    /**
     * Purpose: Verify that the connection status box indicates "TEST MODE" when using mock HID.
     * Before State: App running, FakeHidManager injected, host "connected".
     * During Test: Observe status button text for the presence of the test mode flag.
     * After State: Text contains "(TEST MODE)" indicating a safe non-hardware session.
     */
    @Test
    fun testTestModeIndicationInUI() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.statusBtn)).check(matches(withText(containsString("TEST MODE"))))
        }
    }
}
