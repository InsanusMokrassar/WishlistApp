package dev.inmo.wishlist.features.currency.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.currency.common.CurrencyConstants
import dev.inmo.wishlist.features.currency.common.CurrencyFeature
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Registers the read-only currency HTTP endpoints under [CurrencyConstants.prefixPathPart].
 *
 * Endpoints (all behind authentication, consistent with the rest of the app):
 * - `GET enabled` → JSON [Boolean] feature flag.
 * - `GET currencies` → JSON list of available currencies.
 * - `GET rates` → JSON rates snapshot, or `204 No Content` when unavailable/disabled.
 *
 * @param feature Currency capability the routes delegate to.
 */
class CurrencyRoutingsConfigurator(
    private val feature: CurrencyFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(CurrencyConstants.prefixPathPart) {
            get(CurrencyConstants.enabledPathPart) {
                call.respond(feature.isFeatureEnabled())
            }
            get(CurrencyConstants.currenciesPathPart) {
                call.respond(feature.getCurrencies())
            }
            get(CurrencyConstants.ratesPathPart) {
                val rates = feature.getRates()
                if (rates == null) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(rates)
                }
            }
        }
    }
}
