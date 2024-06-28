package com.sik.sikimageanalysis.image_analysis

import android.graphics.Rect
import android.util.SparseIntArray
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.sik.sikimageanalysis.views.FaceOverlayView

/**
 * 人脸检测分析器
 */
class FaceDetectImageAnalysis(
    private val options: FaceDetectorOptions = createDefaultOptions(),
    private val drawBoundingBox: Boolean = false,
    private val faceOverlayView: FaceOverlayView? = null
) : ImageAnalysis.Analyzer {
    /**
     * 高精度的地标检测和人脸分类和轮廓检测
     */
    private var defaultOptions: FaceDetectorOptions

    /**
     * 检测角度
     */
    private val ORIENTATIONS = SparseIntArray()

    /**
     * 人脸检测器
     */
    private val faceDetector: FaceDetector by lazy {
        FaceDetection.getClient(options)
    }

    /**
     * 检测成功
     */
    var onFaceDetectSuccess: (List<Face>, ImageProxy) -> Unit = { _, _ -> }

    /**
     * 检测失败
     */
    var onFaceDetectFailure: (Exception, ImageProxy) -> Unit = { _, _ -> }

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0)
        ORIENTATIONS.append(Surface.ROTATION_90, 90)
        ORIENTATIONS.append(Surface.ROTATION_180, 180)
        ORIENTATIONS.append(Surface.ROTATION_270, 270)
        this.defaultOptions = options
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        mediaImage?.let {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image =
                InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
            faceDetector.process(image).addOnSuccessListener { faces ->
                if (drawBoundingBox && faceOverlayView != null) {
                    val previewViewWidth = faceOverlayView.width
                    val previewViewHeight = faceOverlayView.height
                    val facesTransformed = faces.map { face ->
                        translateBoundingBox(face.boundingBox, mediaImage.width, mediaImage.height, previewViewWidth, previewViewHeight, rotationDegrees)
                    }
                    faceOverlayView.updateFaces(facesTransformed)
                }
                onFaceDetectSuccess(faces, imageProxy)
            }.addOnFailureListener { exception ->
                onFaceDetectFailure(exception, imageProxy)
            }
        }
    }

    companion object {
        /**
         * 创建默认检测器
         */
        fun createDefaultOptions(): FaceDetectorOptions {
            return FaceDetectorOptions.Builder()
                .apply {
                    setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                }
                .build()
        }

        /**
         * 变化
         */
        private fun translateBoundingBox(rect: Rect, imageWidth: Int, imageHeight: Int, viewWidth: Int, viewHeight: Int, rotationDegrees: Int): Rect {
            val scaleX = viewWidth / imageWidth.toFloat()
            val scaleY = viewHeight / imageHeight.toFloat()
            val scale = Math.min(scaleX, scaleY)

            val offsetX = (viewWidth - imageWidth * scale) / 2
            val offsetY = (viewHeight - imageHeight * scale) / 2

            val mappedRect = when (rotationDegrees) {
                90 -> Rect(
                    (viewWidth - rect.bottom * scale - offsetX).toInt(),
                    (rect.left * scale + offsetY).toInt(),
                    (viewWidth - rect.top * scale - offsetX).toInt(),
                    (rect.right * scale + offsetY).toInt()
                )
                180 -> Rect(
                    (viewWidth - rect.right * scale - offsetX).toInt(),
                    (viewHeight - rect.bottom * scale - offsetY).toInt(),
                    (viewWidth - rect.left * scale - offsetX).toInt(),
                    (viewHeight - rect.top * scale - offsetY).toInt()
                )
                270 -> Rect(
                    (rect.top * scale + offsetX).toInt(),
                    (viewHeight - rect.right * scale - offsetY).toInt(),
                    (rect.bottom * scale + offsetX).toInt(),
                    (viewHeight - rect.left * scale - offsetY).toInt()
                )
                else -> Rect(
                    (rect.left * scale + offsetX).toInt(),
                    (rect.top * scale + offsetY).toInt(),
                    (rect.right * scale + offsetX).toInt(),
                    (rect.bottom * scale + offsetY).toInt()
                )
            }
            return mappedRect
        }
    }
}
