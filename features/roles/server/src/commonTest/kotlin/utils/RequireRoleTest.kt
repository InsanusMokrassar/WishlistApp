package dev.inmo.wishlist.features.roles.server.utils

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.roles.BaseRole
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import dev.inmo.wishlist.features.roles.server.FakeRolesRepo
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [isRoleRequirementSatisfied], the pure allow/deny decision behind [requireRole]. Every
 * test registers its own unique feature id into the process-wide [FeatureRolesRegistry] singleton to
 * stay independent of test execution order and of any other test class sharing the same JVM.
 */
class RequireRoleTest {
    private val callerId = UserId(42L)
    private val subject = BaseRoleSubject.Direct(callerId.long.toString())
    private val role = BaseRole("RequireRoleTest.Role")

    /** Registered id + subject holding the required role → allowed. */
    @Test
    fun allowsWhenRegisteredRoleIsHeld() = runTest {
        val featureId = "requireRoleTest.allowed"
        FeatureRolesRegistry.register(featureId, role)
        val rolesRepo = FakeRolesRepo().apply { includeDirect(subject, role) }

        assertTrue(isRoleRequirementSatisfied(featureId, callerId, rolesRepo))
    }

    /** Registered id + subject missing the required role → denied. */
    @Test
    fun deniesWhenRequiredRoleIsMissing() = runTest {
        val featureId = "requireRoleTest.denied"
        FeatureRolesRegistry.register(featureId, role)
        val rolesRepo = FakeRolesRepo()

        assertFalse(isRoleRequirementSatisfied(featureId, callerId, rolesRepo))
    }

    /** Never-registered id → denied without needing any role held (fail-closed on a typo). */
    @Test
    fun deniesWhenFeatureIdWasNeverRegistered() = runTest {
        val rolesRepo = FakeRolesRepo().apply { includeDirect(subject, role) }

        assertFalse(isRoleRequirementSatisfied("requireRoleTest.neverRegistered", callerId, rolesRepo))
    }
}
