package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * Server-side [EmailFeature] implementation that unifies SMTP delivery and user-email persistence.
 *
 * Delegates SMTP operations to [smtpEmailService]. Resolves the caller's user record from
 * [usersRepo] to enforce access rules and to perform email-address storage updates:
 * - [sendTestEmail] verifies that [callerId] is the root user before delegating SMTP delivery.
 * - [setMyEmail] updates the caller's stored email address directly via [usersRepo].
 *
 * @param smtpEmailService SMTP delivery service used for [isFeatureEnabled] and SMTP sends.
 * @param usersRepo User repository used for privilege checking and email-address persistence.
 */
class EmailFeatureService(
    private val smtpEmailService: SmtpEmailService,
    private val usersRepo: UsersRepo
) : EmailFeature {

    private val rootUsername = "root"

    /**
     * Returns whether the SMTP feature is enabled.
     *
     * @return Delegates to [SmtpEmailService.isFeatureEnabled].
     */
    override suspend fun isFeatureEnabled(): Boolean = smtpEmailService.isFeatureEnabled()

    /**
     * Sends a test email to [recipient] if [callerId] belongs to the root account.
     *
     * Returns `false` immediately when the caller is not found or is not root.
     *
     * @param callerId Caller whose username is checked against the root account.
     * @param recipient Target address for the test message.
     * @return `true` when delivery succeeded; `false` when the caller lacks privilege or SMTP fails.
     */
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean {
        val caller = usersRepo.getById(callerId) ?: return false
        if (caller.username.string != rootUsername) return false
        return smtpEmailService.sendTestEmail(recipient)
    }

    /**
     * Updates or clears the stored email address for [callerId].
     *
     * @param callerId User whose record is updated.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found or the
     *   repository returned no updated record.
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean {
        val user = usersRepo.getById(callerId) ?: return false
        return usersRepo.update(callerId, NewUser(user.username, email)) != null
    }
}
