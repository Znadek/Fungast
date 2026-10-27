package com.fungast.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpotDao {
    @Query("SELECT * FROM spots ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Spot>>

    @Query("SELECT * FROM spots ORDER BY createdAt DESC")
    suspend fun getAll(): List<Spot>

    @Insert
    suspend fun insert(spot: Spot): Long

    @Insert
    suspend fun insertAll(spots: List<Spot>)

    @Delete
    suspend fun delete(spot: Spot)
}
