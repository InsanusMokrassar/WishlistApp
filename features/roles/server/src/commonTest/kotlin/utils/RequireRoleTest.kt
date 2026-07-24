package dev.inmo.wishlist.features.roles.server.utils

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
 * Verifies [isRoleRequirementSatisfied], the pure allow/deny decision behind [requireRole]. Each test
 * builds its own [MapFeatureRolesRegistry] from an explicit requirement list, so there is no shared
 * mutable state between tests.
 */
class RequireRoleTest {
    private val callerId = UserId(42L)
    private val subject = BaseRoleSubject.Direct(callerId.long.toString())
    private val role = BaseRole("RequireRoleTest.Role")
    private val functionalityId = FunctionalityId("requireRoleTest.gated")

    private fun registryOf(vararg requirements: FeatureRolesRegistry.Requirement) =
        MapFeatureRolesRegistry(requirements.toList())

    /** Registered functionality + subject holding the required role → allowed. */
    @Test
    fun allowsWhenRegisteredRoleIsHeld() = runTest {
        val registry = registryOf(FeatureRolesRegistry.Requirement(functionalityId, role))
        val rolesRepo = FakeRolesRepo().apply { includeDirect(subject, role) }

        assertTrue(isRoleRequirementSatisfied(registry, functionalityId, callerId, rolesRepo))
    }

    /** Registered functionality + subject missing the required role → denied. */
    @Test
    fun deniesWhenRequiredRoleIsMissing() = runTest {
        val registry = registryOf(FeatureRolesRegistry.Requirement(functionalityId, role))
        val rolesRepo = FakeRolesRepo()

        assertFalse(isRoleRequirementSatisfied(registry, functionalityId, callerId, rolesRepo))
    }

    /** Never-registered functionality → denied without needing any role held (fail-closed on a typo). */
    @Test
    fun deniesWhenFunctionalityIdWasNeverRegistered() = runTest {
        val registry = registryOf()
        val rolesRepo = FakeRolesRepo().apply { includeDirect(subject, role) }

        assertFalse(
            isRoleRequirementSatisfied(registry, FunctionalityId("requireRoleTest.neverRegistered"), callerId, rolesRepo)
        )
    }
}
