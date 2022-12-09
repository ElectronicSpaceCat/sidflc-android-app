package com.android.greentech.plink.utils.plotter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class GraphView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val _dataSet = mutableListOf<DataPoint>()

    private var xMin = 0
    private var xMax = 0
    private var yMin = 0
    private var yMax = 0

    var xOffset = 0f
    var yOffset = 0f

    private val dataPointLinePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        //isAntiAlias = true
    }

    fun setDataBounds(xMin : Int, xMax: Int, yMin : Int, yMax : Int){
        this.xMin = xMin
        this.xMax = xMax
        this.yMin = yMin
        this.yMax = yMax
    }

    fun setData(newDataSet: List<DataPoint>) {
        _dataSet.clear()
        _dataSet.addAll(newDataSet)
    }

    fun draw() {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        _dataSet.forEachIndexed { idx, currentDataPoint ->
//            val realX = currentDataPoint.xVal.toRealX()
//            val realY = currentDataPoint.yVal.toRealY()
            if (idx < _dataSet.lastIndex) {
                // Convert first point location to pixels
                val startX = currentDataPoint.xVal.toRealX() + xOffset
                val startY = (yMax - currentDataPoint.yVal).toRealY() + yOffset
                // Get next point
                val nextDataPoint = _dataSet[idx + 1]
                // Convert second point location to pixels
                val endX = nextDataPoint.xVal.toRealX() + xOffset
                val endY = (yMax - nextDataPoint.yVal).toRealY() + yOffset
                // Draw line between the points
                canvas.drawLine(startX, startY, endX, endY, dataPointLinePaint)
            }
            //canvas.drawCircle(realX, realY, 7f, dataPointFillPaint)
            //canvas.drawCircle(realX, realY, 7f, dataPointPaint)
        }
    }

    private fun Double.toRealX() = toFloat() / abs(xMax - xMin) * width
    private fun Double.toRealY() = toFloat() / abs(yMax - yMin) * height
}

data class DataPoint(
    val xVal: Double,
    val yVal: Double,
)