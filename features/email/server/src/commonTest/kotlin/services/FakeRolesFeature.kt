package dev.inmo.wishlist.features.email.server.services

import dev.inmo.wishlist.features.roles.common.FunctionalityId
import dev.inmo.wishlist.features.roles.server.RolesFeature
import dev.inmo.wishlist.features.users.common.models.UserId

/**
 * Fixed-answer [RolesFeature] test double: [isFunctionalityAvailable] always returns [result]
 * regardless of the queried user/functionality. Records every call's `(UserId, FunctionalityId)` in
 * [calls] so tests can assert exactly which caller and functionality were checked.
 *
 * @param result Fixed answer every [isFunctionalityAvailable] call returns.
 */
internal class FakeRolesFeature(
    private val result: Boolean = false
) : RolesFeature {
    /** Every `(UserId, FunctionalityId)` passed to [isFunctionalityAvailable], in call order. */
    val calls = mutableListOf<Pair<UserId, FunctionalityId>>()

    override suspend fun isFunctionalityAvailable(userId: UserId, functionalityId: FunctionalityId): Boolean {
        calls.add(userId to functionalityId)
        return result
    }
}
