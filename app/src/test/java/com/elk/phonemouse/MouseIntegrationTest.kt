package com.elk.phonemouse

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.content.Context
import android.os.Handler
import android.os.SystemClock
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MouseIntegrationTest {
    private val context = mockk<Context>(relaxed = true)
    private val hid = mockk<BluetoothHidDevice>(relaxed = true)
    private val host = mockk<BluetoothDevice>(relaxed = true)
    private val handler = mockk<Handler>(relaxed = true)
    private lateinit var service: MouseHidService
    
    private val postedRunnables = mutableListOf<Runnable>()

    @Before
    fun setup() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L

        postedRunnables.clear()
        // Capture posted runnables and delays
        every { handler.post(any()) } answers { 
            postedRunnables.add(firstArg<Runnable>())
            true 
        }
        every { handler.postDelayed(any(), any()) } answers {
            postedRunnables.add(firstArg<Runnable>())
            true
        }
    }

    private fun pumpRunnables() {
        val current = postedRunnables.toList()
        postedRunnables.clear()
        current.forEach { it.run() }
    }

    @Test
    fun `manual move sends correct HID packet`() {
        /**
         * Purpose: Verify that raw manual move values are correctly formatted into standard 4-byte HID packets.
         * Before State: HID service initialized with mocked Bluetooth components.
         * During Test: Sends a (10, -20) movement.
         * After State: Bluetooth device receives [0, 10, -20, 0] (Buttons, X, Y, Scroll).
         */
        service = MouseHidService(context, hid, handler)
        service.setTestHost(host)
        service.sendManualMove(10, -20)
        val expected = byteArrayOf(0, 10, -20, 0)
        verify { hid.sendReport(host, 0, match { it.contentEquals(expected) }) }
    }

    @Test
    fun `recording captures multiple HID events`() {
        /**
         * Purpose: Verify that the recording logic correctly aggregates relative movements over time.
         * Before State: Recording toggled ON.
         * During Test: Injects multiple moves and clicks at simulated time intervals.
         * After State: The final recording string matches the expected time-stamped sequence.
         */
        service = MouseHidService(context, hid, handler)
        service.setTestHost(host)
        
        every { SystemClock.elapsedRealtime() } returns 1000L
        service.toggleRecording()

        every { SystemClock.elapsedRealtime() } returns 1100L
        service.sendManualMove(5, 5)

        every { SystemClock.elapsedRealtime() } returns 1250L
        service.setButtonState(mask = 0x01, pressed = true)

        var finishedData: String? = null
        service.setOnRecordingFinishedListener { finishedData = it }
        
        every { SystemClock.elapsedRealtime() } returns 1500L
        service.toggleRecording()

        val expected = "100:0,5,5,0;250:1,0,0,0"
        assertEquals(expected, finishedData)
    }

    @Test
    fun `playback triggers packets through handler`() {
        /**
         * Purpose: Verify that playback logic correctly re-dispatches packets via the main loop handler.
         * Before State: A valid recording string provided.
         * During Test: Toggles playback ON.
         * After State: Verification that the scheduled handler runnables exist and send the correct data.
         */
        service = MouseHidService(context, hid, handler)
        service.setTestHost(host)
        
        val data = "100:0,10,10,0;300:1,0,0,0"
        service.togglePlayback(data, loop = false)
        
        // Execute first event (100ms)
        postedRunnables[0].run()
        verify { hid.sendReport(host, 0, match { it.contentEquals(byteArrayOf(0, 10, 10, 0)) }) }
    }

    /**
     * Purpose: Verify that the automation loop correctly posts delayed clicks.
     * Before State: Automation config registered.
     * During Test: Calls toggleAutomation().
     * After State: Verification that button report packets (Down then Up) are dispatched through the handler.
     */
    @Test
    fun `automation start sends correct button report`() {
        val profile = AutomationConfig("Test", 100, 100, 50, 50, 1000, 1000, 100)
        service = MouseHidService(context, hid, handler)
        service.setTestHost(host)
        service.registerProfile()
        service.setConfig(profile)
        service.toggleAutomation()
        assertEquals(1, postedRunnables.size)
        pumpRunnables()
        verify { hid.sendReport(host, 0, match { it[0] == 0x01.toByte() }) }
        assertEquals(1, postedRunnables.size)
        pumpRunnables()
        verify { hid.sendReport(host, 0, match { it[0] == 0x00.toByte() }) }
    }

    @Test
    fun `automation jitter analysis verifies gaussian-like distribution`() {
        /**
         * Purpose: Verify the "Gaussian" jitter logic in the automation loop.
         * Before State: Automation active with a wide range (100-300ms).
         * During Test: Captures 50 consecutive click-intervals.
         * After State: Verification that intervals are non-deterministic and vary across the set.
         */
        val delays = mutableListOf<Long>()
        every { handler.postDelayed(any(), any()) } answers {
            delays.add(secondArg<Long>())
            postedRunnables.add(firstArg<Runnable>())
            true
        }

        service = MouseHidService(context, hid, handler)
        service.setTestHost(host)
        service.registerProfile() // register to allow hid send
        
        val profile = AutomationConfig("Jitter Test", 100, 300, 50, 150, 5000, 10000, 100)
        service.setConfig(profile)
        service.toggleAutomation()
        
        // Run many cycles
        repeat(50) {
            pumpRunnables() // Click down -> posts click up delay
            pumpRunnables() // Click up -> posts next interval
        }
        
        val uniqueDelays = delays.distinct().size
        // In a set of 100 random intervals, there should be significant variance.
        assertTrue("Expected variation in intervals, got $uniqueDelays unique values", uniqueDelays > 5)
    }

    /**
     * Purpose: Verify that scroll commands result in correct 4th-byte HID packets.
     * Before State: HID service initialized.
     * During Test: Calls sendManualScroll(-1).
     * After State: Verification that BluetoothHidDevice.sendReport receives a byte array with [3] = -1.
     */
    @Test
    fun `scroll sends correct HID delta`() {
        service = MouseHidService(context, hid, handler)
        service.setTestHost(host)
        service.sendManualScroll(-1) // Down
        verify { hid.sendReport(host, 0, match { it[3] == (-1).toByte() }) }
    }
}
