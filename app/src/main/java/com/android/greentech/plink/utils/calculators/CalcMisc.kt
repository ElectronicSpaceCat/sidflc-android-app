package com.android.greentech.plink.utils.calculators

object CalcMisc {
    fun interpolate(input: Double, in_min: Double, in_max: Double, out_min: Double, out_max: Double): Double {
        return (input - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
    }
}
