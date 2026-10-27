package com.fungast.app.ui.spots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fungast.app.map.MIN_DOWNLOAD_ZOOM
import com.fungast.app.map.MapViewport
import com.fungast.app.map.OfflineMaps
import com.fungast.app.ui.formatDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

sealed interface MapDownloadState {
    data object Idle : MapDownloadState
    data object ZoomTooLow : MapDownloadState
    data class InProgress(val progress: Float) : MapDownloadState
    data object Done : MapDownloadState
    data object Failed : MapDownloadState
}

/** Lives in a ViewModel so a download keeps going when switching between list and map. */
class MapDownloadViewModel(
    private val offlineMaps: OfflineMaps,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow<MapDownloadState>(MapDownloadState.Idle)
    val state: StateFlow<MapDownloadState> = _state.asStateFlow()

    fun download(viewport: MapViewport?) {
        if (_state.value is MapDownloadState.InProgress) return
        if (viewport == null || viewport.zoom < MIN_DOWNLOAD_ZOOM) {
            _state.value = MapDownloadState.ZoomTooLow
            return
        }
        _state.value = MapDownloadState.InProgress(0f)
        viewModelScope.launch {
            val name = "Area del ${formatDateTime(clock.now().toEpochMilliseconds())}"
            val completed = offlineMaps.downloadRegion(name, viewport.bounds) { progress ->
                _state.value = MapDownloadState.InProgress(progress.coerceIn(0f, 1f))
            }
            _state.value = if (completed) MapDownloadState.Done else MapDownloadState.Failed
        }
    }

    /** Called once the outcome (done, failed, zoom too low) has been shown to the user. */
    fun messageShown() {
        if (_state.value !is MapDownloadState.InProgress) _state.value = MapDownloadState.Idle
    }
}
