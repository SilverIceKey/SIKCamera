package com.sik.sikcamera.component

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.sik.sikcamera.CameraXManager
import com.sik.sikcamera.IPreviewProvider

@Composable
fun CameraPreview(
    modifier: Modifier,
    useCameraManager: CameraXManager.() -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            CameraXManager(context).apply {
                initialize { }
                useCameraManager(this)
                val iPreviewProvider = object : IPreviewProvider{
                    override fun asSurfaceProvider(): Preview.SurfaceProvider? {
                        return previewView.surfaceProvider
                    }

                    override fun asPreviewView(): PreviewView? {
                        return previewView
                    }

                }
                bindPreviewProvider(iPreviewProvider, lifecycleOwner)
            }
            previewView
        },
        modifier = modifier
    )
}