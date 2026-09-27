package dev.inmo.wishlist.features.ui.users.utils

import korlibs.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies the owner-only deadline formatter stays stable across UTC day and year boundaries. */
class EmailChangeDeadlineTextTest {
    /** Formats the Unix epoch in UTC without inheriting the device time zone. */
    @Test
    fun formatsEpochInUtc() {
        assertEquals("1970-01-01 00:00:00 UTC", emailChangeDeadlineText(DateTime.fromUnixMillis(0L)))
    }

    /** Formats the first instant of a new UTC year without local time-zone conversion. */
    @Test
    fun formatsUtcYearBoundary() {
        assertEquals("2027-01-01 00:00:00 UTC", emailChangeDeadlineText(DateTime.fromUnixMillis(1_798_761_600_000L)))
    }

    /** Pre-epoch instants retain their UTC day and second without local conversion. */
    @Test
    fun formatsNegativeInstant() {
        assertEquals("1969-12-31 23:59:59 UTC", emailChangeDeadlineText(DateTime.fromUnixMillis(-1_000L)))
    }

    /** Both supported extremes display their expanded UTC years without four-digit truncation. */
    @Test
    fun formatsSupportedExtremesWithExpandedYears() {
        listOf(-4_503_599_627_370_496L, 4_503_599_627_370_496L).forEach { millis ->
            val text = emailChangeDeadlineText(DateTime.fromUnixMillis(millis))
            assertTrue(text.endsWith(" UTC"), text)
            assertTrue(Regex("^[+-]?\\d{5,}-.* UTC$").matches(text), text)
        }
    }
}
