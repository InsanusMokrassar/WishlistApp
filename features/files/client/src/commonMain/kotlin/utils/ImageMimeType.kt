package dev.inmo.wishlist.features.files.client.utils

import dev.inmo.micro_utils.common.FileName

/**
 * Resolves an image MIME type from a [FileName]'s extension. Used client-side to fill
 * [dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest.mimeType] for platforms whose
 * `MPPFile` carries no MIME type of its own (JVM/Android `File`).
 *
 * Falls back to `application/octet-stream` for unknown extensions; the server then rejects the
 * finalize because that is not an image type.
 *
 * @receiver File name whose extension determines the MIME type.
 * @return The mapped MIME type string.
 */
fun FileName.imageMimeType(): String = when (extension.lowercase()) {
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "bmp" -> "image/bmp"
    "svg" -> "image/svg+xml"
    "heic" -> "image/heic"
    "heif" -> "image/heif"
    "avif" -> "image/avif"
    "tif", "tiff" -> "image/tiff"
    "ico" -> "image/x-icon"
    else -> "application/octet-stream"
}
