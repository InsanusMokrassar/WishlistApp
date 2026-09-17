package dev.inmo.wishlist.features.email.server.models

import dev.inmo.wishlist.features.deeplinks.common.models.DeepLinkHandlerId

/** Stable deeplink identity owned by Email's password-change approval flow. */
object EmailPasswordChange {
    /** Handler id persisted with every email-authorized password-change approval. */
    val handlerId = DeepLinkHandlerId("email.password_change")
}
