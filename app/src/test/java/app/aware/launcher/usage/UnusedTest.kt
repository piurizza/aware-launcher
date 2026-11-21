package app.aware.launcher.usage

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnusedTest {

    private val now = TimeUnit.DAYS.toMillis(1000)
    private fun daysAgo(n: Long) = now - TimeUnit.DAYS.toMillis(n)

    @Test
    fun `flags apps not opened for 30 days or more`() {
        assertEquals(45, Unused.days(daysAgo(45), installedAt = daysAgo(500), now = now, usedToday = false))
        assertNull(Unused.days(daysAgo(29), installedAt = daysAgo(500), now = now, usedToday = false))
    }

    @Test
    fun `never opened apps count from install`() {
        assertEquals(60, Unused.days(null, installedAt = daysAgo(60), now = now, usedToday = false))
        assertNull(Unused.days(null, installedAt = daysAgo(3), now = now, usedToday = false))
    }

    @Test
    fun `a recent reinstall resets the clock`() {
        assertNull(Unused.days(daysAgo(200), installedAt = daysAgo(2), now = now, usedToday = false))
    }

    @Test
    fun `apps used today are never flagged`() {
        assertNull(Unused.days(daysAgo(90), installedAt = daysAgo(500), now = now, usedToday = true))
    }

    @Test
    fun `a year or more counts as over a year`() {
        assertFalse(Unused.isOverAYear(364))
        assertTrue(Unused.isOverAYear(365))
    }
}
