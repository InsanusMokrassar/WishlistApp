package dev.inmo.wishlist.features.email.server.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** Server-only deeplink payload binding password approval to current protected account state. */
@Serializable
@SerialName("email.password_change.v1")
data class EmailPasswordChangePayload(
    /** Subject whose password may be changed after completion. */
    val userId: UserId,
    /** Exact current approved address that received the approval link. */
    val approvedEmail: Email,
    /** Absolute expiry in epoch milliseconds. */
    val expiresAtEpochMillis: Long,
    /** Opaque Auth-owned fingerprint of the stored credential state. */
    val credentialState: String,
) {
    /** Avoids leaking private address and fingerprint in incidental server logs. */
    override fun toString(): String = "EmailPasswordChangePayload(userId=$userId, approvedEmail=<redacted>, expiresAtEpochMillis=$expiresAtEpochMillis, credentialState=<redacted>)"
}
