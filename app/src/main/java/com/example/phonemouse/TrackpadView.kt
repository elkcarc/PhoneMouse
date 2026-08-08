package com.example.phonemouse

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * A custom FrameLayout that acts as a trackpad.
 * It detects touch movements and renders a fading trail animation behind the user's finger.
 */
class TrackpadView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** Operation mode: "Trackpad" (relative) or "Trackpoint" (absolute displacement). */
    var mode: String = "Trackpad"
        set(value) {
            field = value
            stopTrackpointLoop()
            resetIconPosition()
        }

    /** Whether the trail animation should be rendered. */
    var isTrailEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                trailPoints.clear()
                invalidate()
            }
        }

    /** The sensitivity multiplier for standard relative movement. */
    var trackpadSensitivity: Float = 1.0f

    /** The sensitivity multiplier for absolute trackpoint movement. */
    var trackpointSensitivity: Float = 1.0f

    /** Whether the trackpoint icon should animate toward the finger. */
    var isTrackpointAnimationEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) resetIconPosition()
        }

    private var onMoveListener: ((Int, Int) -> Unit)? = null
    private var lastX = 0f
    private var lastY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    // Trackpoint specific
    private val handler = Handler(Looper.getMainLooper())
    private var trackpointX = 0f
    private var trackpointY = 0f
    private val trackpointIcon: ImageView? by lazy { findViewById(R.id.trackpointIcon) }

    private val trackpointRunnable = object : Runnable {
        override fun run() {
            if (mode == "Trackpoint" && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                // Calculate normalized displacement from center
                val centerX = width / 2f
                val centerY = height / 2f
                
                val dx = ((trackpointX - centerX) / width * 100 * trackpointSensitivity).toInt()
                val dy = ((trackpointY - centerY) / height * 100 * trackpointSensitivity).toInt()
                
                if (dx != 0 || dy != 0) {
                    onMoveListener?.invoke(dx, dy)
                }
                
                // Update icon position to match finger (clamped to view)
                if (isTrackpointAnimationEnabled) {
                    trackpointIcon?.apply {
                        translationX = (trackpointX - centerX).coerceIn(-centerX + width/4, centerX - width/4)
                        translationY = (trackpointY - centerY).coerceIn(-centerY + height/4, centerY - height/4)
                    }
                }
                
                handler.postDelayed(this, 16) // ~60fps
            }
        }
    }

    private val trailPoints = mutableListOf<TrailPoint>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /** Data class to track the position and lifespan of a trail segment. */
    private data class TrailPoint(val x: Float, val y: Float, var alpha: Int = 200, var radius: Float = 20f)

    init {
        // Required for FrameLayout to call onDraw
        setWillNotDraw(false)
        
        // Use theme attribute for trail color if possible, fallback to primary
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(R.attr.controlIconColor, typedValue, true)
        paint.color = typedValue.data
    }

    /**
     * Sets a callback to be invoked when a movement is detected.
     */
    fun setOnMoveListener(listener: (Int, Int) -> Unit) {
        onMoveListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                
                // If we don't have an active pointer, take this one
                if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                    activePointerId = event.getPointerId(pointerIndex)
                    lastX = x
                    lastY = y
                    trackpointX = x
                    trackpointY = y
                    performClick()
                    
                    if (mode == "Trackpoint") {
                        startTrackpointLoop()
                    }
                }
                
                addTrailPoint(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        
                        if (mode == "Trackpad") {
                            val dx = ((x - lastX) * trackpadSensitivity).toInt()
                            val dy = ((y - lastY) * trackpadSensitivity).toInt()
                            
                            if (dx != 0 || dy != 0) {
                                onMoveListener?.invoke(dx, dy)
                                lastX = x
                                lastY = y
                            }
                        } else {
                            // Update values for the loop
                            trackpointX = x
                            trackpointY = y
                        }
                    }
                }
                
                // Add trail points for all active pointers
                for (i in 0 until event.pointerCount) {
                    addTrailPoint(event.getX(i), event.getY(i))
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (pointerId == activePointerId) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                    stopTrackpointLoop()
                    resetIconPosition()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                stopTrackpointLoop()
                resetIconPosition()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startTrackpointLoop() {
        handler.removeCallbacks(trackpointRunnable)
        handler.post(trackpointRunnable)
    }

    private fun stopTrackpointLoop() {
        handler.removeCallbacks(trackpointRunnable)
    }

    private fun resetIconPosition() {
        trackpointIcon?.animate()?.translationX(0f)?.translationY(0f)?.setDuration(200)?.start()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun addTrailPoint(x: Float, y: Float) {
        if (isTrailEnabled) {
            trailPoints.add(TrailPoint(x, y))
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val iterator = trailPoints.iterator()
        while (iterator.hasNext()) {
            val point = iterator.next()
            
            // Age the point first: shrink and fade
            point.alpha -= 10
            point.radius *= 0.95f

            // If the point has expired, remove it and skip drawing
            if (point.alpha <= 0 || point.radius < 1f) {
                iterator.remove()
                continue
            }

            paint.alpha = point.alpha
            canvas.drawCircle(point.x, point.y, point.radius, paint)
        }

        // If there are still active points, keep animating
        if (trailPoints.isNotEmpty()) {
            postInvalidateOnAnimation()
        }
    }
}