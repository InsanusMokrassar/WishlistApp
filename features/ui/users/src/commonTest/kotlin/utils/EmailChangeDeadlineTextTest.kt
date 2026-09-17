package dev.inmo.wishlist.features.ui.users.utils

import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies the owner-only deadline formatter stays stable across UTC day and year boundaries. */
class EmailChangeDeadlineTextTest {
    /** Formats the Unix epoch in UTC without inheriting the device time zone. */
    @Test
    fun formatsEpochInUtc() {
        assertEquals("1970-01-01 00:00:00 UTC", emailChangeDeadlineText(0L))
    }

    /** Formats the first instant of a new UTC year without local time-zone conversion. */
    @Test
    fun formatsUtcYearBoundary() {
        assertEquals("2027-01-01 00:00:00 UTC", emailChangeDeadlineText(1_798_761_600_000L))
    }
}
