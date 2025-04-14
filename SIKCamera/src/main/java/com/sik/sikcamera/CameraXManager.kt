package com.sik.sikcamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sik.sikimageanalysis.image_analysis.FaceDetectImageAnalysis
import java.nio.ByteBuffer
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 摄像头管理器（基于 CameraX）
 *
 * 修改说明：
 * 1. 统一预览绑定方法：增加 bindPreviewProvider(IPreviewProvider, LifecycleOwner)
 *    根据 IPreviewProvider 的不同实现可适配 CameraX（返回 Preview.SurfaceProvider）
 *    或 Camera2（返回 PreviewView，通过其 surfaceProvider 获取）场景。
 * 2. 删除原 bindCameraView()/bindPreviewView() 方法，使接口更通用。
 */
class CameraXManager(private val context: Context) : ICameraManager {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private val imageAnalyzers = LinkedList<ImageAnalysis.Analyzer>()
    private var isContainerFaceDetector: Boolean = false
    private lateinit var camera: Camera

    @LensFacing
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    override fun initialize(initSuccess: () -> Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            initSuccess()
        }, ContextCompat.getMainExecutor(context))
    }

    override fun setLensFacing(@LensFacing lensFacing: Int): ICameraManager {
        this.lensFacing = lensFacing
        return this
    }

    /**
     * 绑定预览视图（统一接口），支持传入适配 CameraX 或 Camera2 预览模式的 IPreviewProvider。
     *
     * 当 previewProvider.asSurfaceProvider() 返回非 null 时，采用 CameraX 的预览；
     * 否则尝试从 previewProvider.asPreviewView() 中获得其 surfaceProvider 使用。
     */
    override fun bindPreviewProvider(
        previewProvider: IPreviewProvider,
        lifecycleOwner: LifecycleOwner
    ) {
        // 获取预览所需的 SurfaceProvider
        val surfaceProvider =
            previewProvider.asSurfaceProvider() ?: previewProvider.asPreviewView()?.surfaceProvider
        if (surfaceProvider == null) {
            Log.e("CameraXManager", "No valid surface provider provided!")
            return
        }

        // 配置相机选择器，如指定镜头方向（可根据设备是否存在该镜头自动调整）
        var cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        try {
            cameraProvider.hasCamera(cameraSelector)
        } catch (e: CameraInfoUnavailableException) {
            // 如果当前镜头不可用，则切换到另一镜头
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                        CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                )
                .build()
        }

        // 构建预览用例，并设置 SurfaceProvider
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(surfaceProvider)
        }

        // 构建图像分析用例，并添加分析器（采用KEEP_ONLY_LATEST策略）
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            imageAnalyzers.forEach { analyzer ->
                if (analyzer is FaceDetectImageAnalysis) {
                    isContainerFaceDetector = true
                }
                analyzer.analyze(imageProxy)
            }
            if (!isContainerFaceDetector) {
                imageProxy.close()
            }
        }

        // 绑定预览和图像分析用例到生命周期，并保存 Camera 实例
        try {
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Log.e("CameraXManager", "Failed to bind camera use cases", exc)
        }
    }

    override fun bindPreviewProvider(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        // 获取预览所需的 SurfaceProvider
        val surfaceProvider = previewView.surfaceProvider
        // 配置相机选择器，如指定镜头方向（可根据设备是否存在该镜头自动调整）
        var cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        try {
            cameraProvider.hasCamera(cameraSelector)
        } catch (e: CameraInfoUnavailableException) {
            // 如果当前镜头不可用，则切换到另一镜头
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                        CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                )
                .build()
        }

        // 构建预览用例，并设置 SurfaceProvider
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(surfaceProvider)
        }

        // 构建图像分析用例，并添加分析器（采用KEEP_ONLY_LATEST策略）
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            imageAnalyzers.forEach { analyzer ->
                if (analyzer is FaceDetectImageAnalysis) {
                    isContainerFaceDetector = true
                }
                analyzer.analyze(imageProxy)
            }
            if (!isContainerFaceDetector) {
                imageProxy.close()
            }
        }

        // 绑定预览和图像分析用例到生命周期，并保存 Camera 实例
        try {
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Log.e("CameraXManager", "Failed to bind camera use cases", exc)
        }
    }

    override fun bindPreviewProvider(surfaceView: SurfaceView, lifecycleOwner: LifecycleOwner) {
        throw UnsupportedOperationException("CameraX管理器不支持")
    }

    override fun bindPreviewProvider(textureView: TextureView, lifecycleOwner: LifecycleOwner) {
        throw UnsupportedOperationException("CameraX管理器不支持")
    }

    override fun captureImage(callback: (Bitmap?) -> Unit) {
        // 构建拍照用例
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        try {
            // 注意：拍照前先解绑所有用例，再重新绑定 ImageCapture 用例
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, imageCapture)

            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        val bitmap = imageProxyToBitmap(imageProxy)
                        imageProxy.close()
                        callback(bitmap)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraXManager", "Capture failed", exception)
                        callback(null)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("CameraXManager", "Capture exception", e)
            callback(null)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image: Image = imageProxy.image ?: return null
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun addImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalyzers.add(analyzer)
    }

    override fun setZoom(zoomLevel: Float) {
        camera.cameraControl.setZoomRatio(zoomLevel)
    }

    fun removeImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalyzers.remove(analyzer)
    }

    override fun shutdown() {
        imageAnalyzers.clear()
        cameraExecutor.shutdown()
    }
}
