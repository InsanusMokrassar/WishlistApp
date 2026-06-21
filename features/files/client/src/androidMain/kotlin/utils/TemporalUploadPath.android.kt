package dev.inmo.wishlist.features.files.client.utils

import dev.inmo.wishlist.features.files.common.Constants

/**
 * Android path stays bare: `tempUpload` sends through the shared Ktor client, where
 * `DefaultUrlHttpClientConfigurator` already appends `/api` to the configured base URL.
 *
 * @return `/temp_upload` (the client adds the `/api` prefix).
 */
actual fun temporalUploadFullPath(): String = "/${Constants.temporalUploadPathPart}"
