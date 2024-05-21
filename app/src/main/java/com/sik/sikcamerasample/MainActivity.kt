package com.sik.sikcamerasample

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sik.sikcamera.BasicCameraView
import com.sik.sikcamera.CameraManager

class MainActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 检查是否已经授予 CAMERA 权限
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // CAMERA 权限已经被授予，可以开始相机操作
            // 例如，初始化 CameraPreviewView 等
        } else {
            // CAMERA 权限尚未被授予，请求权限
            requestCameraPermission()
        }

        CameraManager(this).apply {
            initialize {
                bindCameraView(findViewById<BasicCameraView>(R.id.cameraView), this@MainActivity)
            }
        }
    }

    private fun requestCameraPermission() {
        // 请求 CAMERA 权限
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }
}