package dev.inmo.wishlist.features.email.common.models

import kotlinx.serialization.Serializable

/**
 * Caller-scoped request to send a verification link for the current stored address.
 *
 * The request carries no user id: the server derives identity from the bearer token. [expectedEmail]
 * lets the server reject a stale form instead of delivering a link for a newer address.
 *
 * @property expectedEmail Address the caller saw when requesting delivery.
 */
@Serializable
data class EmailVerificationRequest(
    val expectedEmail: Email,
)

/** Outcomes of a caller-scoped email-verification delivery request. */
@Serializable
enum class EmailVerificationRequestResult {
    /** A verification message was accepted for the current pending address. */
    Sent,

    /** The current address was already approved, so no message was sent. */
    AlreadyApproved,

    /** SMTP/deeplink delivery infrastructure is unavailable. */
    Unavailable,

    /** The authenticated caller has no currently stored address. */
    NoEmail,

    /** The caller's expected address is no longer the current stored address. */
    EmailChanged,

    /** Delivery failed after a request-local deeplink was cleaned up. */
    DeliveryFailed,
}
