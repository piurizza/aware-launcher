package app.aware.launcher.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.ZoneId

/**
 * Reads today's foreground time from the system's own usage log.
 *
 * Android already records these events, so Aware runs no background service and
 * costs no extra battery: it only reads the log when the home screen is shown.
 */
object UsageTracker {

    fun today(context: Context, now: Long = System.currentTimeMillis()): Map<String, AppUsage> {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val start = UsageCalculator.startOfDay(now, ZoneId.systemDefault())
        val log = manager.queryEvents(start, now)

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
        return UsageCalculator.usage(events, start, now)
    }
}
