package com.fungast.app.photos

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.CompletableDeferred

class AndroidPhotoPicker(
    private val activity: ComponentActivity,
    private val store: AndroidPhotoStore,
) : PhotoPicker {

    private var pendingPermission: CompletableDeferred<Boolean>? = null
    private var pendingCapture: CompletableDeferred<Boolean>? = null
    private var pendingPick: CompletableDeferred<List<Uri>>? = null

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingPermission?.complete(granted)
        pendingPermission = null
    }

    private val captureLauncher = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pendingCapture?.complete(saved)
        pendingCapture = null
    }

    // The system photo picker needs no storage permission
    private val pickLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PICKED_PHOTOS)
    ) { uris ->
        pendingPick?.complete(uris)
        pendingPick = null
    }

    override suspend fun takePhoto(): String? {
        // The QR scanner declares the camera permission, so the camera app may only be opened once it is granted
        if (!hasCameraPermission()) {
            val result = CompletableDeferred<Boolean>().also { pendingPermission = it }
            permissionLauncher.launch(Manifest.permission.CAMERA)
            if (!result.await()) return null
        }
        val file = File(activity.cacheDir, CAPTURE_DIR).apply { mkdirs() }.resolve(CAPTURE_FILE)
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val result = CompletableDeferred<Boolean>().also { pendingCapture = it }
        try {
            captureLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            return null
        }
        if (!result.await()) return null
        return store.saveImage { file.takeIf { it.isFile }?.inputStream() }.also { file.delete() }
    }

    override suspend fun pickPhotos(): List<String> {
        val result = CompletableDeferred<List<Uri>>().also { pendingPick = it }
        pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        return result.await().mapNotNull { uri -> store.saveImage { activity.contentResolver.openInputStream(uri) } }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CAPTURE_DIR = "camera"
        const val CAPTURE_FILE = "capture.jpg"
        const val MAX_PICKED_PHOTOS = 10
    }
}
