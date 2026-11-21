package app.aware.launcher.usage

import java.util.concurrent.TimeUnit

/** How long an app has gone unopened: a hint that it could be uninstalled. */
object Unused {

    /** Apps opened within this many days are not flagged. */
    const val THRESHOLD_DAYS = 30

    private const val DAYS_PER_YEAR = 365

    /**
     * Days since the app was last on screen, or null when it was opened recently or today.
     * Apps never opened count from their install date, so a new app is not flagged right away.
     */
    fun days(lastVisible: Long?, installedAt: Long, now: Long, usedToday: Boolean): Int? {
        if (usedToday) return null
        val lastSeen = maxOf(lastVisible ?: 0L, installedAt)
        val days = TimeUnit.MILLISECONDS.toDays((now - lastSeen).coerceAtLeast(0L)).toInt()
        return days.takeIf { it >= THRESHOLD_DAYS }
    }

    /** "unopened 45d", or "unopened 1y+" past a year. */
    fun label(days: Int): String = if (days >= DAYS_PER_YEAR) "unopened 1y+" else "unopened ${days}d"
}
