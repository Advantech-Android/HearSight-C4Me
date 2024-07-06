package com.codewithkael.firebasevideocall.videointelegence.GemniAI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class BoundingBoxOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 4.0f
    }
    private val boundingBoxes = mutableListOf<Rect>()

    fun setBoundingBoxes(boxes: List<Rect>) {
        boundingBoxes.clear()
        boundingBoxes.addAll(boxes)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (box in boundingBoxes) {
            canvas.drawRect(box, paint)
        }
    }

}
