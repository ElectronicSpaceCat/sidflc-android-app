package com.android.app.utils.calculators

object CalcMisc {
    fun interpolate(input: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return (input - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    fun interpolate(input: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        return (input - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
