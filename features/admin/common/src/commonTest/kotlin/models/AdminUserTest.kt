package dev.inmo.wishlist.features.admin.common.models

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
 * Verifies [AdminUser]'s wire shape (deliberately keeps `email` — root-only surface) and its
 * [asAdminUser] mapper.
 */
class AdminUserTest {

    /** Encoded JSON carries exactly `id`/`username`/`email` — kept deliberately on this root-only surface. */
    @Test
    fun serializedFormContainsExactlyIdUsernameAndEmail() {
        val user = AdminUser(UserId(1L), Username("alice"), Email("alice@example.com"))

        val json = Json.encodeToJsonElement(AdminUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username", "email"), json.keys)
    }

    /** A [RegisteredUser] with a non-null email maps every field through unchanged. */
    @Test
    fun mapperCarriesNonNullEmailThrough() {
        val registered = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"))

        assertEquals(AdminUser(UserId(7L), Username("bob"), Email("bob@example.com")), registered.asAdminUser())
    }

    /** A [RegisteredUser] with no email maps to a null email. */
    @Test
    fun mapperCarriesNullEmailThrough() {
        val registered = RegisteredUser(UserId(8L), Username("carol"), null)

        assertEquals(AdminUser(UserId(8L), Username("carol"), null), registered.asAdminUser())
    }
}
