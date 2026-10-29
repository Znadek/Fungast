package com.fungast.app.routes

import com.fungast.app.data.Route
import com.fungast.app.data.RouteDao
import com.fungast.app.data.RoutePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeRouteDao : RouteDao {
    val routes = MutableStateFlow<List<Route>>(emptyList())
    val points = MutableStateFlow<List<RoutePoint>>(emptyList())
    private var nextId = 1L

    override fun observeFinished(): Flow<List<Route>> =
        routes.map { all -> all.filter { it.endedAt != null }.sortedByDescending { it.startedAt } }

    override fun observeOpen(): Flow<Route?> = routes.map { all -> all.find { it.endedAt == null } }

    override suspend fun getOpen(): Route? = routes.value.find { it.endedAt == null }

    override fun observeAllPoints(): Flow<List<RoutePoint>> = points

    override suspend fun lastPoint(routeId: Long): RoutePoint? = points.value.lastOrNull { it.routeId == routeId }

    override suspend fun pointCount(routeId: Long): Int = points.value.count { it.routeId == routeId }

    override suspend fun insert(route: Route): Long {
        val id = nextId++
        routes.value = routes.value + route.copy(id = id)
        return id
    }

    override suspend fun insertPoint(point: RoutePoint) {
        points.value = points.value + point.copy(id = nextId++)
    }

    override suspend fun addDistance(id: Long, meters: Double) = change(id) { it.copy(distanceMeters = it.distanceMeters + meters) }

    override suspend fun setEnded(id: Long, endedAt: Long) = change(id) { it.copy(endedAt = endedAt) }

    override suspend fun rename(id: Long, name: String) = change(id) { it.copy(name = name) }

    override suspend fun delete(route: Route) {
        routes.value = routes.value.filterNot { it.id == route.id }
        points.value = points.value.filterNot { it.routeId == route.id }
    }

    private fun change(id: Long, transform: (Route) -> Route) {
        routes.value = routes.value.map { if (it.id == id) transform(it) else it }
    }
}
