package com.sik.sikcamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("MissingPermission", "ServiceCast")
class Camera2Manager(
    private val context: Context,
    private val width: Int = 1920,
    private val height: Int = 1080,
    private val imageFormat: Int = ImageFormat.JPEG,
    private val maxImages: Int = 2
) : ICameraManager {

    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraId: String? = null
    private var lensFacing: Int = androidx.camera.core.CameraSelector.LENS_FACING_BACK
    private var previewSurface: Surface? = null
    private lateinit var imageReader: ImageReader
    private var captureCallback: ((Bitmap?) -> Unit)? = null
    private lateinit var cameraManager: CameraManager
    private var currentZoom: Float = 1.0f
    private var sensorRect: Rect? = null

    // 使用 HandlerThread 作为 Camera2 的专用线程
    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    override fun initialize(initSuccess: () -> Unit) {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // 启动 Camera2 专用线程
        cameraThread = HandlerThread("Camera2Thread").apply { start() }
        cameraHandler = Handler(cameraThread.looper)

        try {
            // 遍历相机列表，根据镜头方向选择目标摄像头
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == lensFacing) {
                    cameraId = id
                    sensorRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                    break
                }
            }
            if (cameraId == null) {
                Log.e("Camera2Manager", "No camera found for lens facing: $lensFacing")
                return
            }

            // 初始化 ImageReader 用于捕获 JPEG 图片
            imageReader = ImageReader.newInstance(width, height, imageFormat, maxImages)
            val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
                val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                captureCallback?.invoke(bitmap)
            }
            imageReader.setOnImageAvailableListener(imageAvailableListener, cameraHandler)

            // 打开摄像头（确保调用前已获取 CAMERA 权限）
            cameraManager.openCamera(
                cameraId!!,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        initSuccess()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cameraDevice = null
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e("Camera2Manager", "Camera error: $error")
                        camera.close()
                        cameraDevice = null
                    }
                },
                cameraHandler
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setLensFacing(lensFacing: Int): ICameraManager {
        this.lensFacing = lensFacing
        return this
    }

    /**
     * 此方法不支持 Camera2
     */
    override fun bindPreviewProvider(previewProvider: IPreviewProvider, lifecycleOwner: LifecycleOwner) {
        throw UnsupportedOperationException("Camera2管理器不支持此绑定方式")
    }

    /**
     * CameraX 管理器方法，Camera2 管理器不支持
     */
    override fun bindPreviewProvider(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        throw UnsupportedOperationException("CameraX管理器不支持")
    }

    /**
     * 使用 SurfaceView 绑定预览
     */
    override fun bindPreviewProvider(surfaceView: SurfaceView, lifecycleOwner: LifecycleOwner) {
        val holder = surfaceView.holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                previewSurface = holder.surface
                createCameraPreviewSession()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // 可在此处理 Surface 尺寸变化
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                previewSurface = null
                shutdown()
            }
        })
        // 将生命周期变化与摄像头关闭绑定
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                shutdown()
            }
        })
    }

    /**
     * 使用 TextureView 绑定预览
     */
    override fun bindPreviewProvider(textureView: TextureView, lifecycleOwner: LifecycleOwner) {
        if (textureView.isAvailable) {
            previewSurface = Surface(textureView.surfaceTexture)
            createCameraPreviewSession()
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    previewSurface = Surface(surface)
                    createCameraPreviewSession()
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                    // 可在此响应尺寸变化
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    previewSurface = null
                    shutdown()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }
        // 同样绑定生命周期，停止时关闭摄像头
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                shutdown()
            }
        })
    }

    /**
     * 创建 Camera2 预览会话：
     * - 使用 previewSurface 作为预览目标
     * - 同时包含 imageReader.surface 供拍照使用
     */
    private fun createCameraPreviewSession() {
        if (cameraDevice == null || previewSurface == null) {
            Log.e("Camera2Manager", "CameraDevice or PreviewSurface is null")
            return
        }
        try {
            val surfaces = mutableListOf<Surface>().apply {
                previewSurface?.let { add(it) }
                add(imageReader.surface)
            }
            val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(previewSurface!!)
            sensorRect?.let { rect ->
                val cropRegion = calculateZoomCropRegion(currentZoom, rect)
                previewRequestBuilder?.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)
            }
            cameraDevice?.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        try {
                            val previewRequest = previewRequestBuilder?.build()
                            previewRequest?.let {
                                session.setRepeatingRequest(it, null, cameraHandler)
                            }
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("Camera2Manager", "Camera preview configuration failed")
                    }
                },
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun captureImage(callback: (Bitmap?) -> Unit) {
        captureCallback = callback
        try {
            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            if (captureBuilder == null) {
                Log.e("Camera2Manager", "Failed to create capture request")
                callback(null)
                return
            }
            captureBuilder.addTarget(imageReader.surface)
            sensorRect?.let { rect ->
                val cropRegion = calculateZoomCropRegion(currentZoom, rect)
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)
            }
            cameraCaptureSession?.capture(
                captureBuilder.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                    }
                },
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            callback(null)
        }
    }

    override fun addImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        // Camera2 API 无法直接添加 ImageAnalysis，对数据的处理需要开发者自行通过 ImageReader 来实现
        Log.w("Camera2Manager", "ImageAnalyzer not supported in Camera2Manager")
    }

    override fun setZoom(zoomLevel: Float) {
        currentZoom = zoomLevel
        try {
            val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            if (previewRequestBuilder == null || previewSurface == null) {
                Log.e("Camera2Manager", "Failed to create preview request for zoom setting")
                return
            }
            previewRequestBuilder.addTarget(previewSurface!!)
            sensorRect?.let { rect ->
                val cropRegion = calculateZoomCropRegion(currentZoom, rect)
                previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)
            }
            cameraCaptureSession?.setRepeatingRequest(
                previewRequestBuilder.build(),
                null,
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * 根据当前 zoomLevel 与传入 sensorRect 计算裁剪区域
     */
    private fun calculateZoomCropRegion(zoomLevel: Float, sensorRect: Rect): Rect {
        val maxZoom = try {
            cameraManager.getCameraCharacteristics(cameraId!!)
                .get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        } catch (e: Exception) {
            1.0f
        }
        val clampedZoom = zoomLevel.coerceIn(1.0f, maxZoom)
        val centerX = sensorRect.centerX()
        val centerY = sensorRect.centerY()
        val halfWidth = sensorRect.width() / (2 * clampedZoom)
        val halfHeight = sensorRect.height() / (2 * clampedZoom)
        return Rect(
            (centerX - halfWidth).toInt(),
            (centerY - halfHeight).toInt(),
            (centerX + halfWidth).toInt(),
            (centerY + halfHeight).toInt()
        )
    }

    /**
     * 释放相机资源：关闭预览会话、设备、ImageReader，并停止 HandlerThread
     */
    override fun shutdown() {
        try {
            cameraCaptureSession?.close()
            cameraDevice?.close()
            imageReader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cameraCaptureSession = null
            cameraDevice = null
            // 停止并清理 HandlerThread
            if (::cameraThread.isInitialized) {
                cameraThread.quitSafely()
            }
        }
    }
}
