package com.fungast.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fungast.app.exchange.AndroidFileExchange
import com.fungast.app.location.AndroidHeadingProvider
import com.fungast.app.location.AndroidLinkResolver
import com.fungast.app.location.AndroidLocationProvider
import com.fungast.app.map.AndroidOfflineMaps

class MainActivity : ComponentActivity() {
    // Must be created before the activity is started, as they register activity-result launchers
    private val locationProvider = AndroidLocationProvider(this)
    private val fileExchange = AndroidFileExchange(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val spotDao = (application as FungastApplication).database.spotDao()
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
                sharedText = sharedText,
            )
        }
    }
}

private fun Intent.sharedText(): String? =
    if (action == Intent.ACTION_SEND && type == "text/plain") getStringExtra(Intent.EXTRA_TEXT) else null
