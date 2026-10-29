package com.fungast.app.ui.routes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fungast.app.data.Route
import com.fungast.app.ui.formatDistance
import com.fungast.app.ui.formatDuration
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

/** Shown on top of the list and the map while a route is being recorded. */
@Composable
fun RecordingBanner(route: Route, onStop: (name: String) -> Unit, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(1_000)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.FiberManualRecord,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Registrazione percorso", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${formatDistance(route.distanceMeters)} · ${formatDuration(now - route.startedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(onClick = { confirmStop = true }) { Text("Interrompi") }
        }
    }

    if (confirmStop) {
        var name by rememberSaveable { mutableStateOf(route.name) }
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Terminare il percorso?") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome del percorso") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    onStop(name)
                }) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text("Continua a registrare") }
            },
        )
    }
}
