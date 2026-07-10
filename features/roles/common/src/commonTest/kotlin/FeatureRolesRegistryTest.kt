package dev.inmo.wishlist.features.roles.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Verifies [FeatureRolesRegistry]: registration/lookup round-trip, same-role re-registration
 * idempotency, conflicting-role re-registration failure, and the "never registered" `null` case.
 * Every test uses its own unique `featureId` so tests remain independent of execution order within
 * this process-wide singleton.
 */
class FeatureRolesRegistryTest {

    /** `register` then `requiredRole` round-trips the same role. */
    @Test
    fun registerThenRequiredRoleRoundTrips() {
        val featureId = "featureRolesRegistryTest.roundTrip"
        FeatureRolesRegistry.register(featureId, SuperAdminRole)

        assertEquals(SuperAdminRole, FeatureRolesRegistry.requiredRole(featureId))
    }

    /** Re-registering the same id with the same role does not throw. */
    @Test
    fun reRegisteringSameRoleDoesNotThrow() {
        val featureId = "featureRolesRegistryTest.sameRoleTwice"
        FeatureRolesRegistry.register(featureId, UserRole)

        FeatureRolesRegistry.register(featureId, UserRole)

        assertEquals(UserRole, FeatureRolesRegistry.requiredRole(featureId))
    }

    /** Re-registering the same id with a different role throws. */
    @Test
    fun reRegisteringDifferentRoleThrows() {
        val featureId = "featureRolesRegistryTest.conflictingRole"
        FeatureRolesRegistry.register(featureId, UserRole)

        assertFailsWith<IllegalStateException> {
            FeatureRolesRegistry.register(featureId, SuperAdminRole)
        }
    }

    /** A never-registered id resolves to `null`. */
    @Test
    fun requiredRoleForNeverRegisteredIdIsNull() {
        assertNull(FeatureRolesRegistry.requiredRole("featureRolesRegistryTest.neverRegistered"))
    }
}
