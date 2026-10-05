package dev.inmo.wishlist.features.auth.server

import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Auth-owned server port for Email's password-approval orchestration.
 *
 * Email provides the implementation so Auth routes remain available when Email is absent without
 * introducing an Auth-to-Email server dependency.
 */
interface ServerPasswordChangeFeature {
    /**
     * Requests an approval message for the authenticated caller's exact approved address.
     *
     * @param callerId Caller derived by the authenticated Auth route.
     * @param expectedEmail Address snapshot supplied by the owner editor.
     * @return A non-secret domain outcome.
     */
    suspend fun requestPasswordChangeEmail(
        callerId: UserId,
        expectedEmail: Email,
    ): PasswordChangeEmailRequestResult

    /**
     * Consumes an exact approval and changes only its persisted subject's password.
     *
     * @param request Token-authorized completion request.
     * @return A non-secret domain outcome.
     */
    suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult
}
