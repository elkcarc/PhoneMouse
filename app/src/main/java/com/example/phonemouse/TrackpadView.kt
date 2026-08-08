package com.example.phonemouse

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * A custom FrameLayout that acts as a trackpad.
 * It detects touch movements and renders a fading trail animation behind the user's finger.
 */
class TrackpadView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** Whether the trail animation should be rendered. */
    var isTrailEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                trailPoints.clear()
                invalidate()
            }
        }

    private var onMoveListener: ((Int, Int) -> Unit)? = null
    private var lastX = 0f
    private var lastY = 0f

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
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                performClick()
                lastX = event.x
                lastY = event.y
                addTrailPoint(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.x - lastX).toInt()
                val dy = (event.y - lastY).toInt()
                
                if (dx != 0 || dy != 0) {
                    onMoveListener?.invoke(dx, dy)
                    lastX = event.x
                    lastY = event.y
                }
                
                addTrailPoint(event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
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