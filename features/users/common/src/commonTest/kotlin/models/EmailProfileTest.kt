package dev.inmo.wishlist.features.users.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.utils.emailDraftBaseline
import dev.inmo.wishlist.features.users.common.utils.verificationCandidate
import korlibs.time.DateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

/** Verifies the users feature's private owner-state wire model and lifecycle-only helper functions. */
class EmailProfileTest {
    /** A populated profile round-trips every owned lifecycle property with the expected JSON keys. */
    @Test
    fun populatedProfileRoundTrips() {
        val profile = dev.inmo.wishlist.features.users.common.models.EmailProfile(
            userId = 7L,
            email = Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("replacement@example.com"),
            emailChangeRequestedAt = DateTime.fromUnixMillis(1_700_000_000_000L),
            emailChangeAllowedAt = DateTime.fromUnixMillis(1_700_000_060_000L),
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

    /** Descriptor and explicit defaults retain the complete six-field owner contract after relocation. */
    @Test
    fun descriptorAndExplicitDefaultsKeepExactWireKeys() {
        val keys = setOf("userId", "email", "emailApproved", "pendingEmail", "emailChangeRequestedAt", "emailChangeAllowedAt")
        val descriptor = EmailProfile.serializer().descriptor
        assertEquals(keys, (0 until descriptor.elementsCount).map(descriptor::getElementName).toSet())
        val encoded = Json { encodeDefaults = true }.encodeToJsonElement(EmailProfile.serializer(), EmailProfile(7L))
        assertEquals(keys, encoded.jsonObject.keys)
        assertEquals(EmailProfile(7L), Json.decodeFromJsonElement(EmailProfile.serializer(), encoded))
    }

    /** Literal pre-relocation JSON and new output share the same concrete schema without a discriminator. */
    @Test
    fun literalOldWireRetainsEveryField() {
        val oldWire = """{"userId":7,"email":"approved@example.com","emailApproved":true,"pendingEmail":"replacement@example.com","emailChangeRequestedAt":1700000000000,"emailChangeAllowedAt":1700000060000}"""
        val expected = dev.inmo.wishlist.features.users.common.models.EmailProfile(
            userId = 7L,
            email = Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("replacement@example.com"),
            emailChangeRequestedAt = DateTime.fromUnixMillis(1_700_000_000_000L),
            emailChangeAllowedAt = DateTime.fromUnixMillis(1_700_000_060_000L),
        )
        assertEquals(expected, Json.decodeFromString(EmailProfile.serializer(), oldWire))
        val encoded = Json.encodeToJsonElement(EmailProfile.serializer(), expected).jsonObject
        val legacy = Json.parseToJsonElement(oldWire).jsonObject
        assertEquals(legacy.keys, encoded.keys)
        listOf("userId", "email", "emailApproved", "pendingEmail").forEach { key ->
            assertEquals(legacy.getValue(key), encoded.getValue(key))
        }
        listOf("emailChangeRequestedAt", "emailChangeAllowedAt").forEach { key ->
            assertEquals(legacy.getValue(key).jsonPrimitive.content.toDouble(), encoded.getValue(key).jsonPrimitive.content.toDouble())
        }
        assertEquals(expected, Json.decodeFromJsonElement(EmailProfile.serializer(), encoded))
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
        assertEquals(pending, replacement.copy(emailApproved = false).verificationCandidate())
        assertEquals(pending, replacement.copy(emailApproved = false).emailDraftBaseline())
    }

    /** Nullable lifecycle history accepts omitted, explicit null, and legacy integer values. */
    @Test
    fun nullableTimestampWireCompatibility() {
        val explicitNull = Json.decodeFromString(
            EmailProfile.serializer(),
            """{"userId":9,"emailChangeRequestedAt":null,"emailChangeAllowedAt":null}""",
        )
        assertNull(explicitNull.emailChangeRequestedAt)
        assertNull(explicitNull.emailChangeAllowedAt)
        listOf(-4_503_599_627_370_496L, 4_503_599_627_370_496L).forEach { millis ->
            val decoded = Json.decodeFromString(
                EmailProfile.serializer(),
                """{"userId":9,"emailChangeRequestedAt":$millis,"emailChangeAllowedAt":$millis}""",
            )
            assertEquals(DateTime.fromUnixMillis(millis), decoded.emailChangeRequestedAt)
            val encoded = Json.encodeToJsonElement(EmailProfile.serializer(), decoded).jsonObject
            assertEquals(millis.toDouble(), encoded.getValue("emailChangeAllowedAt").jsonPrimitive.content.toDouble())
            assertEquals(decoded, Json.decodeFromJsonElement(EmailProfile.serializer(), Json.encodeToJsonElement(EmailProfile.serializer(), decoded)))
            assertFails { decoded.copy(emailChangeAllowedAt = DateTime(millis.toDouble() + if (millis < 0) -1 else 1)) }
        }
        assertFails { Json.decodeFromString(EmailProfile.serializer(), """{"userId":9,"emailChangeRequestedAt":0.5}""") }
    }
}
