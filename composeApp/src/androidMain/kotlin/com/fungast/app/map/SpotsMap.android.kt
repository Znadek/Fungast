package com.fungast.app.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fungast.app.data.Spot
import com.fungast.app.data.mainCategory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val SPOTS_SOURCE = "spots"
private const val SPOTS_CIRCLE_LAYER = "spots-circles"
private const val SPOTS_LABEL_LAYER = "spots-labels"
private const val ROUTES_SOURCE = "routes"
private const val ROUTES_LAYER = "routes-lines"
private const val ROUTE_COLOR = "#E67E22"
private const val FOCUSED_ROUTE_COLOR = "#6C3483"
private const val RECORDING_ROUTE_COLOR = "#C0392B"

// Roughly central Italy, used until there are spots to frame
private val DEFAULT_CAMERA = CameraPosition.Builder().target(LatLng(42.5, 12.5)).zoom(5.0).build()
private const val SINGLE_SPOT_ZOOM = 14.0
private const val FIT_PADDING_PX = 160
private const val TAP_TOLERANCE_PX = 24f

@Composable
actual fun SpotsMap(
    spots: List<Spot>,
    routes: List<RouteTrack>,
    focusedRouteId: Long?,
    onSpotClick: (Spot) -> Unit,
    onViewportChange: (MapViewport) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentSpots by rememberUpdatedState(spots)
    val currentOnSpotClick by rememberUpdatedState(onSpotClick)
    val currentOnViewportChange by rememberUpdatedState(onViewportChange)

    val mapView = remember { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var cameraFitted by remember { mutableStateOf(false) }

    // MapView needs the host lifecycle forwarded to it
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            val state = lifecycle.currentState
            if (state.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (state.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { mapLibreMap ->
            mapLibreMap.cameraPosition = DEFAULT_CAMERA
            mapLibreMap.addOnMapClickListener { latLng ->
                val point = mapLibreMap.projection.toScreenLocation(latLng)
                val area = RectF(
                    point.x - TAP_TOLERANCE_PX, point.y - TAP_TOLERANCE_PX,
                    point.x + TAP_TOLERANCE_PX, point.y + TAP_TOLERANCE_PX,
                )
                val spotId = mapLibreMap.queryRenderedFeatures(area, SPOTS_CIRCLE_LAYER)
                    .firstOrNull()?.getNumberProperty("id")?.toLong()
                val spot = currentSpots.find { it.id == spotId } ?: return@addOnMapClickListener false
                currentOnSpotClick(spot)
                true
            }
            mapLibreMap.addOnCameraIdleListener {
                val visible = mapLibreMap.projection.visibleRegion.latLngBounds
                currentOnViewportChange(
                    MapViewport(
                        bounds = GeoBounds(
                            south = visible.latitudeSouth,
                            west = visible.longitudeWest,
                            north = visible.latitudeNorth,
                            east = visible.longitudeEast,
                        ),
                        zoom = mapLibreMap.cameraPosition.zoom,
                    )
                )
            }
            mapLibreMap.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { loadedStyle ->
                // Added first, so lines are drawn under the spot markers
                loadedStyle.addRouteLayer()
                loadedStyle.addSpotLayers()
                mapLibreMap.showUserLocation(context, loadedStyle)
                style = loadedStyle
            }
            map = mapLibreMap
        }
    }

    LaunchedEffect(style, spots) {
        val loadedStyle = style ?: return@LaunchedEffect
        loadedStyle.getSourceAs<GeoJsonSource>(SPOTS_SOURCE)?.setGeoJson(spots.toFeatureCollection())
        // A route to show takes over the first framing
        if (!cameraFitted && spots.isNotEmpty() && focusedRouteId == null) {
            map?.frame(spots.map { LatLng(it.latitude, it.longitude) })
            cameraFitted = true
        }
    }

    LaunchedEffect(style, routes, focusedRouteId) {
        val loadedStyle = style ?: return@LaunchedEffect
        loadedStyle.getSourceAs<GeoJsonSource>(ROUTES_SOURCE)?.setGeoJson(routes.toFeatureCollection(focusedRouteId))
    }

    // Only when the focus changes, so following a recording route doesn't keep moving the camera
    LaunchedEffect(style, focusedRouteId) {
        style ?: return@LaunchedEffect
        val points = routes.find { it.routeId == focusedRouteId }?.points ?: return@LaunchedEffect
        if (points.isEmpty()) return@LaunchedEffect
        map?.frame(points.map { LatLng(it.latitude, it.longitude) })
        cameraFitted = true
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun Style.addSpotLayers() {
    addSource(GeoJsonSource(SPOTS_SOURCE))
    addLayer(
        CircleLayer(SPOTS_CIRCLE_LAYER, SPOTS_SOURCE).withProperties(
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleColor(Expression.toColor(Expression.get("color"))),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeColor(Color.WHITE),
        )
    )
    addLayer(
        SymbolLayer(SPOTS_LABEL_LAYER, SPOTS_SOURCE).withProperties(
            PropertyFactory.textField(Expression.get("name")),
            // Must be a font served by the style's glyph endpoint
            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.textOffset(arrayOf(0f, 1f)),
            PropertyFactory.textHaloColor(Color.WHITE),
            PropertyFactory.textHaloWidth(1.5f),
            PropertyFactory.textOptional(true),
        )
    )
}

private fun Style.addRouteLayer() {
    addSource(GeoJsonSource(ROUTES_SOURCE))
    addLayer(
        LineLayer(ROUTES_LAYER, ROUTES_SOURCE).withProperties(
            PropertyFactory.lineColor(Expression.toColor(Expression.get("color"))),
            PropertyFactory.lineWidth(Expression.get("width")),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
}

private fun List<RouteTrack>.toFeatureCollection(focusedRouteId: Long?): FeatureCollection =
    FeatureCollection.fromFeatures(
        // A line needs two points; the focused one last, so it is drawn on top
        filter { it.points.size >= 2 }.sortedBy { it.routeId == focusedRouteId }.map { track ->
            val line = LineString.fromLngLats(track.points.map { Point.fromLngLat(it.longitude, it.latitude) })
            Feature.fromGeometry(line).apply {
                val (color, width) = when {
                    track.recording -> RECORDING_ROUTE_COLOR to 5f
                    track.routeId == focusedRouteId -> FOCUSED_ROUTE_COLOR to 6f
                    else -> ROUTE_COLOR to 3.5f
                }
                addStringProperty("color", color)
                addNumberProperty("width", width)
            }
        }
    )

private fun List<Spot>.toFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(
        map { spot ->
            Feature.fromGeometry(Point.fromLngLat(spot.longitude, spot.latitude)).apply {
                addNumberProperty("id", spot.id)
                addStringProperty("name", spot.name)
                addStringProperty("color", spot.mainCategory.markerColorHex)
            }
        }
    )

private fun MapLibreMap.frame(points: List<LatLng>) {
    val update = if (points.distinct().size == 1) {
        CameraUpdateFactory.newLatLngZoom(points.first(), SINGLE_SPOT_ZOOM)
    } else {
        CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(), FIT_PADDING_PX)
    }
    moveCamera(update)
}

/** Shows the blue position dot, only if the user already granted location permission. */
@SuppressLint("MissingPermission")
private fun MapLibreMap.showUserLocation(context: Context, style: Style) {
    val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    if (!granted) return
    locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(context, style).build())
    locationComponent.isLocationComponentEnabled = true
}
