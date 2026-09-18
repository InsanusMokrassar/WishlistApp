package dev.inmo.wishlist.features.email.common.models

import kotlinx.serialization.Serializable

/** Owner-only HTTP response carrying the persisted cooldown deadline. */
@Serializable
data class EmailChangeCooldown(
    /** UTC epoch-millisecond instant at which the next change is allowed. */
    val emailChangeAllowedAt: Long,
)

/** Typed client transport failure for a well-formed cooldown response. */
class EmailChangeCooldownException(
    /** Server-supplied durable cooldown state. */
    val cooldown: EmailChangeCooldown,
) : IllegalStateException("Email may be changed after ${cooldown.emailChangeAllowedAt}")
