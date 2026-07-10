package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Fixed-answer [SimpleRolesFeature] test double: [isSuperAdmin] always returns [result] regardless of
 * the queried [UserId]. Records every call's [UserId] in [calls] so tests can assert exactly which
 * caller was checked.
 *
 * @param result Fixed answer every [isSuperAdmin] call returns.
 */
internal class FakeSimpleRolesFeature(
    private val result: Boolean = false
) : SimpleRolesFeature {
    /** Every [UserId] passed to [isSuperAdmin], in call order. */
    val calls = mutableListOf<UserId>()

    override suspend fun isSuperAdmin(userId: UserId): Boolean {
        calls.add(userId)
        return result
    }
}
