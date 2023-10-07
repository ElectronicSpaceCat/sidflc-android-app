package com.android.greentech.plink.utils.calculators

object CalcMisc {
    fun interpolate(input: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return (input - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
