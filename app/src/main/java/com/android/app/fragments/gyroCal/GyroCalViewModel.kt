package com.android.app.fragments.gyroCal

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.android.app.utils.sensors.SensorGyro

class GyroCalViewModel(application: Application) : AndroidViewModel(application) {
    val gyro = SensorGyro()

    fun onActive(context: Context){
        gyro.onActive(context)
    }

    fun onInactive(){
        gyro.onInactive()
    }

    fun onDestroy(context: Context){
        gyro.storeAzimuthOffset(context)
        gyro.storePitchOffset(context)
        gyro.storeRollOffset(context)

        gyro.onDestroy()
    }
}
