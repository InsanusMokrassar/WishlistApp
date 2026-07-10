package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.WriteCRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Write-only surface of the users CRUD repository.
 *
 * The only production implementation whose writes can fail on a constraint collision is the
 * JVM-only [dev.inmo.wishlist.features.users.common.repo.ExposedUsersRepo] (reached through
 * [CacheUsersRepo]): its `update`/`create` throw
 * [dev.inmo.wishlist.features.users.common.repo.exceptions.DuplicateUserFieldException] when the
 * given username or non-null email already belongs to a different user. See that exception's
 * KDoc for the full propagation path.
 */
interface WriteUsersRepo : WriteCRUDRepo<RegisteredUser, UserId, NewUser>
