package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.email.server.EmailsService
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * Server-side [EmailFeature] implementation that unifies SMTP delivery and user-email persistence.
 *
 * [emailsService] stays nullable so this class remains directly constructible (e.g. in unit tests)
 * without an SMTP transport — [isFeatureEnabled] reports that state via `emailsService != null`. In
 * the DI graph wired by [dev.inmo.wishlist.features.email.server.Plugin], `EmailFeatureService` is
 * only ever constructed with a **non-null** [emailsService]: when no [EmailsService] is registered
 * (SMTP not configured), [DisabledEmailFeature] is substituted for the whole [EmailFeature] binding
 * instead of passing `null` in here. The nullable type is retained anyway, per the operator's
 * explicit instruction and for direct-construction testability.
 *
 * Resolves the caller's user record from [usersRepo] to enforce access rules and to perform
 * email-address storage updates:
 * - [sendTestEmail] verifies [callerId] is root AND that [emailsService] is present before
 *   delegating SMTP delivery.
 * - [setMyEmail] updates the caller's stored email address via [updateStoredEmail] — independent of
 *   [emailsService].
 *
 * @param emailsService SMTP delivery service used for sends, or `null` when constructed without one
 *   (never `null` via the production DI wiring — see class doc).
 * @param usersRepo User repository used for privilege checking and email-address persistence.
 */
class EmailFeatureService(
    private val emailsService: EmailsService?,
    private val usersRepo: UsersRepo
) : EmailFeature {

    /** Username [sendTestEmail] compares the caller's username against to gate test-email sends to the root account. */
    private val rootUsername = "root"

    /**
     * Returns whether an SMTP delivery service is available.
     *
     * @return `true` when [emailsService] is non-null; `false` otherwise.
     */
    override suspend fun isFeatureEnabled(): Boolean = emailsService != null

    /**
     * Sends a test email to [recipient] if [callerId] belongs to the root account and an SMTP
     * delivery service is available.
     *
     * Returns `false` immediately when the caller is not found, is not root, or [emailsService] is
     * `null`.
     *
     * @param callerId Caller whose username is checked against the root account.
     * @param recipient Target address for the test message.
     * @return `true` when delivery succeeded; `false` when the caller lacks privilege, SMTP is
     *   unavailable, or SMTP delivery fails.
     */
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean {
        val caller = usersRepo.getById(callerId) ?: return false
        if (caller.username.string != rootUsername) return false
        val service = emailsService ?: return false
        return service.sendText(
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
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        updateStoredEmail(usersRepo, callerId, email)
}
