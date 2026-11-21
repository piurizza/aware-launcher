package app.aware.launcher.usage

/** A foreground-service transition from the system usage log. */
data class ServiceEvent(val packageName: String, val className: String, val type: Type) {
    enum class Type {
        STARTED,
        STOPPED,

        /** Device shutdown or startup: every service is stopped, without individual stop events. */
        RESET
    }
}

/**
 * Which apps are running in the background right now, meaning they hold a foreground service:
 * music, navigation, calls, VPNs, uploads. Those are the apps that keep working and use battery
 * after you leave them, and Android always shows a notification for them.
 *
 * Apps merely kept in Recents are not counted: they are paused or cached, use no CPU, and Android
 * frees them on its own when it needs memory.
 */
object BackgroundServices {

    fun running(events: List<ServiceEvent>): Set<String> {
        val services = HashSet<Pair<String, String>>()
        for (event in events) {
            val service = event.packageName to event.className
            when (event.type) {
                ServiceEvent.Type.STARTED -> services += service
                ServiceEvent.Type.STOPPED -> services -= service
                ServiceEvent.Type.RESET -> services.clear()
            }
        }
        return services.mapTo(HashSet()) { it.first }
    }
}
