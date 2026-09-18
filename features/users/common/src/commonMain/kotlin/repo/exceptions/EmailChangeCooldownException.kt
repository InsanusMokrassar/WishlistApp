package dev.inmo.wishlist.features.users.common.repo.exceptions

/** Signals that a durable owner-email cooldown rejects a state-changing mutation. */
class EmailChangeCooldownException(
    /** UTC epoch-millisecond instant at which a replacement becomes permitted. */
    val emailChangeAllowedAt: Long,
) : IllegalStateException("Email may be changed after $emailChangeAllowedAt")
