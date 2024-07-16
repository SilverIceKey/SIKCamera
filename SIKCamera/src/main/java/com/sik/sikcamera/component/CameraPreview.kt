package com.sik.sikcamera.component

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.sik.sikcamera.CameraManager

@Composable
fun CameraPreview(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            CameraManager(context).apply {
                initialize { }
                bindCameraView(previewView, lifecycleOwner)
            }
            previewView
        },
        modifier = modifier
    )
}