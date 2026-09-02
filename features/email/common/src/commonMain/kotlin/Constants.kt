package dev.inmo.wishlist.features.email.common

import dev.inmo.wishlist.features.roles.common.models.FunctionalityId

/**
 * Shared URL path segment constants for the email feature.
 *
 * Consumed by both the server routing configurator and the client HTTP layer to ensure
 * path strings are defined in a single place and cannot drift out of sync.
 */
object EmailConstants {
    /** Role-gated functionality id for the test-send action (`POST /email/sendTest`). */
    val sendTestFunctionalityId = FunctionalityId("email.sendTest")

    /** Root path segment for all email-feature routes: `/email`. */
    const val prefixPathPart = "email"

    /** Path segment for the feature-enabled probe: `/email/enabled`. */
    const val enabledPathPart = "enabled"

    /** Path segment for the root-only test-send action: `/email/sendTest`. */
    const val sendTestPathPart = "sendTest"

    /** Path segment for the self-service email-update action: `/email/myEmail`. */
    const val myEmailPathPart = "myEmail"

    /** Query parameter consumed by the web shell after a successful email approval redirect. */
    const val approvalQueryParameter = "emailApproval"

    /** Fixed [approvalQueryParameter] value representing a successful approval. */
    const val approvalQueryValue = "approved"

    /** Same-origin root redirect emitted after a successful email approval. */
    const val approvalRedirectPath = "/?emailApproval=approved"
}
