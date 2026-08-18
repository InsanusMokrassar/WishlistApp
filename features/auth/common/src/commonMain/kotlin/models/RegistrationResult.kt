package dev.inmo.wishlist.features.auth.common.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A successful self-service registration outcome.
 *
 * Credential-bearing and pending outcomes are distinct so clients cannot treat email verification
 * as an authenticated session.
 */
@Serializable
sealed interface RegistrationResult {
    /** Registration completed with an approved role and newly issued credentials. */
    @Serializable
    @SerialName("authorized")
    data class Authorized(
        /** Credentials issued to the approved account. */
        val credentials: AuthCredentials,
    ) : RegistrationResult

    /** Registration stored a password and awaits the submitted email's verification link. */
    @Serializable
    @SerialName("pendingEmailVerification")
    data object PendingEmailVerification : RegistrationResult
}
