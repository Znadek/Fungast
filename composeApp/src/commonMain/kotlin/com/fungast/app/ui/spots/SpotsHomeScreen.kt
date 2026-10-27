package com.fungast.app.ui.spots

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Map
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
import com.fungast.app.data.Spot
import com.fungast.app.exchange.ExportFormat
import com.fungast.app.map.MapViewport

private enum class HomeMode { List, Map }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotsHomeScreen(
    spots: List<Spot>,
    downloadState: MapDownloadState,
    onAddSpot: () -> Unit,
    onDeleteSpot: (Spot) -> Unit,
    onGoToSpot: (Spot) -> Unit,
    onDownloadArea: (MapViewport?) -> Unit,
    onDownloadMessageShown: () -> Unit,
    exchangeMessage: ExchangeMessage?,
    onExport: (ExportFormat) -> Unit,
    onImport: () -> Unit,
    onExchangeMessageShown: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(HomeMode.List) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                    when (mode) {
                        HomeMode.List -> IconButton(onClick = { mode = HomeMode.Map }) {
                            Icon(Icons.Outlined.Map, contentDescription = "Mostra mappa")
                        }
                        HomeMode.Map -> IconButton(onClick = { mode = HomeMode.List }) {
                            Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = "Mostra elenco")
                        }
                    }
                    ExchangeMenu(onExport = onExport, onImport = onImport)
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
        when (mode) {
            HomeMode.List -> SpotList(
                spots = spots,
                contentPadding = innerPadding,
                onGoToSpot = onGoToSpot,
                onDeleteSpot = onDeleteSpot,
            )
            HomeMode.Map -> SpotsMapPane(
                spots = spots,
                downloadState = downloadState,
                onDownloadArea = onDownloadArea,
                onGoToSpot = onGoToSpot,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ExchangeMenu(onExport: (ExportFormat) -> Unit, onImport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Altre opzioni")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Esporta backup (.fungast)") },
            leadingIcon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
            onClick = {
                expanded = false
                onExport(ExportFormat.FUNGAST)
            },
        )
        DropdownMenuItem(
            text = { Text("Esporta per altre app (GPX)") },
            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
            onClick = {
                expanded = false
                onExport(ExportFormat.GPX)
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
    }
}

internal fun exchangeMessageText(message: ExchangeMessage): String = when (message) {
    ExchangeMessage.NothingToExport -> "Nessun posto da esportare."
    ExchangeMessage.ImportFailed -> "File non valido: scegli un file .fungast o .gpx."
    is ExchangeMessage.Imported -> when {
        message.added == 0 && message.alreadyPresent == 0 -> "Nessun posto trovato nel file."
        message.added == 0 -> "Tutti i posti del file sono già salvati."
        else -> {
            val added = if (message.added == 1) "1 posto importato" else "${message.added} posti importati"
            if (message.alreadyPresent == 0) "$added." else "$added (${message.alreadyPresent} già presenti)."
        }
    }
}
