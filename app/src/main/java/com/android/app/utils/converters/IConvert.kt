package com.android.app.utils.converters

interface IConvert<E: Enum<E>> {
    fun convert(from: E, to: E, input: Double): Double
    fun getUnit(unit: String) : E
}
