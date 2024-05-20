package com.android.app.utils.calculators

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan

object CalcLinear {
    fun interpolate(input: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return (input - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    fun interpolate(input: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        return (input - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    /**
     * m = (y2 - y1)/(x2 - x1)
     */
    fun getSlope(x1: Double, y1: Double, x2: Double, y2: Double) : Double {
        return (y2 - y1) / (x2 - x1)
    }

    /**
     * m = (y2 - y1)/(x2 - x1)
     */
    fun getSlope(x1: Float, y1: Float, x2: Float, y2: Float) : Float {
        return (y2 - y1) / (x2 - x1)
    }

    /**
     * m = tan(θ)
     */
    fun getSlope(angle: Double) : Double {
        return tan(Math.toRadians(angle))
    }

    /**
     * b = y - mx
     */
    fun getInterceptY(x: Double, y: Double, m: Double) : Double {
        return (y - m*x)
    }

    /**
     * √[(x2 − x1)^2 + (y2 − y1)^2]
     */
    fun getDistanceBetweenPoints(x1: Double, y1: Double, x2: Double, y2: Double) : Double {
        return sqrt((x2-x1).pow(2) + (y2-y1).pow(2))
    }
}
