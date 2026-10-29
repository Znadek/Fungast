package com.fungast.app.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fungast.app.data.Spot

// TODO: wrap MapLibre Native iOS (MLNMapView) in a UIKitView when the iOS app is set up
@Composable
actual fun SpotsMap(
    spots: List<Spot>,
    routes: List<RouteTrack>,
    focusedRouteId: Long?,
    onSpotClick: (Spot) -> Unit,
    onViewportChange: (MapViewport) -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Text("La mappa non è ancora disponibile su iOS.", textAlign = TextAlign.Center)
    }
}
