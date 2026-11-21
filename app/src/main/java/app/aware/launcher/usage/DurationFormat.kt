package app.aware.launcher.usage

import java.util.concurrent.TimeUnit

/** Compact, glanceable durations: "0m", "42m", "1h 05m". */
object DurationFormat {

    private const val MINUTES_PER_HOUR = 60L

    /** Below this an app's usage is not shown at all, to keep the list quiet. */
    val VISIBLE_THRESHOLD_MS: Long = TimeUnit.MINUTES.toMillis(1)

    fun format(millis: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis.coerceAtLeast(0L))
        val hours = totalMinutes / MINUTES_PER_HOUR
        val minutes = totalMinutes % MINUTES_PER_HOUR
        return if (hours == 0L) "${minutes}m" else "${hours}h ${minutes.toString().padStart(2, '0')}m"
    }

    /** Usage label for a single app row, empty when the app was barely used. */
    fun forRow(millis: Long): String = if (millis >= VISIBLE_THRESHOLD_MS) format(millis) else ""
}
