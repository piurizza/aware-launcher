package app.aware.launcher.pause

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import app.aware.launcher.R
import app.aware.launcher.apps.AppEntry
import app.aware.launcher.usage.DurationFormat
import app.aware.launcher.usage.Limit
import java.util.concurrent.TimeUnit

/**
 * A short pause before opening an app marked as distracting: today's time and opens, and an Open
 * button that only becomes active after a few seconds. It never blocks the app, it just breaks the
 * autopilot of opening it.
 */
object PauseDialog {

    const val DELAY_SECONDS = 3

    fun show(activity: Activity, app: AppEntry, limit: Limit?, onOpen: () -> Unit) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle(app.label)
            .setMessage(message(activity, app, limit))
            .setPositiveButton(R.string.pause_open) { _, _ -> onOpen() }
            .setNegativeButton(R.string.pause_not_now, null)
            .create()

        val handler = Handler(Looper.getMainLooper())
        dialog.setOnShowListener { countDown(activity, dialog, handler, DELAY_SECONDS) }
        dialog.setOnDismissListener { handler.removeCallbacksAndMessages(null) }
        dialog.show()
    }

    private fun countDown(activity: Activity, dialog: AlertDialog, handler: Handler, secondsLeft: Int) {
        val open = dialog.getButton(DialogInterface.BUTTON_POSITIVE) ?: return
        if (secondsLeft <= 0) {
            open.isEnabled = true
            open.setText(R.string.pause_open)
        } else {
            open.isEnabled = false
            open.text = activity.getString(R.string.pause_open_countdown, secondsLeft)
            handler.postDelayed(
                { countDown(activity, dialog, handler, secondsLeft - 1) },
                TimeUnit.SECONDS.toMillis(1)
            )
        }
    }

    private fun message(activity: Activity, app: AppEntry, limit: Limit?): String {
        val question = activity.getString(R.string.pause_question)
        // Android keeps work-profile usage separately, so there are no numbers to show for those apps.
        if (app.isWorkProfile) return question
        val time = activity.getString(R.string.pause_time, DurationFormat.format(app.usage.foregroundMillis))
        val opens = activity.resources.getQuantityString(R.plurals.pause_opens, app.usage.opens, app.usage.opens)
        val lines = mutableListOf(time, opens)
        if (limit != null) lines += limitLine(activity, limit)
        return lines.joinToString("\n") + "\n\n" + question
    }

    private fun limitLine(activity: Activity, limit: Limit): String {
        val max = DurationFormat.format(limit.limitMillis)
        val difference = DurationFormat.format(limit.differenceMillis)
        val text = if (limit.isReached) R.string.pause_limit_over else R.string.pause_limit_left
        return activity.getString(text, max, difference)
    }
}
