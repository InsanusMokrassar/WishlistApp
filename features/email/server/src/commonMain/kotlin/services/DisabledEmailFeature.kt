package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.email.server.EmailFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * No-op [EmailFeature] implementation used when SMTP is not configured (no
 * [dev.inmo.wishlist.features.email.server.EmailsService] is registered in the DI graph).
 *
 * Substituted for [EmailFeatureService] by [dev.inmo.wishlist.features.email.server.Plugin]'s
 * `single<EmailFeature>` definition whenever `getOrNull<dev.inmo.wishlist.features.email.server.EmailsService>()`
 * returns `null`. [isFeatureEnabled] and [sendTestEmail] are pure no-ops — there is no SMTP
 * transport to send through. [setMyEmail] is deliberately NOT a no-op: per `features/email/README.md`,
 * per-user email-address storage is intentionally independent of SMTP configuration, so this class
 * still persists the caller's address via [usersRepo], identically to [EmailFeatureService.setMyEmail]
 * (both delegate to the shared [updateStoredEmail] helper).
 *
 * @param usersRepo User repository used to persist the caller's stored email address.
 */
class DisabledEmailFeature(
    private val usersRepo: UsersRepo
) : EmailFeature {

    /**
     * Always returns `false` — SMTP is not configured.
     *
     * @return `false`.
     */
    override suspend fun isFeatureEnabled(): Boolean = false

    /**
     * Always returns `false` — there is no SMTP transport to send a test message through.
     *
     * @param callerId Ignored.
     * @param recipient Ignored.
     * @return `false`.
     */
    override suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean = false

    /**
     * Updates or clears the stored email address for [callerId].
     *
     * Delegates to [updateStoredEmail] — identical behavior to [EmailFeatureService.setMyEmail],
     * since storage is independent of SMTP configuration.
     *
     * @param callerId User whose record is updated.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found.
     */
    override suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean =
        updateStoredEmail(usersRepo, callerId, email)
}
