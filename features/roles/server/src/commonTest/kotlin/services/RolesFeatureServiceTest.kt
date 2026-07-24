package dev.inmo.wishlist.features.roles.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import dev.inmo.wishlist.features.roles.common.FunctionalityId
import dev.inmo.wishlist.features.roles.common.MapFeatureRolesRegistry
import dev.inmo.wishlist.features.roles.server.FakeRolesRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [RolesFeatureService.isFunctionalityAvailable]'s three cases: registered functionality with
 * the required role held, registered functionality with the role missing, and an unregistered
 * functionality (fail-closed). Each test builds its own registry + repo, so there is no shared state.
 */
class RolesFeatureServiceTest {
    private val userId = UserId(7L)
    private val subject = BaseRoleSubject.Direct(userId.long.toString())
    private val role = BaseRole("RolesFeatureServiceTest.Role")
    private val functionalityId = FunctionalityId("rolesFeatureServiceTest.gated")

    private fun registryOf(vararg requirements: FeatureRolesRegistry.Requirement) =
        MapFeatureRolesRegistry(requirements.toList())

    /** Registered functionality + user holds the required role → available. */
    @Test
    fun availableWhenRegisteredAndRoleHeld() = runTest {
        val registry = registryOf(FeatureRolesRegistry.Requirement(functionalityId, role))
        val rolesRepo = FakeRolesRepo().apply { includeDirect(subject, role) }
        val service = RolesFeatureService(registry, rolesRepo)

        assertTrue(service.isFunctionalityAvailable(userId, functionalityId))
    }

    /** Registered functionality + user missing the required role → unavailable. */
    @Test
    fun unavailableWhenRoleMissing() = runTest {
        val registry = registryOf(FeatureRolesRegistry.Requirement(functionalityId, role))
        val service = RolesFeatureService(registry, FakeRolesRepo())

        assertFalse(service.isFunctionalityAvailable(userId, functionalityId))
    }

    /** Never-registered functionality → unavailable even with a role held (fail-closed on a typo). */
    @Test
    fun unavailableWhenFunctionalityUnregistered() = runTest {
        val rolesRepo = FakeRolesRepo().apply { includeDirect(subject, role) }
        val service = RolesFeatureService(registryOf(), rolesRepo)

        assertFalse(service.isFunctionalityAvailable(userId, FunctionalityId("rolesFeatureServiceTest.never")))
    }
}
