package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.repo.UsersRepo

/**
 * Updates or clears the stored email address for [callerId] via [usersRepo].
 *
 * Shared by [EmailFeatureService.setMyEmail] and [DisabledEmailFeature.setMyEmail] — per-user
 * email-address storage is intentionally independent of SMTP configuration (see
 * `features/email/README.md`), so both [dev.inmo.wishlist.features.email.server.EmailFeature]
 * implementations must persist through this identical path.
 *
 * @param usersRepo User repository used to look up and update the caller's record.
 * @param callerId User whose record is updated.
 * @param email New address to store, or `null` to clear the current address.
 * @return `true` when the update was persisted; `false` when the user was not found.
 * @throws dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException
 *   when [email] is already stored for a different user. This function does not catch it, by
 *   design — [dev.inmo.wishlist.features.email.server.configurators.EmailRoutingsConfigurator]
 *   catches it at the HTTP boundary and responds `409 Conflict`.
 */
internal suspend fun updateStoredEmail(usersRepo: UsersRepo, callerId: UserId, email: Email?): Boolean {
    val user = usersRepo.getById(callerId) ?: return false
    return usersRepo.update(callerId, NewUser(user.username, email)) != null
}
