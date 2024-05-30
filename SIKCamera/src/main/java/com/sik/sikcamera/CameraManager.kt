package com.sik.sikcamera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 摄像头管理器
 */
class CameraManager(private val context: Context) {

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private val imageAnalyzers = mutableListOf<ImageAnalysis.Analyzer>()

    @LensFacing
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    fun initialize(initSuccess: () -> Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            initSuccess()
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 设置摄像头
     */
    fun setLensFacing(@LensFacing lensFacing: Int): CameraManager {
        this.lensFacing = lensFacing
        return this
    }

    fun bindCameraView(cameraView: CameraView, lifecycleOwner: LifecycleOwner) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(cameraView.createSurfaceProvider())
        val imageAnalysis = ImageAnalysis.Builder()
            .build()

        imageAnalyzers.forEach { analyzer ->
            imageAnalysis.setAnalyzer(cameraExecutor, analyzer)
        }

        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
    }

    fun addImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalyzers.add(analyzer)
    }

    fun removeImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalyzers.remove(analyzer)
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}

interface CameraView {
    fun createSurfaceProvider(): Preview.SurfaceProvider
}