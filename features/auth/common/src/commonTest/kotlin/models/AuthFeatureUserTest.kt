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
 * Verifies [AuthFeatureUser]'s wire shape (which deliberately keeps private email approval data, unlike
 * [dev.inmo.wishlist.features.users.common.models.UsersFeatureUser]) and its [asAuthFeatureUser]
 * mapper.
 */
class AuthFeatureUserTest {

    /** Encoded JSON carries private email approval when true on this own-record surface. */
    @Test
    fun serializedFormContainsEmailAndApprovalWhenApproved() {
        val user = AuthFeatureUser(UserId(1L), Username("alice"), Email("alice@example.com"), emailApproved = true)

        val json = Json.encodeToJsonElement(AuthFeatureUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username", "email", "emailApproved"), json.keys)
    }

    /** A [RegisteredUser] with an approved non-null email maps every private own-record field unchanged. */
    @Test
    fun mapperCarriesNonNullEmailAndApprovalThrough() {
        val registered = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"), emailApproved = true)

        val projected = registered.asAuthFeatureUser()

        assertEquals(AuthFeatureUser(UserId(7L), Username("bob"), Email("bob@example.com"), emailApproved = true), projected)
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
        val original = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"), emailApproved = true)

        assertEquals(original, original.asAuthFeatureUser().asRegisteredUser())
    }
}
