package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [AdminUser]'s wire shape (deliberately keeps private email approval data — root-only surface) and its
 * [asAdminUser] mapper.
 */
class AdminUserTest {

    /** Encoded JSON carries every non-null lifecycle field — kept deliberately on this root-only surface. */
    @Test
    fun serializedFormCarriesCompletePrivateLifecycle() {
        val user = AdminUser(
            UserId(1L),
            Username("alice"),
            Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("pending@example.com"),
            emailChangeAllowedAt = 1_798_761_600_000L,
        )

        val json = Json.encodeToJsonElement(AdminUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username", "email", "emailApproved", "pendingEmail", "emailChangeAllowedAt"), json.keys)
        assertEquals(user, Json.decodeFromJsonElement(AdminUser.serializer(), json))
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

    /** Round trip base → feature → base restores every non-null lifecycle field unchanged. */
    @Test
    fun reverseMapperRoundTripsCompletePrivateLifecycle() {
        val original = RegisteredUser(
            UserId(7L),
            Username("bob"),
            Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("pending@example.com"),
            emailChangeAllowedAt = 1_798_761_600_000L,
        )

        assertEquals(original, original.asAdminUser().asRegisteredUser())
    }

    /** Legacy payloads without lifecycle keys decode to the additive null/default state. */
    @Test
    fun legacyPayloadDefaultsNewLifecycleFieldsToNull() {
        val decoded = Json.decodeFromString(
            AdminUser.serializer(),
            """{"id":1,"username":"alice","email":"alice@example.com"}""",
        )

        assertEquals(AdminUser(UserId(1L), Username("alice"), Email("alice@example.com")), decoded)
    }
}
