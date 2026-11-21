package app.aware.launcher.usage

import java.time.Instant
import java.time.ZoneId

/**
 * A single foreground transition reported by the system.
 *
 * Kept free of Android types so the time accounting can be unit tested on the JVM.
 */
data class ForegroundEvent(val packageName: String, val timestamp: Long, val type: Type) {
    enum class Type {
        /** An activity of [packageName] came to the foreground. */
        RESUMED,

        /** An activity of [packageName] left the foreground. */
        PAUSED,

        /** The screen turned off or the device shut down: nothing is in the foreground. */
        STOP_ALL
    }
}

/** Time in the foreground and number of times an app was brought to the foreground. */
data class AppUsage(val foregroundMillis: Long = 0L, val opens: Int = 0) {
    operator fun plus(other: AppUsage) = AppUsage(foregroundMillis + other.foregroundMillis, opens + other.opens)
}

/**
 * Turns a stream of [ForegroundEvent]s into time spent in the foreground and opens per package.
 *
 * Model: at most one app is in the foreground at a time. A resume of another app closes the
 * previous app's interval, which keeps totals sane even when the system drops a pause event
 * (for example after a crash or a reboot). Every switch into an app counts as one open.
 */
object UsageCalculator {

    fun usage(events: List<ForegroundEvent>, windowStart: Long, windowEnd: Long): Map<String, AppUsage> {
        val totals = HashMap<String, AppUsage>()
        var current: String? = null
        var since = windowStart
        // Until the first resume, the app open at the window start is unknown; a pause reveals it.
        var resumeSeen = false
        var startCredited = false

        fun add(pkg: String, usage: AppUsage) {
            totals[pkg] = (totals[pkg] ?: AppUsage()) + usage
        }

        fun close(at: Long) {
            val open = current ?: return
            val end = at.coerceIn(windowStart, windowEnd)
            add(open, AppUsage(foregroundMillis = (end - since).coerceAtLeast(0L)))
            current = null
        }

        for (event in events.sortedBy { it.timestamp }) {
            when (event.type) {
                ForegroundEvent.Type.RESUMED -> {
                    resumeSeen = true
                    if (current != event.packageName) {
                        close(event.timestamp)
                        current = event.packageName
                        since = event.timestamp.coerceIn(windowStart, windowEnd)
                        add(event.packageName, AppUsage(opens = 1))
                    }
                }

                ForegroundEvent.Type.PAUSED -> {
                    // A pause before any resume means the app was already open when the window began,
                    // even if a screen-off event was logged before it. It was opened before the window,
                    // so it adds time but not an open.
                    if (current == null && !resumeSeen && !startCredited) {
                        current = event.packageName
                        since = windowStart
                        startCredited = true
                    }
                    if (current == event.packageName) close(event.timestamp)
                }

                ForegroundEvent.Type.STOP_ALL -> close(event.timestamp)
            }
        }
        close(windowEnd)
        return totals
    }

    fun foregroundTime(events: List<ForegroundEvent>, windowStart: Long, windowEnd: Long): Map<String, Long> =
        usage(events, windowStart, windowEnd).mapValues { it.value.foregroundMillis }

    /** Local midnight of the day containing [now], or of the day [daysAgo] days before it. */
    fun startOfDay(now: Long, zone: ZoneId, daysAgo: Long = 0): Long = Instant.ofEpochMilli(now)
        .atZone(zone)
        .toLocalDate()
        .minusDays(daysAgo)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}
