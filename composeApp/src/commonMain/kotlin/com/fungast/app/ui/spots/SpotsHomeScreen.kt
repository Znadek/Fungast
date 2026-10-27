package com.fungast.app.ui.spots

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Map
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
