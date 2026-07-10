package dev.inmo.wishlist.features.email.server

import dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature
import dev.inmo.wishlist.features.email.server.services.EmailFeatureService
import dev.inmo.wishlist.features.email.server.services.FakeEmailsService
import dev.inmo.wishlist.features.email.server.services.FakeUsersRepo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies the two pure decision functions [Plugin.kt] uses to drive its conditional Koin wiring:
 * [emailConfigElementOrNull] (whether the `"email"` config block is present) and
 * [selectEmailFeature] (which [EmailFeature] implementation is selected). Both are pure — no Koin
 * container is constructed anywhere in this file. The `single { }` registration calls that wire
 * these functions' results into Koin are intentionally not separately unit tested — see
 * `agents/task/10.07.2026_06.40.48-2407c3c3-e09a-4139-976a-305652d931a3/003-architecturing.md`'s
 * "Testability decision" section.
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

    // --- selectEmailFeature ---

    /** `emailsService == null` — must select [DisabledEmailFeature]. */
    @Test
    fun selectEmailFeatureReturnsDisabledEmailFeatureWhenEmailsServiceNull() = runTest {
        val result = selectEmailFeature(emailsService = null, usersRepo = FakeUsersRepo())
        assertIs<DisabledEmailFeature>(result)
    }

    /** `emailsService != null` — must select [EmailFeatureService], reporting itself enabled. */
    @Test
    fun selectEmailFeatureReturnsEmailFeatureServiceWhenEmailsServicePresent() = runTest {
        val result = selectEmailFeature(emailsService = FakeEmailsService(), usersRepo = FakeUsersRepo())
        assertIs<EmailFeatureService>(result)
        assertTrue(result.isFeatureEnabled())
    }
}
