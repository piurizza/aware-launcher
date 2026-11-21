package app.aware.launcher

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.widget.TextView
import app.aware.launcher.usage.DayTotal
import app.aware.launcher.usage.DurationFormat
import app.aware.launcher.usage.WeekChart

/** The last 7 days as a plain-text bar chart. */
object WeekDialog {

    fun show(activity: Activity, days: List<DayTotal>) {
        val lines = WeekChart.lines(days).toMutableList()
        WeekChart.average(days)?.let { average ->
            lines += ""
            lines += activity.getString(R.string.week_average, DurationFormat.format(average))
        }

        val padding = activity.resources.getDimensionPixelSize(R.dimen.screen_margin)
        val chart = TextView(activity).apply {
            text = lines.joinToString("\n")
            typeface = Typeface.MONOSPACE
            setTextColor(activity.getColor(R.color.text_primary))
            setPadding(padding, padding, padding, 0)
            setTextIsSelectable(false)
        }

        AlertDialog.Builder(activity)
            .setTitle(R.string.week_title)
            .setView(chart)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
