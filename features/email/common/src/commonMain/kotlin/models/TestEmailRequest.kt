package dev.inmo.wishlist.features.email.common.models

import kotlinx.serialization.Serializable

/**
 * Request body for the root-only test-email endpoint.
 *
 * @property to Destination address the test message should be sent to.
 */
@Serializable
data class TestEmailRequest(
    val to: Email
)
