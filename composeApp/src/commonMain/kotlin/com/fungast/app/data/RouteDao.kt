package com.fungast.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinished(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE endedAt IS NULL LIMIT 1")
    fun observeOpen(): Flow<Route?>

    @Query("SELECT * FROM routes WHERE endedAt IS NULL LIMIT 1")
    suspend fun getOpen(): Route?

    /** Every point of every route, in recording order; enough for the map (thousands of points, not millions). */
    @Query("SELECT * FROM route_points ORDER BY routeId, time, id")
    fun observeAllPoints(): Flow<List<RoutePoint>>

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY time DESC, id DESC LIMIT 1")
    suspend fun lastPoint(routeId: Long): RoutePoint?

    @Query("SELECT COUNT(*) FROM route_points WHERE routeId = :routeId")
    suspend fun pointCount(routeId: Long): Int

    @Insert
    suspend fun insert(route: Route): Long

    @Insert
    suspend fun insertPoint(point: RoutePoint)

    // Single-column updates: the recorder and the UI write the same route at the same time
    @Query("UPDATE routes SET distanceMeters = distanceMeters + :meters WHERE id = :id")
    suspend fun addDistance(id: Long, meters: Double)

    @Query("UPDATE routes SET endedAt = :endedAt WHERE id = :id")
    suspend fun setEnded(id: Long, endedAt: Long)

    @Query("UPDATE routes SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Delete
    suspend fun delete(route: Route)
}
