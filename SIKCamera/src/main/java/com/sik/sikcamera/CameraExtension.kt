package com.sik.sikcamera

import android.graphics.Bitmap
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

// ImageProxy 扩展函数：将 ImageProxy 中的图片转换为 Base64 编码字符串
fun ImageProxy.toBase64(quality: Int = 100, withPrefix: Boolean = false): String {
    // 获取 Bitmap 对象
    val bitmap = this.toBitmap()

    // 将 Bitmap 对象转换为 Base64 编码字符串
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    val byteArray = outputStream.toByteArray()
    if (withPrefix) {
        return "data:image/jpeg;base64,${Base64.encodeToString(byteArray, Base64.DEFAULT)}"
    }
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

// ImageProxy 扩展函数：将 ImageProxy 中的图片转换为 NV21 字节数组
fun ImageProxy.toNv21ByteArray(): ByteArray {
    val width = this.width
    val height = this.height
    val argb = IntArray(width * height)
    val yuv = ByteArray(width * height * 3 / 2)

    // 将 Bitmap 转换为 ARGB 格式
    this.toBitmap().getPixels(argb, 0, width, 0, 0, width, height)

    // 将 ARGB 格式的像素数据转换为 NV21 格式
    encodeYUV420SP(yuv, argb, width, height)

    return yuv
}

// 将 ARGB 格式的像素数据转换为 NV21 格式
private fun encodeYUV420SP(yuv: ByteArray, argb: IntArray, width: Int, height: Int) {
    val frameSize = width * height
    var yIndex = 0
    var uvIndex = frameSize

    var y: Int
    var u: Int
    var v: Int

    var r: Int
    var g: Int
    var b: Int

    for (j in 0 until height) {
        for (i in 0 until width) {
            // 获取像素的 ARGB 值
            val index = width * j + i
            r = argb[index] and 0xff0000 shr 16
            g = argb[index] and 0xff00 shr 8
            b = argb[index] and 0xff

            // 将 ARGB 值转换为 YUV 值
            y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
            u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
            v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128

            // 将 YUV 值写入 NV21 字节数组
            y = y.coerceIn(16, 255)
            u = u.coerceIn(0, 255)
            v = v.coerceIn(0, 255)

            yuv[yIndex++] = y.toByte()
            if (j % 2 == 0 && i % 2 == 0) {
                yuv[uvIndex++] = u.toByte()
                yuv[uvIndex++] = v.toByte()
            }
        }
    }
}
