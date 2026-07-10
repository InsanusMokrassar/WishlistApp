package dev.inmo.wishlist.features.email.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.wishlist.features.email.server.configurators.EmailRoutingsConfigurator
import dev.inmo.wishlist.features.email.server.services.DisabledEmailFeature
import dev.inmo.wishlist.features.email.server.services.EmailFeatureService
import dev.inmo.wishlist.features.email.server.services.SmtpEmailService
import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import dev.inmo.wishlist.features.users.common.repo.UsersRepo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Common (JVM) startup plugin for the email server feature.
 *
 * Registers in the shared DI graph:
 * - [EmailConfig], [SmtpEmailService], and the [SmtpEmailService]-backed [EmailsService] binding —
 *   ALL THREE registered together, and ONLY when [emailConfigElementOrNull] returns non-null (a
 *   non-null `"email"` JSON object is present at `config["email"]` in the root server config). When
 *   absent (or explicitly JSON `null`), none of the three are registered — "email delivery disabled"
 *   is a DI-graph-shape fact rather than a runtime value check.
 * - [EmailFeature], always registered unconditionally via an inline `single<EmailFeature>` block
 *   that resolves `getOrNull<EmailsService>()`: present → [EmailFeatureService]; absent →
 *   [DisabledEmailFeature] (both wrapping [UsersRepo], so per-user email storage keeps working even
 *   with SMTP disabled).
 * - [EmailRoutingsConfigurator], registered with a random qualifier so Ktor picks it up automatically.
 *
 * [emailConfigElementOrNull] is extracted as a pure, Koin-free top-level function specifically so the
 * `"email"` config-key-presence decision is directly unit testable (see `PluginTest`) without needing
 * a Koin test harness (none exists in this repo). The [EmailFeature]-implementation choice
 * (present-[EmailsService] vs [DisabledEmailFeature]) is simple enough that it stays inline in the
 * `single<EmailFeature>` block rather than being extracted to its own named helper.
 *
 * The email server module targets JVM only (`mppJavaProject`), so Jakarta Mail references are safe
 * in this `commonMain` source set — the same approach used by `currency/server` with OkHttp.
 */
object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        val emailConfigElement = emailConfigElementOrNull(config)
        if (emailConfigElement != null) {
            single { get<Json>().decodeFromJsonElement(EmailConfig.serializer(), emailConfigElement) }
            single { SmtpEmailService(get<EmailConfig>()) }
            single<EmailsService> { get<SmtpEmailService>() }
        }
        single<EmailFeature> {
            getOrNull<EmailsService>() ?.let {
                EmailFeatureService(it, get<UsersRepo>(), get<SimpleRolesFeature>())
            } ?: DisabledEmailFeature(get<UsersRepo>())
        }
        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            EmailRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}

/**
 * Returns the nested `"email"` JSON object from [config], or `null` when the key is absent or
 * explicitly JSON `null` — both are treated as "email feature disabled" (see [EmailConfig] and
 * [Plugin]). Pure and Koin-free so it is directly unit-testable (see `PluginTest`).
 *
 * @param config Root server config JSON object.
 * @return The `"email"` [JsonElement] to decode [EmailConfig] from, or `null` to skip registration.
 */
internal fun emailConfigElementOrNull(config: JsonObject): JsonElement? =
    config["email"]?.takeUnless { it == JsonNull }
