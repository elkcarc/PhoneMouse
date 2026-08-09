package com.elk.phonemouse

import android.view.Choreographer
import android.view.MotionEvent
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import kotlin.math.*

/** Logic provider for both Relative (Trackpad) and Absolute (Trackpoint) modes. */
class TrackpadManager(
    private val onMove: (Int, Int) -> Unit,
    private val onScroll: (Int) -> Unit,
    private val onButtonClick: (Byte, Boolean) -> Unit,
    private val onUpdateAnimation: (Float, Float) -> Unit,
) : Choreographer.FrameCallback {
    var mode = "Trackpad"
        set(value) { field = value; stopLoop() }
    var trackpadSensitivity = 1.0f
    var trackpointSensitivity = 1.0f
    var isTrackpointAnimationEnabled = true
    var isTwoFingerScrollEnabled = true
    var isTapToClickEnabled = true
    var isDoubleTapToRightClickEnabled = true
    
    /** Ballistics acceleration exponent (1.0 = linear/disabled). */
    var trackpadAcceleration = 1.0f
    /** Mapping strategy for trackpoint distance: "Linear", "Quadratic", "Cubic". */
    var trackpointCurve = "Linear"

    private var lx = 0f
    private var ly = 0f
    private var tx = 0f
    private var ty = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private var pointerId = MotionEvent.INVALID_POINTER_ID
    private var secondPointerId = MotionEvent.INVALID_POINTER_ID
    
    // Tap detection
    private var lastTapTime = 0L
    private var downX = 0f
    private var downY = 0f
    private val tapThreshold = 15f
    private val tapTimeout = 200L
    private val doubleTapTimeout = 300L

    // Scroll accumulation
    private var remScrollY = 0f
    private var lastSecondY = 0f
    
    // Sub-pixel Accumulation
    private var remX = 0f
    private var remY = 0f
    
    // Exponential Smoothing (Low-pass Filter)
    private var lastRawDx = 0f
    private var lastRawDy = 0f
    private val smoothAlpha = 0.75f // Balance between jitter reduction and snapiness

    /** Precison frame callback for continuous Trackpoint movement. */
    override fun doFrame(frameTimeNanos: Long) {
        if ((mode == "Trackpoint") && (pointerId != MotionEvent.INVALID_POINTER_ID)) {
            val cx = viewWidth / 2f
            val cy = viewHeight / 2f
            
            // Normalized distance from center (-1.0 to 1.0)
            val nx = (tx - cx) / cx
            val ny = (ty - cy) / cy
            
            // Apply curve ballistics
            val curveX = applyCurve(nx, trackpointCurve)
            val curveY = applyCurve(ny, trackpointCurve)
            
            val dx = (curveX * 25 * trackpointSensitivity).toInt()
            val dy = (curveY * 25 * trackpointSensitivity).toInt()
            
            if ((dx != 0) || (dy != 0)) onMove(dx, dy)
            if (isTrackpointAnimationEnabled) {
                // Animation follows raw touch position linearly, ignoring the movement curve
                onUpdateAnimation(nx * (viewWidth / 4f), ny * (viewHeight / 4f))
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun applyCurve(input: Float, type: String): Float {
        val sign = sign(input)
        val abs = abs(input).toDouble()
        return (sign * when (type) {
            "Quadratic" -> abs.pow(2.0)
            "Cubic" -> abs.pow(3.0)
            else -> abs // Linear
        }).toFloat()
    }

    fun onSizeChanged(w: Int, h: Int) {
        viewWidth = w
        viewHeight = h
    }

    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val idx = event.actionIndex
                pointerId = event.getPointerId(idx)
                lx = event.getX(idx)
                ly = event.getY(idx)
                downX = lx
                downY = ly
                tx = lx
                ty = ly
                lastRawDx = 0f
                lastRawDy = 0f
                remX = 0f
                remY = 0f
                if (mode == "Trackpoint") {
                    Choreographer.getInstance().removeFrameCallback(this)
                    Choreographer.getInstance().postFrameCallback(this)
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (isTwoFingerScrollEnabled && (mode == "Trackpad") && (secondPointerId == MotionEvent.INVALID_POINTER_ID)) {
                    val idx = event.actionIndex
                    secondPointerId = event.getPointerId(idx)
                    lastSecondY = event.getY(idx)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerId != MotionEvent.INVALID_POINTER_ID) {
                    val idx = event.findPointerIndex(pointerId)
                    if (idx != -1) {
                        val x = event.getX(idx)
                        val y = event.getY(idx)
                        
                        if (isTwoFingerScrollEnabled && (secondPointerId != MotionEvent.INVALID_POINTER_ID) && (mode == "Trackpad")) {
                            val sIdx = event.findPointerIndex(secondPointerId)
                            if (sIdx != -1) {
                                val sy = event.getY(sIdx)
                                val dy1 = y - ly
                                val dy2 = sy - lastSecondY
                                val avgDy = (dy1 + dy2) / 2f
                                
                                val scrollVal = (avgDy / 10f) + remScrollY
                                val outScroll = scrollVal.toInt()
                                if (outScroll != 0) {
                                    onScroll(outScroll)
                                }
                                remScrollY = scrollVal - outScroll
                                
                                lastSecondY = sy
                                lx = x
                                ly = y
                                return true
                            }
                        }

                        if (mode == "Trackpad") {
                            // 1. Raw movement
                            val rawDx = x - lx
                            val rawDy = y - ly
                            
                            // 2. Exponential Smoothing (De-jitter)
                            val smoothDx = (rawDx * smoothAlpha) + (lastRawDx * (1 - smoothAlpha))
                            val smoothDy = (rawDy * smoothAlpha) + (lastRawDy * (1 - smoothAlpha))
                            lastRawDx = smoothDx
                            lastRawDy = smoothDy
                            
                            // 3. Velocity-based Acceleration
                            val velocity = sqrt(smoothDx.pow(2) + smoothDy.pow(2))
                            val accelFactor = if (trackpadAcceleration > 1.0f) {
                                velocity.toDouble().pow((trackpadAcceleration - 1.0).coerceAtLeast(0.0)).toFloat()
                            } else 1.0f
                            
                            // 4. Sub-pixel Accumulation
                            val moveX = (smoothDx * trackpadSensitivity * accelFactor) + remX
                            val moveY = (smoothDy * trackpadSensitivity * accelFactor) + remY
                            
                            val outX = moveX.toInt()
                            val outY = moveY.toInt()
                            
                            if ((outX != 0) || (outY != 0)) {
                                onMove(outX, outY)
                            }
                            remX = moveX - outX
                            remY = moveY - outY
                            
                            lx = x
                            ly = y
                        } else {
                            tx = x
                            ty = y
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == secondPointerId) {
                    secondPointerId = MotionEvent.INVALID_POINTER_ID
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.getPointerId(event.actionIndex) == pointerId || event.action == MotionEvent.ACTION_CANCEL) {
                    if (event.action == MotionEvent.ACTION_UP && mode == "Trackpad") {
                        val x = event.getX(event.actionIndex)
                        val y = event.getY(event.actionIndex)
                        val dist = sqrt((x - downX).pow(2) + (y - downY).pow(2))
                        val time = event.eventTime - event.downTime
                        
                        if (dist < tapThreshold && time < tapTimeout) {
                            val now = SystemClock.elapsedRealtime()
                            if (isDoubleTapToRightClickEnabled && now - lastTapTime < doubleTapTimeout) {
                                // Double tap -> Right click
                                onButtonClick(0x02.toByte(), true)
                                Handler(Looper.getMainLooper()).postDelayed({ onButtonClick(0x02.toByte(), false) }, 50)
                                lastTapTime = 0
                            } else if (isTapToClickEnabled) {
                                // Single tap -> Left click
                                onButtonClick(0x01.toByte(), true)
                                Handler(Looper.getMainLooper()).postDelayed({ onButtonClick(0x01.toByte(), false) }, 50)
                                lastTapTime = now
                            }
                        }
                    }
                    pointerId = MotionEvent.INVALID_POINTER_ID
                    secondPointerId = MotionEvent.INVALID_POINTER_ID
                    stopLoop()
                }
                return true
            }
        }
        return false
    }

    fun stopLoop() = Choreographer.getInstance().removeFrameCallback(this)
}