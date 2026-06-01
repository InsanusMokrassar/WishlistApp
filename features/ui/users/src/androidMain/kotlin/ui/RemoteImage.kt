package dev.inmo.wishlist.features.ui.users.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Android remote image: loads encoded bytes via [loader] (keyed by [key]) off the composition,
 * decodes them with [BitmapFactory] and renders the result. Renders nothing while loading or on
 * failure.
 *
 * @param key Stable identity (e.g. the avatar file id) that triggers a reload when changed.
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
            loader()?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        }.getOrNull()
    }
    bitmap?.let { Image(bitmap = it, contentDescription = contentDescription, modifier = modifier) }
}
