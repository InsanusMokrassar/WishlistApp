package dev.inmo.wishlist.features.auth.common.models

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
 * Verifies [AuthFeatureUser]'s wire shape (which deliberately keeps private email approval data, unlike
 * [dev.inmo.wishlist.features.users.common.models.UsersFeatureUser]) and its [asAuthFeatureUser]
 * mapper.
 */
class AuthFeatureUserTest {

    /** Encoded JSON carries every non-null private lifecycle field on this own-record surface. */
    @Test
    fun serializedFormCarriesCompletePrivateLifecycle() {
        val user = AuthFeatureUser(
            UserId(1L),
            Username("alice"),
            Email("approved@example.com"),
            emailApproved = true,
            pendingEmail = Email("pending@example.com"),
            emailChangeAllowedAt = 1_798_761_600_000L,
        )

        val json = Json.encodeToJsonElement(AuthFeatureUser.serializer(), user).jsonObject

        assertEquals(setOf("id", "username", "email", "emailApproved", "pendingEmail", "emailChangeAllowedAt"), json.keys)
        assertEquals(user, Json.decodeFromJsonElement(AuthFeatureUser.serializer(), json))
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

        assertEquals(original, original.asAuthFeatureUser().asRegisteredUser())
    }

    /** Legacy payloads without lifecycle keys decode to the additive null/default state. */
    @Test
    fun legacyPayloadDefaultsNewLifecycleFieldsToNull() {
        val decoded = Json.decodeFromString(
            AuthFeatureUser.serializer(),
            """{"id":1,"username":"alice","email":"alice@example.com"}""",
        )

        assertEquals(AuthFeatureUser(UserId(1L), Username("alice"), Email("alice@example.com")), decoded)
    }
}
