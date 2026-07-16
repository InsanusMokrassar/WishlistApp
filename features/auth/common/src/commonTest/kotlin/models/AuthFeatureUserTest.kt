package dev.inmo.wishlist.features.auth.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [AuthFeatureUser]'s wire shape (which deliberately keeps `email`, unlike
 * [dev.inmo.wishlist.features.users.common.models.UsersFeatureUser]) and its [asAuthFeatureUser]
 * mapper.
 */
class AuthFeatureUserTest {

    /** Encoded JSON carries exactly `id`/`username`/`email` — `email` is kept deliberately on this own-record surface. */
    @Test
    fun serializedFormContainsExactlyIdUsernameAndEmail() {
        val user = AuthFeatureUser(UserId(1L), Username("alice"), Email("alice@example.com"))

        val json = Json.encodeToJsonElement(AuthFeatureUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username", "email"), json.keys)
    }

    /** A [RegisteredUser] with a non-null email maps every field through unchanged, including email. */
    @Test
    fun mapperCarriesNonNullEmailThrough() {
        val registered = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))

        val projected = registered.asAuthFeatureUser()

        assertEquals(AuthFeatureUser(UserId(7L), Username("bob"), Email("bob@example.com")), projected)
    }

    /** A [RegisteredUser] with no email maps to a null email, not a default/placeholder value. */
    @Test
    fun mapperCarriesNullEmailThrough() {
        val registered = RegisteredUser(UserId(8L), Username("carol"), null)

        val projected = registered.asAuthFeatureUser()

        assertEquals(AuthFeatureUser(UserId(8L), Username("carol"), null), projected)
    }

    /** Round trip base → feature → base restores the original unchanged — no extra arguments required. */
    @Test
    fun reverseMapperRoundTripsToOriginalRegisteredUser() {
        val original = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))

        assertEquals(original, original.asAuthFeatureUser().asRegisteredUser())
    }
}
