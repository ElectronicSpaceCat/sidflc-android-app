package com.android.greentech.plink.device.sensor

import com.android.greentech.plink.device.bluetooth.device.DeviceData

class VL53L4CD(override val sensor: Sensor) : VL53L4CX(sensor) { // VL53L4CD uses the same driver as VL53L4CX
    override val type: DeviceData.Sensor.Type = DeviceData.Sensor.Type.VL53L4CD
}