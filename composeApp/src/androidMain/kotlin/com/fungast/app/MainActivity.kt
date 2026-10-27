package com.fungast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fungast.app.exchange.AndroidFileExchange
import com.fungast.app.location.AndroidHeadingProvider
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
        setContent {
            App(
                spotDao = spotDao,
                locationProvider = locationProvider,
                headingProvider = AndroidHeadingProvider(this),
                offlineMaps = AndroidOfflineMaps(applicationContext),
                fileExchange = fileExchange,
            )
        }
    }
}
