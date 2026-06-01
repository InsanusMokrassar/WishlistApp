package dev.inmo.wishlist.features.ui.wishlist.utils

import dev.inmo.micro_utils.common.MPPFile
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.HTMLInputElement

/**
 * JS image picker: programmatically creates and clicks a hidden file input restricted to images,
 * then resolves with the selected [org.w3c.files.File] (which is the JS `MPPFile`).
 *
 * @return The chosen file, or `null` if the change event yields no file.
 */
actual suspend fun pickImageFile(): MPPFile? {
    val input = (document.createElement("input") as HTMLInputElement).apply {
        type = "file"
        accept = "image/*"
        style.display = "none"
    }
    val deferred = CompletableDeferred<MPPFile?>()
    input.onchange = {
        deferred.complete(input.files?.item(0))
        document.body?.removeChild(input)
    }
    document.body?.appendChild(input)
    input.click()
    return deferred.await()
}
