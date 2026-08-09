package com.example.phonemouse

import android.view.Choreographer
import android.view.MotionEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TrackpadManagerTest {
    private lateinit var manager: TrackpadManager
    private var lastDx: Int = 0
    private var lastDy: Int = 0
    private var lastAnimX: Float = 0f
    private var lastAnimY: Float = 0f

    @Before
    fun setup() {
        mockkStatic(Choreographer::class)
        val choreographer = mockk<Choreographer>(relaxed = true)
        every { Choreographer.getInstance() } returns choreographer

        manager = TrackpadManager(
            onMove = { dx, dy -> 
                lastDx = dx
                lastDy = dy
            },
            onScroll = { _ -> },
            onButtonClick = { _, _ -> },
        ) { x, y ->
            lastAnimX = x
            lastAnimY = y
        }
        manager.onSizeChanged(1000, 1000)
    }

    /**
     * Purpose: Verify the responsiveness of relative trackpad movement including smoothing.
     * Before State: Trackpad mode active, sensitivity at 1.0.
     * During Test: Injects ACTION_MOVE event after a DOWN event.
     * After State: verification that raw delta is smoothed (e.g. 10px raw -> 7px smoothed).
     */
    @Test
    fun `trackpad mode smoothed movement`() {
        manager.mode = "Trackpad"
        manager.trackpadSensitivity = 1.0f
        manager.trackpadAcceleration = 1.0f

        // Down at 500,500
        manager.onTouch(createMotionEvent(MotionEvent.ACTION_DOWN, 500f, 500f))

        // Move to 510, 520. Raw Delta = 10, 20.
        // Smoothed Delta (alpha=0.75) = (10 * 0.75) + (0 * 0.25) = 7.5 -> 7
        manager.onTouch(createMotionEvent(MotionEvent.ACTION_MOVE, 510f, 520f))

        assertEquals(7, lastDx)
        assertEquals(15, lastDy)
    }

    /**
     * Purpose: Verify that trackpoint mode correctly calculates velocity based on distance from center.
     * Before State: Trackpoint mode active, Linear curve selected.
     * During Test: Places touch 100 pixels away from the 500px center.
     * After State: verification that a velocity of 5px is generated (normalized distance 0.2 * base 25).
     */
    @Test
    fun `trackpoint mode linear curve`() {
        manager.mode = "Trackpoint"
        manager.trackpointSensitivity = 1.0f
        manager.trackpointCurve = "Linear"

        // Center at 500,500. Touch at 600, 500.
        // nx = (600 - 500) / 500 = 0.2
        // dx = 0.2 * 25 * 1.0 = 5
        manager.onTouch(createMotionEvent(MotionEvent.ACTION_DOWN, 600f, 500f))
        manager.doFrame(0)

        assertEquals(5, lastDx)
        assertEquals(0, lastDy)
        // Animation follows touch linearly: 0.2 * 250 = 50
        assertEquals(50f, lastAnimX)
    }

    /**
     * Purpose: Verify that trackpoint mode correctly calculates quadratic velocity.
     * Before State: Trackpoint mode active, Quadratic curve selected.
     * During Test: Places touch 100 pixels away from the 500px center (nx=0.2).
     * After State: Verification that a velocity of 1px is generated (0.2^2 * 25).
     */
    @Test
    fun `trackpoint mode quadratic curve`() {
        manager.mode = "Trackpoint"
        manager.trackpointSensitivity = 1.0f
        manager.trackpointCurve = "Quadratic"

        // Touch at 600, 500 -> nx = 0.2
        // curveX = 0.2^2 = 0.04
        // dx = 0.04 * 25 = 1
        manager.onTouch(createMotionEvent(MotionEvent.ACTION_DOWN, 600f, 500f))
        manager.doFrame(0)

        assertEquals(1, lastDx)
        // Animation is STILL linear: 0.2 * 250 = 50
        assertEquals(50f, lastAnimX)
    }

    /**
     * Purpose: Verify that trackpoint mode correctly calculates cubic velocity.
     * Before State: Trackpoint mode active, Cubic curve selected.
     * During Test: Places touch 250 pixels away from center (nx=0.5).
     * After State: Verification that a velocity of 3px is generated (0.5^3 * 25 = 3.125).
     */
    @Test
    fun `trackpoint mode cubic curve`() {
        manager.mode = "Trackpoint"
        manager.trackpointSensitivity = 1.0f
        manager.trackpointCurve = "Cubic"

        // Touch at 750, 500 -> nx = 0.5
        // curveX = 0.5^3 = 0.125
        // dx = 0.125 * 25 = 3.125 -> 3
        manager.onTouch(createMotionEvent(MotionEvent.ACTION_DOWN, 750f, 500f))
        manager.doFrame(0)

        assertEquals(3, lastDx)
        assertEquals(125f, lastAnimX) // 0.5 * 250
    }

    /**
     * Purpose: Verify that trackpad mode acceleration scales delta correctly.
     * Before State: Trackpad mode active, acceleration set to 1.5.
     * During Test: Moves 100 pixels (smooth=75).
     * After State: Verification that moveX is approx 649 (75 * 75^0.5).
     */
    @Test
    fun `trackpad mode acceleration`() {
        manager.mode = "Trackpad"
        manager.trackpadSensitivity = 1.0f
        manager.trackpadAcceleration = 1.5f // Delta ^ 0.5 acceleration factor (approx)

        manager.onTouch(createMotionEvent(MotionEvent.ACTION_DOWN, 500f, 500f))
        
        // Move 100 pixels. smoothDx = 75. velocity = 75. 
        // accelFactor = 75 ^ (1.5 - 1) = 75 ^ 0.5 = 8.66
        // moveX = 75 * 1.0 * 8.66 = 649.5 -> 649
        manager.onTouch(createMotionEvent(MotionEvent.ACTION_MOVE, 600f, 500f))
        
        assertEquals(649, lastDx)
    }

    /**
     * Purpose: Verify that sensitivity multiplier applies to smoothed trackpad movement.
     * Before State: Trackpad mode active, sensitivity at 2.0.
     * During Test: Injects ACTION_MOVE (10, 20).
     * After State: Verification that smoothed delta (7.5, 15) is doubled to (15, 30).
     */
    @Test
    fun `trackpad mode with sensitivity and smoothing`() {
        manager.mode = "Trackpad"
        manager.trackpadSensitivity = 2.0f
        manager.trackpadAcceleration = 1.0f

        manager.onTouch(createMotionEvent(MotionEvent.ACTION_DOWN, 500f, 500f))
        
        // Raw Delta = 10, 20. Smooth = 7.5, 15.0. 
        // With Sensitivity 2.0 = 15.0, 30.0
        manager.onTouch(createMotionEvent(MotionEvent.ACTION_MOVE, 510f, 520f))

        assertEquals(15, lastDx)
        assertEquals(30, lastDy)
    }

    /**
     * Purpose: Verify sub-pixel accumulation allows fine movement at low sensitivity.
     * Before State: Trackpad mode active, sensitivity at 0.1.
     * During Test: Performs two consecutive 10px moves.
     * After State: Verification that first move generates 0px and second generates 1px.
     */
    @Test
    fun `trackpad sub-pixel accumulation with smoothing`() {
        manager.mode = "Trackpad"
        manager.trackpadSensitivity = 0.1f // Very low sensitivity
        manager.trackpadAcceleration = 1.0f

        manager.onTouch(createMotionEvent(MotionEvent.ACTION_DOWN, 500f, 500f))
        
        // Raw Move 10 pixels. Smooth = 7.5. Out = 7.5 * 0.1 = 0.75 -> 0. Remainder 0.75
        manager.onTouch(createMotionEvent(MotionEvent.ACTION_MOVE, 510f, 500f))
        assertEquals(0, lastDx)

        // Raw Move another 10 pixels (lx was 510). 
        // Smooth = (10 * 0.75) + (7.5 * 0.25) = 7.5 + 1.875 = 9.375
        // Move = (9.375 * 0.1) + 0.75 = 0.9375 + 0.75 = 1.6875 -> 1. Remainder 0.6875
        manager.onTouch(createMotionEvent(MotionEvent.ACTION_MOVE, 520f, 500f))
        assertEquals(1, lastDx)
    }

    private fun createMotionEvent(action: Int, x: Float, y: Float): MotionEvent {
        val event = mockk<MotionEvent>()
        every { event.actionMasked } returns action
        every { event.actionIndex } returns 0
        every { event.getPointerId(0) } returns 0
        every { event.findPointerIndex(0) } returns 0
        every { event.getX(0) } returns x
        every { event.getY(0) } returns y
        every { event.pointerCount } returns 1
        return event
    }
}
