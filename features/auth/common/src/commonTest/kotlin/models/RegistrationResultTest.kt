package dev.inmo.wishlist.features.auth.common.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies the discriminated registration-success wire contract. */
class RegistrationResultTest {
    /** Authorized responses retain credentials under the stable `authorized` discriminator. */
    @Test
    fun authorizedRoundTripsInApplicationFormat() {
        val json = Json { useArrayPolymorphism = true }
        val result = RegistrationResult.Authorized(AuthCredentials(Token("access"), RefreshToken("refresh")))

        val encoded = json.encodeToString<RegistrationResult>(result)

        assertTrue(encoded.contains("authorized"))
        assertEquals(result, json.decodeFromString<RegistrationResult>(encoded))
    }

    /** Pending responses carry no credential-bearing fields. */
    @Test
    fun pendingRoundTripsWithoutCredentialFields() {
        val json = Json { useArrayPolymorphism = true }

        val encoded = json.encodeToString<RegistrationResult>(RegistrationResult.PendingEmailVerification)

        assertTrue(encoded.contains("pendingEmailVerification"))
        assertFalse(encoded.contains("credentials"))
        assertFalse(encoded.contains("token"))
        assertFalse(encoded.contains("refreshToken"))
        assertEquals(RegistrationResult.PendingEmailVerification, json.decodeFromString<RegistrationResult>(encoded))
    }
}
