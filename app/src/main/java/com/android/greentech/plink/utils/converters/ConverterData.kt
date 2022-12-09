package com.android.greentech.plink.utils.converters

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.android.greentech.plink.utils.misc.Utils
import java.util.*

typealias LengthData = ConverterData<ConvertLength, ConvertLength.Unit>
typealias WeightData = ConverterData<ConvertLength, ConvertLength.Unit>

/**
 * The purpose of this class is to manage unit conversions for various unit types
 * such as weight or length.
 */
class ConverterData<T: IConvert<E>, E : Enum<E>>(
    private val converter: T,
    private val keyData: String,
    unit: E,
    dataInit: Double = 0.0,
    var precision: Int = 2) {

    private val _data: MutableLiveData<Double> = MutableLiveData(dataInit)
    private val _unit: MutableLiveData<E> = MutableLiveData(unit)

    /**
     * Get the stored value
     */
    val value: Double
        get() = _data.value!!

    /**
     * Get the stored value as LiveData
     */
    val valueOnChange: LiveData<Double>
        get() = _data

    /**
     * Get the value unit
     */
    val unit: E
        get() = _unit.value!!

    /**
     * Get the value unit as LiveData
     */
    val unitOnChange: LiveData<E>
        get() = _unit

    /**
     * Set value of the data unit.
     *
     * @param unit
     */
    fun setUnit(unit: E){
        val prevUnit = _unit.value
        _unit.value = unit
        setValue(converter.convert(prevUnit!!, unit, value))
    }

    /**
     * Get a conversion of the current stored unit type
     * @param unitTo
     * @return converted value
     */
    fun getConverted(unitTo: E): Double {
        return converter.convert(unit, unitTo, value)
    }

    /**
     * Set value of the data. User should be aware
     * of the input unit type to make sure it matches the
     * current value held.
     *
     * @param value
     */
    fun setValue(value: Double) {
        _data.value = value
    }

    /**
     * Set value of the length data when the input data
     * in is of a different unit type. It will be
     * converted to its active unit type internally.
     *
     * @param unitFrom
     * @param value
     */
    fun setValue(unitFrom: E, value: Double){
        setValue(converter.convert(unitFrom, unit, value))
    }

    /**
     * Post value of the data. User should be aware
     * of the input unit type to make sure it matches the
     * current value held.
     *
     * @param value
     */
    fun postValue(value: Double) {
        _data.postValue(value)
    }

    /**
     * Post value of the length data when the input data
     * in is of a different unit type. It will be
     * converted to its active unit type internally.
     *
     * @param value
     */
    fun postValue(unitFrom: E, value: Double) {
        _data.postValue(converter.convert(unitFrom, unit, value))
    }

    /**
     * Get the stored value as a string
     *
     * @return value as string
     */
    fun valueStr(): String{
        return String.format(Locale.getDefault(), "%.${precision}f", value)
    }

    /**
     * Get the stored value active unit type as a string
     *
     * @return unit as string
     */
    fun unitStr(): String{
        return unit.name.lowercase()
    }

    /**
     * Store the current unit to preferences
     *
     * @param context
     */
    fun storeUnitToPrefs(context: Context, unit: E) {
        val prefsEdit = PreferenceManager.getDefaultSharedPreferences(context).edit()
        prefsEdit.putString(keyData + "_unit", unit.name.lowercase()).apply()
    }

    /**
     * Store the current value to preferences
     *
     * @param context
     */
    fun storeValueToPrefs(context: Context, value: Double) {
        val prefsEdit = PreferenceManager.getDefaultSharedPreferences(context).edit()
        prefsEdit.putString(keyData + "_value", String.format(Locale.getDefault(), "%.${precision}f", value)).apply()
    }

    /**
     * Store the external value to preferences
     *
     * @param context
     * @param unitFrom
     * @param value
     */
    fun storeValueToPrefs(context: Context, unitFrom: E, value: Double) {
        val prefsEdit = PreferenceManager.getDefaultSharedPreferences(context).edit()
        val newValue = converter.convert(unitFrom, unit, value)
        prefsEdit.putString(keyData + "_value", String.format(Locale.getDefault(), "%.${precision}f", newValue)).apply()
    }

    /**
     * Store the current value and unit to preferences
     *
     * @param context
     */
    fun storeToPrefs(context: Context){
        storeValueToPrefs(context, value)
        storeUnitToPrefs(context, unit)
    }

    /**
     * Load the current value from preferences
     *
     * @param context
     */
    fun loadValueFromPrefs(context: Context){
        _data.value = getValueFromPrefs(context)
    }

    /**
     * Load the current unit from preferences
     *
     * @param context
     */
    fun loadUnitFromPrefs(context: Context){
        _unit.value = getUnitFromPrefs(context)
    }

    /**
     * Load the current value and unit from preferences
     *
     * @param context
     */
    fun loadFromPrefs(context: Context){
        loadValueFromPrefs(context)
        loadUnitFromPrefs(context)
    }

    /**
     * Get the current value from preferences
     *
     * @param context
     */
    fun getValueFromPrefs(context: Context) : Double {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val valueStr = prefs.getString(keyData + "_value", 0.0.toString())
        return Utils.convertStrToDouble(valueStr)
    }

    /**
     * Get the current unit from preferences
     *
     * @param context
     */
    fun getUnitFromPrefs(context: Context): E {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val unitStr = prefs.getString(keyData + "_unit", unitStr())
        return converter.getUnit(unitStr!!.uppercase())
    }
}
