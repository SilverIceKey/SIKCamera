package com.sik.sikcamera

import android.graphics.Bitmap
import android.view.SurfaceView
import android.view.TextureView
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner

/**
 * 摄像头管理接口，支持 CameraX 与 Camera2 两种模式
 */
interface ICameraManager {
    /**
     * 初始化摄像头（异步打开摄像头等操作）
     */
    fun initialize(initSuccess: () -> Unit)

    /**
     * 设置使用前置或后置摄像头
     */
    fun setLensFacing(lensFacing: Int): ICameraManager

    /**
     * 绑定预览视图
     *
     * Camerax专用 Camera2使用报错
     * 通过传入 IPreviewProvider 可统一适配 CameraX 和 Camera2 预览需求：
     * - 当 IPreviewProvider.asSurfaceProvider() 返回不为 null 时，使用 CameraX 逻辑；
     * - 当 IPreviewProvider.asPreviewView() 返回不为 null 时，使用 Camera2 逻辑；
     *
     * 调用者可根据项目实际情况构造合适的 IPreviewProvider 实现
     */
    fun bindPreviewProvider(previewProvider: IPreviewProvider, lifecycleOwner: LifecycleOwner)

    /**
     * 绑定预览试图
     * Camerax专用 Camera2使用报错
     */
    fun bindPreviewProvider(previewView: PreviewView, lifecycleOwner: LifecycleOwner)

    /**
     * 绑定预览试图
     * Camera2专用 CameraX使用报错
     */
    fun bindPreviewProvider(textureView: TextureView,lifecycleOwner: LifecycleOwner)
    /**
     * 绑定预览试图
     * Camera2专用 CameraX使用报错
     */
    fun bindPreviewProvider(surfaceView: SurfaceView,lifecycleOwner: LifecycleOwner)

    /**
     * 拍照，并通过回调返回最终图片
     */
    fun captureImage(callback: (Bitmap?) -> Unit)

    /**
     * 添加图像分析器
     */
    fun addImageAnalyzer(analyzer: ImageAnalysis.Analyzer)

    /**
     * 设置变焦（数字变焦或基于物理裁切）
     */
    fun setZoom(zoomLevel: Float)

    /**
     * 释放资源
     */
    fun shutdown()
}
