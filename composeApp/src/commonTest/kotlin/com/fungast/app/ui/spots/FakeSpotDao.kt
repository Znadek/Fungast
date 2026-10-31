package com.fungast.app.ui.spots

import com.fungast.app.data.Spot
import androidx.compose.ui.graphics.ImageBitmap
import com.fungast.app.data.SpotDao
import com.fungast.app.photos.PhotoStore
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

    override suspend fun getById(id: Long): Spot? = spots.value.find { it.id == id }

    override suspend fun update(spot: Spot) {
        spots.value = spots.value.map { if (it.id == spot.id) spot else it }
    }

    override suspend fun delete(spot: Spot) {
        spots.value = spots.value.filterNot { it.id == spot.id }
    }
}

internal class FakePhotoStore : PhotoStore {
    val files = mutableMapOf<String, ByteArray>()
    private var nextId = 1

    override suspend fun save(jpeg: ByteArray): String = "photo${nextId++}.jpg".also { files[it] = jpeg }

    override suspend fun read(name: String): ByteArray? = files[name]

    override suspend fun load(name: String, maxSize: Int): ImageBitmap? = null

    override suspend fun delete(names: Collection<String>) {
        names.forEach { files.remove(it) }
    }
}

internal class FixedClock(private val instant: Instant) : Clock {
    override fun now() = instant
}
