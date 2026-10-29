package com.fungast.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fungast.app.exchange.AndroidFileExchange
import com.fungast.app.exchange.AndroidQrScanner
import com.fungast.app.location.AndroidHeadingProvider
import com.fungast.app.location.AndroidLinkResolver
import com.fungast.app.location.AndroidLocationProvider
import com.fungast.app.map.AndroidOfflineMaps
import com.fungast.app.photos.AndroidPhotoPicker

class MainActivity : ComponentActivity() {
    // Must be created before the activity is started, as they register activity-result launchers
    private val locationProvider = AndroidLocationProvider(this)
    private val fileExchange = AndroidFileExchange(this)
    private val qrScanner = AndroidQrScanner(this)
    private val photoPicker by lazy(LazyThreadSafetyMode.NONE) {
        AndroidPhotoPicker(this, (application as FungastApplication).photoStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val spotDao = (application as FungastApplication).database.spotDao()
        // Its launchers must be registered before the activity is started; application is only set from here on
        photoPicker
        // Only on first creation: after a rotation the screen state already holds the shared link
        val sharedText = if (savedInstanceState == null) intent.sharedText() else null
        setContent {
            App(
                spotDao = spotDao,
                locationProvider = locationProvider,
                headingProvider = AndroidHeadingProvider(this),
                linkResolver = AndroidLinkResolver(),
                offlineMaps = AndroidOfflineMaps(applicationContext),
                fileExchange = fileExchange,
                qrScanner = qrScanner,
                photoStore = (application as FungastApplication).photoStore,
                photoPicker = photoPicker,
                sharedText = sharedText,
            )
        }
    }
}

private fun Intent.sharedText(): String? =
    if (action == Intent.ACTION_SEND && type == "text/plain") getStringExtra(Intent.EXTRA_TEXT) else null
