package com.fungast.app.exchange

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidFileExchange(private val activity: ComponentActivity) : FileExchange {

    private var pendingPick: CompletableDeferred<Uri?>? = null

    private val openDocumentLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingPick?.complete(uri)
        pendingPick = null
    }

    override suspend fun share(file: SharedFile) {
        val uri = withContext(Dispatchers.IO) {
            // Replaced on every export, so old exports don't pile up in the cache
            val dir = File(activity.cacheDir, EXPORT_DIR).apply {
                deleteRecursively()
                mkdirs()
            }
            val target = File(dir, file.name).apply { writeText(file.content) }
            FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", target)
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = file.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, file.name)
            // ClipData makes the read grant reach the app picked in the chooser
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(send, null))
    }

    override suspend fun pickTextFile(): String? {
        val result = CompletableDeferred<Uri?>().also { pendingPick = it }
        // Any type: .fungast has no registered MIME type, and chat apps label files inconsistently
        openDocumentLauncher.launch(arrayOf("*/*"))
        val uri = result.await() ?: return null
        return withContext(Dispatchers.IO) {
            try {
                activity.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readNBytesCompat(MAX_IMPORT_BYTES + 1)
                    if (bytes.size > MAX_IMPORT_BYTES) null else bytes.decodeToString()
                }
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
        }
    }

    private companion object {
        const val EXPORT_DIR = "exports"

        /** Thousands of spots fit in under 1 MB; the rest is room for a backup with ~100 photos. */
        const val MAX_IMPORT_BYTES = 50 * 1024 * 1024
    }
}

/** InputStream.readNBytes needs API 33; minSdk is 26. */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (out.size() < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read < 0) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}
