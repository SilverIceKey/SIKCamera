package com.sik.sikcamerasample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import com.sik.sikcamera.component.CameraPreview

class MainActivity : ComponentActivity() {
    // 请求相机权限的 Launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // 权限已经被授予，可以开始相机操作
                // 例如，初始化 CameraPreviewView 等
            } else {
                // 权限被拒绝，无法进行相机操作
                // 可以向用户解释为什么需要相机权限，并提示用户手动授权
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Column {
                CameraPreview(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    private fun requestCameraPermission() {
        // 请求 CAMERA 权限
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }
}