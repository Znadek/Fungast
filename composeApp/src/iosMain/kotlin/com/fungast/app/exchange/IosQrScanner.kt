package com.fungast.app.exchange

// TODO: implement with AVCaptureMetadataOutput (or VisionKit's DataScannerViewController) when the iOS app is set up
class IosQrScanner : QrScanner {
    override suspend fun scan(): String? = null
}
