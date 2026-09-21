package dev.inmo.wishlist.features.email.common.models

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
 * @property emailChangeRequestedAt UTC epoch milliseconds when the active candidate was accepted.
 * @property emailChangeAllowedAt UTC epoch milliseconds when the next state-changing address
 *   mutation becomes allowed.
 */
@Serializable
data class EmailProfile(
    val userId: Long,
    val email: Email? = null,
    val emailApproved: Boolean = false,
    val pendingEmail: Email? = null,
    val emailChangeRequestedAt: Long? = null,
    val emailChangeAllowedAt: Long? = null,
)
