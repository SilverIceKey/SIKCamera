package com.sik.sikimageanalysis.image_analysis

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.sik.sikimage.BarCodeUtils

/**
 * 条形码分析器
 */
class BarCodeAnalysis(
    barCodeFormat: BarcodeFormat = BarcodeFormat.CODE_128,
    onBarCodeDetectedCallback: (String) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private var onBarCodeDetectedCallback: (String) -> Unit = {}

    init {
        BarCodeUtils.setBarCodeFormat(barCodeFormat)
        this.onBarCodeDetectedCallback = onBarCodeDetectedCallback
    }

    /**
     * 设置检测回调
     */
    fun setOnBarCodeDetectedCallback(callback: (String) -> Unit) {
        this.onBarCodeDetectedCallback = callback
    }

    override fun analyze(image: ImageProxy) {
        onBarCodeDetectedCallback(BarCodeUtils.readBarCode(image.toBitmap()))
    }
}