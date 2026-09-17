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
 * Verifies [AdminUser]'s wire shape (deliberately keeps private email approval data — root-only surface) and its
 * [asAdminUser] mapper.
 */
class AdminUserTest {

    /** Encoded JSON carries email approval when true — kept deliberately on this root-only surface. */
    @Test
    fun serializedFormContainsEmailAndApprovalWhenApproved() {
        val user = AdminUser(UserId(1L), Username("alice"), Email("alice@example.com"), emailApproved = true)

        val json = Json.encodeToJsonElement(AdminUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username", "email", "emailApproved"), json.keys)
    }

    /** A [RegisteredUser] with an approved non-null email maps every root-only field unchanged. */
    @Test
    fun mapperCarriesNonNullEmailAndApprovalThrough() {
        val registered = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"), emailApproved = true)

        assertEquals(AdminUser(UserId(7L), Username("bob"), Email("bob@example.com"), emailApproved = true), registered.asAdminUser())
    }

    /** A [RegisteredUser] with no email maps to a null email. */
    @Test
    fun mapperCarriesNullEmailThrough() {
        val registered = RegisteredUser(UserId(8L), Username("carol"), null)

        assertEquals(AdminUser(UserId(8L), Username("carol"), null), registered.asAdminUser())
    }

    /** Round trip base → feature → base restores the original unchanged — no extra arguments required. */
    @Test
    fun reverseMapperRoundTripsToOriginalRegisteredUser() {
        val original = RegisteredUser(UserId(7L), Username("bob"), Email("bob@example.com"), emailApproved = true)

        assertEquals(original, original.asAdminUser().asRegisteredUser())
    }
}
