package com.fungast.app.ui.spots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fungast.app.data.Spot
import com.fungast.app.data.SpotDao
import com.fungast.app.exchange.ExportFormat
import com.fungast.app.exchange.SharedFile
import com.fungast.app.exchange.SpotFileException
import com.fungast.app.exchange.decodeSpots
import com.fungast.app.exchange.encodeSpots
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
}

/**
 * Builds export files and imports spots. Platform file access (share sheet, picker) stays in the UI,
 * because it is tied to the Activity, which this ViewModel outlives.
 */
class SpotExchangeViewModel(
    private val spotDao: SpotDao,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _message = MutableStateFlow<ExchangeMessage?>(null)
    val message: StateFlow<ExchangeMessage?> = _message.asStateFlow()

    /** Returns the file to share, or null (with a message) if there is nothing to export. */
    suspend fun buildExport(format: ExportFormat): SharedFile? {
        val spots = spotDao.getAll()
        if (spots.isEmpty()) {
            _message.value = ExchangeMessage.NothingToExport
            return null
        }
        val now = clock.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return SharedFile(
            name = "fungast-$date.${format.extension}",
            mimeType = format.mimeType,
            content = encodeSpots(spots, format, exportedAt = now),
        )
    }

    /** Adds the spots in [fileText], skipping those already saved (e.g. when importing the same backup twice). */
    fun import(fileText: String) {
        viewModelScope.launch {
            val imported = try {
                decodeSpots(fileText, now = clock.now())
            } catch (e: SpotFileException) {
                _message.value = ExchangeMessage.ImportFailed
                return@launch
            }
            val knownKeys = spotDao.getAll().mapTo(mutableSetOf()) { it.duplicateKey() }
            // add() also drops duplicates inside the file itself
            val newSpots = imported.filter { knownKeys.add(it.duplicateKey()) }
            if (newSpots.isNotEmpty()) spotDao.insertAll(newSpots)
            _message.value = ExchangeMessage.Imported(
                added = newSpots.size,
                alreadyPresent = imported.size - newSpots.size,
            )
        }
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
