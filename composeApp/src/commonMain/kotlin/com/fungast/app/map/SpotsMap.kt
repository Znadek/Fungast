package com.fungast.app.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fungast.app.data.Spot

/**
 * Native map showing [spots] as colored markers and [routes] as lines; when [focusedRouteId] changes
 * the camera moves to that route. Reports the visible area every time the
 * camera stops moving, so the caller can offer to download it for offline use.
 */
@Composable
expect fun SpotsMap(
    spots: List<Spot>,
    routes: List<RouteTrack>,
    focusedRouteId: Long?,
    onSpotClick: (Spot) -> Unit,
    onViewportChange: (MapViewport) -> Unit,
    modifier: Modifier = Modifier,
)
