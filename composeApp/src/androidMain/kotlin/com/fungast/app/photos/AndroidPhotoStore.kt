package com.fungast.app.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPhotoStore(context: Context) : PhotoStore {
    private val dir = File(context.filesDir, PHOTO_DIR)

    // Thumbnails are shown again on every scroll of the list
    private val cache = object : LruCache<String, ImageBitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    override suspend fun save(jpeg: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            newName().also { File(dir.apply { mkdirs() }, it).writeBytes(jpeg) }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Stores an image from the camera or the gallery, upright and shrunk to [MAX_PHOTO_SIZE] pixels per side.
     * [open] must return a fresh stream on every call: the image is read once for its size and once to decode it.
     */
    suspend fun saveImage(open: () -> InputStream?): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            // Returns null by design when only reading bounds: check the size it filled in instead
            (open() ?: return@withContext null).use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_PHOTO_SIZE)
            }
            val decoded = open()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return@withContext null
            val rotation = open()?.use { exifRotation(it) } ?: 0
            val photo = decoded.fitInto(MAX_PHOTO_SIZE, rotation)
            val name = newName()
            File(dir.apply { mkdirs() }, name).outputStream().use {
                photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
            }
            name
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    override suspend fun read(name: String): ByteArray? = withContext(Dispatchers.IO) {
        File(dir, name).takeIf { it.isFile }?.readBytes()
    }

    override suspend fun load(name: String, maxSize: Int): ImageBitmap? {
        val key = "$name@$maxSize"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val file = File(dir, name)
            if (!file.isFile) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
            }
            BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()?.also { cache.put(key, it) }
        }
    }

    override suspend fun delete(names: Collection<String>) = withContext(Dispatchers.IO) {
        names.forEach { name ->
            File(dir, name).delete()
            cache.snapshot().keys.filter { it.startsWith("$name@") }.forEach(cache::remove)
        }
    }

    private fun newName() = "${UUID.randomUUID()}.jpg"

    private companion object {
        const val PHOTO_DIR = "photos"

        /** Enough to see the details of a mushroom on a phone screen, ~200-400 KB per photo. */
        const val MAX_PHOTO_SIZE = 1600
        const val JPEG_QUALITY = 85
        const val CACHE_BYTES = 24 * 1024 * 1024
    }
}

/** Largest power of two that keeps both sides at least [target] (or the original size when smaller). */
private fun sampleSize(width: Int, height: Int, target: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= target && height / (sample * 2) >= target) sample *= 2
    return sample
}

private fun exifRotation(input: InputStream): Int =
    when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

/** Scales so the longer side is at most [maxSize] and turns the image upright. */
private fun Bitmap.fitInto(maxSize: Int, rotation: Int): Bitmap {
    val scale = minOf(1f, maxSize.toFloat() / maxOf(width, height))
    if (scale == 1f && rotation == 0) return this
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(rotation.toFloat())
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
