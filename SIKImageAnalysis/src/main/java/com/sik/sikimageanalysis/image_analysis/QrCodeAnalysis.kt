package com.sik.sikimageanalysis.image_analysis

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.sik.sikimage.QRCodeUtils

/**
 * 二维码分析器
 */
class QrCodeAnalysis(onQrCodeDetectedCallback: (String) -> Unit = {}) : ImageAnalysis.Analyzer {
    private var onQrCodeDetectedCallback: (String) -> Unit = {}

    init {
        this.onQrCodeDetectedCallback = onQrCodeDetectedCallback
    }

    fun setOnQrCodeDetectedCallback(onQrCodeDetectedCallback: (String) -> Unit) {
        this.onQrCodeDetectedCallback = onQrCodeDetectedCallback
    }

    override fun analyze(image: ImageProxy) {
        onQrCodeDetectedCallback(QRCodeUtils.readQRCode(image.toBitmap()))
    }
}