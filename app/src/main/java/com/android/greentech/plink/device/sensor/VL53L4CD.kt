package com.android.greentech.plink.device.sensor

import com.android.greentech.plink.device.bluetooth.sensor.SensorData

class VL53L4CD(
    override val sensor: Sensor,
    override val type: SensorData.Sensor.Type = SensorData.Sensor.Type.VL53L4CD)
    : VL53L4CX(sensor, type) { // VL53L4CD uses the same driver as VL53L4CX
}