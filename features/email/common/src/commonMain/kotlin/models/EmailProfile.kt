package dev.inmo.wishlist.features.email.common.models

import dev.inmo.micro_utils.common.DateTimeSerializer
import dev.inmo.wishlist.features.email.common.utils.requireValidEmailTimestamp
import korlibs.time.DateTime
import kotlinx.serialization.Serializable

/**
 * Email verification state belonging to one authenticated owner.
 *
 * The owner identifier stays a [Long] so this email-owned wire model does not introduce a
 * dependency from `email/common` to `users/common`. A `null` [emailChangeRequestedAt] means that
 * no address is awaiting approval or that a historical candidate has no known request time.
 *
 * @property userId Persistent owner identifier.
 * @property email Current address; it is the candidate before first approval and the retained
 *   approved address after approval.
 * @property emailApproved Whether [email] is approved.
 * @property pendingEmail Replacement candidate retained while [email] remains approved.
 * @property emailChangeRequestedAt UTC instant when the active candidate was accepted.
 * @property emailChangeAllowedAt UTC instant when the next state-changing address
 *   mutation becomes allowed.
 */
@Serializable
data class EmailProfile(
    val userId: Long,
    val email: Email? = null,
    val emailApproved: Boolean = false,
    val pendingEmail: Email? = null,
    @Serializable(with = DateTimeSerializer::class)
    val emailChangeRequestedAt: DateTime? = null,
    @Serializable(with = DateTimeSerializer::class)
    val emailChangeAllowedAt: DateTime? = null,
) {
    /**
     * Compatibility constructor for callers at an explicit primitive epoch-millisecond boundary.
     *
     * @param userId Persistent owner identifier.
     * @param email Current address.
     * @param emailApproved Approval state of [email].
     * @param pendingEmail Replacement candidate.
     * @param emailChangeRequestedAt Raw request timestamp, or `null`.
     * @param emailChangeAllowedAt Raw cooldown deadline, or `null`.
     */
    constructor(
        userId: Long,
        email: Email? = null,
        emailApproved: Boolean = false,
        pendingEmail: Email? = null,
        emailChangeRequestedAt: Long?,
        emailChangeAllowedAt: Long?,
    ) : this(
        userId = userId,
        email = email,
        emailApproved = emailApproved,
        pendingEmail = pendingEmail,
        emailChangeRequestedAt = emailChangeRequestedAt?.let { DateTime(it.toDouble()) },
        emailChangeAllowedAt = emailChangeAllowedAt?.let { DateTime(it.toDouble()) },
    )

    init {
        emailChangeRequestedAt?.let(::requireValidEmailTimestamp)
        emailChangeAllowedAt?.let(::requireValidEmailTimestamp)
    }
}
