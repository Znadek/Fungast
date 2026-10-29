package com.fungast.app.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fungast.app.data.Route
import com.fungast.app.data.RouteDao
import com.fungast.app.location.GeoPoint
import com.fungast.app.map.RouteTrack
import com.fungast.app.routes.RouteTracker
import com.fungast.app.ui.formatDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed interface RouteMessage {
    data class Saved(val name: String) : RouteMessage
    data object TooShort : RouteMessage
    data object NoLocationPermission : RouteMessage
}

/** Routes in the database. Starting the platform recorder is left to the UI, which owns the Activity it needs. */
class RoutesViewModel(
    private val routeDao: RouteDao,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val tracker = RouteTracker(routeDao, clock)

    val routes: StateFlow<List<Route>> = routeDao.observeFinished()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The route being recorded, or null. */
    val recording: StateFlow<Route?> = routeDao.observeOpen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tracks: StateFlow<List<RouteTrack>> = combine(routeDao.observeAllPoints(), routeDao.observeOpen()) { points, open ->
        points.groupBy { it.routeId }.map { (routeId, routePoints) ->
            RouteTrack(
                routeId = routeId,
                points = routePoints.map { GeoPoint(it.latitude, it.longitude, it.altitude, it.accuracyMeters) },
                recording = routeId == open?.id,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<RouteMessage?>(null)
    val message: StateFlow<RouteMessage?> = _message.asStateFlow()

    /** True if a route was left open, e.g. the phone was restarted while recording: the recorder should resume. */
    suspend fun isRecording(): Boolean = routeDao.getOpen() != null

    fun locationPermissionDenied() {
        _message.value = RouteMessage.NoLocationPermission
    }

    /** Opens a new route for the recorder to fill, unless one is already being recorded. */
    suspend fun startRecording() {
        if (routeDao.getOpen() != null) return
        val now = clock.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        routeDao.insert(Route(name = "Percorso del ${formatDate(date)}", startedAt = now.toEpochMilliseconds()))
    }

    /** Ends the recording, keeping the route under [name] unless it is too short to be useful. */
    fun stopRecording(name: String) {
        viewModelScope.launch {
            val route = routeDao.getOpen() ?: return@launch
            val finalName = name.trim().ifEmpty { route.name }
            routeDao.rename(route.id, finalName)
            _message.value = if (tracker.finish()) RouteMessage.Saved(finalName) else RouteMessage.TooShort
        }
    }

    fun rename(route: Route, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { routeDao.rename(route.id, trimmed) }
    }

    fun delete(route: Route) {
        viewModelScope.launch { routeDao.delete(route) }
    }

    fun messageShown() {
        _message.value = null
    }
}
