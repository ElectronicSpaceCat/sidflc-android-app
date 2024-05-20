package com.android.app.utils.plotter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.android.app.utils.calculators.CalcLinear
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

data class DataPoint(
    val xVal: Float,
    val yVal: Float,
)

class CanvasPlotter(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    enum class PlotType{
        LAYOUT,
        PLOT,
        ALL
    }

    var plotType = PlotType.ALL

    // Shared parameters
    var xMin = 0f
    var xMax = 0f
    var yMin = 0f
    var yMax = 0f

    // Layout parameters
    private var _xIncrement = 0f
    private var _yIncrement = 0f
    var drawAxisX = true
    var drawAxisY = true
    var xAxisHeight = 0f
    var yAxisWidth = 0f
    var textOffset = 10f
    var ref = 0f

    // Data plot parameters
    private val _dataSet = mutableListOf<DataPoint>()
    private var _colorDataLine = Color.WHITE
    private var _colorRefLine = Color.WHITE

    // Interpolation parameters
    private var _rangeX = 0f
    private var _rangeY = 0f
    private var _xM = 0f
    private var _yM = 0f
    private var _updateInterpolationData = true

    var colorDataLine: Int
        get() = _colorDataLine
        set(value) {
            dataPointLinePaint = Paint().apply {
                color = value
                strokeWidth = 2f
                isAntiAlias = true
            }
        }

    var colorRefLine: Int
        get() = _colorRefLine
        set(value) {
            refLinePaint = Paint().apply {
                color = value
                strokeWidth = 2f
                isAntiAlias = true
            }
        }

    var xIncrement : Float
        get() = _xIncrement
        set(value) {
            _xIncrement = max(1f, value)
        }

    var yIncrement : Float
        get() = _yIncrement
        set(value) {
            _yIncrement = max(1f, value)
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

    private var refLinePaint = Paint().apply {
        color = colorRefLine
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 0f
        isAntiAlias = true
    }

    private var dataPointLinePaint = Paint().apply {
        color = colorDataLine
        strokeWidth = 2f
    }

    fun updateBounds() {
        _updateInterpolationData = true
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

        if(_updateInterpolationData){
            _updateInterpolationData = false
            updateInterpolationData()
        }

        when(plotType) {
            PlotType.ALL -> {
                drawLayout(canvas)
                drawPlot(canvas)
            }
            PlotType.LAYOUT -> {
                drawLayout(canvas)
            }
            PlotType.PLOT -> {
                drawPlot(canvas)
            }
        }
    }

    private fun drawLayout(canvas: Canvas) {
        drawOutline(canvas)

        if(drawAxisX) {
            drawAxisX(canvas)
        }
        if(drawAxisY) {
            drawAxisY(canvas)
        }
    }

    private fun drawPlot(canvas: Canvas) {
        // Plot the data set
        for(idx in 0.._dataSet.lastIndex) {
            if (idx < _dataSet.lastIndex) {
                if(_dataSet[idx].xVal < xMin && _dataSet[idx + 1].xVal < xMin) {
                    continue
                }
                if(_dataSet[idx].yVal < yMin && _dataSet[idx + 1].yVal < yMin) {
                    continue
                }

                var xStart : Float = _dataSet[idx].xVal
                var xEnd : Float = _dataSet[idx+1].xVal
                var yStart : Float = _dataSet[idx].yVal
                var yEnd : Float = _dataSet[idx+1].yVal

                if(xStart < xMin) {
                    xStart = xMin
                    yStart = getYatXIntercept(_dataSet[idx], _dataSet[idx+1])
                }

                if(xEnd < xMin) {
                    xEnd = xMin
                    yEnd = getYatXIntercept(_dataSet[idx], _dataSet[idx+1])
                }

                if(xStart >= xMin && yStart < yMin) {
                    yStart = yMin
                    xStart = getXatYIntercept(_dataSet[idx], _dataSet[idx+1])
                }

                if(xEnd >= xMin && yEnd < yMin) {
                    yEnd = yMin
                    xEnd = getXatYIntercept(_dataSet[idx], _dataSet[idx+1])
                }

                // Draw line between the points
                canvas.drawLine(
                    xStart.toRealX(),
                    yStart.toRealY(),
                    xEnd.toRealX(),
                    yEnd.toRealY(),
                    dataPointLinePaint)
            }
        }
    }

    private fun drawPlotPoints(canvas: Canvas) {
        // Plot the data set
        for(idx in 0.._dataSet.lastIndex) {
            if(_dataSet[idx].xVal < xMin || _dataSet[idx].yVal < yMin) {
                continue
            }
            // Draw point
            canvas.drawPoint(_dataSet[idx].xVal.toRealX(), _dataSet[idx].yVal.toRealY(), dataPointLinePaint)
        }
    }

    private fun drawOutline(canvas: Canvas) {
        // Box side left
        canvas.drawLine(0f, 0f, 0f, height.toFloat(), axisLinePaint)
        // Box side right
        canvas.drawLine(width.toFloat(), height.toFloat(), width.toFloat(), 0f, axisLinePaint)
        // Box side top
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, axisLinePaint)
        // Box side bottom
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), axisLinePaint)
    }

    private fun drawAxisX(canvas: Canvas) {
        // Calculate number of sections to draw out
        val deltaX = abs(xMax - xMin)
        val numSections = (deltaX / xIncrement).toInt()
        // Plot the X axis data
        for(i in 0..numSections){
            val x = xMin + (xIncrement * i)
            // Draw the increment values
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", x), x.toRealX() + textOffset, height.toFloat() - textOffset, textPaint)
            // Draw the increment separation lines
            canvas.drawLine(x.toRealX(), 0f, x.toRealX() , height.toFloat(),  linePaint)
        }
        // Horizontal line to enclose the x axis values
        if(xAxisHeight > 0f) {
            canvas.drawLine(0f, (height.toFloat() - xAxisHeight), width.toFloat(), (height.toFloat() - xAxisHeight), linePaint)
        }
    }

    private fun drawAxisY(canvas: Canvas) {
        // Calculate number of sections to draw out
        val deltaY = abs(yMax - yMin)
        val numSections = (deltaY / yIncrement).toInt()
        // Plot the Y axis data
        for(i in 0..numSections){
            val y = yMin + (yIncrement * i)
            // Draw the increment values
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", y), textOffset, y.toRealY() - textOffset, textPaint)
            // Draw the increment separation lines
            canvas.drawLine(0f, y.toRealY(), width.toFloat(), y.toRealY(), linePaint)
        }

        // Vertical line to enclose the y axis values
        if(yAxisWidth > 0f) {
            canvas.drawLine(yAxisWidth, 0f, yAxisWidth, height.toFloat(), linePaint)
        }
        // Draw the ref line if not set to zero
        if(ref > 0) {
            canvas.drawLine(yAxisWidth, ref.toRealY(), width.toFloat(), ref.toRealY(), refLinePaint)
        }
    }

    private fun getXatYIntercept(dataPointA: DataPoint, dataPointB: DataPoint) : Float {
        val m = CalcLinear.getSlope(dataPointA.xVal, dataPointA.yVal, dataPointB.xVal, dataPointB.yVal)
        val b = (dataPointA.yVal - m * dataPointA.xVal)
        return ((yMin - b) / m)
    }

    private fun getYatXIntercept(dataPointA: DataPoint, dataPointB: DataPoint) : Float {
        val m = CalcLinear.getSlope(dataPointA.xVal, dataPointA.yVal, dataPointB.xVal, dataPointB.yVal)
        val b = (dataPointA.yVal - m * dataPointA.xVal)
        return (m * xMin + b)
    }

    private fun updateInterpolationData() {
        // Update X data
        _rangeX = abs(width - yAxisWidth)
        _xM = _rangeX / abs(xMax - xMin)

        // Update Y data
        _rangeY = abs(height - xAxisHeight)
        _yM = _rangeY / abs(yMax - yMin)
    }

    private fun Float.toRealX() = ((toFloat() - xMin) * _xM + yAxisWidth)
    private fun Float.toRealY() = (_rangeY - ((toFloat() - yMin) * _yM))
}