package dev.inmo.wishlist.features.deeplinks.common

/**
 * Shared URL path parts for the deeplinks feature, kept in one place so server routing and any
 * future client code never drift from the wire format.
 */
object DeepLinksConstants {
    /**
     * Root path segment under which a deeplink resolves: `GET /links/{deeplink_uuid}`.
     *
     * Served at the SITE ROOT (NOT under `/api`), because a deeplink is a user-clickable URL and
     * the `/api` namespace is reserved for the internal API (and carries a `404` catch-all).
     */
    const val linksPrefixPathPart = "links"

    /**
     * Name of the path parameter holding the deeplink UUID; shared by the route declaration and the
     * handler reading `call.parameters[...]`, so the two never diverge.
     */
    const val deeplinkIdParameter = "deeplink_uuid"
}
