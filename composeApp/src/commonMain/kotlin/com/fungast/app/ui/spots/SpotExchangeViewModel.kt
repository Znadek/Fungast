package com.fungast.app.ui.spots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fungast.app.data.Spot
import com.fungast.app.data.SpotDao
import com.fungast.app.exchange.DecodedSpot
import com.fungast.app.exchange.ExportFormat
import com.fungast.app.exchange.SharedFile
import com.fungast.app.exchange.SpotFileException
import com.fungast.app.exchange.decodeSpotsWithPhotos
import com.fungast.app.exchange.encodeSpots
import com.fungast.app.photos.PhotoStore
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed interface ExchangeMessage {
    data object NothingToExport : ExchangeMessage
    data class Imported(val added: Int, val alreadyPresent: Int) : ExchangeMessage
    data object ImportFailed : ExchangeMessage
    data class QrImported(val spotName: String) : ExchangeMessage
    data class QrAlreadySaved(val spotName: String) : ExchangeMessage
    data object QrInvalid : ExchangeMessage
}

/**
 * Builds export files and imports spots. Platform file access (share sheet, picker) stays in the UI,
 * because it is tied to the Activity, which this ViewModel outlives.
 */
class SpotExchangeViewModel(
    private val spotDao: SpotDao,
    private val photoStore: PhotoStore,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _message = MutableStateFlow<ExchangeMessage?>(null)
    val message: StateFlow<ExchangeMessage?> = _message.asStateFlow()

    /**
     * Returns the file to share, or null (with a message) if there is nothing to export.
     * [includePhotos] embeds the photos in a .fungast backup, making it much larger.
     */
    suspend fun buildExport(format: ExportFormat, includePhotos: Boolean = false): SharedFile? {
        val spots = spotDao.getAll()
        if (spots.isEmpty()) {
            _message.value = ExchangeMessage.NothingToExport
            return null
        }
        val now = clock.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val photos = if (includePhotos && format == ExportFormat.FUNGAST) {
            spots.flatMap { it.photos }.mapNotNull { name -> photoStore.read(name)?.let { name to it } }.toMap()
        } else {
            emptyMap()
        }
        return SharedFile(
            name = "fungast-$date${if (photos.isNotEmpty()) "-con-foto" else ""}.${format.extension}",
            mimeType = format.mimeType,
            content = encodeSpots(spots, format, exportedAt = now, photos = photos),
        )
    }

    /** Adds the spots in [fileText], skipping those already saved (e.g. when importing the same backup twice). */
    fun import(fileText: String) {
        viewModelScope.launch {
            val imported = decodeOrNull(fileText) ?: run {
                _message.value = ExchangeMessage.ImportFailed
                return@launch
            }
            val added = addNew(imported)
            _message.value = ExchangeMessage.Imported(added = added, alreadyPresent = imported.size - added)
        }
    }

    /** Adds the spot in a code made by [com.fungast.app.exchange.encodeSpotQr]. */
    fun importQr(qrText: String) {
        viewModelScope.launch {
            val decoded = decodeOrNull(qrText)?.singleOrNull() ?: run {
                _message.value = ExchangeMessage.QrInvalid
                return@launch
            }
            val spot = decoded.spot
            _message.value = if (addNew(listOf(decoded)) == 1) {
                ExchangeMessage.QrImported(spot.name)
            } else {
                ExchangeMessage.QrAlreadySaved(spot.name)
            }
        }
    }

    private fun decodeOrNull(text: String): List<DecodedSpot>? = try {
        decodeSpotsWithPhotos(text, now = clock.now())
    } catch (e: SpotFileException) {
        null
    }

    /** Inserts the spots not saved yet and returns how many they were. */
    private suspend fun addNew(spots: List<DecodedSpot>): Int {
        val knownKeys = spotDao.getAll().mapTo(mutableSetOf()) { it.duplicateKey() }
        // add() also drops duplicates inside the list itself
        val newSpots = spots.filter { knownKeys.add(it.spot.duplicateKey()) }
            .map { decoded -> decoded.spot.copy(photos = decoded.photos.mapNotNull { photoStore.save(it) }) }
        if (newSpots.isNotEmpty()) spotDao.insertAll(newSpots)
        return newSpots.size
    }

    fun messageShown() {
        _message.value = null
    }
}

/** Same name at the same place (to ~1 m) counts as the same spot, whatever its notes or date. */
private fun Spot.duplicateKey() = Triple(
    name.trim().lowercase(),
    (latitude * 100_000).roundToLong(),
    (longitude * 100_000).roundToLong(),
)
