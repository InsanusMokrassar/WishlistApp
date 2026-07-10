package dev.inmo.wishlist.features.simpleRoles.server.services

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.wishlist.features.roles.common.SuperAdminRole
import dev.inmo.wishlist.features.roles.common.UserRole
import dev.inmo.wishlist.features.users.common.models.UserId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [SimpleRolesFeatureService.isSuperAdmin]'s three cases: directly-granted SuperAdmin,
 * User-only (no SuperAdmin), and a completely unknown/never-granted [UserId].
 */
class SimpleRolesFeatureServiceTest {
    private val superAdminUserId = UserId(1L)
    private val plainUserId = UserId(2L)
    private val unknownUserId = UserId(999L)

    /** A subject with SuperAdmin directly granted → `true`. */
    @Test
    fun isSuperAdminReturnsTrueForSubjectWithSuperAdminRole() = runTest {
        val rolesRepo = FakeRolesRepo(
            mapOf(BaseRoleSubject.Direct(superAdminUserId.long.toString()) to setOf(UserRole, SuperAdminRole))
        )
        val service = SimpleRolesFeatureService(rolesRepo)

        assertTrue(service.isSuperAdmin(superAdminUserId))
    }

    /** A subject with only User (no SuperAdmin) → `false`. */
    @Test
    fun isSuperAdminReturnsFalseForSubjectWithOnlyUserRole() = runTest {
        val rolesRepo = FakeRolesRepo(
            mapOf(BaseRoleSubject.Direct(plainUserId.long.toString()) to setOf(UserRole))
        )
        val service = SimpleRolesFeatureService(rolesRepo)

        assertFalse(service.isSuperAdmin(plainUserId))
    }

    /** An unknown/never-granted [UserId] → `false`. */
    @Test
    fun isSuperAdminReturnsFalseForUnknownUser() = runTest {
        val service = SimpleRolesFeatureService(FakeRolesRepo())

        assertFalse(service.isSuperAdmin(unknownUserId))
    }
}
