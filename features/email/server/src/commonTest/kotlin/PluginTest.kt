package dev.inmo.wishlist.features.email.server

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Verifies `emailConfigElementOrNull` — the pure decision function `Plugin.kt` uses to drive its
 * conditional Koin wiring for whether the `"email"` config block is present. This file keeps the
 * pure config-slice coverage; `EmailVerificationAccountCoordinatorTest` separately constructs both
 * real Koin graph shapes and verifies their singleton coordination behavior.
 */
class PluginTest {

    // --- emailConfigElementOrNull ---

    /** No `"email"` key at all (other root keys present) — must return `null`. */
    @Test
    fun emailConfigElementOrNullReturnsNullWhenKeyAbsent() {
        val config = buildJsonObject {
            put("host", JsonPrimitive("0.0.0.0"))
            put("port", JsonPrimitive(8196))
        }
        assertNull(emailConfigElementOrNull(config))
    }

    /** `"email": null` (key present, value explicitly JSON null) — must return `null`. */
    @Test
    fun emailConfigElementOrNullReturnsNullWhenKeyIsJsonNull() {
        val config = buildJsonObject {
            put("email", JsonNull)
        }
        assertNull(emailConfigElementOrNull(config))
    }

    /** A present, non-null `"email"` object — must return that exact element. */
    @Test
    fun emailConfigElementOrNullReturnsElementWhenKeyPresentAndNonNull() {
        val emailBlock = buildJsonObject {
            putJsonObject("smtp") {
                put("host", JsonPrimitive("smtp.example.com"))
                put("from", JsonPrimitive("noreply@example.com"))
            }
        }
        val config = buildJsonObject {
            put("email", emailBlock)
        }

        assertEquals(emailBlock, emailConfigElementOrNull(config))
    }
}
