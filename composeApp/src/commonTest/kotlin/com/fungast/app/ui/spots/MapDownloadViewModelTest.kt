package com.fungast.app.ui.spots

import com.fungast.app.map.GeoBounds
import com.fungast.app.map.MIN_DOWNLOAD_ZOOM
import com.fungast.app.map.MapViewport
import com.fungast.app.map.OfflineMaps
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

private class FakeOfflineMaps : OfflineMaps {
    var requestedBounds: GeoBounds? = null
    var progress: ((Float) -> Unit)? = null
    val result = CompletableDeferred<Boolean>()

    override suspend fun downloadRegion(name: String, bounds: GeoBounds, onProgress: (Float) -> Unit): Boolean {
        requestedBounds = bounds
        progress = onProgress
        return result.await()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MapDownloadViewModelTest {
    private val offlineMaps = FakeOfflineMaps()
    private val bounds = GeoBounds(south = 46.0, west = 11.0, north = 46.1, east = 11.1)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun refusesToDownloadWhenZoomedOutOrMapNotReady() {
        val viewModel = MapDownloadViewModel(offlineMaps)

        viewModel.download(MapViewport(bounds, zoom = MIN_DOWNLOAD_ZOOM - 1))
        assertEquals(MapDownloadState.ZoomTooLow, viewModel.state.value)

        viewModel.messageShown()
        viewModel.download(null)
        assertEquals(MapDownloadState.ZoomTooLow, viewModel.state.value)
        assertNull(offlineMaps.requestedBounds)
    }

    @Test
    fun reportsProgressThenCompletion() {
        val viewModel = MapDownloadViewModel(offlineMaps)

        viewModel.download(MapViewport(bounds, zoom = 13.0))
        assertEquals(bounds, offlineMaps.requestedBounds)
        assertEquals(MapDownloadState.InProgress(0f), viewModel.state.value)

        offlineMaps.progress!!(0.5f)
        assertEquals(MapDownloadState.InProgress(0.5f), viewModel.state.value)

        offlineMaps.result.complete(true)
        assertEquals(MapDownloadState.Done, viewModel.state.value)

        viewModel.messageShown()
        assertEquals(MapDownloadState.Idle, viewModel.state.value)
    }

    @Test
    fun reportsFailure() {
        val viewModel = MapDownloadViewModel(offlineMaps)

        viewModel.download(MapViewport(bounds, zoom = 13.0))
        offlineMaps.result.complete(false)

        assertEquals(MapDownloadState.Failed, viewModel.state.value)
    }

    @Test
    fun ignoresNewRequestsWhileDownloading() {
        val viewModel = MapDownloadViewModel(offlineMaps)
        viewModel.download(MapViewport(bounds, zoom = 13.0))

        viewModel.download(MapViewport(bounds, zoom = 5.0))

        assertEquals(MapDownloadState.InProgress(0f), viewModel.state.value)
    }
}
