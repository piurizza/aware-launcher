package app.aware.launcher.usage

import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Today's screen time compared with yesterday's at the same time of day. */
data class Comparison(val direction: Direction, val differenceMillis: Long) {

    enum class Direction { LESS, MORE, SAME }

    companion object {
        /** Differences smaller than this read as "about the same". */
        val SAME_THRESHOLD_MS: Long = TimeUnit.MINUTES.toMillis(5)

        fun of(todayMillis: Long, yesterdaySoFarMillis: Long): Comparison {
            val difference = todayMillis - yesterdaySoFarMillis
            val direction = when {
                abs(difference) < SAME_THRESHOLD_MS -> Direction.SAME
                difference < 0 -> Direction.LESS
                else -> Direction.MORE
            }
            return Comparison(direction, abs(difference))
        }
    }
}
