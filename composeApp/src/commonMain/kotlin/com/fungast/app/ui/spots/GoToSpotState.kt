package com.fungast.app.ui.spots

import com.fungast.app.location.GeoPoint
import com.fungast.app.location.Heading
import com.fungast.app.location.HeadingProvider
import com.fungast.app.location.LocationProvider
import com.fungast.app.location.bearingDegrees
import com.fungast.app.location.distanceMeters
import com.fungast.app.location.normalizeDegrees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

/** Within this distance the user has arrived, even with a very precise fix. */
internal const val MIN_ARRIVAL_RADIUS_METERS = 10.0

/** A poor fix (e.g. ±100 m) must not claim arrival from far away. */
internal const val MAX_ARRIVAL_RADIUS_METERS = 30.0

sealed interface GoToSpotState {
    data object Searching : GoToSpotState
    data object PermissionDenied : GoToSpotState

    data class Tracking(
        val distanceMeters: Double,
        /** Direction of the spot, in degrees clockwise from true north. */
        val bearingDegrees: Double,
        val accuracyMeters: Float?,
        /** Null until the compass reports, or forever if the device has none. */
        val heading: Heading?,
    ) : GoToSpotState {
        /** Rotation of an arrow pointing at the spot, relative to the top of the phone. */
        val arrowDegrees: Double? get() = heading?.let { normalizeDegrees(bearingDegrees - it.degrees) }

        val arrived: Boolean
            get() = distanceMeters <= (accuracyMeters?.toDouble() ?: 0.0)
                .coerceIn(MIN_ARRIVAL_RADIUS_METERS, MAX_ARRIVAL_RADIUS_METERS)
    }
}

/** Guides the user to [target]: asks for permission, then follows position and compass while collected. */
fun goToSpotState(
    target: GeoPoint,
    locationProvider: LocationProvider,
    headingProvider: HeadingProvider,
): Flow<GoToSpotState> = flow {
    emit(GoToSpotState.Searching)
    if (!locationProvider.requestPermission()) {
        emit(GoToSpotState.PermissionDenied)
        return@flow
    }
    val headings = headingProvider.headingUpdates().onStart<Heading?> { emit(null) }
    emitAll(
        combine(locationProvider.locationUpdates(), headings) { location, heading ->
            GoToSpotState.Tracking(
                distanceMeters = distanceMeters(location, target),
                bearingDegrees = bearingDegrees(location, target),
                accuracyMeters = location.accuracyMeters,
                heading = heading,
            )
        }
    )
}
