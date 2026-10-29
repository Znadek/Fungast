package com.fungast.app.routes

import com.fungast.app.data.RouteDao
import com.fungast.app.data.RoutePoint
import com.fungast.app.location.GeoPoint
import com.fungast.app.location.distanceMeters
import kotlinx.datetime.Clock

/**
 * Turns GPS fixes into the points of the route being recorded. Shared by the platform recorders,
 * which only feed it fixes; the database says which route (if any) is being recorded.
 */
class RouteTracker(
    private val routeDao: RouteDao,
    private val clock: Clock = Clock.System,
) {
    /**
     * Adds [fix] to the open route if it is precise enough and far enough from the previous point.
     * Returns false when no route is being recorded any more, so the caller can stop the GPS.
     */
    suspend fun record(fix: GeoPoint): Boolean {
        val route = routeDao.getOpen() ?: return false
        val accuracy = fix.accuracyMeters
        if (accuracy != null && accuracy > MAX_ACCURACY_METERS) return true
        val last = routeDao.lastPoint(route.id)
        val step = last?.let { distanceMeters(GeoPoint(it.latitude, it.longitude), fix) } ?: 0.0
        // Standing still, the GPS position wanders by a few meters: don't count that as walking
        if (last != null && step < maxOf(MIN_STEP_METERS, (accuracy ?: 0f).toDouble())) return true
        routeDao.insertPoint(
            RoutePoint(
                routeId = route.id,
                latitude = fix.latitude,
                longitude = fix.longitude,
                altitude = fix.altitude,
                accuracyMeters = accuracy,
                time = clock.now().toEpochMilliseconds(),
            )
        )
        routeDao.addDistance(route.id, step)
        return true
    }

    /** Ends the route being recorded, if any. Routes too short to be useful are deleted; returns whether one was kept. */
    suspend fun finish(): Boolean {
        val route = routeDao.getOpen() ?: return false
        if (routeDao.pointCount(route.id) < MIN_POINTS) {
            routeDao.delete(route)
            return false
        }
        routeDao.setEnded(route.id, clock.now().toEpochMilliseconds())
        return true
    }

    private companion object {
        /** Fixes worse than this (e.g. from cell towers) would draw zigzags. */
        const val MAX_ACCURACY_METERS = 30f
        const val MIN_STEP_METERS = 5.0
        const val MIN_POINTS = 2
    }
}

/** Starts and stops the platform's background recording, which keeps going with the screen off. */
interface RouteRecorder {
    /** Starts recording into the open route. Location permission must already be granted. */
    suspend fun start()

    fun stop()
}
