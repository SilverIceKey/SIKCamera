package com.sik.sikcamera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(private val context: Context) {

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private val imageAnalyzers = mutableListOf<ImageAnalysis.Analyzer>()

    fun initialize(initSuccess: () -> Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            initSuccess()
        }, ContextCompat.getMainExecutor(context))
    }

    fun bindCameraView(cameraView: CameraView, lifecycleOwner: LifecycleOwner) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
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