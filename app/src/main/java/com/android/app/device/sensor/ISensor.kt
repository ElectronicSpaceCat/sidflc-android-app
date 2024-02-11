package com.android.app.device.sensor

import com.android.app.device.bluetooth.device.DeviceData

interface ISensor : ISensorCalibrate {
    val type : DeviceData.Sensor.Type
    val sensor : Sensor
    class Config(val name : String, var value : Int)
    val configs: Array<Config>
}
