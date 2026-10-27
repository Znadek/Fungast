package com.fungast.app.location

import kotlinx.coroutines.flow.Flow

data class Heading(
    /** Direction the top of the phone points to, in degrees clockwise from true north. */
    val degrees: Double,
    /** True when the magnetometer reports low accuracy (e.g. near metal; a figure-8 motion fixes it). */
    val needsCalibration: Boolean = false,
)

interface HeadingProvider {
    /** False if the device has no compass sensor; [headingUpdates] then emits nothing. */
    val hasCompass: Boolean

    /** Emits the phone's heading while collected; sensors are released when collection stops. */
    fun headingUpdates(): Flow<Heading>
}
