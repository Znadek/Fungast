package com.fungast.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fungast.app.data.SpotDao
import com.fungast.app.exchange.FileExchange
import com.fungast.app.exchange.QrScanner
import com.fungast.app.exchange.isSpotQr
import com.fungast.app.location.HeadingProvider
import com.fungast.app.location.LinkResolver
import com.fungast.app.location.LocationProvider
import com.fungast.app.map.OfflineMaps
import com.fungast.app.ui.PlatformBackHandler
import com.fungast.app.ui.spots.AddSpotScreen
import com.fungast.app.ui.spots.GoToSpotScreen
import com.fungast.app.ui.spots.MapDownloadViewModel
import com.fungast.app.ui.spots.SpotExchangeViewModel
import com.fungast.app.ui.spots.SpotsHomeScreen
import com.fungast.app.ui.spots.SpotsViewModel
import com.fungast.app.ui.theme.FungastTheme
import kotlinx.coroutines.launch

private enum class Screen { SpotList, AddSpot, GoToSpot }

@Composable
fun App(
    spotDao: SpotDao,
    locationProvider: LocationProvider,
    headingProvider: HeadingProvider,
    linkResolver: LinkResolver,
    offlineMaps: OfflineMaps,
    fileExchange: FileExchange,
    qrScanner: QrScanner,
    /** Text shared to the app from another one (e.g. a Google Maps link): opens "new spot" with it. */
    sharedText: String? = null,
) {
    FungastTheme {
        val viewModel = viewModel { SpotsViewModel(spotDao) }
        val mapDownloadViewModel = viewModel { MapDownloadViewModel(offlineMaps) }
        val exchangeViewModel = viewModel { SpotExchangeViewModel(spotDao) }
        val scope = rememberCoroutineScope()
        var screen by rememberSaveable { mutableStateOf(if (sharedText != null) Screen.AddSpot else Screen.SpotList) }
        var initialLinkText by rememberSaveable { mutableStateOf(sharedText) }
        var targetSpotId by rememberSaveable { mutableStateOf<Long?>(null) }

        val spots by viewModel.spots.collectAsStateWithLifecycle()

        when (screen) {
            Screen.SpotList -> {
                val downloadState by mapDownloadViewModel.state.collectAsStateWithLifecycle()
                val exchangeMessage by exchangeViewModel.message.collectAsStateWithLifecycle()
                SpotsHomeScreen(
                    spots = spots,
                    downloadState = downloadState,
                    onAddSpot = {
                        initialLinkText = null
                        screen = Screen.AddSpot
                    },
                    onDeleteSpot = viewModel::deleteSpot,
                    onGoToSpot = {
                        targetSpotId = it.id
                        screen = Screen.GoToSpot
                    },
                    onDownloadArea = mapDownloadViewModel::download,
                    onDownloadMessageShown = mapDownloadViewModel::messageShown,
                    exchangeMessage = exchangeMessage,
                    onExport = { format ->
                        scope.launch { exchangeViewModel.buildExport(format)?.let { fileExchange.share(it) } }
                    },
                    onImport = {
                        scope.launch { fileExchange.pickTextFile()?.let(exchangeViewModel::import) }
                    },
                    onScanQr = {
                        scope.launch {
                            val text = qrScanner.scan() ?: return@launch
                            if (isSpotQr(text)) {
                                exchangeViewModel.importQr(text)
                            } else {
                                // Any other code (e.g. a Google Maps link) may still hold a position
                                initialLinkText = text
                                screen = Screen.AddSpot
                            }
                        }
                    },
                    onExchangeMessageShown = exchangeViewModel::messageShown,
                )
            }
            Screen.AddSpot -> {
                PlatformBackHandler { screen = Screen.SpotList }
                AddSpotScreen(
                    locationProvider = locationProvider,
                    linkResolver = linkResolver,
                    initialLinkText = initialLinkText,
                    onSave = { name, categories, notes, findDates, location ->
                        viewModel.addSpot(name, categories, notes, location, findDates)
                        screen = Screen.SpotList
                    },
                    onBack = { screen = Screen.SpotList },
                )
            }
            Screen.GoToSpot -> {
                PlatformBackHandler { screen = Screen.SpotList }
                // Null only briefly after process restart, while spots reload from the database
                spots.find { it.id == targetSpotId }?.let { spot ->
                    GoToSpotScreen(
                        spot = spot,
                        locationProvider = locationProvider,
                        headingProvider = headingProvider,
                        onBack = { screen = Screen.SpotList },
                    )
                }
            }
        }
    }
}
