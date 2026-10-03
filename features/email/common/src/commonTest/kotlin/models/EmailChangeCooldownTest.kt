package dev.inmo.wishlist.features.email.common.models

import korlibs.time.DateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

/** Checks the required cooldown deadline on both legacy and current numeric wires. */
class EmailChangeCooldownTest {
    /** Legacy integer input decodes and new numeric output keeps the established field key. */
    @Test
    fun numericDeadlineCompatibility() {
        listOf(-4_503_599_627_370_496L, 0L, 4_503_599_627_370_496L).forEach { millis ->
            val decoded = Json.decodeFromString(
                EmailChangeCooldown.serializer(),
                """{"emailChangeAllowedAt":$millis}""",
            )
            assertEquals(DateTime.fromUnixMillis(millis), decoded.emailChangeAllowedAt)
            val encoded = Json.encodeToString(EmailChangeCooldown.serializer(), decoded)
            assertEquals(setOf("emailChangeAllowedAt"), Json.parseToJsonElement(encoded).jsonObject.keys)
            assertEquals(millis.toDouble(), Json.parseToJsonElement(encoded).jsonObject.getValue("emailChangeAllowedAt").jsonPrimitive.content.toDouble())
            assertEquals(decoded, Json.decodeFromString(EmailChangeCooldown.serializer(), encoded))
        }
    }

    /** Missing, null, fractional, and unsupported deadlines cannot form a typed cooldown. */
    @Test
    fun invalidDeadlineFails() {
        listOf("{}", """{"emailChangeAllowedAt":null}""", """{"emailChangeAllowedAt":0.5}""", """{"emailChangeAllowedAt":4503599627370497}""").forEach {
            assertFails { Json.decodeFromString(EmailChangeCooldown.serializer(), it) }
        }
    }
}
