package dev.inmo.wishlist.features.email.common.models

import dev.inmo.micro_utils.common.DateTimeSerializer
import dev.inmo.wishlist.features.email.common.utils.requireValidEmailTimestamp
import korlibs.time.DateTime
import kotlinx.serialization.Serializable

/** Owner-only HTTP response carrying the persisted cooldown deadline. */
@Serializable
data class EmailChangeCooldown(
    /** UTC instant at which the next change is allowed. */
    @Serializable(with = DateTimeSerializer::class)
    val emailChangeAllowedAt: DateTime,
) {
    /** Converts an explicit primitive epoch-millisecond transport boundary into a DateTime deadline. */
    constructor(emailChangeAllowedAt: Long) : this(DateTime(emailChangeAllowedAt.toDouble()))

    init {
        requireValidEmailTimestamp(emailChangeAllowedAt)
    }
}

/** Typed client transport failure for a well-formed cooldown response. */
class EmailChangeCooldownException(
    /** Server-supplied durable cooldown state. */
    val cooldown: EmailChangeCooldown,
) : IllegalStateException("Email may be changed after ${cooldown.emailChangeAllowedAt}")
