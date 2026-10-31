package com.fungast.app.ui.spots

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import com.fungast.app.data.SpotQuantity
import com.fungast.app.location.GeoPoint
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SpotsViewModelTest {
    private val dao = FakeSpotDao()
    private val photoStore = FakePhotoStore()
    private val now = Instant.fromEpochMilliseconds(1_790_000_000_000)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun addSpotStoresTrimmedFieldsLocationAndTimestamp() {
        val viewModel = SpotsViewModel(dao, photoStore, FixedClock(now))

        viewModel.addSpot(
            name = "  Faggeta  ",
            categories = setOf(SpotCategory.MUSHROOMS, SpotCategory.CHESTNUTS),
            notes = " porcini dopo la pioggia\n",
            location = GeoPoint(46.1, 11.2, altitude = 1200.0, accuracyMeters = 5f),
        )

        val saved = dao.spots.value.single()
        assertEquals("Faggeta", saved.name)
        assertEquals("porcini dopo la pioggia", saved.notes)
        assertEquals(setOf(SpotCategory.MUSHROOMS, SpotCategory.CHESTNUTS), saved.categories)
        assertEquals(46.1, saved.latitude)
        assertEquals(11.2, saved.longitude)
        assertEquals(1200.0, saved.altitude)
        assertEquals(5f, saved.accuracyMeters)
        assertEquals(now.toEpochMilliseconds(), saved.createdAt)
        assertEquals(null, saved.lastFoundOn)
        assertEquals(null, saved.previousFoundOn)
    }

    @Test
    fun findDatesAreStoredMostRecentFirst() {
        val viewModel = SpotsViewModel(dao, photoStore, FixedClock(now))

        viewModel.addSpot(
            "Faggeta",
            setOf(SpotCategory.MUSHROOMS),
            "",
            GeoPoint(46.1, 11.2),
            findDates = listOf(LocalDate(2025, 9, 30), LocalDate(2026, 10, 20)),
        )

        val saved = dao.spots.value.single()
        assertEquals(LocalDate(2026, 10, 20), saved.lastFoundOn)
        assertEquals(LocalDate(2025, 9, 30), saved.previousFoundOn)
    }

    @Test
    fun singleFindDateIsTheLastOne() {
        val viewModel = SpotsViewModel(dao, photoStore, FixedClock(now))

        viewModel.addSpot("Faggeta", setOf(SpotCategory.MUSHROOMS), "", GeoPoint(46.1, 11.2), listOf(LocalDate(2026, 10, 20)))

        val saved = dao.spots.value.single()
        assertEquals(LocalDate(2026, 10, 20), saved.lastFoundOn)
        assertEquals(null, saved.previousFoundOn)
    }

    @Test
    fun updateSpotChangesFieldsButKeepsIdAndCreationTime() {
        val viewModel = SpotsViewModel(dao, photoStore, FixedClock(now))
        viewModel.addSpot("Faggeta", setOf(SpotCategory.MUSHROOMS), "", GeoPoint(46.1, 11.2))
        val original = dao.spots.value.single()

        viewModel.updateSpot(
            original.id,
            name = " Castagneto ",
            categories = setOf(SpotCategory.CHESTNUTS),
            notes = "vicino al ponte ",
            location = GeoPoint(45.0, 9.0, altitude = 600.0),
            findDates = listOf(LocalDate(2026, 10, 20)),
            photos = emptyList(),
            quantity = SpotQuantity.GOOD,
        )

        val saved = dao.spots.value.single()
        assertEquals(original.id, saved.id)
        assertEquals(original.createdAt, saved.createdAt)
        assertEquals("Castagneto", saved.name)
        assertEquals("vicino al ponte", saved.notes)
        assertEquals(setOf(SpotCategory.CHESTNUTS), saved.categories)
        assertEquals(45.0, saved.latitude)
        assertEquals(600.0, saved.altitude)
        assertEquals(LocalDate(2026, 10, 20), saved.lastFoundOn)
        assertEquals(SpotQuantity.GOOD, saved.quantity)
    }

    @Test
    fun updateSpotDeletesOnlyThePhotosRemovedFromIt() = runTest {
        val viewModel = SpotsViewModel(dao, photoStore, FixedClock(now))
        val kept = photoStore.save(byteArrayOf(1))
        val removed = photoStore.save(byteArrayOf(2))
        val added = photoStore.save(byteArrayOf(3))
        viewModel.addSpot("Faggeta", setOf(SpotCategory.MUSHROOMS), "", GeoPoint(46.1, 11.2), photos = listOf(kept, removed))
        val spot = dao.spots.value.single()

        viewModel.updateSpot(spot.id, spot.name, spot.categories, spot.notes, GeoPoint(46.1, 11.2), emptyList(), listOf(kept, added), null)

        assertEquals(listOf(kept, added), dao.spots.value.single().photos)
        assertEquals(setOf(kept, added), photoStore.files.keys)
    }

    @Test
    fun deleteSpotRemovesIt() {
        val viewModel = SpotsViewModel(dao, photoStore, FixedClock(now))
        viewModel.addSpot("Castagneto", setOf(SpotCategory.CHESTNUTS), "", GeoPoint(45.0, 9.0))

        viewModel.deleteSpot(dao.spots.value.single())

        assertTrue(dao.spots.value.isEmpty())
    }

    @Test
    fun photosCanBeAddedAndRemovedOnASavedSpot() = runTest {
        val viewModel = SpotsViewModel(dao, photoStore, FixedClock(now))
        val first = photoStore.save(byteArrayOf(1))
        val second = photoStore.save(byteArrayOf(2))
        viewModel.addSpot("Faggeta", setOf(SpotCategory.MUSHROOMS), "", GeoPoint(46.1, 11.2), photos = listOf(first))
        val id = dao.spots.value.single().id

        viewModel.addPhotos(id, listOf(second))
        assertEquals(listOf(first, second), dao.spots.value.single().photos)

        viewModel.removePhoto(id, first)
        assertEquals(listOf(second), dao.spots.value.single().photos)
        assertEquals(setOf(second), photoStore.files.keys)
    }

    @Test
    fun deletingASpotDeletesItsPhotos() = runTest {
        val viewModel = SpotsViewModel(dao, photoStore, FixedClock(now))
        val photo = photoStore.save(byteArrayOf(1))
        viewModel.addSpot("Faggeta", setOf(SpotCategory.MUSHROOMS), "", GeoPoint(46.1, 11.2), photos = listOf(photo))

        viewModel.deleteSpot(dao.spots.value.single())

        assertTrue(photoStore.files.isEmpty())
    }

    @Test
    fun discardedPhotosAreDeleted() = runTest {
        val viewModel = SpotsViewModel(dao, photoStore, FixedClock(now))
        val photo = photoStore.save(byteArrayOf(1))

        viewModel.discardPhotos(listOf(photo))

        assertTrue(photoStore.files.isEmpty())
    }
}
