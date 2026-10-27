package com.fungast.app.ui.spots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fungast.app.data.Spot
import com.fungast.app.map.MapViewport
import com.fungast.app.map.SpotsMap
import kotlin.math.roundToInt

@Composable
fun SpotsMapPane(
    spots: List<Spot>,
    downloadState: MapDownloadState,
    onDownloadArea: (MapViewport?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewport by remember { mutableStateOf<MapViewport?>(null) }
    var selectedSpotId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedSpot = spots.find { it.id == selectedSpotId }

    Box(modifier = modifier.fillMaxSize()) {
        SpotsMap(
            spots = spots,
            onSpotClick = { selectedSpotId = it.id },
            onViewportChange = { viewport = it },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            selectedSpot?.let { spot ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        SpotSummary(spot, modifier = Modifier.weight(1f))
                        IconButton(onClick = { selectedSpotId = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Chiudi")
                        }
                    }
                }
            }
            if (downloadState is MapDownloadState.InProgress) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Download mappa offline… ${(downloadState.progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Bottom-start, so it doesn't collide with the "Salva posto" FAB on the right
        SmallFloatingActionButton(
            onClick = { onDownloadArea(viewport) },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        ) {
            Icon(Icons.Filled.Download, contentDescription = "Scarica quest'area per l'uso offline")
        }
    }
}
