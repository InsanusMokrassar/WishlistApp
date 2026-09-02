package dev.inmo.wishlist.features.deeplinks.common.models

import kotlinx.serialization.Serializable

/**
 * Outcome of resolving an opened deeplink.
 *
 * The hierarchy belongs to the common deeplink contract because a [DeepLinkHandler] must be able
 * to choose the successful outcome without depending on the server module. The server routing
 * boundary maps the result to HTTP: [NotFound] and [Unhandled] become `404`, [Handled.Common]
 * becomes `200`, and [Handled.Redirect] becomes a temporary redirect.
 */
@Serializable
sealed interface HandleResult {
    /** No deeplink is stored for the requested identifier. */
    @Serializable
    data object NotFound : HandleResult

    /** A deeplink exists but no registered handler can process its payload. */
    @Serializable
    data object Unhandled : HandleResult

    /** Successful handler outcomes that the HTTP boundary must preserve. */
    @Serializable
    sealed interface Handled : HandleResult {
        /** A handler processed the deeplink and requires the ordinary empty `200 OK` response. */
        @Serializable
        data object Common : Handled

        /** A handler processed the deeplink and requests a temporary redirect to [url]. */
        @Serializable
        data class Redirect(val url: String) : Handled
    }
}
