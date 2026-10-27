package com.fungast.app.location

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.view.Surface
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Compass from the rotation-vector sensor (accelerometer + magnetometer + gyroscope, fused by Android),
 * corrected from magnetic to true north so it matches the GPS bearing.
 */
class AndroidHeadingProvider(private val activity: Activity) : HeadingProvider {

    private val sensorManager = activity.getSystemService(SensorManager::class.java)
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    override val hasCompass: Boolean get() = rotationSensor != null

    override fun headingUpdates(): Flow<Heading> = callbackFlow {
        if (sensorManager == null || rotationSensor == null) {
            close()
            return@callbackFlow
        }
        val declination = magneticDeclination()
        val rotationMatrix = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        // Low-pass filter on the unit vector, which (unlike the angle) has no jump at 0°/360°
        var smoothedSin = Double.NaN
        var smoothedCos = Double.NaN
        var lastSent: Heading? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val (axisX, axisY) = screenAxes()
                SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remapped)
                SensorManager.getOrientation(remapped, orientation)
                val azimuth = orientation[0].toDouble()

                if (smoothedSin.isNaN()) {
                    smoothedSin = sin(azimuth)
                    smoothedCos = cos(azimuth)
                } else {
                    smoothedSin += SMOOTHING * (sin(azimuth) - smoothedSin)
                    smoothedCos += SMOOTHING * (cos(azimuth) - smoothedCos)
                }
                val heading = Heading(
                    degrees = normalizeDegrees(Math.toDegrees(atan2(smoothedSin, smoothedCos)) + declination),
                    needsCalibration = event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                        event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW,
                )
                // The sensor fires dozens of times per second; skip changes the eye can't see
                val previous = lastSent
                if (previous == null || previous.needsCalibration != heading.needsCalibration ||
                    abs(normalizeDegrees(heading.degrees - previous.degrees + 180) - 180) >= MIN_CHANGE_DEGREES
                ) {
                    lastSent = heading
                    trySend(heading)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /** Device axes to use as X and Y so that azimuth follows the top of the screen in any rotation. */
    @Suppress("DEPRECATION") // Activity.display needs API 30; minSdk is 26
    private fun screenAxes(): Pair<Int, Int> = when (activity.windowManager.defaultDisplay.rotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }

    /**
     * Angle between magnetic and true north at the last known position (about +3° in Italy).
     * It changes very slowly across space, so a stale or coarse position is good enough.
     */
    @Suppress("MissingPermission") // Checked below
    private fun magneticDeclination(): Float {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }
        if (!granted) return 0f
        val locationManager = activity.getSystemService(LocationManager::class.java) ?: return 0f
        val location = locationManager.getProviders(true)
            .mapNotNull { locationManager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?: return 0f
        return GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis(),
        ).declination
    }

    private companion object {
        const val SMOOTHING = 0.15
        const val MIN_CHANGE_DEGREES = 1.0
    }
}
