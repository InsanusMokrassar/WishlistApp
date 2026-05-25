package dev.inmo.wishlist.features.ui.auth.utils

import kotlinx.browser.window

actual fun defaultServerUrl(): String = window.location.origin
