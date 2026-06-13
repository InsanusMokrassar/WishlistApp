package dev.inmo.wishlist.features.ui.wishlist.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage

/**
 * Desktop remote image: loads encoded bytes via [loader] (keyed by [key]) off the composition,
 * decodes them with Skia and renders the result. Renders nothing while loading or on failure.
 *
 * @param key Stable identity (e.g. the file id) that triggers a reload when changed.
 * @param loader Suspending byte provider, typically `{ viewModel.loadImageBytes(id) }`.
 * @param contentDescription Accessibility description.
 * @param modifier Layout modifier for the rendered image.
 */
@Composable
fun RemoteImage(
    key: String,
    loader: suspend () -> ByteArray?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<ImageBitmap?>(null, key) {
        value = runCatching {
            loader()?.let { SkiaImage.makeFromEncoded(it).toComposeImageBitmap() }
        }.getOrNull()
    }
    bitmap?.let { Image(bitmap = it, contentDescription = contentDescription, modifier = modifier) }
}
