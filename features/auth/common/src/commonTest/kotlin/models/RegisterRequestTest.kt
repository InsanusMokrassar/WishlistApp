package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
