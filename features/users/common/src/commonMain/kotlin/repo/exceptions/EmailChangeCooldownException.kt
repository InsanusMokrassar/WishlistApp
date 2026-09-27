package dev.inmo.wishlist.features.users.common.repo.exceptions

import korlibs.time.DateTime

/** Signals that a durable owner-email cooldown rejects a state-changing mutation. */
class EmailChangeCooldownException(
    /** UTC instant at which a replacement becomes permitted. */
    val emailChangeAllowedAt: DateTime,
) : IllegalStateException("Email may be changed after $emailChangeAllowedAt")
