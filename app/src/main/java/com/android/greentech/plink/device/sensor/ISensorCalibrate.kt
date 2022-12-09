package com.android.greentech.plink.device.sensor

import androidx.lifecycle.LiveData

interface ISensorCalibrate {
    enum class State {
        PREPARE,
        START,
        CALIBRATING,
        WAIT_FOR_RESPONSE,
        FINISHED,
        ERROR,
        NA
    }

    val calibrationInProgress : Boolean
    val calibrationState : State
    val calibrationStateOnChange : LiveData<State>
    val calibrationStateMsg : String
    val calibrationStateMsgOnChange : LiveData<String>

    fun startCalibration()
    fun stopCalibration()
    fun runCalibration()
}