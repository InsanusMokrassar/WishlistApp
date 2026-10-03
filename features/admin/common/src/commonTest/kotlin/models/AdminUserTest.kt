package dev.inmo.wishlist.features.admin.common.models

import dev.inmo.wishlist.features.email.common.models.Email
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies [AdminUser]'s reduced root-only identity wire shape and mapper. */
class AdminUserTest {

    /** Encoded JSON carries current email identity and approval but no verification lifecycle. */
    @Test
    fun serializedFormCarriesExactCurrentIdentity() {
        val user = AdminUser(
            UserId(1L),
            Username("alice"),
            Email("approved@example.com"),
            emailApproved = true,
        )

        val json = Json.encodeToJsonElement(AdminUser.serializer(), user).jsonObject

        assertEquals(
            setOf("id", "username", "email", "emailApproved"),
            AdminUser.serializer().descriptor.run { (0 until elementsCount).map(::getElementName).toSet() },
        )
        assertEquals(setOf("id", "username", "email", "emailApproved"), json.keys)
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

    /** Round trip base → feature → base restores current email identity and approval. */
    @Test
    fun reverseMapperRoundTripsCurrentIdentity() {
        val original = RegisteredUser(
            UserId(7L),
            Username("bob"),
            Email("approved@example.com"),
            emailApproved = true,
        )

        assertEquals(original, original.asAdminUser().asRegisteredUser())
    }

    /** Old lifecycle keys decode compatibly but never reappear in the admin payload. */
    @Test
    fun oldLifecycleKeysDecodeWithoutReemission() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val decoded = json.decodeFromString<AdminUser>(
            """{"id":1,"username":"alice","email":"alice@example.com","emailApproved":true,"pendingEmail":"pending@example.com","emailChangeRequestedAt":1,"emailChangeAllowedAt":2}""",
        )

        assertEquals(AdminUser(UserId(1L), Username("alice"), Email("alice@example.com"), true), decoded)
        assertEquals(
            setOf("id", "username", "email", "emailApproved"),
            json.encodeToJsonElement(AdminUser.serializer(), decoded).jsonObject.keys,
        )
    }
}
