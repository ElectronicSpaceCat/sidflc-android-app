package com.android.greentech.plink.dataShared

import android.content.Context
import com.android.greentech.plink.R
import com.android.greentech.plink.device.Device
import com.android.greentech.plink.utils.converters.ConvertLength
import com.android.greentech.plink.utils.converters.ConverterData
import com.android.greentech.plink.utils.converters.LengthData

/**
 * The purpose of this class is to hold global data used throughout the app.
 */
class DataShared(context: Context) {
    companion object{
        lateinit var device : Device

        lateinit var deviceHeight : LengthData
        lateinit var targetDistance : LengthData
        lateinit var targetHeight : LengthData

        lateinit var lensOffset : LengthData

        lateinit var carriagePosition : LengthData
        lateinit var carriagePositionOverride : LengthData
    }

    init {
        device = Device(context)

        deviceHeight = ConverterData(ConvertLength, context.getString(R.string.key_device_height), ConvertLength.Unit.FT)
        targetDistance = ConverterData(ConvertLength, context.getString(R.string.key_target_distance), ConvertLength.Unit.FT)
        targetHeight = ConverterData(ConvertLength, context.getString(R.string.key_target_height), ConvertLength.Unit.FT)

        lensOffset = ConverterData(ConvertLength, context.getString(R.string.key_lens_offset), ConvertLength.Unit.IN)
        lensOffset.loadFromPrefs(context)

        carriagePosition = ConverterData(ConvertLength, context.getString(R.string.key_carriage_position), ConvertLength.Unit.MM, 0.0, 0)
        // Only need the stored unit, not the value since it is dynamic
        carriagePosition.loadUnitFromPrefs(context)

        carriagePositionOverride = ConverterData(ConvertLength, context.getString(R.string.key_carriage_positionOverride), ConvertLength.Unit.MM, device.model.getMaxCarriagePosition(), 0)
        carriagePositionOverride.loadUnitFromPrefs(context)
    }
}
