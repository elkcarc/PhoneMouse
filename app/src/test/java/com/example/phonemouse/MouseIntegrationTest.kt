package com.example.phonemouse

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.content.Context
import android.os.Handler
import android.os.SystemClock
import io.mockk.*
import org.junit.Assert.assertEquals
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
        // Capture posted runnables instead of running them immediately (prevents StackOverflow)
        every { handler.post(any()) } answers { 
            postedRunnables.add(firstArg<Runnable>())
            true 
        }
        every { handler.postDelayed(any(), any()) } answers { 
            postedRunnables.add(firstArg<Runnable>())
            true 
        }

        service = MouseHidService(context, hid, handler)
        service.setTestHost(host)
    }

    private fun pumpRunnables() {
        val current = postedRunnables.toList()
        postedRunnables.clear()
        current.forEach { it.run() }
    }

    @Test
    fun `manual move sends correct HID packet`() {
        service.sendManualMove(10, -20)
        val expected = byteArrayOf(0, 10, -20, 0)
        verify { hid.sendReport(host, 0, match { it.contentEquals(expected) }) }
    }

    @Test
    fun `recording captures multiple HID events`() {
        every { SystemClock.elapsedRealtime() } returns 1000L
        service.toggleRecording()

        every { SystemClock.elapsedRealtime() } returns 1100L
        service.sendManualMove(5, 5)

        every { SystemClock.elapsedRealtime() } returns 1250L
        service.setButtonState(0x01, true)

        var finishedData: String? = null
        service.setOnRecordingFinishedListener { finishedData = it }
        
        every { SystemClock.elapsedRealtime() } returns 1500L
        service.toggleRecording()

        val expected = "100:0,5,5,0;250:1,0,0,0"
        assertEquals(expected, finishedData)
    }

    @Test
    fun `playback triggers packets through handler`() {
        val data = "100:0,10,10,0;300:1,0,0,0"
        service.togglePlayback(data, false)
        
        // Triggers runEvents() which posts to handler
        assertEquals(3, postedRunnables.size) // 2 events + 1 end-of-playback check
        
        // Execute first event (100ms)
        postedRunnables[0].run()
        verify { hid.sendReport(host, 0, match { it.contentEquals(byteArrayOf(0, 10, 10, 0)) }) }
    }

    @Test
    fun `autoclicker start sends correct button report`() {
        val profile = AutomationConfig("Test", 100, 100, 50, 50, 1000, 1000, 100)
        service.setConfig(profile)
        
        service.toggleAutomation()
        
        // autoRunnable is posted
        assertEquals(1, postedRunnables.size)
        
        // Run autoRunnable -> sends click down -> posts delayed click up
        pumpRunnables()
        verify { hid.sendReport(host, 0, match { it[0] == 0x01.toByte() }) }
        
        // Run click up task -> sends click up -> posts delayed next click
        assertEquals(1, postedRunnables.size)
        pumpRunnables()
        verify { hid.sendReport(host, 0, match { it[0] == 0x00.toByte() }) }
    }

    @Test
    fun `scroll sends correct HID delta`() {
        service.sendManualScroll(-1) // Down
        verify { hid.sendReport(host, 0, match { it[3] == (-1).toByte() }) }
    }
}
