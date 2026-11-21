package app.aware.launcher.usage

import app.aware.launcher.usage.ForegroundEvent.Type.PAUSED
import app.aware.launcher.usage.ForegroundEvent.Type.RESUMED
import app.aware.launcher.usage.ForegroundEvent.Type.STOP_ALL
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCalculatorTest {

    private val start = 1_000_000L
    private val end = start + 10_000L

    private fun event(pkg: String, offset: Long, type: ForegroundEvent.Type) =
        ForegroundEvent(pkg, start + offset, type)

    @Test
    fun `sums a simple resume and pause`() {
        val result = UsageCalculator.foregroundTime(
            listOf(event("a", 100, RESUMED), event("a", 600, PAUSED)),
            start,
            end
        )
        assertEquals(mapOf("a" to 500L), result)
    }

    @Test
    fun `adds up multiple sessions of the same app`() {
        val result = UsageCalculator.foregroundTime(
            listOf(
                event("a", 0, RESUMED),
                event("a", 100, PAUSED),
                event("a", 1_000, RESUMED),
                event("a", 1_300, PAUSED)
            ),
            start,
            end
        )
        assertEquals(400L, result["a"])
    }

    @Test
    fun `app open when the window starts is counted from the window start`() {
        val result = UsageCalculator.foregroundTime(listOf(event("a", 250, PAUSED)), start, end)
        assertEquals(250L, result["a"])
    }

    @Test
    fun `app still open is counted until the window end`() {
        val result = UsageCalculator.foregroundTime(listOf(event("a", 9_000, RESUMED)), start, end)
        assertEquals(1_000L, result["a"])
    }

    @Test
    fun `resuming another app closes the previous one when its pause is missing`() {
        val result = UsageCalculator.foregroundTime(
            listOf(event("a", 0, RESUMED), event("b", 300, RESUMED), event("b", 500, PAUSED)),
            start,
            end
        )
        assertEquals(mapOf("a" to 300L, "b" to 200L), result)
    }

    @Test
    fun `screen off stops the clock`() {
        val result = UsageCalculator.foregroundTime(
            listOf(event("a", 0, RESUMED), event("", 200, STOP_ALL), event("a", 5_000, PAUSED)),
            start,
            end
        )
        assertEquals(200L, result["a"])
    }

    @Test
    fun `switching between activities of one app is continuous`() {
        val result = UsageCalculator.foregroundTime(
            listOf(event("a", 0, RESUMED), event("a", 100, RESUMED), event("a", 400, PAUSED)),
            start,
            end
        )
        assertEquals(400L, result["a"])
    }

    @Test
    fun `events out of order are sorted first`() {
        val result = UsageCalculator.foregroundTime(
            listOf(event("a", 600, PAUSED), event("a", 100, RESUMED)),
            start,
            end
        )
        assertEquals(500L, result["a"])
    }

    @Test
    fun `no events means no usage`() {
        assertTrue(UsageCalculator.foregroundTime(emptyList(), start, end).isEmpty())
    }

    @Test
    fun `start of day is local midnight`() {
        val zone = ZoneId.of("Europe/Rome")
        val afternoon = LocalDateTime.of(2026, 11, 20, 14, 30).atZone(zone).toInstant().toEpochMilli()
        val midnight = LocalDateTime.of(2026, 11, 20, 0, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(midnight, UsageCalculator.startOfDay(afternoon, zone))
    }
}
