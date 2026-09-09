package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.auth.common.models.CompletePasswordChangeRequest
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeEmailRequestResult
import dev.inmo.wishlist.features.auth.common.models.PasswordChangeResult
import dev.inmo.wishlist.features.auth.common.utils.isAcceptablePasswordChangePassword
import dev.inmo.wishlist.features.auth.common.utils.isCanonicalPasswordChangeApprovalId
import dev.inmo.wishlist.features.auth.common.utils.passwordChangePendingPath
import dev.inmo.wishlist.features.auth.server.ServerPasswordChangeFeature
import dev.inmo.wishlist.features.auth.server.services.AuthFeatureService
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerInfo
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.deeplinks.server.services.DeepLinksService
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChange
import dev.inmo.wishlist.features.email.server.models.EmailPasswordChangePayload
import dev.inmo.wishlist.features.email.server.utils.buildPublicDeepLinkUrl
import dev.inmo.wishlist.features.email.server.utils.normalizePublicHttpOrigin
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Email-owned orchestration for one-use, email-authorized password changes.
 *
 * Auth keeps password hashing and its credential lock; this service owns the persisted deeplink,
 * current approved-email coordination, delivery, expiry, and consume-before-write callback.
 *
 * @param emailsService Configured SMTP transport, when present.
 * @param deepLinksService Persisted deeplink capability, when present.
 * @param accountCoordinator Shared current-email mutation coordinator.
 * @param authFeatureService Auth-owned password and credential-state boundary.
 * @param publicHttpOrigin Trusted configured public origin for delivered URLs.
 * @param nowEpochMillis Injectable wall clock used for bounded approval expiry.
 */
class EmailPasswordChangeService(
    private val emailsService: EmailsService?,
    private val deepLinksService: DeepLinksService?,
    private val accountCoordinator: EmailVerificationAccountCoordinator,
    private val authFeatureService: AuthFeatureService,
    publicHttpOrigin: String,
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : ServerPasswordChangeFeature {
    /** Validated origin used only for fixed server-generated links. */
    private val publicHttpOrigin = normalizePublicHttpOrigin(publicHttpOrigin)

    /** Bounded approval lifetime matching the existing access-token default. */
    private val approvalLifetimeMillis = 15L * 60L * 1000L

    /** Human-readable subject for the only password-change message. */
    private val subject = "Change your WishlistApp password"

    override suspend fun requestPasswordChangeEmail(
        callerId: UserId,
        expectedEmail: Email,
    ): PasswordChangeEmailRequestResult {
        val links = deepLinksService ?: return PasswordChangeEmailRequestResult.Unavailable
        val emails = emailsService ?: return PasswordChangeEmailRequestResult.Unavailable
        val credentialState = accountCoordinator.withApprovedEmail(callerId, expectedEmail) {
            authFeatureService.passwordChangeState(callerId)
        } ?: return PasswordChangeEmailRequestResult.Ineligible
        val expiresAtEpochMillis = nowEpochMillis() + approvalLifetimeMillis
        val payload = EmailPasswordChangePayload(
            userId = callerId,
            approvedEmail = expectedEmail,
            expiresAtEpochMillis = expiresAtEpochMillis,
            credentialState = credentialState,
        )
        var deeplinkId: DeepLinkId? = null
        try {
            currentCoroutineContext().ensureActive()
            deeplinkId = withContext(NonCancellable) {
                links.createDeepLink(EmailPasswordChange.handlerId, payload)
            }
            currentCoroutineContext().ensureActive()
            val delivered = try {
                emails.sendHtml(
                recipient = expectedEmail,
                subject = subject,
                html = "<p>Change your WishlistApp password by <a href=\"${buildPublicDeepLinkUrl(publicHttpOrigin, deeplinkId)}\">changing your password</a>.</p>",
            )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                try {
                    removeApproval(links, deeplinkId)
                } catch (cleanupError: Throwable) {
                    error.addSuppressed(cleanupError)
                    throw error
                }
                return PasswordChangeEmailRequestResult.DeliveryFailed
            }
            if (!delivered) {
                removeApproval(links, deeplinkId)
                return PasswordChangeEmailRequestResult.DeliveryFailed
            }
            val stillEligible = accountCoordinator.withApprovedEmail(callerId, expectedEmail) {
                nowEpochMillis() < expiresAtEpochMillis &&
                    authFeatureService.passwordChangeState(callerId) == credentialState
            } ?: false
            if (!stillEligible) {
                removeApproval(links, deeplinkId)
                return PasswordChangeEmailRequestResult.Ineligible
            }
            currentCoroutineContext().ensureActive()
            return PasswordChangeEmailRequestResult.Sent
        } catch (error: CancellationException) {
            deeplinkId?.let { id -> removeApprovalSuppressing(links, id, error) }
            throw error
        }
    }

    override suspend fun completePasswordChange(request: CompletePasswordChangeRequest): PasswordChangeResult {
        if (!isCanonicalPasswordChangeApprovalId(request.approvalId)) return PasswordChangeResult.InvalidApproval
        if (!isAcceptablePasswordChangePassword(request.password)) return PasswordChangeResult.InvalidPassword
        val links = deepLinksService ?: return PasswordChangeResult.InvalidApproval
        val payload = passwordChangePayload(links.getDeepLinkInfo(request.approvalId))
            ?: return PasswordChangeResult.InvalidApproval
        if (payload.userId != request.userId) return PasswordChangeResult.InvalidApproval
        return accountCoordinator.withApprovedEmail(payload.userId, payload.approvedEmail) {
            if (nowEpochMillis() >= payload.expiresAtEpochMillis) {
                return@withApprovedEmail PasswordChangeResult.InvalidApproval
            }
            authFeatureService.setPasswordIfAuthorized(
                userId = payload.userId,
                expectedCredentialState = payload.credentialState,
                rawPassword = request.password,
            ) {
                val currentPayload = passwordChangePayload(links.getDeepLinkInfo(request.approvalId))
                    ?: return@setPasswordIfAuthorized false
                if (currentPayload != payload || nowEpochMillis() >= payload.expiresAtEpochMillis) {
                    return@setPasswordIfAuthorized false
                }
                links.removeDeepLink(request.approvalId)
                true
            }
        } ?: PasswordChangeResult.InvalidApproval
    }

    /**
     * Validates a read-only opened approval and returns its fixed client redirect path.
     *
     * @param deeplinkId Exact opened deeplink identifier.
     * @return Fixed pending screen path, or `null` when current authorization has become invalid.
     */
    suspend fun pendingPasswordChangePath(deeplinkId: DeepLinkId): String? {
        if (!isCanonicalPasswordChangeApprovalId(deeplinkId)) return null
        val links = deepLinksService ?: return null
        val payload = passwordChangePayload(links.getDeepLinkInfo(deeplinkId)) ?: return null
        val valid = accountCoordinator.withApprovedEmail(payload.userId, payload.approvedEmail) {
            nowEpochMillis() < payload.expiresAtEpochMillis &&
                authFeatureService.passwordChangeState(payload.userId) == payload.credentialState
        } ?: false
        if (!valid) return null
        return passwordChangePendingPath(payload.userId, deeplinkId)
    }

    /** Extracts only a password-purpose payload owned by this service. */
    private fun passwordChangePayload(info: DeepLinkHandlerInfo?): EmailPasswordChangePayload? {
        if (info?.handlerId != EmailPasswordChange.handlerId) return null
        return info.value as? EmailPasswordChangePayload
    }

    /** Removes exactly one owned approval in a bounded non-cancellable cleanup region. */
    private suspend fun removeApproval(links: DeepLinksService, deeplinkId: DeepLinkId) {
        withContext(NonCancellable) {
            links.removeDeepLink(deeplinkId)
        }
    }

    /** Preserves [error] while reporting a cleanup failure through suppressed exceptions. */
    private suspend fun removeApprovalSuppressing(
        links: DeepLinksService,
        deeplinkId: DeepLinkId,
        error: Throwable,
    ) {
        try {
            removeApproval(links, deeplinkId)
        } catch (cleanupError: Throwable) {
            error.addSuppressed(cleanupError)
        }
    }
}
