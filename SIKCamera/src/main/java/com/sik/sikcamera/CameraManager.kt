package com.sik.sikcamera

import android.content.Context
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sik.sikimageanalysis.image_analysis.FaceDetectImageAnalysis
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 摄像头管理器
 */
class CameraManager(private val context: Context) {

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private val imageAnalyzers = LinkedList<ImageAnalysis.Analyzer>()
    private var isContainerFaceDetector: Boolean = false

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
        val imageAnalysis =
            ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
        imageAnalysis.setAnalyzer(cameraExecutor) {
            imageAnalyzers.forEach { analyzer ->
                if (analyzer is FaceDetectImageAnalysis){
                    isContainerFaceDetector =  true
                }
                analyzer.analyze(it)
            }
            if (!isContainerFaceDetector){
                it.close()
            }
        }

        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
    }

    fun bindCameraView(cameraView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(cameraView.context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            var cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                // 检查设备上是否存在指定的镜头
                cameraProvider.hasCamera(cameraSelector)
            } catch (e: CameraInfoUnavailableException) {
                // 如果指定的镜头不存在，则切换到另一镜头
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(
                        if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                            CameraSelector.LENS_FACING_BACK
                        else
                            CameraSelector.LENS_FACING_FRONT
                    )
                    .build()
            }
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(cameraView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor) {
                imageAnalyzers.forEach { analyzer ->
                    if (analyzer is FaceDetectImageAnalysis){
                        isContainerFaceDetector =  true
                    }
                    analyzer.analyze(it)
                }
                if (!isContainerFaceDetector){
                    it.close()
                }
            }

            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                // 捕获绑定生命周期失败的异常
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(cameraView.context))
    }

    fun addImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalyzers.add(analyzer)
    }

    fun removeImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalyzers.remove(analyzer)
    }

    fun shutdown() {
        imageAnalyzers.clear()
        cameraExecutor.shutdown()
    }
}

fun interface CameraView {
    fun createSurfaceProvider(): Preview.SurfaceProvider
}