package com.android.app.utils.plotter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import java.lang.Integer.max
import kotlin.math.abs

open class CanvasPlotterBox(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private var _xIncrement = 0
    private var _yIncrement = 0
    private var xMin = 0
    private var yMin = 0
    private var xMax = 0
    private var yMax = 0
    var ref = 0

    var xOffset = 0f
    var yOffset = 0f

    var xIncrement : Int
        get() = _xIncrement
        set(value) {
            _xIncrement = max(1, value)
        }

    var yIncrement : Int
        get() = _yIncrement
        set(value) {
            _yIncrement = max(1, value)
        }

    private val textPaint = TextPaint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        textSize = 14f
        isAntiAlias = true
    }

    private val axisLinePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val refLinePaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 0f
        isAntiAlias = true
    }

    fun setDataBounds(xMin : Int, xMax : Int, yMin : Int, yMax : Int){
        this.xMin = xMin
        this.xMax = xMax
        this.yMin = yMin
        this.yMax = yMax
    }

    fun draw() {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val delta = abs(yMax - yMin)
        val numIncrements = (delta / max(1, yIncrement))
        val incrementInPixels = yIncrement.toRealY()

        val textOffsetX = 10f // Offset text so it is not butted against the line

        for(i in 0..numIncrements){
            // Draw the increment values
            canvas.drawText((yMax - (yIncrement * i)).toString(), textOffsetX, (incrementInPixels * i) - textOffsetX, textPaint)
            // Draw the increment separation lines
            canvas.drawLine(0f, incrementInPixels * i, width.toFloat(), incrementInPixels * i, linePaint)
        }

        // Draw a ref line if not zero
        if(ref > 0) {
            val yVal = (yMax - ref).toRealY()
            canvas.drawLine(xOffset, yVal, width.toFloat(), yVal, refLinePaint)
        }

        // Vertical line to enclose the increment data values
        canvas.drawLine(xOffset, 0f, xOffset, height.toFloat(), linePaint)

        // Box side left
        canvas.drawLine(0f, 0f, 0f, height.toFloat(), axisLinePaint)
        // Box side right
        canvas.drawLine(width.toFloat(), height.toFloat(), width.toFloat(), 0f, axisLinePaint)
        // Box side top
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, axisLinePaint)
        // Box side bottom
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), axisLinePaint)
    }

    private fun Int.toRealX() = toFloat() / abs(xMax - xMin) * width
    private fun Int.toRealY() = toFloat() / abs(yMax - yMin) * height
}