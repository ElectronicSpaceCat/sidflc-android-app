package com.android.greentech.plink.device.sensor

import com.android.greentech.plink.device.bluetooth.sensor.SensorData

interface ISensor : ISensorCalibrate {
    val type : SensorData.Sensor.Type
    val sensor: Sensor
    class Config(val name : String, var value : Int)
    val configs: Array<Config>
}