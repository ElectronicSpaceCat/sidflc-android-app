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
        // Device location
        lateinit var deviceHeight : LengthData
        // Target location
        lateinit var targetDistance : LengthData
        lateinit var targetHeight : LengthData
        // Device offset from phone camera lens
        lateinit var lensOffset : LengthData
        // Device carriage position, used for display
        lateinit var carriagePosition : LengthData
        lateinit var carriagePositionOverride : LengthData
    }

    init {
        device = Device(context)

        deviceHeight = ConverterData(context.getString(R.string.key_device_height), ConvertLength, ConvertLength.Unit.FT)
        targetDistance = ConverterData(context.getString(R.string.key_target_distance), ConvertLength, ConvertLength.Unit.FT)
        targetHeight = ConverterData(context.getString(R.string.key_target_height), ConvertLength, ConvertLength.Unit.FT)

        lensOffset = ConverterData(context.getString(R.string.key_lens_offset), ConvertLength, ConvertLength.Unit.IN)
        lensOffset.loadFromPrefs(context)

        carriagePosition = ConverterData(context.getString(R.string.key_carriage_position), ConvertLength, ConvertLength.Unit.MM, 0.0, 0)
        // Only need the stored unit, not the value since it is dynamic
        carriagePosition.loadUnitFromPrefs(context)

        carriagePositionOverride = ConverterData( context.getString(R.string.key_carriage_positionOverride), ConvertLength, ConvertLength.Unit.MM, device.model.getMaxCarriagePosition(), 0)
        carriagePositionOverride.loadUnitFromPrefs(context)
    }
}
