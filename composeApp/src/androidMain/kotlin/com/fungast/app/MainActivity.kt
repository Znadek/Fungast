package com.fungast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fungast.app.location.AndroidLocationProvider
import com.fungast.app.map.AndroidOfflineMaps

class MainActivity : ComponentActivity() {
    // Must be created before the activity is started, as it registers a permission launcher
    private val locationProvider = AndroidLocationProvider(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val spotDao = (application as FungastApplication).database.spotDao()
        setContent {
            App(
                spotDao = spotDao,
                locationProvider = locationProvider,
                offlineMaps = AndroidOfflineMaps(applicationContext),
            )
        }
    }
}
