package app.aware.launcher.usage

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ComparisonTest {

    private fun minutes(n: Long) = TimeUnit.MINUTES.toMillis(n)

    @Test
    fun `less than yesterday`() {
        assertEquals(
            Comparison(Comparison.Direction.LESS, minutes(20)),
            Comparison.of(minutes(100), minutes(120))
        )
    }

    @Test
    fun `more than yesterday`() {
        assertEquals(
            Comparison(Comparison.Direction.MORE, minutes(45)),
            Comparison.of(minutes(165), minutes(120))
        )
    }

    @Test
    fun `small differences read as the same`() {
        assertEquals(Comparison.Direction.SAME, Comparison.of(minutes(122), minutes(120)).direction)
        assertEquals(Comparison.Direction.SAME, Comparison.of(minutes(116), minutes(120)).direction)
        assertEquals(Comparison.Direction.LESS, Comparison.of(minutes(115), minutes(120)).direction)
    }
}
