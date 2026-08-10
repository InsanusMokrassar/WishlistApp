package dev.inmo.wishlist.features.email.server.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Polymorphic deeplink payload binding account approval to the address that received the invite.
 *
 * @property userId User account awaiting approval.
 * @property email Invited address. `null` only for persisted legacy payloads, which fail closed.
 */
@Serializable
data class EmailVerificationPayload(
    val userId: UserId,
    val email: Email? = null,
)
