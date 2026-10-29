package com.fungast.app.routes

import com.fungast.app.data.Route
import com.fungast.app.location.GeoPoint
import com.fungast.app.ui.spots.FixedClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant

class RouteTrackerTest {
    private val dao = FakeRouteDao()
    private val now = Instant.parse("2026-10-29T08:00:00Z")
    private val tracker = RouteTracker(dao, FixedClock(now))

    // 0.0001° of latitude is about 11 m
    private fun fix(northSteps: Int, accuracy: Float? = 5f) = GeoPoint(46.0 + northSteps * 0.0001, 11.0, accuracyMeters = accuracy)

    @Test
    fun recordsPointsAndAddsUpDistance() = runTest {
        dao.insert(Route(name = "Giro", startedAt = 0))

        assertTrue(tracker.record(fix(0)))
        assertTrue(tracker.record(fix(1)))
        assertTrue(tracker.record(fix(3)))

        assertEquals(3, dao.points.value.size)
        assertEquals(33.4, dao.routes.value.single().distanceMeters, 0.5)
    }

    @Test
    fun ignoresImpreciseFixesAndStandingStill() = runTest {
        dao.insert(Route(name = "Giro", startedAt = 0))

        tracker.record(fix(0))
        tracker.record(fix(5, accuracy = 80f))
        tracker.record(GeoPoint(46.00002, 11.0, accuracyMeters = 5f))

        assertEquals(1, dao.points.value.size)
        assertEquals(0.0, dao.routes.value.single().distanceMeters)
    }

    @Test
    fun withoutAnOpenRouteTheRecorderIsToldToStop() = runTest {
        assertFalse(tracker.record(fix(0)))
        assertTrue(dao.points.value.isEmpty())
    }

    @Test
    fun finishKeepsRoutesWithAtLeastTwoPoints() = runTest {
        dao.insert(Route(name = "Giro", startedAt = 0))
        tracker.record(fix(0))
        tracker.record(fix(1))

        assertTrue(tracker.finish())

        assertEquals(now.toEpochMilliseconds(), dao.routes.value.single().endedAt)
    }

    @Test
    fun finishDeletesRoutesTooShortToShow() = runTest {
        dao.insert(Route(name = "Giro", startedAt = 0))
        tracker.record(fix(0))

        assertFalse(tracker.finish())

        assertTrue(dao.routes.value.isEmpty())
        assertTrue(dao.points.value.isEmpty())
    }
}
