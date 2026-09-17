package dev.inmo.wishlist.features.users.common.models

import dev.inmo.wishlist.features.email.common.models.Email
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

    /** A populated private lifecycle projects to exactly the public id/username wire shape. */
    @Test
    fun mapperDropsCompletePrivateLifecycle() {
        val registered = RegisteredUser(
            UserId(7L),
            Username("bob"),
            Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("pending@example.com"),
            emailChangeAllowedAt = 1_798_761_600_000L,
        )

        val projected = registered.asUsersFeatureUser()

        assertEquals(UsersFeatureUser(UserId(7L), Username("bob")), projected)
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

    /** Reverse conversion needs all four private lifecycle values to preserve a populated source row. */
    @Test
    fun reverseMapperRequiresAllPrivateLifecycleArguments() {
        val original = RegisteredUser(
            UserId(7L),
            Username("bob"),
            Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("pending@example.com"),
            emailChangeAllowedAt = 1_798_761_600_000L,
        )

        val restored = original.asUsersFeatureUser().asRegisteredUser(
            email = original.email,
            emailApproved = original.emailApproved,
            pendingEmail = original.pendingEmail,
            emailChangeAllowedAt = original.emailChangeAllowedAt,
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
            pendingEmail = null,
            emailChangeAllowedAt = null,
        )

        assertEquals(original, restored)
    }
}
