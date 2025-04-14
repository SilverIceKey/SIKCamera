package com.sik.sikcamera

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView

/**
 * 预览视图抽象接口，用于统一适配 CameraX 和 Camera2 的预览需求
 */
interface IPreviewProvider {
    /**
     * 当使用 CameraX 时，返回对应的 Preview.SurfaceProvider
     * 若当前场景不支持 CameraX，则可以返回 null
     */
    fun asSurfaceProvider(): Preview.SurfaceProvider?

    /**
     * 当使用 Camera2 时，返回对应的 PreviewView 或其它包含 Surface 的视图
     * 若当前场景不支持 Camera2，则可以返回 null
     */
    fun asPreviewView(): PreviewView?
}
