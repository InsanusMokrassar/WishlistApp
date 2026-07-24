package dev.inmo.wishlist.features.roles.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Verifies [MapFeatureRolesRegistry] (the [FeatureRolesRegistry] realization): requirement→role
 * lookup, tolerance of exact-duplicate requirements, fail-fast on a conflicting requirement, and the
 * "never registered" `null` case. Each test constructs its own registry from an explicit requirement
 * list, so there is no shared mutable state between tests.
 */
class FeatureRolesRegistryTest {

    private val functionalityId = FunctionalityId("featureRolesRegistryTest.roundTrip")

    /** A registered requirement is resolved back to its role. */
    @Test
    fun requiredRoleReturnsRegisteredRole() {
        val registry = MapFeatureRolesRegistry(
            listOf(FeatureRolesRegistry.Requirement(functionalityId, SuperAdminRole))
        )

        assertEquals(SuperAdminRole, registry.requiredRole(functionalityId))
    }

    /** Two identical requirements for one id (same role) fold cleanly — no throw. */
    @Test
    fun duplicateSameRoleRequirementDoesNotThrow() {
        val id = FunctionalityId("featureRolesRegistryTest.sameRoleTwice")
        val registry = MapFeatureRolesRegistry(
            listOf(
                FeatureRolesRegistry.Requirement(id, UserRole),
                FeatureRolesRegistry.Requirement(id, UserRole)
            )
        )

        assertEquals(UserRole, registry.requiredRole(id))
    }

    /** Two requirements assigning different roles to one id fail construction fast. */
    @Test
    fun conflictingRoleRequirementThrows() {
        val id = FunctionalityId("featureRolesRegistryTest.conflictingRole")

        assertFailsWith<IllegalStateException> {
            MapFeatureRolesRegistry(
                listOf(
                    FeatureRolesRegistry.Requirement(id, UserRole),
                    FeatureRolesRegistry.Requirement(id, SuperAdminRole)
                )
            )
        }
    }

    /** A never-registered id resolves to `null`. */
    @Test
    fun requiredRoleForNeverRegisteredIdIsNull() {
        val registry = MapFeatureRolesRegistry(emptyList())

        assertNull(registry.requiredRole(FunctionalityId("featureRolesRegistryTest.neverRegistered")))
    }
}
