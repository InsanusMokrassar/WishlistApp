package dev.inmo.wishlist.features.email.server.models

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId

/** Stable deeplink identifiers owned by the email feature. */
object EmailVerification {
    /** Handler id stored in every registration verification deeplink. */
    val handlerId = DeepLinkHandlerId("email.registration_verification")
}
