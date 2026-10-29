package com.fungast.app.ui.spots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fungast.app.data.Spot
import com.fungast.app.data.findDates
import com.fungast.app.data.sortedCategories
import com.fungast.app.ui.formatDate
import com.fungast.app.ui.formatDateTime
import com.fungast.app.ui.formatLatLon

@Composable
fun SpotList(
    spots: List<Spot>,
    contentPadding: PaddingValues,
    onGoToSpot: (Spot) -> Unit,
    onDeleteSpot: (Spot) -> Unit,
) {
    var spotToDelete by remember { mutableStateOf<Spot?>(null) }

    if (spots.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Nessun posto salvato.\nQuando sei sul posto, tocca \"Salva posto\".",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                // Leave room so the FAB doesn't cover the last item
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(spots, key = { it.id }) { spot ->
                SpotItem(spot = spot, onGoTo = { onGoToSpot(spot) }, onDelete = { spotToDelete = spot })
            }
        }
    }

    spotToDelete?.let { spot ->
        AlertDialog(
            onDismissRequest = { spotToDelete = null },
            title = { Text("Eliminare \"${spot.name}\"?") },
            text = { Text("Il posto verrà cancellato definitivamente.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSpot(spot)
                    spotToDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { spotToDelete = null }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun SpotItem(spot: Spot, onGoTo: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpotSummary(spot, modifier = Modifier.weight(1f))
            ShowSpotQrButton(spot)
            GoToSpotButton(onClick = onGoTo)
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Elimina")
            }
        }
    }
}

/** Opens the compass that guides the user to the spot. */
@Composable
fun GoToSpotButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Outlined.Explore, contentDescription = "Portami lì")
    }
}

/** Name, categories, dates, coordinates and notes of a spot; shared by the list and the map. */
@Composable
fun SpotSummary(spot: Spot, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(spot.name, style = MaterialTheme.typography.titleMedium)
        Text(
            "${spot.sortedCategories.joinToString(", ") { it.label }} · ${formatDateTime(spot.createdAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (spot.findDates.isNotEmpty()) {
            Text(
                (if (spot.findDates.size == 1) "Ultimo ritrovamento: " else "Ultimi ritrovamenti: ") +
                    spot.findDates.joinToString(", ") { formatDate(it) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatLatLon(spot.latitude, spot.longitude),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (spot.notes.isNotEmpty()) {
            Text(spot.notes, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
