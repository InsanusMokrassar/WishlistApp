package dev.inmo.wishlist.features.email.common.models

import dev.inmo.wishlist.features.email.common.utils.emailDraftBaseline
import dev.inmo.wishlist.features.email.common.utils.verificationCandidate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

/** Verifies the email feature's owner-state wire model and lifecycle-only helper functions. */
class EmailProfileTest {
    /** A populated profile round-trips every owned lifecycle property with the expected JSON keys. */
    @Test
    fun populatedProfileRoundTrips() {
        val profile = EmailProfile(
            userId = 7L,
            email = Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("replacement@example.com"),
            emailChangeRequestedAt = 1_700_000_000_000L,
            emailChangeAllowedAt = 1_700_000_060_000L,
        )

        val encoded = Json.encodeToJsonElement(EmailProfile.serializer(), profile)

        assertEquals(
            setOf(
                "userId",
                "email",
                "emailApproved",
                "pendingEmail",
                "emailChangeRequestedAt",
                "emailChangeAllowedAt",
            ),
            encoded.jsonObject.keys,
        )
        assertEquals(profile, Json.decodeFromJsonElement(EmailProfile.serializer(), encoded))
    }

    /** Omitted optional state decodes to an empty account and retains unknown legacy request time. */
    @Test
    fun omittedOptionalFieldsDecodeDefaults() {
        assertEquals(
            EmailProfile(userId = 7L),
            Json.decodeFromString(EmailProfile.serializer(), "{\"userId\":7}"),
        )
        assertEquals(
            EmailProfile(
                userId = 8L,
                email = Email("approved@example.com"),
                emailApproved = true,
                pendingEmail = Email("legacy-pending@example.com"),
            ),
            Json.decodeFromString(
                EmailProfile.serializer(),
                "{\"userId\":8,\"email\":\"approved@example.com\",\"emailApproved\":true,\"pendingEmail\":\"legacy-pending@example.com\"}",
            ),
        )
    }

    /** Missing identity, invalid addresses, wrong scalar types, and JSON null never decode as profiles. */
    @Test
    fun invalidRequiredIdentityOrEmailFails() {
        assertFails { Json.decodeFromString(EmailProfile.serializer(), "{}") }
        assertFails { Json.decodeFromString(EmailProfile.serializer(), "{\"userId\":false}") }
        assertFails { Json.decodeFromString(EmailProfile.serializer(), "{\"userId\":7,\"email\":\"not-an-email\"}") }
        assertFails { Json.decodeFromString(EmailProfile.serializer(), "null") }
    }

    /** Candidate and draft selection prioritize a replacement without losing first-address behavior. */
    @Test
    fun candidateAndDraftUsePendingFirst() {
        val initial = Email("initial@example.com")
        val approved = Email("approved@example.com")
        val pending = Email("pending@example.com")

        assertNull(EmailProfile(userId = 1L).verificationCandidate())
        assertNull(EmailProfile(userId = 1L).emailDraftBaseline())
        assertEquals(initial, EmailProfile(userId = 1L, email = initial).verificationCandidate())
        assertEquals(initial, EmailProfile(userId = 1L, email = initial).emailDraftBaseline())
        assertNull(EmailProfile(userId = 1L, email = approved, emailApproved = true).verificationCandidate())
        assertEquals(approved, EmailProfile(userId = 1L, email = approved, emailApproved = true).emailDraftBaseline())
        val replacement = EmailProfile(userId = 1L, email = approved, emailApproved = true, pendingEmail = pending)
        assertEquals(pending, replacement.verificationCandidate())
        assertEquals(pending, replacement.emailDraftBaseline())
    }
}
