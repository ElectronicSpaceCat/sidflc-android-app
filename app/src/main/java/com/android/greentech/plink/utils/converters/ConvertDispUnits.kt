package com.android.greentech.plink.utils.converters

import android.content.Context

object ConvertDispUnits {
    fun pxToMm(context : Context, value : Float) : Float {
        return value / context.resources.displayMetrics.xdpi * 25.4f
    }

    fun dpToPx(context : Context, value : Float) : Float {
        return value * context.resources.displayMetrics.density
    }

    fun pxToDp(context : Context, value : Float) : Float {
        return value / context.resources.displayMetrics.density
    }
}