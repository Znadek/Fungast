package com.fungast.app.exchange

import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

actual fun encodeQrCode(text: String): QrCode? {
    val matrix = try {
        // M survives a scratched screen or glare without making the code much denser
        Encoder.encode(text, ErrorCorrectionLevel.M, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")).matrix
    } catch (e: WriterException) {
        return null
    }
    val size = matrix.width
    return QrCode(size, BooleanArray(size * size) { matrix.get(it % size, it / size).toInt() == 1 })
}
