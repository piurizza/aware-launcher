package app.aware.launcher.usage

import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** A daily time limit for one app, and where today's usage stands against it. */
data class Limit(val limitMillis: Long, val usedMillis: Long) {

    val isReached: Boolean get() = usedMillis >= limitMillis

    /** Time left before the limit, or time spent past it once [isReached]. */
    val differenceMillis: Long get() = abs(limitMillis - usedMillis)

    /** Row label with the limit: "12m / 30m". */
    fun rowLabel(): String = "${DurationFormat.format(usedMillis)} / ${DurationFormat.format(limitMillis)}"

    companion object {
        /** Choices offered in the menu, in minutes; 0 means no limit. */
        val CHOICES_MINUTES = listOf(0, 15, 30, 45, 60, 90, 120, 180)

        fun minutesToMillis(minutes: Int): Long = TimeUnit.MINUTES.toMillis(minutes.toLong())
    }
}
