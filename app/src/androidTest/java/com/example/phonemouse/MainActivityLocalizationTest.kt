package com.example.phonemouse

import android.Manifest
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLocalizationTest {

    @Rule
    @JvmField
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
    )

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
        Thread.sleep(1500)
    }

    @Test
    fun testLanguage_Russian() {
        navigateToSettings()
        changeLanguageByPosition(3) // 3 is Russian

        onView(withId(R.id.settingsTitleText)).check(matches(withText("Настройки")))
        
        onView(withId(R.id.settingsBackBtn)).perform(click())
        Thread.sleep(500)
        
        onView(withId(R.id.profilesBtn)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.addVariationBtn)).perform(click())
        Thread.sleep(1000)
        // Check hint of the first field in the dialog
        onView(withId(R.id.editName)).check(matches(withHint("Имя профиля")))
    }

    @Test
    fun testLanguage_Spanish() {
        navigateToSettings()
        changeLanguageByPosition(1) // 1 is Spanish

        onView(withId(R.id.settingsTitleText)).check(matches(withText("Ajustes")))
        
        onView(withId(R.id.settingsBackBtn)).perform(click())
        Thread.sleep(500)
        
        onView(withId(R.id.profilesBtn)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.addVariationBtn)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.editName)).check(matches(withHint("Nombre del perfil")))
    }

    @Test
    fun testLanguage_Japanese() {
        navigateToSettings()
        changeLanguageByPosition(2) // 2 is Japanese

        onView(withId(R.id.settingsTitleText)).check(matches(withText("設定")))
        
        onView(withId(R.id.settingsBackBtn)).perform(click())
        Thread.sleep(500)
        
        onView(withId(R.id.profilesBtn)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.addVariationBtn)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.editName)).check(matches(withHint("プロファイル名")))
    }

    @Test
    fun testLanguage_Chinese() {
        navigateToSettings()
        changeLanguageByPosition(4) // 4 is Chinese

        onView(withId(R.id.settingsTitleText)).check(matches(withText("设置")))
        
        onView(withId(R.id.settingsBackBtn)).perform(click())
        Thread.sleep(500)
        
        onView(withId(R.id.profilesBtn)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.addVariationBtn)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.editName)).check(matches(withHint("配置名称")))
    }
}
