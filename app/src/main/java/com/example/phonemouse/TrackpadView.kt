package com.example.phonemouse

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.*

/** Custom View for mouse input. Handles rendering (trail) while delegating logic to [TrackpadManager]. */
class TrackpadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    var mode: String
        get() = manager.mode
        set(v) { manager.mode = v; resetIcon() }
    var isTrailEnabled = true
        set(v) { field = v; if (!v) { trail.clear(); invalidate() } }
    var trackpadSensitivity: Float
        get() = manager.trackpadSensitivity
        set(v) { manager.trackpadSensitivity = v }
    var trackpadAcceleration: Float
        get() = manager.trackpadAcceleration
        set(v) { manager.trackpadAcceleration = v }
    var trackpointSensitivity: Float
        get() = manager.trackpointSensitivity
        set(v) { manager.trackpointSensitivity = v }
    var trackpointCurve: String
        get() = manager.trackpointCurve
        set(v) { manager.trackpointCurve = v }
    var isTrackpointAnimationEnabled: Boolean
        get() = manager.isTrackpointAnimationEnabled
        set(v) { manager.isTrackpointAnimationEnabled = v; if (!v) resetIcon() }

    private val manager = TrackpadManager(
        onMove = { dx, dy -> onMove?.invoke(dx, dy) }
    ) { x, y ->
        icon?.translationX = x
        icon?.translationY = y
    }

    private var onMove: ((Int, Int) -> Unit)? = null
    private val icon by lazy { findViewById<ImageView>(R.id.trackpointIcon) }
    private val trail = mutableListOf<Point>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private data class Point(val x: Float, val y: Float, var a: Int = 200, var r: Float = 20f)

    init {
        setWillNotDraw(false)
        val tv = android.util.TypedValue()
        context.theme.resolveAttribute(R.attr.controlIconColor, tv, true)
        paint.color = tv.data
    }

    fun setOnMoveListener(l: (Int, Int) -> Unit) { onMove = l }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        manager.onSizeChanged(w, h)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val result = manager.onTouch(e)
        if ((e.actionMasked == MotionEvent.ACTION_DOWN) || (e.actionMasked == MotionEvent.ACTION_POINTER_DOWN)) {
            performClick()
        }
        if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) {
            resetIcon()
        }
        // Add trail points
        if (isTrailEnabled && mode == "Trackpad") {
            for (i in 0 until e.pointerCount) {
                trail.add(Point(e.getX(i), e.getY(i)))
            }
            invalidate()
        }
        return result
    }

    private fun resetIcon() { icon?.animate()?.translationX(0f)?.translationY(0f)?.setDuration(200)?.start() }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        if (!isTrailEnabled || mode != "Trackpad") { trail.clear(); return }
        super.onDraw(canvas)
        val it = trail.iterator()
        while (it.hasNext()) {
            val p = it.next(); p.a -= 10; p.r *= 0.95f
            if (p.a <= 0 || p.r < 1f) { it.remove(); continue }
            paint.alpha = p.a; canvas.drawCircle(p.x, p.y, p.r, paint)
        }
        if (trail.isNotEmpty()) postInvalidateOnAnimation()
    }
}