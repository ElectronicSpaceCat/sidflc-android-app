package com.android.greentech.plink.utils.textFilters

import android.text.InputFilter
import android.text.Spanned

object TextInputFilter {
    class MinMax(minValue: Number, maxValue: Number) : InputFilter {
        private var intMin: Number = 0
        private var intMax: Number = 0

        init{
            this.intMin = minValue
            this.intMax = maxValue
        }

        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dStart: Int, dEnd: Int): CharSequence? {
            if(source == "-"){
                return source
            }

            if(this.intMin.toFloat() < 1.0){
                if((dStart == 0 && source == "0") || (dStart == 1 && source == ".")) {
                    return null
                }
            }

            try {
                val input = (dest.toString() + source.toString()).toFloat()
                if (isInRange(intMin.toFloat(), intMax.toFloat(), input)) {
                    return null
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }

            return ""
        }
    }

    fun isStrInRange(intMin: Number, intMax: Number, strVal : String) : Boolean{
        try {
            val input = strVal.toFloat()
            return isInRange(intMin.toFloat(), intMax.toFloat(), input)
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return false
    }

    // Check if input c is in between min a and max b and
    // returns corresponding boolean
    private fun isInRange(a: Float, b: Float, c: Float): Boolean {
        return if (b > a) c in a..b else c in b..a
    }
}