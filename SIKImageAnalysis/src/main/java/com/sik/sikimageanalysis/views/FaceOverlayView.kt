package com.sik.sikimageanalysis.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face

class FaceOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.argb(255,135, 206, 235)
        style = Paint.Style.STROKE
        strokeWidth = 5.0f
    }

    private var boundingBoxes: List<Rect> = emptyList()

    fun updateFaces(boundingBoxes: List<Rect>) {
        this.boundingBoxes = boundingBoxes
        invalidate() // 重新绘制
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in boundingBoxes) {
            canvas.drawRect(rect, paint)
        }
    }
}