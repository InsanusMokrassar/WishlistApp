package dev.inmo.wishlist.features.deeplinks.common

/**
 * URL path segment constants for the deeplinks feature, shared between the server routing
 * configurator and any external link builders to avoid out-of-sync strings.
 */
object DeepLinksConstants {
    /**
     * Root path segment for deeplink resolution: a deeplink is invoked at `links/<deeplink_uuid>`.
     */
    const val linksPrefixPathPart = "links"
}
