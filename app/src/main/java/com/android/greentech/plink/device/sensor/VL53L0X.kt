package com.android.greentech.plink.device.sensor

import com.android.greentech.plink.device.bluetooth.sensor.SensorData

class VL53L0X(
    override val sensor: Sensor,
    override val type: SensorData.Sensor.Type = SensorData.Sensor.Type.VL53L0X
) : ISensor, SensorCalibrate() {

    override val configs: Array<ISensor.Config> = emptyArray()
}