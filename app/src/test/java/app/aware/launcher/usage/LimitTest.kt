package app.aware.launcher.usage

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitTest {

    private fun minutes(n: Long) = TimeUnit.MINUTES.toMillis(n)

    @Test
    fun `under the limit shows time left`() {
        val limit = Limit(limitMillis = minutes(30), usedMillis = minutes(12))
        assertFalse(limit.isReached)
        assertEquals(minutes(18), limit.differenceMillis)
    }

    @Test
    fun `reaching the limit counts as reached and shows time over`() {
        assertTrue(Limit(minutes(30), minutes(30)).isReached)
        val over = Limit(limitMillis = minutes(30), usedMillis = minutes(47))
        assertTrue(over.isReached)
        assertEquals(minutes(17), over.differenceMillis)
    }

    @Test
    fun `row label shows usage against the limit`() {
        assertEquals("12m / 30m", Limit(minutes(30), minutes(12)).rowLabel(opens = 0))
        assertEquals("1h 05m / 1h 00m · 9×", Limit(minutes(60), minutes(65)).rowLabel(opens = 9))
    }

    @Test
    fun `choices start with no limit`() {
        assertEquals(0, Limit.CHOICES_MINUTES.first())
        assertEquals(minutes(45), Limit.minutesToMillis(45))
    }
}
