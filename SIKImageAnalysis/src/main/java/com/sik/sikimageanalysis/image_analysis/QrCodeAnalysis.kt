package com.sik.sikimageanalysis.image_analysis

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.sik.sikimage.QRCodeUtils
import java.nio.charset.Charset

/**
 * 二维码分析器
 */
class QrCodeAnalysis(
    toHex: Boolean = false,
    charset: Charset = Charsets.ISO_8859_1,
    onQrCodeDetectedCallback: (String) -> Unit = {}
) : ImageAnalysis.Analyzer {
    private var onQrCodeDetectedCallback: (String) -> Unit = {}
    private var charset: Charset = Charsets.ISO_8859_1
    private var toHex: Boolean = false

    init {
        this.onQrCodeDetectedCallback = onQrCodeDetectedCallback
        this.charset = charset
        this.toHex = toHex
    }

    fun setOnQrCodeDetectedCallback(onQrCodeDetectedCallback: (String) -> Unit) {
        this.onQrCodeDetectedCallback = onQrCodeDetectedCallback
    }

    override fun analyze(image: ImageProxy) {
        val resultString = QRCodeUtils.readQRCodeString(image.toBitmap(), charset, toHex)
        onQrCodeDetectedCallback(
            resultString
        )
    }
}