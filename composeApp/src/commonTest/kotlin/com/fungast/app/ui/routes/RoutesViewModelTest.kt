package com.fungast.app.ui.routes

import com.fungast.app.location.GeoPoint
import com.fungast.app.routes.FakeRouteDao
import com.fungast.app.routes.RouteTracker
import com.fungast.app.ui.spots.FixedClock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RoutesViewModelTest {
    private val dao = FakeRouteDao()
    private val clock = FixedClock(Instant.parse("2026-10-29T08:00:00Z"))
    private val viewModel by lazy { RoutesViewModel(dao, clock) }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startOpensOneRouteNamedAfterTheDay() = runTest {
        viewModel.startRecording()
        viewModel.startRecording()

        val route = dao.routes.value.single()
        assertTrue(route.name.startsWith("Percorso del "))
        assertTrue(viewModel.isRecording())
    }

    @Test
    fun stopSavesTheRouteUnderTheChosenName() = runTest {
        viewModel.startRecording()
        RouteTracker(dao, clock).apply {
            record(GeoPoint(46.0, 11.0, accuracyMeters = 5f))
            record(GeoPoint(46.001, 11.0, accuracyMeters = 5f))
        }

        viewModel.stopRecording("  Giro dei porcini ")

        assertEquals("Giro dei porcini", dao.routes.value.single().name)
        assertEquals(RouteMessage.Saved("Giro dei porcini"), viewModel.message.value)
        assertFalse(viewModel.isRecording())
    }

    @Test
    fun stopWithoutMovingSavesNothing() = runTest {
        viewModel.startRecording()

        viewModel.stopRecording("")

        assertTrue(dao.routes.value.isEmpty())
        assertEquals(RouteMessage.TooShort, viewModel.message.value)
    }
}
