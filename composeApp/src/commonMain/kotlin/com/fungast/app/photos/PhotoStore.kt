package com.fungast.app.photos

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** The app's private photo folder. Photos are JPEG files identified by a generated name. */
interface PhotoStore {
    /** Stores a JPEG as it is (e.g. from an imported backup) and returns its name, or null on failure. */
    suspend fun save(jpeg: ByteArray): String?

    /** The JPEG bytes, or null if the photo is missing. */
    suspend fun read(name: String): ByteArray?

    /** Decoded photo no larger than [maxSize] pixels per side, or null if missing or unreadable. */
    suspend fun load(name: String, maxSize: Int): ImageBitmap?

    suspend fun delete(names: Collection<String>)
}

/**
 * Takes or picks new photos and puts them in the [PhotoStore], shrunk to a size that keeps backups small.
 * Tied to the current screen like [com.fungast.app.exchange.FileExchange].
 */
interface PhotoPicker {
    /** Opens the camera; returns the stored photo's name, or null if cancelled or the camera is unavailable. */
    suspend fun takePhoto(): String?

    /** Lets the user choose photos from the gallery; returns the names of the stored copies. */
    suspend fun pickPhotos(): List<String>
}

/** Lets photo thumbnails anywhere in the UI load images, like an image loader would. */
val LocalPhotoStore = staticCompositionLocalOf<PhotoStore?> { null }
