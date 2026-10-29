package com.fungast.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Uses the platform [LocationManager] (no Google Play Services), so it works offline
 * and on devices without Google services.
 */
class AndroidLocationProvider(private val activity: ComponentActivity) : LocationProvider {

    private var pendingPermission: CompletableDeferred<Boolean>? = null

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        pendingPermission?.complete(grants.values.any { it })
        pendingPermission = null
    }

    override suspend fun requestPermission(): Boolean {
        if (hasAnyLocationPermission()) return true

        val result = CompletableDeferred<Boolean>().also { pendingPermission = it }
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        return result.await()
    }

    override suspend fun currentLocation(): GeoPoint? {
        val locationManager = activity.getSystemService(LocationManager::class.java) ?: return null
        val providers = enabledProviders(locationManager)
        if (providers.isEmpty()) return null

        // A cold GPS start in the woods can take a while
        val timeoutPerProvider = LOCATION_TIMEOUT_MS / providers.size
        for (provider in providers) {
            withTimeoutOrNull(timeoutPerProvider) { requestSingleFix(locationManager, provider) }
                ?.let { return it.toGeoPoint() }
        }
        return null
    }

    override fun locationUpdates(): Flow<GeoPoint> = activity.locationUpdates(UPDATE_INTERVAL_MS)

    private fun enabledProviders(locationManager: LocationManager) = activity.enabledProviders(locationManager)

    private fun hasAnyLocationPermission() = activity.hasAnyLocationPermission()

    @Suppress("MissingPermission") // Checked in currentLocation()
    private suspend fun requestSingleFix(locationManager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(activity),
            ) { location -> continuation.resume(location) }
        }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 60_000L
        const val UPDATE_INTERVAL_MS = 1_000L
    }
}

/**
 * Emits position fixes about every [intervalMs] while collected; stops the GPS when collection stops.
 * Takes any [Context], so a service can use it with the screen off. Completes at once without permission.
 */
@Suppress("MissingPermission") // Checked before registering
fun Context.locationUpdates(intervalMs: Long): Flow<GeoPoint> = callbackFlow {
    val locationManager = getSystemService(LocationManager::class.java)
    val provider = locationManager?.let { enabledProviders(it).firstOrNull() }
    if (provider == null || !hasAnyLocationPermission()) {
        close()
        return@callbackFlow
    }
    val listener = LocationListenerCompat { location -> trySend(location.toGeoPoint()) }
    LocationManagerCompat.requestLocationUpdates(
        locationManager,
        provider,
        LocationRequestCompat.Builder(intervalMs)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .build(),
        ContextCompat.getMainExecutor(this@locationUpdates),
        listener,
    )
    awaitClose { LocationManagerCompat.removeUpdates(locationManager, listener) }
}

/**
 * Enabled providers, best first. GPS alone can't get a fix indoors, so the network provider is the fallback.
 * Fused (Android 12+) combines GPS, Wi-Fi and cell, and is fast both indoors and in the woods.
 */
private fun Context.enabledProviders(locationManager: LocationManager): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        locationManager.hasProvider(LocationManager.FUSED_PROVIDER)
    ) add(LocationManager.FUSED_PROVIDER)
    if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) add(LocationManager.GPS_PROVIDER)
    add(LocationManager.NETWORK_PROVIDER)
}.filter { locationManager.isProviderEnabled(it) }

fun Context.hasAnyLocationPermission() =
    hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

private fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Location.toGeoPoint() = GeoPoint(
    latitude = latitude,
    longitude = longitude,
    altitude = if (hasAltitude()) altitude else null,
    accuracyMeters = if (hasAccuracy()) accuracy else null,
)
