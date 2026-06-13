package dev.inmo.wishlist.features.ui.users.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.benasher44.uuid.uuid4
import dev.inmo.micro_utils.common.MPPFile
import kotlinx.coroutines.CompletableDeferred
import java.io.File

/**
 * Bridges the Android `ActivityResultContracts.GetContent` API — which must be registered before the
 * activity starts — to the coroutine-based [pickImageFile] expect declaration for avatar selection.
 *
 * `MainActivity` calls [register] in `onCreate`. Each [pick] launches the system picker, awaits the
 * chosen `content://` URI, then copies its bytes into a private temp file so the rest of the upload
 * pipeline can treat it as a regular `java.io.File` (the Android `MPPFile`). Kept independent of the
 * wishlist feature's picker so the users feature has no cross-UI-feature dependency.
 */
object AvatarImagePicker {
    private var launcher: ActivityResultLauncher<String>? = null
    private var appContext: Context? = null
    private var pending: CompletableDeferred<Uri?>? = null

    /**
     * Registers the picker against [activity]. Must be invoked during the activity's `onCreate`,
     * before it reaches the STARTED state.
     *
     * @param activity Host activity that owns the result launcher.
     */
    fun register(activity: ComponentActivity) {
        appContext = activity.applicationContext
        launcher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            pending?.complete(uri)
            pending = null
        }
    }

    /**
     * Launches the system image picker and returns the chosen file copied to a temp location.
     *
     * @return The picked image as an [MPPFile], or `null` when cancelled or not registered.
     */
    suspend fun pick(): MPPFile? {
        val launcher = launcher ?: return null
        val context = appContext ?: return null
        val deferred = CompletableDeferred<Uri?>()
        pending = deferred
        launcher.launch("image/*")
        val uri = deferred.await() ?: return null
        return copyToTempFile(context, uri)
    }

    /** Copies the content at [uri] into a temp file named with the matching image extension. */
    private fun copyToTempFile(context: Context, uri: Uri): File? {
        val mime = context.contentResolver.getType(uri)
        val extension = mime
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: "img"
        val target = File.createTempFile("avatar_${uuid4()}", ".$extension")
        target.deleteOnExit()
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        return target
    }
}

/**
 * Android image picker: delegates to [AvatarImagePicker], which `MainActivity` wires up.
 *
 * @return The chosen image as an [MPPFile], or `null` when cancelled / picker unavailable.
 */
actual suspend fun pickImageFile(): MPPFile? = AvatarImagePicker.pick()
