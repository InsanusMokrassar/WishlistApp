package dev.inmo.wishlist.features.email.server

import dev.inmo.kroles.repos.RolesRepo
import dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature
import dev.inmo.wishlist.features.email.server.services.EmailFeatureService
import dev.inmo.wishlist.features.email.server.services.EmailVerificationAccountCoordinator
import dev.inmo.wishlist.features.email.server.services.FakeRolesFeature
import dev.inmo.wishlist.features.email.server.services.FakeRolesRepo
import dev.inmo.wishlist.features.email.server.services.FakeUsersRepo
import dev.inmo.wishlist.features.roles.server.RolesFeature
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.koin.core.KoinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Verifies `emailConfigElementOrNull` — the pure decision function `Plugin.kt` uses to drive its
 * conditional Koin wiring for whether the `"email"` config block is present. This file keeps the
 * pure config-slice coverage; `EmailVerificationAccountCoordinatorTest` separately constructs both
 * real Koin graph shapes and verifies their singleton coordination behavior.
 */
class PluginTest {

    /** Builds the actual email plugin graph with fixture-owned non-SMTP collaborators. */
    private fun pluginApplication(config: kotlinx.serialization.json.JsonObject): KoinApplication =
        KoinApplication.init().modules(
            module {
                single<UsersRepo> { FakeUsersRepo() }
                single<RolesRepo> { FakeRolesRepo() }
                single<RolesFeature> { FakeRolesFeature() }
                single<Json> { Json { ignoreUnknownKeys = true } }
                with(Plugin) { setupDI(config) }
            },
        )

    /** Produces a root config with an independent policy and selected optional SMTP graph shape. */
    private fun rootConfig(
        policy: JsonElement? = null,
        email: JsonElement? = null,
        includeEmail: Boolean = false,
    ) = buildJsonObject {
        if (policy != null) put("emailChangeCooldown", policy)
        if (includeEmail) put("email", email ?: JsonNull)
    }

    /** Valid configured SMTP object; no test invokes its transport. */
    private fun smtpConfig() = buildJsonObject {
        putJsonObject("smtp") {
            put("host", "smtp.example.com")
            put("from", "noreply@example.com")
        }
    }

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

    /** Root policy decoding is independent from all SMTP graph shapes and keeps one shared coordinator. */
    @Test
    fun setupDiDecodesValidRootPoliciesAcrossSmtpShapes() {
        val policies = listOf(null, JsonPrimitive("PT0S"), JsonPrimitive("P1D"), JsonPrimitive("PT0.000000001S"))
        val smtpShapes = listOf(
            false to null,
            true to JsonNull,
            true to smtpConfig(),
        )

        policies.forEach { policy ->
            smtpShapes.forEach { (includeEmail, email) ->
                val application = pluginApplication(rootConfig(policy, email, includeEmail))
                try {
                    val coordinator = application.koin.get<EmailVerificationAccountCoordinator>()
                    assertSame(coordinator, application.koin.get<EmailVerificationAccountCoordinator>())
                    when (email) {
                        is kotlinx.serialization.json.JsonObject -> {
                            assertIs<EmailFeatureService>(application.koin.get<EmailFeature>())
                            assertIs<EmailsService>(application.koin.get<EmailsService>())
                        }
                        else -> {
                            assertIs<DisabledEmailFeature>(application.koin.get<EmailFeature>())
                            assertNull(runCatching { application.koin.get<EmailsService>() }.getOrNull())
                        }
                    }
                } finally {
                    application.close()
                }
            }
        }
    }

    /** Invalid root policies fail graph setup before optional SMTP configuration can affect the result. */
    @Test
    fun setupDiRejectsInvalidRootPoliciesAcrossSmtpShapes() {
        val invalidPolicies = listOf<JsonElement>(
            JsonPrimitive("-PT0.001S"),
            JsonPrimitive("malformed-duration"),
            JsonNull,
            JsonPrimitive("PTInfinityS"),
            JsonPrimitive("-PTInfinityS"),
            JsonPrimitive("P106751991167300D"),
        )
        val smtpShapes = listOf(false to null, true to JsonNull, true to smtpConfig())

        invalidPolicies.forEach { policy ->
            smtpShapes.forEach { (includeEmail, email) ->
                assertFailsWith<Exception> {
                    pluginApplication(rootConfig(policy, email, includeEmail)).close()
                }
            }
        }
    }
}
