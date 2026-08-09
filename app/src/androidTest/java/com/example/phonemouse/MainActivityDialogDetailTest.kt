package com.example.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test class focusing on the behavior and validation of input dialogs.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityDialogDetailTest {

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
        // Reset state and locale before every test to avoid contamination.
        context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE).edit().clear().commit()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        
        MainViewModel.testingHidManager = FakeHidManager(fakeService)
    }

    @After
    fun teardown() {
        MainViewModel.testingHidManager = null
    }

    /**
     * Purpose: Verify the end-to-end flow of creating a new autoclicker profile via the edit dialog.
     * Before State: App launched in English, profiles panel open, "Add New" clicked.
     * During Test: Fills all numeric and text fields in the dialog and clicks "Save".
     * After State: A new profile card with the specified name is visible in the RecyclerView.
     */
    @Test
    fun testEditProfileDialogFields() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.addVariationBtn)).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.editName)).perform(replaceText("Detailed Test"), closeSoftKeyboard())
            onView(withId(R.id.editMinInt)).perform(replaceText("111"), closeSoftKeyboard())
            onView(withId(R.id.editMaxInt)).perform(replaceText("222"), closeSoftKeyboard())
            onView(withId(R.id.editMinPress)).perform(replaceText("33"), closeSoftKeyboard())
            onView(withId(R.id.editMaxPress)).perform(replaceText("44"), closeSoftKeyboard())
            onView(withId(R.id.editMinBreak)).perform(replaceText("555"), closeSoftKeyboard())
            onView(withId(R.id.editMaxBreak)).perform(replaceText("666"), closeSoftKeyboard())
            onView(withId(R.id.editFreq)).perform(replaceText("777"), closeSoftKeyboard())

            onView(withId(android.R.id.button1)).perform(click())
            Thread.sleep(1000)

            onView(withText("Detailed Test")).check(matches(isDisplayed()))
        }
    }

    /**
     * Purpose: Verify that the "Cancel" button in the edit profile dialog discards changes.
     * Before State: Profiles panel open, "Add New" clicked, name field filled.
     * During Test: Enters text into the name field then clicks the standard Negative button (Cancel).
     * After State: The dialog closes and no new profile is added to the list.
     */
    @Test
    fun testCancelEditProfileDialog() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withContentDescription("Open drawer")).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.profilesBtn)).perform(click())
            Thread.sleep(500)
            onView(withId(R.id.addVariationBtn)).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.editName)).perform(replaceText("Cancel Test"), closeSoftKeyboard())
            
            onView(withId(android.R.id.button2)).perform(click()) // Standard ID for Cancel
            Thread.sleep(500)

            onView(withText("Cancel Test")).check(doesNotExist())
        }
    }
}
