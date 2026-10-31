package com.fungast.app

import androidx.compose.ui.window.ComposeUIViewController
import androidx.room.Room
import com.fungast.app.data.DATABASE_FILE_NAME
import com.fungast.app.data.FungastDatabase
import com.fungast.app.data.buildFungastDatabase
import com.fungast.app.exchange.IosFileExchange
import com.fungast.app.exchange.IosQrScanner
import com.fungast.app.location.IosHeadingProvider
import com.fungast.app.location.IosLinkResolver
import com.fungast.app.location.IosLocationProvider
import com.fungast.app.map.IosOfflineMaps
import com.fungast.app.photos.IosPhotoPicker
import com.fungast.app.photos.IosPhotoStore
import com.fungast.app.routes.IosRouteRecorder
import com.fungast.app.settings.IosAppSettings
import platform.Foundation.NSHomeDirectory

private val database: FungastDatabase by lazy {
    Room.databaseBuilder<FungastDatabase>(
        name = "${NSHomeDirectory()}/Documents/$DATABASE_FILE_NAME",
    ).buildFungastDatabase()
}

// Entry point for the future iOS app (Xcode project to be added on macOS)
fun MainViewController() = ComposeUIViewController {
    App(
        spotDao = database.spotDao(),
        routeDao = database.routeDao(),
        locationProvider = IosLocationProvider(),
        headingProvider = IosHeadingProvider(),
        linkResolver = IosLinkResolver(),
        offlineMaps = IosOfflineMaps(),
        fileExchange = IosFileExchange(),
        qrScanner = IosQrScanner(),
        photoStore = IosPhotoStore(),
        photoPicker = IosPhotoPicker(),
        routeRecorder = IosRouteRecorder(),
        settings = IosAppSettings(),
    )
}
