package com.fungast.app.exchange

// TODO: implement with UIActivityViewController and UIDocumentPickerViewController when the iOS app is set up
class IosFileExchange : FileExchange {
    override suspend fun share(file: SharedFile) = Unit
    override suspend fun pickTextFile(): String? = null
}
