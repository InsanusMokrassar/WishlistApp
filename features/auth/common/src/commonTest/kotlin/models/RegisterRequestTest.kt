package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.auth.common.AuthFeature
import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

/** Legacy-style auth implementation used to verify the default configuration method. */
private class LegacyAuthFeature : AuthFeature {
    /** Legacy login surface has no transport behavior in this compatibility fixture. */
    override suspend fun login(username: Username, password: Password): AuthCredentials? = null

    /** Legacy refresh surface has no transport behavior in this compatibility fixture. */
    override suspend fun refresh(refreshToken: RefreshToken): AuthCredentials? = null

    /** Legacy registration surface has no transport behavior in this compatibility fixture. */
    override suspend fun register(
        username: Username,
        password: Password,
        email: Email?,
    ): AuthCredentials? = null

    /** Legacy implementation still provides the registration availability flag. */
    override suspend fun isRegistrationAvailable(): Boolean = true
}

/** Verifies optional registration-email wire compatibility. */
class RegisterRequestTest {
    /** An explicit email round-trips through the shared JSON contract. */
    @Test
    fun emailRoundTrips() {
        val request = RegisterRequest(Username("alice"), Password("password"), Email("alice@example.com"))

        val decoded = Json.decodeFromString<RegisterRequest>(Json.encodeToString(request))

        assertEquals(request, decoded)
    }

    /** Payloads produced before email support decode with a null email. */
    @Test
    fun omittedEmailRemainsCompatible() {
        val decoded = Json.decodeFromString<RegisterRequest>(
            "{\"username\":\"alice\",\"password\":\"password\"}"
        )

        assertEquals(null, decoded.email)
    }

    /** Omitted auth email policy keeps the backward-compatible false default. */
    @Test
    fun omittedEmailRequirementDefaultsToFalse() {
        val decoded = Json.decodeFromString<AuthConfig>("{\"enableRegistration\":true}")

        assertEquals(AuthConfig(enableRegistration = true), decoded)
    }

    /** Explicit auth email policy is retained by JSON decoding. */
    @Test
    fun explicitEmailRequirementIsDecoded() {
        val decoded = Json.decodeFromString<AuthConfig>(
            "{\"enableRegistration\":true,\"requireEmailForRegistration\":true}"
        )

        assertEquals(AuthConfig(true, true), decoded)
    }

    /** Legacy implementations receive optional-email defaults without overriding [AuthFeature.getConfig]. */
    @Test
    fun legacyAuthFeatureUsesDefaultConfigCompatibility() = kotlinx.coroutines.test.runTest {
        assertEquals(AuthConfig(enableRegistration = true), LegacyAuthFeature().getConfig())
    }
}
