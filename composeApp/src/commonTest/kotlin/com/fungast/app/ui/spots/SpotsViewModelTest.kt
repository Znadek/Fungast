package com.fungast.app.ui.spots

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import com.fungast.app.location.GeoPoint
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SpotsViewModelTest {
    private val dao = FakeSpotDao()
    private val now = Instant.fromEpochMilliseconds(1_790_000_000_000)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun addSpotStoresTrimmedFieldsLocationAndTimestamp() {
        val viewModel = SpotsViewModel(dao, FixedClock(now))

        viewModel.addSpot(
            name = "  Faggeta  ",
            category = SpotCategory.MUSHROOMS,
            notes = " porcini dopo la pioggia\n",
            location = GeoPoint(46.1, 11.2, altitude = 1200.0, accuracyMeters = 5f),
        )

        val saved = dao.spots.value.single()
        assertEquals("Faggeta", saved.name)
        assertEquals("porcini dopo la pioggia", saved.notes)
        assertEquals(SpotCategory.MUSHROOMS, saved.category)
        assertEquals(46.1, saved.latitude)
        assertEquals(11.2, saved.longitude)
        assertEquals(1200.0, saved.altitude)
        assertEquals(5f, saved.accuracyMeters)
        assertEquals(now.toEpochMilliseconds(), saved.createdAt)
    }

    @Test
    fun deleteSpotRemovesIt() {
        val viewModel = SpotsViewModel(dao, FixedClock(now))
        viewModel.addSpot("Castagneto", SpotCategory.CHESTNUTS, "", GeoPoint(45.0, 9.0))

        viewModel.deleteSpot(dao.spots.value.single())

        assertTrue(dao.spots.value.isEmpty())
    }
}
