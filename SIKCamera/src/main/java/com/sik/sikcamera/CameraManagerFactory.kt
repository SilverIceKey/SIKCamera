package com.sik.sikcamera

import android.content.Context

object CameraManagerFactory {
    /**
     * 根据参数决定使用 CameraX 还是 Camera2 实现
     */
    fun getCameraManager(context: Context, useCamera2: Boolean = false): ICameraManager {
        return if (useCamera2) {
            Camera2Manager(context)
        } else {
            CameraXManager(context)
        }
    }
}