package com.android.greentech.plink.device.sensor

import com.android.greentech.plink.device.bluetooth.sensor.SensorData

class VL6180X(
    override val sensor: Sensor,
    override val type: SensorData.Sensor.Type = SensorData.Sensor.Type.VL6180X
) : ISensor, SensorCalibrate() {

    override val configs: Array<ISensor.Config> = emptyArray()
}