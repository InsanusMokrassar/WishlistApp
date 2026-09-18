package dev.inmo.wishlist.features.users.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [UsersFeatureUser]'s wire shape and its [asUsersFeatureUser] mapper. This is the regression
 * test for the issue #67 leak: [RegisteredUser.email] must never reach the public users listing.
 */
class UsersFeatureUserTest {

    /** Encoded JSON carries exactly the `id`/`username` keys — no private email fields are present. */
    @Test
    fun serializedFormContainsExactlyIdAndUsernameNoEmail() {
        val user = UsersFeatureUser(id = UserId(1L), username = Username("alice"))

        val json = Json.encodeToJsonElement(UsersFeatureUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username"), json.keys)
    }

    /** Reduced persistence and public serializers expose only their intentional fields. */
    @Test
    fun serializersExposeExactCurrentIdentityAndPublicKeys() {
        val registered = RegisteredUser(
            UserId(7L),
            Username("bob"),
            Email("approved@example.com"),
            emailApproved = true,
        )

        val projected = registered.asUsersFeatureUser()

        assertEquals(UsersFeatureUser(UserId(7L), Username("bob")), projected)
        assertEquals(
            setOf("id", "username", "email", "emailApproved"),
            RegisteredUser.serializer().descriptor.run { (0 until elementsCount).map(::getElementName).toSet() },
        )
        assertEquals(
            setOf("id", "username"),
            Json.encodeToJsonElement(UsersFeatureUser.serializer(), projected).jsonObject.keys,
        )
    }

    /** A [RegisteredUser] with no email still maps id/username correctly. */
    @Test
    fun mapperHandlesNullEmail() {
        val registered = RegisteredUser(UserId(8L), Username("carol"), null)

        val projected = registered.asUsersFeatureUser()

        assertEquals(UsersFeatureUser(UserId(8L), Username("carol")), projected)
    }

    /** Reverse conversion needs explicit current email and approval values. */
    @Test
    fun reverseMapperRequiresCurrentEmailAndApprovalArguments() {
        val original = RegisteredUser(
            UserId(7L),
            Username("bob"),
            Email("approved@example.com"),
            emailApproved = true,
        )

        val restored = original.asUsersFeatureUser().asRegisteredUser(
            email = original.email,
            emailApproved = original.emailApproved,
        )

        assertEquals(original, restored)
    }

    /** Round trip with absent email and false approval passed consciously preserves the original. */
    @Test
    fun reverseMapperPreservesNullEmailRoundTrip() {
        val original = RegisteredUser(UserId(8L), Username("carol"), null)

        val restored = original.asUsersFeatureUser().asRegisteredUser(
            email = null,
            emailApproved = false,
        )

        assertEquals(original, restored)
    }

    /** Old lifecycle keys decode compatibly but reduced user JSON never re-emits them. */
    @Test
    fun oldLifecycleKeysDecodeWithoutReemission() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        val decoded = json.decodeFromString<RegisteredUser>(
            """{"id":7,"username":"bob","email":"approved@example.com","emailApproved":true,"pendingEmail":"pending@example.com","emailChangeRequestedAt":1,"emailChangeAllowedAt":2}""",
        )
        val encoded = json.encodeToJsonElement(RegisteredUser.serializer(), decoded).jsonObject

        assertEquals(RegisteredUser(UserId(7L), Username("bob"), Email("approved@example.com"), true), decoded)
        assertEquals(setOf("id", "username", "email", "emailApproved"), encoded.keys)
    }
}
