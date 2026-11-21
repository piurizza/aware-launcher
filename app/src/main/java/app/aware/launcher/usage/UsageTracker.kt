package app.aware.launcher.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Reads foreground time from the system's own usage log.
 *
 * Android already records these events, so Aware runs no background service and
 * costs no extra battery: it only reads the log when the home screen is shown.
 */
object UsageTracker {

    fun today(context: Context, now: Long = System.currentTimeMillis()): Map<String, AppUsage> {
        val start = UsageCalculator.startOfDay(now, ZoneId.systemDefault())
        return range(context, start, now).orEmpty()
    }

    /**
     * Usage per package between [start] and [end], or null when the system has no events at all for
     * that period (Android keeps the detailed log for a limited number of days).
     */
    fun range(context: Context, start: Long, end: Long): Map<String, AppUsage>? {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val log = manager.queryEvents(start, end)

        val events = ArrayList<ForegroundEvent>()
        val event = UsageEvents.Event()
        while (log.hasNextEvent()) {
            log.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> ForegroundEvent.Type.RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> ForegroundEvent.Type.PAUSED
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.DEVICE_SHUTDOWN -> ForegroundEvent.Type.STOP_ALL
                else -> null
            }
            if (type != null) {
                events += ForegroundEvent(event.packageName.orEmpty(), event.timeStamp, type)
            }
        }
        return if (events.isEmpty()) null else UsageCalculator.usage(events, start, end)
    }

    /**
     * When each package was last on screen, from the system's long-term usage summaries. Cached for
     * an hour: the answer only matters for apps unopened for weeks, and the query covers a year.
     */
    @Synchronized
    fun lastVisible(context: Context, now: Long = System.currentTimeMillis()): Map<String, Long> {
        val cached = lastVisibleCache
        val fresh = if (cached != null && now - lastVisibleAt < LAST_VISIBLE_TTL_MS) {
            cached
        } else {
            queryLastVisible(context, now).also {
                lastVisibleCache = it
                lastVisibleAt = now
            }
        }
        return fresh
    }

    private fun queryLastVisible(context: Context, now: Long): Map<String, Long> {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val stats = manager.queryAndAggregateUsageStats(now - TimeUnit.DAYS.toMillis(LAST_VISIBLE_RANGE_DAYS), now)
        return stats.mapValues { it.value.lastTimeVisible }.filterValues { it > 0L }
    }

    private const val LAST_VISIBLE_RANGE_DAYS = 400L
    private val LAST_VISIBLE_TTL_MS = TimeUnit.HOURS.toMillis(1)
    private var lastVisibleCache: Map<String, Long>? = null
    private var lastVisibleAt = 0L
}
