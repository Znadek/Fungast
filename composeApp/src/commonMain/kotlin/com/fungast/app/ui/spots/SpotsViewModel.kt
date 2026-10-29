package com.fungast.app.ui.spots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import com.fungast.app.data.SpotDao
import com.fungast.app.data.lastTwoFinds
import com.fungast.app.location.GeoPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class SpotsViewModel(
    private val spotDao: SpotDao,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    val spots: StateFlow<List<Spot>> = spotDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSpot(
        name: String,
        categories: Set<SpotCategory>,
        notes: String,
        location: GeoPoint,
        findDates: List<LocalDate> = emptyList(),
    ) {
        val (lastFoundOn, previousFoundOn) = lastTwoFinds(findDates)
        val spot = Spot(
            name = name.trim(),
            categories = categories,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracyMeters = location.accuracyMeters,
            notes = notes.trim(),
            createdAt = clock.now().toEpochMilliseconds(),
            lastFoundOn = lastFoundOn,
            previousFoundOn = previousFoundOn,
        )
        viewModelScope.launch { spotDao.insert(spot) }
    }

    fun deleteSpot(spot: Spot) {
        viewModelScope.launch { spotDao.delete(spot) }
    }
}
