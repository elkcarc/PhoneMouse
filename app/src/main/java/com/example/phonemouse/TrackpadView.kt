package com.example.phonemouse

import android.content.Context
import android.graphics.*
import android.os.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.*

/** Custom View for mouse input. Supports relative trackpad and absolute trackpoint modes. */
class TrackpadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    /** Toggle between standard relative "Trackpad" and absolute "Trackpoint" movement. */
    var mode = "Trackpad"
        set(v) { field = v; stopLoop(); resetIcon() }
    /** Toggles the rendering of the fading touch trail (Relative mode only). */
    var isTrailEnabled = true
        set(v) { field = v; if (!v) { trail.clear(); invalidate() } }
    /** Speed multiplier for Relative Trackpad mode. */
    var trackpadSensitivity = 1.0f
    /** Speed multiplier for Absolute Trackpoint mode. */
    var trackpointSensitivity = 1.0f
    /** Toggles the central icon animation in Trackpoint mode. */
    var isTrackpointAnimationEnabled = true
        set(v) { field = v; if (!v) resetIcon() }

    private var onMove: ((Int, Int) -> Unit)? = null
    private var lx = 0f; private var ly = 0f
    private var pid = MotionEvent.INVALID_POINTER_ID
    private val handler = Handler(Looper.getMainLooper())
    private var tx = 0f; private var ty = 0f
    private val icon by lazy { findViewById<ImageView>(R.id.trackpointIcon) }

    /** Hardware-timed loop for continuous cursor movement in Trackpoint mode. */
    private val loop = object : Runnable {
        override fun run() {
            if ((mode == "Trackpoint") && (pid != MotionEvent.INVALID_POINTER_ID)) {
                val cx = width / 2f; val cy = height / 2f
                val dx = (((tx - cx) / width) * 100 * trackpointSensitivity).toInt()
                val dy = (((ty - cy) / height) * 100 * trackpointSensitivity).toInt()
                if ((dx != 0) || (dy != 0)) onMove?.invoke(dx, dy)
                if (isTrackpointAnimationEnabled) icon?.apply {
                    translationX = (tx - cx).coerceIn(-cx + width / 4, cx - width / 4)
                    translationY = (ty - cy).coerceIn(-cy + height / 4, cy - height / 4)
                }
                handler.postDelayed(this, 16)
            }
        }
    }

    private val trail = mutableListOf<Point>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    /** Represents a single segment of the visual trail. */
    private data class Point(val x: Float, val y: Float, var a: Int = 200, var r: Float = 20f)

    init {
        setWillNotDraw(false)
        val tv = android.util.TypedValue()
        context.theme.resolveAttribute(R.attr.controlIconColor, tv, true)
        paint.color = tv.data
    }

    /** Assigns the callback for mouse movement reports. */
    fun setOnMoveListener(l: (Int, Int) -> Unit) { onMove = l }

    /** Intercepts touch events to track multi-touch and trigger movement logic. */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = e.actionIndex
                if (pid == MotionEvent.INVALID_POINTER_ID) {
                    pid = e.getPointerId(idx); lx = e.getX(idx); ly = e.getY(idx); tx = lx; ty = ly
                    performClick()
                    if (mode == "Trackpoint") { handler.removeCallbacks(loop); handler.post(loop) }
                }
                addPoint(e.getX(idx), e.getY(idx))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (pid != MotionEvent.INVALID_POINTER_ID) {
                    val idx = e.findPointerIndex(pid)
                    if (idx != -1) {
                        val x = e.getX(idx); val y = e.getY(idx)
                        if (mode == "Trackpad") {
                            val dx = ((x - lx) * trackpadSensitivity).toInt()
                            val dy = ((y - ly) * trackpadSensitivity).toInt()
                            if ((dx != 0) || (dy != 0)) { onMove?.invoke(dx, dy); lx = x; ly = y }
                        } else { tx = x; ty = y }
                    }
                }
                for (i in 0 until e.pointerCount) addPoint(e.getX(i), e.getY(i))
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (e.getPointerId(e.actionIndex) == pid || e.action == MotionEvent.ACTION_CANCEL) {
                    pid = MotionEvent.INVALID_POINTER_ID; stopLoop(); resetIcon()
                }
                return true
            }
        }
        return super.onTouchEvent(e)
    }

    /** Terminates the continuous Trackpoint movement loop. */
    private fun stopLoop() = handler.removeCallbacks(loop)
    /** Smoothly returns the trackpoint icon to its central rest position. */
    private fun resetIcon() { icon?.animate()?.translationX(0f)?.translationY(0f)?.setDuration(200)?.start() }
    
    /** Standard accessibility click handler. */
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /** Records a new coordinate for the visual fading trail. */
    private fun addPoint(x: Float, y: Float) { if (isTrailEnabled && mode == "Trackpad") { trail.add(Point(x, y)); invalidate() } }

    /** Renders the touch trail on the canvas. */
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