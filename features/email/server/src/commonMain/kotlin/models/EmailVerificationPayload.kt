package dev.inmo.wishlist.features.email.server.models

import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Polymorphic deeplink payload identifying the account approved by an email verification link.
 *
 * @property userId User account awaiting approval.
 */
@Serializable
data class EmailVerificationPayload(
    val userId: UserId,
)
