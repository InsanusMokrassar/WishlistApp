package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.email.common.models.Email

/** HTTP-only client capability for Auth's email-authorized password-change routes. */
interface PasswordChangeFeature {
    /**
     * Requests a password-change approval email for the authenticated owner's exact address.
     *
     * @param expectedEmail Current approved address displayed by the owner editor.
     * @return Domain result, or `null` for non-success HTTP, malformed response, or transport failure.
     */
    suspend fun requestPasswordChangeEmail(expectedEmail: Email): PasswordChangeEmailRequestResult?

    /**
     * Redeems an emailed approval without consulting browser credentials or auth storage.
     *
     * @param request Exact subject, approval UUID, and new password entered by the user.
     * @return Domain result, or `null` for unconfirmed transport/HTTP failures.
     */
    suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult?
}
