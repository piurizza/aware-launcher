package app.aware.launcher.usage

import app.aware.launcher.usage.ServiceEvent.Type.RESET
import app.aware.launcher.usage.ServiceEvent.Type.STARTED
import app.aware.launcher.usage.ServiceEvent.Type.STOPPED
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundServicesTest {

    private fun event(pkg: String, service: String, type: ServiceEvent.Type) = ServiceEvent(pkg, service, type)

    @Test
    fun `a started service that was not stopped is running`() {
        val running = BackgroundServices.running(
            listOf(
                event("music", "Player", STARTED),
                event("maps", "Navigation", STARTED),
                event("maps", "Navigation", STOPPED)
            )
        )
        assertEquals(setOf("music"), running)
    }

    @Test
    fun `an app stays running while any of its services is`() {
        val running = BackgroundServices.running(
            listOf(
                event("app", "Upload", STARTED),
                event("app", "Sync", STARTED),
                event("app", "Upload", STOPPED)
            )
        )
        assertEquals(setOf("app"), running)
    }

    @Test
    fun `a reboot stops everything started before it`() {
        val running = BackgroundServices.running(
            listOf(
                event("music", "Player", STARTED),
                event("", "", RESET),
                event("vpn", "Tunnel", STARTED)
            )
        )
        assertEquals(setOf("vpn"), running)
    }

    @Test
    fun `a stop without a start is ignored`() {
        assertEquals(emptySet<String>(), BackgroundServices.running(listOf(event("app", "Sync", STOPPED))))
    }
}
