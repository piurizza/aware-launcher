package app.aware.launcher.gestures

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import app.aware.launcher.R
import kotlin.math.abs

/**
 * Detects a downward fling anywhere on the home screen. [canTrigger] lets the screen veto it, for
 * example while the app list is scrolled down and the fling should scroll it back up instead.
 */
class SwipeDownDetector(context: Context, private val canTrigger: () -> Boolean, private val onSwipeDown: () -> Unit) :
    GestureDetector.SimpleOnGestureListener() {

    private val detector = GestureDetector(context, this)
    private val minDistance = context.resources.getDimensionPixelSize(R.dimen.touch_target)
    private val minVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity

    /** Feed every touch event of the screen; never consumes them. */
    fun onTouchEvent(event: MotionEvent) {
        detector.onTouchEvent(event)
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val triggered = e1 != null && isMostlyDown(e1, e2) && velocityY > minVelocity && canTrigger()
        if (triggered) onSwipeDown()
        return triggered
    }

    private fun isMostlyDown(start: MotionEvent, end: MotionEvent): Boolean {
        val dy = end.y - start.y
        val dx = end.x - start.x
        return dy > minDistance && dy > abs(dx) * 2
    }
}
