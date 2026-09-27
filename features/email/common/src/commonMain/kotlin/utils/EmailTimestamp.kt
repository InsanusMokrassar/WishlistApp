package dev.inmo.wishlist.features.email.common.utils

import korlibs.time.DateTime

/** Inclusive maximum epoch-millisecond instant supported by the email wire and storage contract. */
const val maxEmailTimestampMillis: Long = 4_503_599_627_370_496L

/**
 * Validates an application email timestamp before it crosses a model, storage, or policy boundary.
 *
 * @param value Candidate instant.
 * @return The unchanged, validated instant.
 * @throws IllegalArgumentException When [value] is non-finite, fractional, or outside the supported range.
 */
fun requireValidEmailTimestamp(value: DateTime): DateTime {
    val millis = value.unixMillis
    require(millis.isFinite() && millis % 1.0 == 0.0) {
        "Email timestamp must be a finite whole millisecond"
    }
    require(millis >= -maxEmailTimestampMillis && millis <= maxEmailTimestampMillis) {
        "Email timestamp is outside the supported range"
    }
    return value
}

/**
 * Converts a checked durable epoch-millisecond value into an application instant.
 *
 * @param value Raw database value.
 * @return Validated application instant.
 */
fun emailTimestampFromStorage(value: Long): DateTime {
    require(value in -maxEmailTimestampMillis..maxEmailTimestampMillis) {
        "Email timestamp is outside the supported range"
    }
    return DateTime(value.toDouble())
}

/**
 * Converts a checked application instant to the durable BIGINT representation.
 *
 * @param value Application instant to persist.
 * @return Exact epoch milliseconds for database storage.
 */
fun emailTimestampToStorage(value: DateTime): Long = requireValidEmailTimestamp(value).unixMillis.toLong()

/**
 * Computes an email approval deadline without rounding or exceeding the supported timestamp range.
 *
 * @param approvedAt Validated approval instant.
 * @param cooldownMillis Non-negative cooldown duration in whole milliseconds.
 * @return Checked deadline instant.
 * @throws IllegalArgumentException When an input is invalid.
 * @throws ArithmeticException When the deadline cannot be represented.
 */
fun emailApprovalDeadline(approvedAt: DateTime, cooldownMillis: Long): DateTime {
    val approvedMillis = emailTimestampToStorage(approvedAt)
    require(cooldownMillis >= 0) { "Email cooldown must not be negative" }
    if (cooldownMillis == 0L) return approvedAt
    if (cooldownMillis > maxEmailTimestampMillis - approvedMillis) {
        throw ArithmeticException("Email approval deadline is outside the supported range")
    }
    return emailTimestampFromStorage(approvedMillis + cooldownMillis)
}
