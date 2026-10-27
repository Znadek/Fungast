package com.fungast.app.exchange

import com.fungast.app.data.Spot

/** Opens the camera to read a QR code. */
interface QrScanner {
    /** Returns the text in the code, or null if the user cancelled or the camera is unavailable. */
    suspend fun scan(): String?
}

/** Square grid of a QR code, without the quiet zone around it. */
class QrCode(val size: Int, private val dark: BooleanArray) {
    init {
        require(dark.size == size * size)
    }

    fun isDark(x: Int, y: Int): Boolean = dark[y * size + x]
}

/** Null if the platform cannot draw QR codes yet. */
expect fun encodeQrCode(text: String): QrCode?

/** Longer notes make the code too dense to read from a phone screen; the full text travels in the .fungast file. */
internal const val QR_MAX_NOTES_LENGTH = 300

/**
 * A single spot as a one-line .fungast file, so a scanned code goes through the same import as a file,
 * duplicate check included.
 */
fun encodeSpotQr(spot: Spot): String {
    val notes = if (spot.notes.length > QR_MAX_NOTES_LENGTH) {
        spot.notes.take(QR_MAX_NOTES_LENGTH - 1).trimEnd() + "…"
    } else {
        spot.notes
    }
    return encodeFungast(listOf(spot.copy(notes = notes)), exportedAt = null, compact = true)
}

/** True for codes made by [encodeSpotQr]; other codes (e.g. a Google Maps link) are not spots. */
fun isSpotQr(text: String): Boolean = text.trimStart().startsWith("{")
