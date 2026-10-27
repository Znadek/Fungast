package com.fungast.app.exchange

import androidx.activity.ComponentActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CompletableDeferred

/** ZXing's scanner: works offline and without Google Play Services, and asks for the camera permission itself. */
class AndroidQrScanner(activity: ComponentActivity) : QrScanner {

    private var pendingScan: CompletableDeferred<String?>? = null

    private val scanLauncher = activity.registerForActivityResult(ScanContract()) { result ->
        pendingScan?.complete(result.contents)
        pendingScan = null
    }

    override suspend fun scan(): String? {
        val result = CompletableDeferred<String?>().also { pendingScan = it }
        scanLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Inquadra il codice QR del posto")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
        )
        return result.await()
    }
}
