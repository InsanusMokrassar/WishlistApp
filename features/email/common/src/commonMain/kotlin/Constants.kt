package dev.inmo.wishlist.features.email.common

/**
 * Shared URL path constants for the email feature.
 *
 * Reused by both the server routing configurator and the Ktor client implementation so the two sides
 * never drift apart.
 */
object Constants {
    /** Top-level route prefix for the email feature. */
    const val emailPrefixPathPart = "email"

    /** Root-only subpath that triggers sending a test e-mail. */
    const val sendTestPathPart = "sendTest"

    /** Authenticated subpath for the caller to set/clear their own e-mail address. */
    const val myEmailPathPart = "myEmail"
}