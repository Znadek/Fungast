package com.fungast.app.ui.spots

import com.fungast.app.location.GeoPoint
import com.fungast.app.location.Heading
import com.fungast.app.location.HeadingProvider
import com.fungast.app.location.LocationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

private class FakeLocationProvider(private val permissionGranted: Boolean = true) : LocationProvider {
    val updates = MutableSharedFlow<GeoPoint>()
    override suspend fun requestPermission() = permissionGranted
    override suspend fun currentLocation(): GeoPoint? = null
    override fun locationUpdates(): Flow<GeoPoint> = updates
}

private class FakeHeadingProvider(override val hasCompass: Boolean = true) : HeadingProvider {
    val updates = MutableSharedFlow<Heading>()
    override fun headingUpdates(): Flow<Heading> = if (hasCompass) updates else emptyFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GoToSpotStateTest {
    // About 111 m due north of the start point
    private val target = GeoPoint(46.001, 11.0)
    private val start = GeoPoint(46.0, 11.0, accuracyMeters = 5f)

    @Test
    fun permissionDeniedStopsTheFlow() = runTest {
        val states = goToSpotState(target, FakeLocationProvider(permissionGranted = false), FakeHeadingProvider())
            .toList()

        assertEquals(listOf(GoToSpotState.Searching, GoToSpotState.PermissionDenied), states)
    }

    @Test
    fun arrowCombinesBearingAndHeading() = runTest {
        val location = FakeLocationProvider()
        val heading = FakeHeadingProvider()
        val states = mutableListOf<GoToSpotState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            goToSpotState(target, location, heading).collect { states += it }
        }

        assertEquals(GoToSpotState.Searching, states.last())

        location.updates.emit(start)
        val beforeCompass = assertIs<GoToSpotState.Tracking>(states.last())
        assertEquals(111.2, beforeCompass.distanceMeters, absoluteTolerance = 0.5)
        assertEquals(0.0, beforeCompass.bearingDegrees, absoluteTolerance = 1e-6)
        assertNull(beforeCompass.arrowDegrees)
        assertFalse(beforeCompass.arrived)

        // Facing east, a spot to the north is on the left
        heading.updates.emit(Heading(90.0))
        assertEquals(270.0, assertIs<GoToSpotState.Tracking>(states.last()).arrowDegrees!!, absoluteTolerance = 1e-6)

        job.cancel()
    }

    @Test
    fun tracksPositionWithoutCompass() = runTest {
        val location = FakeLocationProvider()
        val states = mutableListOf<GoToSpotState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            goToSpotState(target, location, FakeHeadingProvider(hasCompass = false)).collect { states += it }
        }

        location.updates.emit(start)

        val tracking = assertIs<GoToSpotState.Tracking>(states.last())
        assertNull(tracking.heading)
        job.cancel()
    }

    @Test
    fun arrivalRadiusFollowsAccuracyWithinLimits() {
        fun tracking(distance: Double, accuracy: Float?) =
            GoToSpotState.Tracking(distance, bearingDegrees = 0.0, accuracyMeters = accuracy, heading = null)

        assertTrue(tracking(MIN_ARRIVAL_RADIUS_METERS, accuracy = 3f).arrived)
        assertFalse(tracking(MIN_ARRIVAL_RADIUS_METERS + 1, accuracy = 3f).arrived)
        assertTrue(tracking(20.0, accuracy = 25f).arrived)
        assertTrue(tracking(MAX_ARRIVAL_RADIUS_METERS, accuracy = 100f).arrived)
        assertFalse(tracking(MAX_ARRIVAL_RADIUS_METERS + 1, accuracy = 100f).arrived)
        assertFalse(tracking(11.0, accuracy = null).arrived)
    }

    @Test
    fun arrowTakesTheShortestTurnAcrossNorth() {
        val unwrapper = AngleUnwrapper(350.0)

        assertEquals(370.0, unwrapper.next(10.0))
        assertEquals(340.0, unwrapper.next(340.0))
        assertEquals(170.0, unwrapper.next(170.0))
    }
}
