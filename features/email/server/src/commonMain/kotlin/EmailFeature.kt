package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Server-side capability surface for the email feature.
 *
 * Unlike the client interface, every method that acts on behalf of a caller receives the
 * authenticated [UserId] explicitly. Implementations are responsible for enforcing access rules
 * (e.g. root-only for [sendTestEmail]) and for persisting caller-scoped state (e.g. [setMyEmail]).
 * The routing configurator only extracts the caller identity from the request context and
 * delegates here — authorization logic lives entirely in the implementation.
 */
interface EmailFeature {

    /**
     * Returns whether SMTP delivery is configured and operational.
     *
     * @return `true` when an SMTP host is configured; `false` otherwise.
     */
    suspend fun isFeatureEnabled(): Boolean

    /**
     * Sends a test email to [recipient] on behalf of [callerId].
     *
     * Implementations must verify that [callerId] is the root user and return `false` when it is
     * not. Returns `false` when the feature is disabled or an SMTP error occurs.
     *
     * @param callerId Authenticated caller whose privileges are checked.
     * @param recipient Target email address for the test message.
     * @return `true` when the message was accepted by the SMTP server; `false` otherwise.
     */
    suspend fun sendTestEmail(callerId: UserId, recipient: Email): Boolean

    /**
     * Updates or clears the email address stored for [callerId].
     *
     * Self-service — no elevated privilege required. Returns `false` when [callerId] is not found.
     *
     * @param callerId Authenticated caller whose email address is being changed.
     * @param email New address to store, or `null` to clear the current address.
     * @return `true` when the update was persisted; `false` when the user was not found or the
     *   update failed.
     * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
     *   when [email] is already stored for a different user.
     */
    suspend fun setMyEmail(callerId: UserId, email: Email?): Boolean
}
