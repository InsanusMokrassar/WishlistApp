package dev.inmo.wishlist.features.ui.wishlist.utils

import dev.inmo.micro_utils.common.MPPFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Desktop image picker: shows a Swing [JFileChooser] filtered to common image extensions on an IO
 * dispatcher (the dialog is blocking). The selected `java.io.File` is the JVM `MPPFile`.
 *
 * @return The chosen file, or `null` when the dialog is dismissed without a selection.
 */
actual suspend fun pickImageFile(): MPPFile? = withContext(Dispatchers.IO) {
    val chooser = JFileChooser().apply {
        dialogTitle = "Select image"
        isAcceptAllFileFilterUsed = false
        fileFilter = FileNameExtensionFilter(
            "Images", "png", "jpg", "jpeg", "gif", "webp", "bmp", "tif", "tiff", "ico"
        )
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}
