package app.aware.launcher.usage

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    private fun minutes(n: Long) = TimeUnit.MINUTES.toMillis(n)

    @Test
    fun `formats minutes`() {
        assertEquals("0m", DurationFormat.format(0))
        assertEquals("42m", DurationFormat.format(minutes(42)))
    }

    @Test
    fun `formats hours with padded minutes`() {
        assertEquals("1h 05m", DurationFormat.format(minutes(65)))
        assertEquals("12h 00m", DurationFormat.format(minutes(720)))
    }

    @Test
    fun `negative durations are treated as zero`() {
        assertEquals("0m", DurationFormat.format(-5_000))
    }

    @Test
    fun `rows hide usage under a minute`() {
        assertEquals("", DurationFormat.forRow(TimeUnit.SECONDS.toMillis(59)))
        assertEquals("1m", DurationFormat.forRow(minutes(1)))
    }
}
