package dev.inmo.wishlist.features.email.common.utils

import korlibs.time.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Verifies the exact email timestamp boundary shared by wire, storage, and deadline logic. */
class EmailTimestampTest {
    /** Accepted whole-millisecond instants retain their exact durable representation. */
    @Test
    fun acceptsExactSupportedInstants() {
        listOf(0L, 1_700_000_000_000L, -1L, -maxEmailTimestampMillis, -maxEmailTimestampMillis + 1, maxEmailTimestampMillis - 1, maxEmailTimestampMillis).forEach { millis ->
            val instant = emailTimestampFromStorage(millis)
            assertEquals(millis, emailTimestampToStorage(instant))
            assertEquals(instant, requireValidEmailTimestamp(instant))
        }
    }

    /** Raw values outside the supported range fail before conversion to a floating representation. */
    @Test
    fun rejectsUnsupportedStorageValues() {
        listOf(Long.MIN_VALUE, Long.MAX_VALUE, -maxEmailTimestampMillis - 1, maxEmailTimestampMillis + 1).forEach {
            assertFailsWith<IllegalArgumentException> { emailTimestampFromStorage(it) }
        }
    }

    /** Non-integral and non-finite application values are never rounded or saturated. */
    @Test
    fun rejectsInvalidApplicationValues() {
        listOf(DateTime(0.5), DateTime(Double.NaN), DateTime(Double.POSITIVE_INFINITY), DateTime(Double.NEGATIVE_INFINITY), DateTime((maxEmailTimestampMillis + 1).toDouble())).forEach {
            assertFailsWith<IllegalArgumentException> { requireValidEmailTimestamp(it) }
        }
    }

    /** Deadline arithmetic preserves exact bounds and reports overflow separately from invalid input. */
    @Test
    fun checksApprovalDeadlineArithmetic() {
        assertEquals(DateTime.fromUnixMillis(10L), emailApprovalDeadline(DateTime.fromUnixMillis(5L), 5L))
        assertEquals(DateTime.fromUnixMillis(-5L), emailApprovalDeadline(DateTime.fromUnixMillis(-10L), 5L))
        assertEquals(DateTime.fromUnixMillis(maxEmailTimestampMillis), emailApprovalDeadline(DateTime.fromUnixMillis(maxEmailTimestampMillis - 5L), 5L))
        assertEquals(DateTime.fromUnixMillis(maxEmailTimestampMillis), emailApprovalDeadline(DateTime.fromUnixMillis(maxEmailTimestampMillis), 0L))
        assertFailsWith<IllegalArgumentException> { emailApprovalDeadline(DateTime.EPOCH, -1L) }
        assertFailsWith<ArithmeticException> {
            emailApprovalDeadline(DateTime.fromUnixMillis(maxEmailTimestampMillis), 1L)
        }
        assertFailsWith<ArithmeticException> {
            emailApprovalDeadline(DateTime.fromUnixMillis(maxEmailTimestampMillis - 5L), 10L)
        }
        assertFailsWith<ArithmeticException> { emailApprovalDeadline(DateTime.EPOCH, Long.MAX_VALUE) }
    }
}
