package com.android.app.dataShared

import android.content.Context
import com.android.app.device.Device
import com.android.app.utils.converters.ConvertLength
import com.android.app.utils.converters.ConverterData
import com.android.app.utils.converters.LengthData
import com.android.app.R

/**
 * The purpose of this class is to hold global data used throughout the app.
 */
class DataShared(context: Context) {
    companion object{
        lateinit var device : Device
        lateinit var phoneHeight : LengthData
        lateinit var deviceOffsetFromBase : LengthData
        lateinit var lensOffsetFromBase : LengthData
        lateinit var targetDistance : LengthData
        lateinit var targetHeight : LengthData
        lateinit var carriagePosition : LengthData
        lateinit var carriagePositionOverride : LengthData
    }

    init {
        device = Device(context)

        // Create the unit parameters
        phoneHeight = ConverterData(context.getString(R.string.key_phone_height), ConvertLength, ConvertLength.Unit.FT, displayPrecision = 1)
        targetDistance = ConverterData(context.getString(R.string.key_target_distance), ConvertLength, ConvertLength.Unit.FT, displayPrecision = 1)
        targetHeight = ConverterData(context.getString(R.string.key_target_height), ConvertLength, ConvertLength.Unit.FT, displayPrecision = 1)
        lensOffsetFromBase = ConverterData(context.getString(R.string.key_lens_offset), ConvertLength, ConvertLength.Unit.IN)
        deviceOffsetFromBase = ConverterData(context.getString(R.string.key_device_offset), ConvertLength, ConvertLength.Unit.IN)
        carriagePosition = ConverterData(context.getString(R.string.key_carriage_position), ConvertLength, ConvertLength.Unit.MM, displayPrecision = 0)
        carriagePositionOverride = ConverterData( context.getString(R.string.key_carriage_positionOverride), ConvertLength, ConvertLength.Unit.MM, device.model.getMaxCarriagePosition(), 0)
        // Load from preferences
        lensOffsetFromBase.loadFromPrefs(context)
        deviceOffsetFromBase.loadFromPrefs(context)
        carriagePosition.loadUnitFromPrefs(context)
        carriagePositionOverride.loadUnitFromPrefs(context)
    }
}
