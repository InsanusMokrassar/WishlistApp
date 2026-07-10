package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * Server-side [EmailFeature] implementation that unifies SMTP delivery and user-email persistence.
 *
 * [emailsService] is always a real, non-null [EmailsService] — [dev.inmo.wishlist.features.email.server.Plugin]
 * only ever constructs this class when one is registered (SMTP configured). When no [EmailsService] is
 * registered (SMTP not configured), [DisabledEmailFeature] is substituted for the whole [EmailFeature]
 * binding instead. Because of that, [isFeatureEnabled] always returns `true`.
 *
 * - [sendTestEmail] verifies [callerId] is superadmin (via [simpleRolesFeature]) before delegating SMTP
 *   delivery via [emailsService].
 * - [setMyEmail] updates the caller's stored email address via [updateStoredEmail] — independent of
 *   [emailsService] and of superadmin status.
 *
 * @param emailsService SMTP delivery service used for sends. Always non-null — see class doc.
 * @param usersRepo User repository used for email-address persistence.
 * @param simpleRolesFeature Superadmin-status check used to gate [sendTestEmail]; see
 *   `features/roles`/`features/simpleRoles` (issue #68).
 */
class EmailFeatureService(
    private val emailsService: EmailsService,
    private val usersRepo: UsersRepo,
    private val simpleRolesFeature: SimpleRolesFeature
) : EmailFeature {

    /**
     * Returns whether an SMTP delivery service is available.
     *
     * @return Always `true` — this class is only ever constructed with a real [emailsService]; see
     *   [DisabledEmailFeature] for the SMTP-disabled no-op path.
     */
    override suspend fun isFeatureEnabled(): Boolean = true

    /**
     * Sends a test email to [recipient] if [callerId] is superadmin.
     *
     * @param callerId Caller checked against [simpleRolesFeature].
     * @param recipient Target address for the test message.
     * @return `true` when delivery succeeded; `false` when the caller lacks privilege or SMTP
     *   delivery fails.
     */
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean {
        if (!simpleRolesFeature.isSuperAdmin(callerId)) return false
        return emailsService.sendText(
            recipient = recipient,
            subject = "Test email from WishlistApp",
            text = "This is a test email sent from WishlistApp to verify SMTP configuration."
        )
    }

    /**
     * Updates or clears the stored email address for [callerId].
     *
     * Delegates to [updateStoredEmail] — identical to [DisabledEmailFeature.setMyEmail].
     *
     * @param callerId User whose record is updated.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found.
     * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
     *   when [email] is already stored for a different user; propagates unchanged from
     *   [updateStoredEmail] / `UsersRepo.update` — this method does not catch it.
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        updateStoredEmail(usersRepo, callerId, email)
}
