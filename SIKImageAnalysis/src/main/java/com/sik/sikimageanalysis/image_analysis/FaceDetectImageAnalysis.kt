package com.sik.sikimageanalysis.image_analysis

import android.media.ImageReader
import android.util.SparseIntArray
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * 人脸检测分析器
 */
class FaceDetectImageAnalysis(private val options: FaceDetectorOptions = createDefaultOptions()) :
    ImageAnalysis.Analyzer {
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
    private var onFaceDetectSuccess: (List<Face>) -> Unit = {}

    /**
     * 检测失败
     */
    private var onFaceDetectFailure: (Exception) -> Unit = {}


    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0)
        ORIENTATIONS.append(Surface.ROTATION_90, 90)
        ORIENTATIONS.append(Surface.ROTATION_180, 180)
        ORIENTATIONS.append(Surface.ROTATION_270, 270)
        this.defaultOptions = options
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.use {
            val imageReader =
                ImageReader.newInstance(imageProxy.width, imageProxy.height, imageProxy.format, 1)
            val mediaImage = imageReader.acquireLatestImage()
            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                faceDetector.process(image).addOnSuccessListener { faces ->
                    onFaceDetectSuccess(faces)
                }.addOnFailureListener { exception ->
                    onFaceDetectFailure(exception)
                }
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
    }
}