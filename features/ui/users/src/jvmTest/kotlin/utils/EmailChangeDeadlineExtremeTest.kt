package dev.inmo.wishlist.features.ui.users.utils

import korlibs.time.DateTime
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Uses the JVM UTC calendar as an independent oracle for supported extreme deadlines. */
class EmailChangeDeadlineExtremeTest {
    /** Expanded signed years retain their actual UTC calendar date and time. */
    @Test
    fun extremeYearsMatchUtcCalendar() {
        val format = Regex("([+-]?\\d+)-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2}) UTC")
        listOf(-4_503_599_627_370_496L, 4_503_599_627_370_496L).forEach { millis ->
            val actual = emailChangeDeadlineText(DateTime.fromUnixMillis(millis))
            val parts = assertNotNull(format.matchEntire(actual), actual).groupValues
            val expected = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC)
            assertEquals(expected.year, parts[1].toInt())
            assertEquals(expected.monthValue, parts[2].toInt())
            assertEquals(expected.dayOfMonth, parts[3].toInt())
            assertEquals(expected.hour, parts[4].toInt())
            assertEquals(expected.minute, parts[5].toInt())
            assertEquals(expected.second, parts[6].toInt())
        }
    }
}
