package app.aware.launcher.gestures

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log

/**
 * Pulls down the notification shade, like swiping down on most launchers' home screens.
 *
 * Android has no public API for this, so it calls the status bar service's
 * expandNotificationsPanel() by reflection (the same approach other launchers use; it needs the
 * normal EXPAND_STATUS_BAR permission). If a future Android version blocks it, the swipe simply
 * does nothing and the shade is still one swipe from the top edge away.
 */
object NotificationShade {

    private const val TAG = "Aware"

    @SuppressLint("WrongConstant", "PrivateApi", "DiscouragedPrivateApi")
    fun expand(context: Context) {
        try {
            val statusBar = context.getSystemService("statusbar") ?: return
            statusBar.javaClass.getMethod("expandNotificationsPanel").invoke(statusBar)
        } catch (e: ReflectiveOperationException) {
            Log.w(TAG, "Cannot open the notification shade", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to open the notification shade", e)
        }
    }
}
