package dev.inmo.wishlist.features.common.server

actual val isInDebugMode
    get() = System.getenv("DEBUG") ?.lowercase() == "true"
