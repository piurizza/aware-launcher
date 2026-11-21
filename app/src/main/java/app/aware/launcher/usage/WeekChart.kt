package app.aware.launcher.usage

import kotlin.math.roundToInt

/** One day of the 7-day view; [totalMillis] is null when Android kept no usage data for that day. */
data class DayTotal(val label: String, val totalMillis: Long?)

/**
 * Renders the 7-day view as plain monospace text lines, so it needs no chart library:
 *
 * ```
 * Sat 21  ███████       2h 14m
 * Fri 20  ████████████  3h 51m
 * ```
 */
object WeekChart {

    private const val BAR = '█'
    private const val NO_DATA = "—"

    fun lines(days: List<DayTotal>, barWidth: Int = DEFAULT_BAR_WIDTH): List<String> {
        val max = days.mapNotNull { it.totalMillis }.maxOrNull()?.takeIf { it > 0 } ?: 1L
        val labelWidth = days.maxOfOrNull { it.label.length } ?: 0
        return days.map { day ->
            val label = day.label.padEnd(labelWidth)
            val total = day.totalMillis
            if (total == null) {
                "$label  ${"".padEnd(barWidth)}  $NO_DATA"
            } else {
                val length = (total.toDouble() / max * barWidth).roundToInt()
                val bar = BAR.toString().repeat(length).padEnd(barWidth)
                "$label  $bar  ${DurationFormat.format(total)}"
            }
        }
    }

    /** Mean over the days that have data, or null when none do. */
    fun average(days: List<DayTotal>): Long? {
        val totals = days.mapNotNull { it.totalMillis }
        return if (totals.isEmpty()) null else totals.sum() / totals.size
    }

    private const val DEFAULT_BAR_WIDTH = 12
}
