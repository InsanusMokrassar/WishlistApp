package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.serialization.Serializable

/**
 * Request from an authenticated owner for a password-change approval email.
 *
 * @param expectedEmail Current approved address displayed to the owner and checked by the server.
 */
@Serializable
data class PasswordChangeEmailRequest(
    /** Current approved address displayed to the owner. */
    val expectedEmail: Email,
)

/** Ordinary outcomes of requesting a password-change approval email. */
@Serializable
enum class PasswordChangeEmailRequestResult {
    /** The approval message was delivered and still matched current account state. */
    Sent,

    /** SMTP or deeplink infrastructure is unavailable for new requests. */
    Unavailable,

    /** Current owner, address, direct role, or credential state cannot authorize a request. */
    Ineligible,

    /** SMTP did not accept the message; no success is claimed. */
    DeliveryFailed,
}

/**
 * Token-authorized submission that replaces one approval-bound account password.
 *
 * @param userId Subject assertion that must equal the persisted approval subject.
 * @param approvalId Persisted deeplink UUID used as the single-use approval identifier.
 * @param password New plaintext password sent only in this request body.
 */
@Serializable
data class CompletePasswordChangeRequest(
    /** Subject assertion that must equal the persisted approval subject. */
    val userId: UserId,
    /** Persisted deeplink UUID used as the single-use approval identifier. */
    val approvalId: DeepLinkId,
    /** New plaintext password; sent only in this request body. */
    val password: Password,
) {
    /** Keeps approval UUID and plaintext password out of incidental logs. */
    override fun toString(): String = "CompletePasswordChangeRequest(userId=$userId, approvalId=<redacted>, password=<redacted>)"
}

/** Ordinary outcomes of completing a password change. */
@Serializable
enum class PasswordChangeResult {
    /** The exact approval was consumed and its subject password was replaced. */
    Changed,

    /** The approval is missing, stale, mismatched, expired, or already consumed. */
    InvalidApproval,

    /** The proposed password violates the password-change policy. */
    InvalidPassword,
}
