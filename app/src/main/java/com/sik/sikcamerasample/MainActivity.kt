package com.sik.sikcamerasample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sik.sikcamera.component.CameraPreview
import com.sik.sikcore.data.ConvertUtils
import com.sik.sikcore.log.LogUtils
import com.sik.sikimageanalysis.image_analysis.QrCodeAnalysis

class MainActivity : ComponentActivity() {
    private val logger = LogUtils.getLogger(MainActivity::class)

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
            var message by remember {
                mutableStateOf("")
            }
            Column(modifier = Modifier.fillMaxSize()) {
                CameraPreview(modifier = Modifier.fillMaxWidth()) {
                    addImageAnalyzer(QrCodeAnalysis {
                        if (message.length < 728) {
                            val resultRawBytes = it.toByteArray(Charsets.ISO_8859_1)
                            logger.i("${resultRawBytes.size}")
                            message = ConvertUtils.bytesToHex(resultRawBytes)
                            logger.i(message)
                        }
                    })
                }
                Text(
                    text = message, modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = Color.Green
                )
            }
        }
    }

    private fun requestCameraPermission() {
        // 请求 CAMERA 权限
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }
}