package dev.inmo.wishlist.features.files.client.utils

import dev.inmo.wishlist.features.common.common.apiPathPart
import dev.inmo.wishlist.features.files.common.Constants

/**
 * JS path carries the `/api` prefix: the JS `tempUpload` actual uploads via a raw `XMLHttpRequest`
 * that bypasses the Ktor client plugins, so the prefix is not added by `DefaultUrlHttpClientConfigurator`.
 *
 * @return `/api/temp_upload`, resolved by the browser against the page origin.
 */
actual fun temporalUploadFullPath(): String = "/$apiPathPart/${Constants.temporalUploadPathPart}"
