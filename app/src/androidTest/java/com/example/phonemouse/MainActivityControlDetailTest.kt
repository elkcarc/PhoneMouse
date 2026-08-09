package com.example.phonemouse

import android.Manifest
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityControlDetailTest {

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
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
    }

    @After
    fun teardown() {
        MainViewModel.testingHidManager = null
    }

    @Test
    fun testInitialControlStates() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.autoclickerBtn)).check(matches(isNotEnabled()))
            onView(withId(R.id.recordBtn)).check(matches(isNotEnabled()))
            onView(withId(R.id.playbackBtn)).check(matches(isNotEnabled()))
        }
    }
}
