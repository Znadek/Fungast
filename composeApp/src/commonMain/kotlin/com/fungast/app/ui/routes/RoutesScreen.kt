package com.fungast.app.ui.routes

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fungast.app.data.Route
import com.fungast.app.ui.formatDateTime
import com.fungast.app.ui.formatDistance
import com.fungast.app.ui.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    routes: List<Route>,
    onShowOnMap: (Route) -> Unit,
    onRename: (Route, String) -> Unit,
    onDelete: (Route) -> Unit,
    onBack: () -> Unit,
) {
    var routeToRename by remember { mutableStateOf<Route?>(null) }
    var routeToDelete by remember { mutableStateOf<Route?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I miei percorsi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Nessun percorso registrato.\nPer registrarne uno, tocca l'icona del percorso in alto " +
                        "nella schermata principale.",
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
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(routes, key = { it.id }) { route ->
                    RouteItem(
                        route = route,
                        onShowOnMap = { onShowOnMap(route) },
                        onRename = { routeToRename = route },
                        onDelete = { routeToDelete = route },
                    )
                }
            }
        }
    }

    routeToRename?.let { route ->
        var name by rememberSaveable { mutableStateOf(route.name) }
        AlertDialog(
            onDismissRequest = { routeToRename = null },
            title = { Text("Rinomina percorso") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(route, name)
                        routeToRename = null
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { routeToRename = null }) { Text("Annulla") }
            },
        )
    }

    routeToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = { Text("Eliminare \"${route.name}\"?") },
            text = { Text("Il percorso verrà cancellato definitivamente.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(route)
                    routeToDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun RouteItem(route: Route, onShowOnMap: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(route.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatDateTime(route.startedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val duration = route.endedAt?.let { formatDuration(it - route.startedAt) }
                Text(
                    listOfNotNull(formatDistance(route.distanceMeters), duration).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onShowOnMap) {
                Icon(Icons.Outlined.Map, contentDescription = "Mostra sulla mappa")
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Outlined.Edit, contentDescription = "Rinomina")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Elimina")
            }
        }
    }
}
