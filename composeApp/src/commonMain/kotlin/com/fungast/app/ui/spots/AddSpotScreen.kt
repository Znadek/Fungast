package com.fungast.app.ui.spots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fungast.app.data.SpotCategory
import com.fungast.app.location.GeoPoint
import com.fungast.app.location.LocationProvider
import com.fungast.app.ui.formatLatLon
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

sealed interface LocationState {
    data object Searching : LocationState
    data object PermissionDenied : LocationState
    data object Unavailable : LocationState
    data class Found(val point: GeoPoint) : LocationState
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSpotScreen(
    locationProvider: LocationProvider,
    onSave: (name: String, category: SpotCategory, notes: String, location: GeoPoint) -> Unit,
    onBack: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(SpotCategory.MUSHROOMS) }
    var notes by rememberSaveable { mutableStateOf("") }
    var locationState by remember { mutableStateOf<LocationState>(LocationState.Searching) }
    val scope = rememberCoroutineScope()

    suspend fun locate() {
        locationState = LocationState.Searching
        locationState = when {
            !locationProvider.requestPermission() -> LocationState.PermissionDenied
            else -> locationProvider.currentLocation()?.let { LocationState.Found(it) }
                ?: LocationState.Unavailable
        }
    }

    LaunchedEffect(Unit) { locate() }

    val location = (locationState as? LocationState.Found)?.point
    val canSave = name.isNotBlank() && location != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuovo posto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LocationCard(
                state = locationState,
                onRetry = { scope.launch { locate() } },
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                placeholder = { Text("Es. Faggeta sopra il rifugio") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Cosa si trova", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpotCategory.entries.forEach { option ->
                    FilterChip(
                        selected = option == category,
                        onClick = { category = option },
                        label = { Text(option.label) },
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Note") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { location?.let { onSave(name, category, notes, it) } },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salva")
            }
        }
    }
}

@Composable
private fun LocationCard(state: LocationState, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Posizione", style = MaterialTheme.typography.titleSmall)
            when (state) {
                LocationState.Searching -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Ricerca del segnale GPS…")
                }
                LocationState.PermissionDenied ->
                    Text("Permesso di posizione negato. Concedilo per salvare il posto.")
                LocationState.Unavailable ->
                    Text("Posizione non disponibile. Controlla che il GPS sia attivo e riprova all'aperto.")
                is LocationState.Found -> {
                    Text(formatLatLon(state.point.latitude, state.point.longitude), style = MaterialTheme.typography.bodyLarge)
                    val details = listOfNotNull(
                        state.point.accuracyMeters?.let { "precisione ±${it.roundToInt()} m" },
                        state.point.altitude?.let { "altitudine ${it.roundToInt()} m" },
                    )
                    if (details.isNotEmpty()) {
                        Text(
                            details.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state != LocationState.Searching) {
                OutlinedButton(onClick = onRetry) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Aggiorna posizione", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
