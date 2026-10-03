package dev.inmo.wishlist.features.users.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.micro_utils.common.DateTimeSerializer
import dev.inmo.wishlist.features.email.common.utils.requireValidEmailTimestamp
import korlibs.time.DateTime
import kotlinx.serialization.Serializable

/**
 * Owner-private external users-feature email lifecycle projection.
 *
 * The existing authenticated email GET transports this separate users-feature model only to
 * the owner; the public users listing never exposes private lifecycle state. The owner identifier
 * stays a [Long] to preserve the wire contract. A `null` [emailChangeRequestedAt] means that
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
    init {
        emailChangeRequestedAt?.let(::requireValidEmailTimestamp)
        emailChangeAllowedAt?.let(::requireValidEmailTimestamp)
    }
}
