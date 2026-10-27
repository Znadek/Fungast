package com.fungast.app.ui.spots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fungast.app.data.Spot
import com.fungast.app.exchange.QR_MAX_NOTES_LENGTH
import com.fungast.app.exchange.QrCode
import com.fungast.app.exchange.encodeQrCode
import com.fungast.app.exchange.encodeSpotQr
import com.fungast.app.ui.KeepScreenOn

/** Shows the spot as a QR code, for a friend to scan with Fungast. */
@Composable
fun ShowSpotQrButton(spot: Spot) {
    var shown by remember { mutableStateOf(false) }
    IconButton(onClick = { shown = true }) {
        Icon(Icons.Outlined.QrCode2, contentDescription = "Mostra codice QR")
    }
    if (shown) SpotQrDialog(spot, onDismiss = { shown = false })
}

@Composable
private fun SpotQrDialog(spot: Spot, onDismiss: () -> Unit) {
    // The friend may take a while to open the scanner
    KeepScreenOn()
    val code = remember(spot) { encodeQrCode(encodeSpotQr(spot)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spot.name) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (code == null) {
                    Text("Codice QR non disponibile su questo dispositivo.")
                } else {
                    QrImage(code, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                    Text(
                        "Fai inquadrare il codice a chi ha Fungast: menu ⋮ → \"Ricevi posto da QR\".",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (spot.notes.length > QR_MAX_NOTES_LENGTH) {
                        Text(
                            "Le note sono accorciate: per inviarle complete usa l'esportazione su file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        },
    )
}

/** Black on white whatever the theme, with the quiet zone scanners need around the code. */
@Composable
private fun QrImage(code: QrCode, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Color.White)
        val quietZone = 4
        val cell = size.minDimension / (code.size + 2 * quietZone)
        for (y in 0 until code.size) {
            for (x in 0 until code.size) {
                if (!code.isDark(x, y)) continue
                drawRect(
                    color = Color.Black,
                    topLeft = Offset((x + quietZone) * cell, (y + quietZone) * cell),
                    // A hair larger, so antialiasing doesn't leave seams between neighbouring cells
                    size = Size(cell + 0.5f, cell + 0.5f),
                )
            }
        }
    }
}
