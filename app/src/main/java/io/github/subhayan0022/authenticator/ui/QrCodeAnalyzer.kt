package io.github.subhayan0022.authenticator.ui

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

class QrCodeAnalyzer(
    private val onQrCode: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    @Volatile
    private var handled = false

    override fun analyze(image: ImageProxy) {
        try {
            if (handled) return

            val text = decode(image) ?: return
            handled = true
            onQrCode(text)
        } finally {
            image.close()
        }
    }

    private fun decode(image: ImageProxy): String? {
        if (image.format != ImageFormat.YUV_420_888) return null

        val plane = image.planes[0]
        val buffer = plane.buffer
        val luminance = ByteArray(buffer.remaining())
        buffer.get(luminance)

        val source = PlanarYUVLuminanceSource(
            luminance,
            plane.rowStride,
            image.height,
            0,
            0,
            image.width,
            image.height,
            false,
        )

        return read(source) ?: read(source.invert())
    }

    private fun read(source: LuminanceSource): String? =
        try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (e: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
}
