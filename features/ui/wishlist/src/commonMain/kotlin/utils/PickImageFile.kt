package dev.inmo.wishlist.features.ui.wishlist.utils

import dev.inmo.micro_utils.common.MPPFile

/**
 * Opens the platform's native image picker and suspends until the user chooses a file or cancels.
 *
 * Platform actuals:
 * - JS: a hidden file input element restricted to image types.
 * - JVM (Desktop): a Swing `JFileChooser` filtered to image extensions.
 * - Android: `ActivityResultContracts.GetContent` via the launcher registered by `MainActivity`
 *   (see `AndroidImagePicker`); the chosen `content://` stream is copied to a temp file.
 *
 * @return The chosen file as an [MPPFile], or `null` when the user cancels or no picker is available.
 */
expect suspend fun pickImageFile(): MPPFile?
