package com.fungast.app.ui.spots

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import com.fungast.app.exchange.ExportFormat
import com.fungast.app.exchange.encodeSpotQr
import com.fungast.app.exchange.encodeSpots
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SpotExchangeViewModelTest {
    private val dao = FakeSpotDao()
    private val now = Instant.parse("2026-10-27T18:00:00Z")
    private val photoStore = FakePhotoStore()
    private val viewModel = SpotExchangeViewModel(dao, photoStore, FixedClock(now))

    private fun spot(name: String, latitude: Double = 46.0, longitude: Double = 11.0) = Spot(
        name = name,
        categories = setOf(SpotCategory.MUSHROOMS),
        latitude = latitude,
        longitude = longitude,
        altitude = null,
        accuracyMeters = null,
        notes = "",
        createdAt = now.toEpochMilliseconds(),
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun exportWithNoSpotsShowsMessage() = runTest {
        assertNull(viewModel.buildExport(ExportFormat.FUNGAST))
        assertEquals(ExchangeMessage.NothingToExport, viewModel.message.value)
    }

    @Test
    fun exportIsNamedByDateAndFormat() = runTest {
        dao.insert(spot("Faggeta"))

        val file = assertNotNull(viewModel.buildExport(ExportFormat.GPX))

        assertTrue(file.name.startsWith("fungast-2026-10-2") && file.name.endsWith(".gpx"), file.name)
        assertEquals("application/gpx+xml", file.mimeType)
        assertTrue("Faggeta" in file.content)
    }

    @Test
    fun backupWithPhotosMovesThemToAnotherPhone() = runTest {
        val photo = photoStore.save(byteArrayOf(1, 2, 3))
        dao.insert(spot("Faggeta").copy(photos = listOf(photo)))
        val file = assertNotNull(viewModel.buildExport(ExportFormat.FUNGAST, includePhotos = true))
        assertEquals("fungast-2026-10-27-con-foto.fungast", file.name)

        // Another phone: empty database and photo folder
        val otherDao = FakeSpotDao()
        val otherPhotos = FakePhotoStore()
        SpotExchangeViewModel(otherDao, otherPhotos, FixedClock(now)).import(file.content)

        val imported = otherDao.spots.value.single()
        assertEquals(1, imported.photos.size)
        assertContentEquals(byteArrayOf(1, 2, 3), otherPhotos.files[imported.photos.single()])
    }

    @Test
    fun backupWithoutPhotosLeavesThemOut() = runTest {
        val photo = photoStore.save(byteArrayOf(1, 2, 3))
        dao.insert(spot("Faggeta").copy(photos = listOf(photo)))
        val file = assertNotNull(viewModel.buildExport(ExportFormat.FUNGAST))

        val otherDao = FakeSpotDao()
        SpotExchangeViewModel(otherDao, FakePhotoStore(), FixedClock(now)).import(file.content)

        assertEquals(emptyList(), otherDao.spots.value.single().photos)
    }

    @Test
    fun importSkipsSpotsAlreadySavedAndDuplicatesInsideTheFile() {
        dao.spots.value = listOf(spot("Faggeta").copy(id = 1, notes = "note diverse"))
        val file = encodeSpots(
            listOf(
                spot("faggeta ", latitude = 46.000001), // same name and place (~0.1 m away)
                spot("Castagneto", latitude = 46.01),
                spot("Castagneto", latitude = 46.01),
            ),
            ExportFormat.FUNGAST,
            now,
        )

        viewModel.import(file)

        assertEquals(ExchangeMessage.Imported(added = 1, alreadyPresent = 2), viewModel.message.value)
        assertEquals(listOf("Castagneto", "Faggeta"), dao.spots.value.map { it.name })

        viewModel.messageShown()
        assertNull(viewModel.message.value)
    }

    @Test
    fun invalidFileReportsFailure() {
        viewModel.import("non è un file")

        assertEquals(ExchangeMessage.ImportFailed, viewModel.message.value)
        assertTrue(dao.spots.value.isEmpty())
    }

    @Test
    fun scannedSpotIsAddedOnce() {
        val qr = encodeSpotQr(spot("Faggeta"))

        viewModel.importQr(qr)
        assertEquals(ExchangeMessage.QrImported("Faggeta"), viewModel.message.value)

        viewModel.importQr(qr)
        assertEquals(ExchangeMessage.QrAlreadySaved("Faggeta"), viewModel.message.value)
        assertEquals(1, dao.spots.value.size)
    }

    @Test
    fun scannedBackupWithManySpotsIsRejected() {
        // A QR holds a single spot; a multi-spot file is not a valid code
        viewModel.importQr(encodeSpots(listOf(spot("A"), spot("B", latitude = 45.0)), ExportFormat.FUNGAST, now))

        assertEquals(ExchangeMessage.QrInvalid, viewModel.message.value)
        assertTrue(dao.spots.value.isEmpty())
    }

    @Test
    fun messagesAreReadable() {
        assertEquals("1 posto importato.", exchangeMessageText(ExchangeMessage.Imported(1, 0)))
        assertEquals("3 posti importati (2 già presenti).", exchangeMessageText(ExchangeMessage.Imported(3, 2)))
        assertEquals("Tutti i posti del file sono già salvati.", exchangeMessageText(ExchangeMessage.Imported(0, 4)))
        assertEquals("Nessun posto trovato nel file.", exchangeMessageText(ExchangeMessage.Imported(0, 0)))
    }
}
