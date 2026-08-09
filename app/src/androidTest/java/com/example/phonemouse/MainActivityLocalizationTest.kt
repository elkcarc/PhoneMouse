package com.example.phonemouse

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLocalizationTest {

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
        MainViewModel.testingHidManager = FakeHidManager(fakeService)
        
        // Reset to English and clear prefs
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("PhoneMousePrefs", Context.MODE_PRIVATE).edit().clear().commit()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
    }

    @After
    fun teardown() {
        MainViewModel.testingHidManager = null
        // Ensure we leave it in English for other tests
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
    }

    private fun navigateToSettings() {
        onView(withContentDescription("Open drawer")).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.settingsBtn)).perform(click())
        Thread.sleep(500)
    }

    private fun changeLanguageByPosition(index: Int) {
        onView(withId(R.id.languageDropdown)).perform(click())
        Thread.sleep(500)
        onData(anything())
            .inRoot(isPlatformPopup())
            .atPosition(index)
            .perform(click())
        Thread.sleep(2000) // Longer wait for locale switch
    }

    /**
     * Purpose: Verify that switching to the Russian language updates the UI strings accordingly.
     * Before State: App launched in English (forced in setup), Settings panel open.
     * During Test: Selects the 3rd index in the language dropdown (Russian).
     * After State: The settings title text is verified to be the Russian string "Настройки".
     */
    @Test
    fun testLanguage_Russian() {
        ActivityScenario.launch(MainActivity::class.java).use {
            navigateToSettings()
            changeLanguageByPosition(3)
            onView(withId(R.id.settingsTitleText)).check(matches(withText("Настройки")))
        }
    }

    /**
     * Purpose: Verify that switching to the Spanish language updates the UI strings accordingly.
     * Before State: App launched in English, Settings panel open.
     * During Test: Selects the 1st index in the language dropdown (Spanish).
     * After State: The settings title text is verified to be the Spanish string "Ajustes".
     */
    @Test
    fun testLanguage_Spanish() {
        ActivityScenario.launch(MainActivity::class.java).use {
            navigateToSettings()
            changeLanguageByPosition(1)
            onView(withId(R.id.settingsTitleText)).check(matches(withText("Ajustes")))
        }
    }

    /**
     * Purpose: Verify that switching to the Japanese language updates the UI strings accordingly.
     * Before State: App launched in English, Settings panel open.
     * During Test: Selects the 2nd index in the language dropdown (Japanese).
     * After State: The settings title text is verified to be the Japanese string "設定".
     */
    @Test
    fun testLanguage_Japanese() {
        ActivityScenario.launch(MainActivity::class.java).use {
            navigateToSettings()
            changeLanguageByPosition(2)
            onView(withId(R.id.settingsTitleText)).check(matches(withText("設定")))
        }
    }

    /**
     * Purpose: Verify that switching to the Chinese language updates the UI strings accordingly.
     * Before State: App launched in English, Settings panel open.
     * During Test: Selects the 4th index in the language dropdown (Chinese).
     * After State: The settings title text is verified to be the Chinese string "设置".
     */
    @Test
    fun testLanguage_Chinese() {
        ActivityScenario.launch(MainActivity::class.java).use {
            navigateToSettings()
            changeLanguageByPosition(4)
            onView(withId(R.id.settingsTitleText)).check(matches(withText("设置")))
        }
    }
}
