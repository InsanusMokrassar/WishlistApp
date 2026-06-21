package dev.inmo.wishlist.features.deeplinks.common

/**
 * Shared path segment constants for the deeplinks feature.
 * Used by server routing and any external link builders to keep paths in sync.
 */
object DeepLinksConstants {
    /**
     * Relative route path segment for deep-link resolution: `links`.
     * Final server path (via InternalApplicationRoutingConfigurator): `GET /api/links/{deeplinkId}`.
     */
    const val linksPrefixPathPart = "links"

    /**
     * Route parameter name for the deep-link identifier in `{deeplinkId}` path segments.
     */
    const val deeplinkIdPathParam = "deeplinkId"
}
