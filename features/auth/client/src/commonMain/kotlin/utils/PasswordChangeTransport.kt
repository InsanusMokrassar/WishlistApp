package dev.inmo.wishlist.features.auth.client.utils

import io.ktor.util.AttributeKey

/** Ktor request marker that prevents configured-server URL merging for browser approval completion. */
val skipDefaultServerUrl: AttributeKey<Unit> = AttributeKey("skipDefaultServerUrl")

/** Trusted optional absolute completion endpoint supplied only by the JS platform plugin. */
data class PasswordChangeCompletionUrl(
    /** Fixed origin-plus-path URL constructed from the current browser origin. */
    val string: String,
)
