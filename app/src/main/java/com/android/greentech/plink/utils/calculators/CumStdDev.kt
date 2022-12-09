package com.android.greentech.plink.utils.calculators

import kotlin.math.sqrt

/**
 * Cumulative Standard Deviation
 */
class CumStdDev {
        private var n = 0
        private var sum = 0.0
        private var sum2 = 0.0

        fun sd(x: Double): Double {
            n++
            sum += x
            sum2 += x * x
            return sqrt(sum2 / n - sum * sum / n / n)
        }

    fun reset() {
        n = 0
        sum = 0.0
        sum2 = 0.0
    }

    fun csdTest() {
        val testData = doubleArrayOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        val csd = CumStdDev()
        for (d in testData) println("Add $d => ${csd.sd(d)}")
    }
}
