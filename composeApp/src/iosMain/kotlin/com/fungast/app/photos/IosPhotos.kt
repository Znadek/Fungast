package com.fungast.app.photos

import androidx.compose.ui.graphics.ImageBitmap

// TODO: implement with the app's Documents folder and PHPickerViewController/UIImagePickerController on macOS
class IosPhotoStore : PhotoStore {
    override suspend fun save(jpeg: ByteArray): String? = null
    override suspend fun read(name: String): ByteArray? = null
    override suspend fun load(name: String, maxSize: Int): ImageBitmap? = null
    override suspend fun delete(names: Collection<String>) = Unit
}

class IosPhotoPicker : PhotoPicker {
    override suspend fun takePhoto(): String? = null
    override suspend fun pickPhotos(): List<String> = emptyList()
}
