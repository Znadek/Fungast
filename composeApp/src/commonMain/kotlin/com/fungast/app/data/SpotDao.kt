package com.fungast.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpotDao {
    @Query("SELECT * FROM spots ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Spot>>

    @Query("SELECT * FROM spots ORDER BY createdAt DESC")
    suspend fun getAll(): List<Spot>

    @Query("SELECT * FROM spots WHERE id = :id")
    suspend fun getById(id: Long): Spot?

    @Insert
    suspend fun insert(spot: Spot): Long

    @Insert
    suspend fun insertAll(spots: List<Spot>)

    @Update
    suspend fun update(spot: Spot)

    @Delete
    suspend fun delete(spot: Spot)
}
