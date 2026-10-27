package com.fungast.app.ui.spots

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fungast.app.data.Spot
import com.fungast.app.location.GeoPoint
import com.fungast.app.location.HeadingProvider
import com.fungast.app.location.LocationProvider
import com.fungast.app.ui.KeepScreenOn
import com.fungast.app.ui.cardinalDirection
import com.fungast.app.ui.formatDistance
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoToSpotScreen(
    spot: Spot,
    locationProvider: LocationProvider,
    headingProvider: HeadingProvider,
    onBack: () -> Unit,
) {
    KeepScreenOn()
    // collectAsStateWithLifecycle stops GPS and compass when the app goes to the background
    val stateFlow = remember(spot.id) {
        goToSpotState(GeoPoint(spot.latitude, spot.longitude), locationProvider, headingProvider)
    }
    val state by stateFlow.collectAsStateWithLifecycle(GoToSpotState.Searching)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(spot.name) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            when (val current = state) {
                GoToSpotState.Searching -> {
                    CircularProgressIndicator()
                    Text("Ricerca del segnale GPS…")
                }
                GoToSpotState.PermissionDenied -> Text(
                    "Per guidarti al posto serve il permesso di posizione. Concedilo dalle impostazioni del telefono.",
                    textAlign = TextAlign.Center,
                )
                is GoToSpotState.Tracking -> TrackingContent(current, hasCompass = headingProvider.hasCompass)
            }
        }
    }
}

@Composable
private fun TrackingContent(state: GoToSpotState.Tracking, hasCompass: Boolean) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant

    if (state.arrived) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(160.dp),
        )
        Text("Sei arrivato!", style = MaterialTheme.typography.headlineMedium)
    } else {
        state.arrowDegrees?.let { CompassArrow(it) }
        Text(formatDistance(state.distanceMeters), style = MaterialTheme.typography.displayMedium)
    }

    Text(
        "Direzione: ${cardinalDirection(state.bearingDegrees)} (${state.bearingDegrees.roundToInt()}°)",
        style = MaterialTheme.typography.bodyLarge,
    )
    state.accuracyMeters?.let {
        Text("Precisione GPS ±${it.roundToInt()} m", style = MaterialTheme.typography.bodyMedium, color = secondary)
    }

    val hint = when {
        !hasCompass -> "Il telefono non ha la bussola: orientati con la direzione indicata."
        state.heading == null -> "Attivazione della bussola…"
        state.heading.needsCalibration -> "Bussola poco precisa: muovi il telefono disegnando un 8 nell'aria."
        state.arrived -> null
        else -> "Tieni il telefono in piano, lontano da oggetti metallici."
    }
    hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = secondary, textAlign = TextAlign.Center) }
}

@Composable
private fun CompassArrow(degrees: Double) {
    val unwrapper = remember { AngleUnwrapper(degrees) }
    val rotation by animateFloatAsState(unwrapper.next(degrees).toFloat(), label = "arrow")

    Icon(
        Icons.Filled.Navigation,
        contentDescription = "Freccia verso il posto",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(200.dp).rotate(rotation),
    )
}

/**
 * Turns angles in [0, 360) into a continuous value that always takes the shortest turn,
 * so the arrow goes from 350° to 10° by rotating 20° instead of spinning back 340°.
 */
internal class AngleUnwrapper(private var current: Double) {
    fun next(degrees: Double): Double {
        current += ((degrees - current) % 360 + 540) % 360 - 180
        return current
    }
}
