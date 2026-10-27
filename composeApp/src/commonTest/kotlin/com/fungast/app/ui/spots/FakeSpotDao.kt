package com.fungast.app.ui.spots

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal class FakeSpotDao : SpotDao {
    val spots = MutableStateFlow<List<Spot>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<Spot>> = spots

    override suspend fun getAll(): List<Spot> = spots.value

    override suspend fun insertAll(spots: List<Spot>) {
        spots.forEach { insert(it) }
    }

    override suspend fun insert(spot: Spot): Long {
        val id = nextId++
        spots.value = listOf(spot.copy(id = id)) + spots.value
        return id
    }

    override suspend fun delete(spot: Spot) {
        spots.value = spots.value.filterNot { it.id == spot.id }
    }
}

internal class FixedClock(private val instant: Instant) : Clock {
    override fun now() = instant
}
