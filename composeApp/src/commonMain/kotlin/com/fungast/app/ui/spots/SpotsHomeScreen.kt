package com.fungast.app.ui.spots

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fungast.app.data.Route
import com.fungast.app.data.Spot
import com.fungast.app.exchange.ExportFormat
import com.fungast.app.map.MapViewport
import com.fungast.app.map.RouteTrack
import com.fungast.app.ui.routes.RecordingBanner
import com.fungast.app.ui.routes.RouteMessage

private enum class HomeMode { List, Map }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotsHomeScreen(
    spots: List<Spot>,
    downloadState: MapDownloadState,
    onAddSpot: () -> Unit,
    onDeleteSpot: (Spot) -> Unit,
    onGoToSpot: (Spot) -> Unit,
    photoActions: (Spot) -> PhotoActions,
    onDownloadArea: (MapViewport?) -> Unit,
    onDownloadMessageShown: () -> Unit,
    exchangeMessage: ExchangeMessage?,
    onExport: (format: ExportFormat, includePhotos: Boolean) -> Unit,
    onImport: () -> Unit,
    onScanQr: () -> Unit,
    onExchangeMessageShown: () -> Unit,
    routes: List<RouteTrack>,
    recordingRoute: Route?,
    focusedRoute: Route?,
    routeMessage: RouteMessage?,
    onStartRecording: () -> Unit,
    onStopRecording: (name: String) -> Unit,
    onShowRoutes: () -> Unit,
    onShowGuide: () -> Unit,
    onClearRouteFocus: () -> Unit,
    onRouteMessageShown: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(HomeMode.List) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Chosen with "Mostra sulla mappa" in the routes list
    LaunchedEffect(focusedRoute?.id) {
        if (focusedRoute != null) mode = HomeMode.Map
    }

    LaunchedEffect(routeMessage) {
        val text = when (routeMessage) {
            is RouteMessage.Saved -> "Percorso \"${routeMessage.name}\" salvato."
            RouteMessage.TooShort -> "Percorso troppo corto: non è stato salvato."
            RouteMessage.NoLocationPermission -> "Per registrare un percorso serve il permesso di posizione."
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(text)
        onRouteMessageShown()
    }

    LaunchedEffect(downloadState) {
        val message = when (downloadState) {
            MapDownloadState.Done -> "Area scaricata: la mappa funzionerà anche senza rete."
            MapDownloadState.Failed -> "Download non riuscito. Controlla la connessione e riprova."
            MapDownloadState.ZoomTooLow -> "Avvicinati alla zona che ti interessa per scaricarla."
            else -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        onDownloadMessageShown()
    }

    LaunchedEffect(exchangeMessage) {
        val message = exchangeMessage?.let(::exchangeMessageText) ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onExchangeMessageShown()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I miei posti") },
                actions = {
                    if (recordingRoute == null) {
                        IconButton(onClick = onStartRecording) {
                            Icon(Icons.Outlined.Route, contentDescription = "Registra percorso")
                        }
                    }
                    when (mode) {
                        HomeMode.List -> IconButton(onClick = { mode = HomeMode.Map }) {
                            Icon(Icons.Outlined.Map, contentDescription = "Mostra mappa")
                        }
                        HomeMode.Map -> IconButton(onClick = { mode = HomeMode.List }) {
                            Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = "Mostra elenco")
                        }
                    }
                    ExchangeMenu(
                        onExport = onExport,
                        onImport = onImport,
                        onScanQr = onScanQr,
                        onShowRoutes = onShowRoutes,
                        onShowGuide = onShowGuide,
                    )
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSpot,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Salva posto") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
          Column(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            recordingRoute?.let {
                RecordingBanner(
                    route = it,
                    onStop = onStopRecording,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
            }
            // The top bar and the banner are above: only the bottom inset is left for the content
            val contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            when (mode) {
                HomeMode.List -> SpotList(
                    spots = spots,
                    contentPadding = contentPadding,
                    onGoToSpot = onGoToSpot,
                    onDeleteSpot = onDeleteSpot,
                    photoActions = photoActions,
                    onShowGuide = onShowGuide,
                )
                HomeMode.Map -> SpotsMapPane(
                    spots = spots,
                    downloadState = downloadState,
                    onDownloadArea = onDownloadArea,
                    onGoToSpot = onGoToSpot,
                    photoActions = photoActions,
                    routes = routes,
                    focusedRoute = focusedRoute,
                    onClearRouteFocus = onClearRouteFocus,
                    modifier = Modifier.padding(contentPadding),
                )
            }
        }
    }
}

@Composable
private fun ExchangeMenu(
    onExport: (ExportFormat, Boolean) -> Unit,
    onImport: () -> Unit,
    onScanQr: () -> Unit,
    onShowRoutes: () -> Unit,
    onShowGuide: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Altre opzioni")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("I miei percorsi") },
            leadingIcon = { Icon(Icons.Outlined.Route, contentDescription = null) },
            onClick = {
                expanded = false
                onShowRoutes()
            },
        )
        DropdownMenuItem(
            text = { Text("Ricevi posto da QR") },
            leadingIcon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) },
            onClick = {
                expanded = false
                onScanQr()
            },
        )
        DropdownMenuItem(
            text = { Text("Esporta backup (.fungast)") },
            leadingIcon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
            onClick = {
                expanded = false
                onExport(ExportFormat.FUNGAST, false)
            },
        )
        DropdownMenuItem(
            text = { Text("Esporta backup con foto (.fungast)") },
            leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
            onClick = {
                expanded = false
                onExport(ExportFormat.FUNGAST, true)
            },
        )
        DropdownMenuItem(
            text = { Text("Esporta per altre app (GPX)") },
            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
            onClick = {
                expanded = false
                onExport(ExportFormat.GPX, false)
            },
        )
        DropdownMenuItem(
            text = { Text("Importa da file") },
            leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
            onClick = {
                expanded = false
                onImport()
            },
        )
        DropdownMenuItem(
            text = { Text("Guida") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null) },
            onClick = {
                expanded = false
                onShowGuide()
            },
        )
    }
}

internal fun exchangeMessageText(message: ExchangeMessage): String = when (message) {
    ExchangeMessage.NothingToExport -> "Nessun posto da esportare."
    ExchangeMessage.ImportFailed -> "File non valido: scegli un file .fungast o .gpx."
    is ExchangeMessage.QrImported -> "Posto \"${message.spotName}\" aggiunto."
    is ExchangeMessage.QrAlreadySaved -> "Hai già il posto \"${message.spotName}\"."
    ExchangeMessage.QrInvalid -> "Questo codice QR non contiene un posto Fungast."
    is ExchangeMessage.Imported -> when {
        message.added == 0 && message.alreadyPresent == 0 -> "Nessun posto trovato nel file."
        message.added == 0 -> "Tutti i posti del file sono già salvati."
        else -> {
            val added = if (message.added == 1) "1 posto importato" else "${message.added} posti importati"
            if (message.alreadyPresent == 0) "$added." else "$added (${message.alreadyPresent} già presenti)."
        }
    }
}
