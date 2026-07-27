package dev.inmo.wishlist.features.email.server.services

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.deeplinks.common.DeepLinkHandler
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId
import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkId
import dev.inmo.wishlist.features.email.server.models.EmailVerification
import dev.inmo.wishlist.features.email.server.models.EmailVerificationPayload
import dev.inmo.wishlist.features.roles.server.promoteNewUserToUser
import dev.inmo.wishlist.features.users.common.repo.ReadUsersRepo

/**
 * Deeplink handler that approves an existing account and makes repeated link opens harmless.
 *
 * @param usersRepo User lookup used to reject stale or fabricated account ids.
 * @param rolesRepo Role graph updated by the approval transition.
 */
class EmailVerificationDeepLinkHandler(
    private val usersRepo: ReadUsersRepo,
    private val rolesRepo: RolesRepo,
) : DeepLinkHandler {
    /** Stable handler id persisted in email verification deeplinks. */
    override val id: DeepLinkHandlerId = EmailVerification.handlerId

    /**
     * Promotes the payload's account when the payload type and account still exist.
     *
     * @param deeplinkId Opened deeplink identifier; retained for the handler contract.
     * @param value Decoded polymorphic payload.
     * @return `true` when an existing account was approved; `false` for invalid or stale links.
     */
    override suspend fun tryHandle(deeplinkId: DeepLinkId, value: Any): Boolean {
        val payload = value as? EmailVerificationPayload ?: return false
        usersRepo.getById(payload.userId) ?: return false
        promoteNewUserToUser(rolesRepo, payload.userId)
        return true
    }
}
