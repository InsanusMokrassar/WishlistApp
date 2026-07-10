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

    /** Encoded JSON carries exactly the `id`/`username` keys — no `email` key present. */
    @Test
    fun serializedFormContainsExactlyIdAndUsernameNoEmail() {
        val user = UsersFeatureUser(id = UserId(1L), username = Username("alice"))

        val json = Json.encodeToJsonElement(UsersFeatureUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username"), json.keys)
    }

    /** A [RegisteredUser] with a non-null email maps to id/username and drops the email. */
    @Test
    fun mapperDropsNonNullEmail() {
        val registered = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))

        val projected = registered.asUsersFeatureUser()

        assertEquals(UsersFeatureUser(UserId(7L), Username("bob")), projected)
    }

    /** A [RegisteredUser] with no email still maps id/username correctly. */
    @Test
    fun mapperHandlesNullEmail() {
        val registered = RegisteredUser(UserId(8L), Username("carol"), null)

        val projected = registered.asUsersFeatureUser()

        assertEquals(UsersFeatureUser(UserId(8L), Username("carol")), projected)
    }
}
