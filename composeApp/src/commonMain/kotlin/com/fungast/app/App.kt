package com.fungast.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fungast.app.data.RouteDao
import com.fungast.app.data.SpotDao
import com.fungast.app.exchange.FileExchange
import com.fungast.app.exchange.QrScanner
import com.fungast.app.exchange.isSpotQr
import com.fungast.app.location.HeadingProvider
import com.fungast.app.location.LinkResolver
import com.fungast.app.location.LocationProvider
import com.fungast.app.map.OfflineMaps
import com.fungast.app.photos.LocalPhotoStore
import com.fungast.app.photos.PhotoPicker
import com.fungast.app.photos.PhotoStore
import com.fungast.app.routes.RouteRecorder
import com.fungast.app.settings.AppSettings
import com.fungast.app.ui.PlatformBackHandler
import com.fungast.app.ui.guide.GuideScreen
import com.fungast.app.ui.routes.RoutesScreen
import com.fungast.app.ui.routes.RoutesViewModel
import com.fungast.app.ui.spots.AddSpotScreen
import com.fungast.app.ui.spots.GoToSpotScreen
import com.fungast.app.ui.spots.MapDownloadViewModel
import com.fungast.app.ui.spots.PhotoActions
import com.fungast.app.ui.spots.SpotExchangeViewModel
import com.fungast.app.ui.spots.SpotsHomeScreen
import com.fungast.app.ui.spots.SpotsViewModel
import com.fungast.app.ui.theme.FungastTheme
import kotlinx.coroutines.launch

private enum class Screen { SpotList, AddSpot, GoToSpot, Routes, Guide }

@Composable
fun App(
    spotDao: SpotDao,
    routeDao: RouteDao,
    locationProvider: LocationProvider,
    headingProvider: HeadingProvider,
    linkResolver: LinkResolver,
    offlineMaps: OfflineMaps,
    fileExchange: FileExchange,
    qrScanner: QrScanner,
    photoStore: PhotoStore,
    photoPicker: PhotoPicker,
    routeRecorder: RouteRecorder,
    settings: AppSettings,
    /** Text shared to the app from another one (e.g. a Google Maps link): opens "new spot" with it. */
    sharedText: String? = null,
) {
    FungastTheme {
        CompositionLocalProvider(LocalPhotoStore provides photoStore) {
            val viewModel = viewModel { SpotsViewModel(spotDao, photoStore) }
            val mapDownloadViewModel = viewModel { MapDownloadViewModel(offlineMaps) }
            val exchangeViewModel = viewModel { SpotExchangeViewModel(spotDao, photoStore) }
            val routesViewModel = viewModel { RoutesViewModel(routeDao) }
            val scope = rememberCoroutineScope()
            // Shown once, on the very first launch
            var guideFirstLaunch by rememberSaveable { mutableStateOf(!settings.guideSeen) }
            var screen by rememberSaveable {
                mutableStateOf(
                    when {
                        sharedText != null -> Screen.AddSpot
                        guideFirstLaunch -> Screen.Guide
                        else -> Screen.SpotList
                    }
                )
            }
            var initialLinkText by rememberSaveable { mutableStateOf(sharedText) }
            var targetSpotId by rememberSaveable { mutableStateOf<Long?>(null) }
            var focusedRouteId by rememberSaveable { mutableStateOf<Long?>(null) }

            val spots by viewModel.spots.collectAsStateWithLifecycle()
            val routes by routesViewModel.routes.collectAsStateWithLifecycle()
            val recordingRoute by routesViewModel.recording.collectAsStateWithLifecycle()

            // A recording interrupted by a reboot or a force stop carries on when the app is opened again
            LaunchedEffect(Unit) {
                if (routesViewModel.isRecording()) routeRecorder.start()
            }

            when (screen) {
                Screen.SpotList -> {
                    val downloadState by mapDownloadViewModel.state.collectAsStateWithLifecycle()
                    val exchangeMessage by exchangeViewModel.message.collectAsStateWithLifecycle()
                    val routeTracks by routesViewModel.tracks.collectAsStateWithLifecycle()
                    val routeMessage by routesViewModel.message.collectAsStateWithLifecycle()
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
                        photoActions = { spot ->
                            PhotoActions(
                                onTakePhoto = {
                                    scope.launch {
                                        photoPicker.takePhoto()?.let { viewModel.addPhotos(spot.id, listOf(it)) }
                                    }
                                },
                                onPickPhotos = {
                                    scope.launch { viewModel.addPhotos(spot.id, photoPicker.pickPhotos()) }
                                },
                                onRemovePhoto = { viewModel.removePhoto(spot.id, it) },
                            )
                        },
                        onDownloadArea = mapDownloadViewModel::download,
                        onDownloadMessageShown = mapDownloadViewModel::messageShown,
                        exchangeMessage = exchangeMessage,
                        onExport = { format, includePhotos ->
                            scope.launch {
                                exchangeViewModel.buildExport(format, includePhotos)?.let { fileExchange.share(it) }
                            }
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
                        routes = routeTracks,
                        recordingRoute = recordingRoute,
                        focusedRoute = routes.find { it.id == focusedRouteId },
                        routeMessage = routeMessage,
                        onStartRecording = {
                            scope.launch {
                                if (locationProvider.requestPermission()) {
                                    routesViewModel.startRecording()
                                    routeRecorder.start()
                                } else {
                                    routesViewModel.locationPermissionDenied()
                                }
                            }
                        },
                        onStopRecording = { name ->
                            routesViewModel.stopRecording(name)
                            routeRecorder.stop()
                        },
                        onShowRoutes = { screen = Screen.Routes },
                        onShowGuide = { screen = Screen.Guide },
                        onClearRouteFocus = { focusedRouteId = null },
                        onRouteMessageShown = routesViewModel::messageShown,
                    )
                }
                Screen.AddSpot -> {
                    // Back is handled by the screen, which also discards its unsaved photos
                    AddSpotScreen(
                        locationProvider = locationProvider,
                        linkResolver = linkResolver,
                        initialLinkText = initialLinkText,
                        photoPicker = photoPicker,
                        onSave = { name, categories, notes, findDates, photos, location ->
                            viewModel.addSpot(name, categories, notes, location, findDates, photos)
                            screen = Screen.SpotList
                        },
                        onDiscardPhotos = viewModel::discardPhotos,
                        onBack = { screen = Screen.SpotList },
                    )
                }
                Screen.Guide -> {
                    val closeGuide = {
                        settings.guideSeen = true
                        guideFirstLaunch = false
                        screen = Screen.SpotList
                    }
                    PlatformBackHandler(onBack = closeGuide)
                    GuideScreen(firstLaunch = guideFirstLaunch, onClose = closeGuide)
                }
                Screen.Routes -> {
                    PlatformBackHandler { screen = Screen.SpotList }
                    RoutesScreen(
                        routes = routes,
                        onShowOnMap = { route ->
                            focusedRouteId = route.id
                            screen = Screen.SpotList
                        },
                        onRename = routesViewModel::rename,
                        onDelete = { route ->
                            if (route.id == focusedRouteId) focusedRouteId = null
                            routesViewModel.delete(route)
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
}
