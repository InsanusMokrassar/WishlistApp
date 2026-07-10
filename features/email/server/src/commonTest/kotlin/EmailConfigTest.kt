package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.common.models.Email
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Verifies [EmailConfig]'s decode contract in isolation — a plain [Json] instance, no Koin/Plugin
 * involved. [EmailConfig] is decoded from the *nested* `"email"` [kotlinx.serialization.json.JsonElement]
 * (the value already extracted from `config["email"]` by [Plugin]), not the whole root config object.
 */
class EmailConfigTest {

    /** Plain default [Json] instance used to decode every fixture in this class. */
    private val json = Json

    /** Fully-populated `smtp` object decodes every field exactly, including non-default ones. */
    @Test
    fun decodesAllFieldsWhenFullyPopulated() {
        val element = buildJsonObject {
            putJsonObject("smtp") {
                put("host", "smtp.example.com")
                put("port", 25)
                put("username", "user")
                put("password", "pass")
                put("from", "noreply@example.com")
                put("useTls", false)
                put("useSsl", true)
            }
        }

        val config = json.decodeFromJsonElement(EmailConfig.serializer(), element)

        assertEquals("smtp.example.com", config.smtp.host)
        assertEquals(25, config.smtp.port)
        assertEquals("user", config.smtp.username)
        assertEquals("pass", config.smtp.password)
        assertEquals(Email("noreply@example.com"), config.smtp.from)
        assertEquals(false, config.smtp.useTls)
        assertEquals(true, config.smtp.useSsl)
    }

    /** Omitted optional `smtp` fields fall back to [SmtpConfig]'s declared defaults. */
    @Test
    fun appliesSmtpDefaultsForOmittedOptionalFields() {
        val element = buildJsonObject {
            putJsonObject("smtp") {
                put("host", "smtp.example.com")
                put("from", "noreply@example.com")
            }
        }

        val config = json.decodeFromJsonElement(EmailConfig.serializer(), element)

        assertEquals(587, config.smtp.port)
        assertNull(config.smtp.username)
        assertNull(config.smtp.password)
        assertEquals(true, config.smtp.useTls)
        assertEquals(false, config.smtp.useSsl)
    }

    /** [EmailConfig.smtp] has no default: an `"email"` object with no `"smtp"` key fails to decode. */
    @Test
    fun decodingFailsWhenSmtpKeyIsMissing() {
        val element = buildJsonObject {}

        assertFailsWith<SerializationException> {
            json.decodeFromJsonElement(EmailConfig.serializer(), element)
        }
    }

    /** `smtp` is non-nullable: an explicit `"smtp": null` also fails to decode. */
    @Test
    fun decodingFailsWhenSmtpIsExplicitNull() {
        val element = buildJsonObject {
            put("smtp", JsonNull)
        }

        assertFailsWith<SerializationException> {
            json.decodeFromJsonElement(EmailConfig.serializer(), element)
        }
    }
}
