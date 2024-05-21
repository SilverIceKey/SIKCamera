package com.sik.sikcamera

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat

class BasicCameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), CameraView {

    override fun createSurfaceProvider(): Preview.SurfaceProvider {
        return Preview.SurfaceProvider { request ->
            val surfaceTexture = this.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(width, height)
            val surface = Surface(surfaceTexture)
            request.provideSurface(surface, ContextCompat.getMainExecutor(context)) { result ->
                // Handle surface result if needed
            }
        }
    }
}
