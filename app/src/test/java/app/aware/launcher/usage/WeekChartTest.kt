package app.aware.launcher.usage

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeekChartTest {

    private fun minutes(n: Long) = TimeUnit.MINUTES.toMillis(n)

    @Test
    fun `bars scale to the busiest day and labels align`() {
        val lines = WeekChart.lines(
            listOf(DayTotal("Sat 21", minutes(60)), DayTotal("Fri 20", minutes(120)), DayTotal("Thu 9", 0)),
            barWidth = 4
        )
        assertEquals(
            listOf(
                "Sat 21  ██    1h 00m",
                "Fri 20  ████  2h 00m",
                "Thu 9" + " ".repeat(9) + "0m"
            ),
            lines
        )
    }

    @Test
    fun `days without data show a dash`() {
        val lines = WeekChart.lines(listOf(DayTotal("Mon 2", null)), barWidth = 4)
        assertEquals(listOf("Mon 2" + " ".repeat(8) + "—"), lines)
    }

    @Test
    fun `average ignores days without data`() {
        val days = listOf(DayTotal("a", minutes(60)), DayTotal("b", null), DayTotal("c", minutes(120)))
        assertEquals(minutes(90), WeekChart.average(days))
        assertNull(WeekChart.average(listOf(DayTotal("a", null))))
    }
}
