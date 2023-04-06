/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.greentech.plink.fragments.device.deviceCalibrate

import android.app.Application
import androidx.lifecycle.*
import com.android.greentech.plink.dataShared.DataShared
import com.android.greentech.plink.device.bluetooth.device.DeviceData
import kotlinx.coroutines.*

class DeviceCalibrateViewModel (application: Application) : AndroidViewModel(application) {
    enum class CalSelect {
        CAL_ALL,
        CAL_SENSOR_SHORT,
        CAL_SENSOR_LONG
    }

    private var _calSelected = CalSelect.CAL_ALL
    private var _prevSelectedSensor = DataShared.device.sensorSelected

    val calSelected : CalSelect
        get() = _calSelected

    val activeSensorId : DeviceData.Sensor.Id
        get() = DataShared.device.activeSensor.id

    fun startCalibration() {
        when(_calSelected){
            CalSelect.CAL_SENSOR_SHORT,
            CalSelect.CAL_ALL -> {
                setSensor(DeviceData.Sensor.Id.SHORT)
            }
            CalSelect.CAL_SENSOR_LONG -> {
                setSensor(DeviceData.Sensor.Id.LONG)
            }
        }
    }

    fun startCalibration(calSelect: CalSelect) {
        when(calSelect){
            CalSelect.CAL_SENSOR_SHORT,
            CalSelect.CAL_ALL -> {
                setSensor(DeviceData.Sensor.Id.SHORT)
            }
            CalSelect.CAL_SENSOR_LONG -> {
                setSensor(DeviceData.Sensor.Id.LONG)
            }
        }
    }

    fun stopCalibration() {
        DataShared.device.activeSensor.stopCalibration()
    }

    private fun setSensor(sensorId: DeviceData.Sensor.Id) {
        when(sensorId) {
            DeviceData.Sensor.Id.SHORT,
            DeviceData.Sensor.Id.LONG -> {
                DataShared.device.setSensor(sensorId)
                CoroutineScope(Dispatchers.IO).launch {
                    withTimeout(2500){
                        // Wait until sensor to calibrate is active
                        while(DataShared.device.sensorSelected.value!!.id != sensorId){
                            delay(250)
                        }
                        // Start calibration
                        CoroutineScope(Dispatchers.Main).launch {
                            DataShared.device.activeSensor.startCalibration()
                        }
                    }
                }
            }
            else -> {}
        }
    }

    fun onResume() {
        DataShared.device.setSensorEnable(false)
    }

    fun onDestroy() {
        // Selected the sensor that was active before entering the Calibration screen
        DataShared.device.setSensor(_prevSelectedSensor.value!!.id)
        // Ensure we stop any running cals on destroy
        stopCalibration()
        // Set previous enable state
        DataShared.device.setSensorEnable(false)
    }
}
