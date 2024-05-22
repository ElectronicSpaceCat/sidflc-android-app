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
        lateinit var deviceOffset : LengthData
        lateinit var lensOffset : LengthData
        lateinit var targetDistance : LengthData
        lateinit var targetHeight : LengthData
        lateinit var carriagePosition : LengthData
        lateinit var carriagePositionOverride : LengthData
    }

    init {
        device = Device(context)

        phoneHeight = ConverterData(context.getString(R.string.key_phone_height), ConvertLength, ConvertLength.Unit.FT, displayPrecision = 1)
        targetDistance = ConverterData(context.getString(R.string.key_target_distance), ConvertLength, ConvertLength.Unit.FT, displayPrecision = 1)
        targetHeight = ConverterData(context.getString(R.string.key_target_height), ConvertLength, ConvertLength.Unit.FT, displayPrecision = 1)
        lensOffset = ConverterData(context.getString(R.string.key_lens_offset), ConvertLength, ConvertLength.Unit.IN)
        deviceOffset = ConverterData(context.getString(R.string.key_device_offset), ConvertLength, ConvertLength.Unit.IN)
        carriagePosition = ConverterData(context.getString(R.string.key_carriage_position), ConvertLength, ConvertLength.Unit.MM, displayPrecision = 0)
        carriagePositionOverride = ConverterData( context.getString(R.string.key_carriage_positionOverride), ConvertLength, ConvertLength.Unit.MM, device.model.getMaxCarriagePosition(), 0)

        lensOffset.loadFromPrefs(context)
        deviceOffset.loadFromPrefs(context)
        carriagePosition.loadUnitFromPrefs(context)
        carriagePositionOverride.loadUnitFromPrefs(context)
    }
}
