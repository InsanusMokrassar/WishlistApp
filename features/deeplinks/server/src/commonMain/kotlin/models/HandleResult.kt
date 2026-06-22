package dev.inmo.wishlist.features.deeplinks.server.models

import kotlinx.serialization.Serializable

/**
 * Outcome of resolving an opened deeplink, mapped to an HTTP status by `DeepLinksRoutingConfigurator`.
 *
 * The three outcomes are kept distinct end-to-end (even though the route collapses [NotFound] and
 * [Unhandled] to the same status) so the result stays testable and open to richer future semantics.
 * The sealed hierarchy is `@Serializable`, so subtypes serialize polymorphically out of the box.
 */
@Serializable
sealed interface HandleResult {
    /** No deeplink stored for the given id -> HTTP 404. */
    @Serializable
    data object NotFound : HandleResult

    /**
     * Deeplink exists but no registered handler claimed it -> HTTP 404 (a stored-but-dead link is
     * indistinguishable to the caller from a missing one, and this avoids leaking link existence).
     */
    @Serializable
    data object Unhandled : HandleResult

    /** A handler owned and processed the deeplink -> HTTP 200. */
    @Serializable
    data object Handled : HandleResult
}
