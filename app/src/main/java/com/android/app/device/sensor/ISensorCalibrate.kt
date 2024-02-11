package com.android.app.device.sensor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.app.device.bluetooth.device.DeviceData

interface ISensorCalibrate {
    enum class State {
        INIT,
        PREPARE,
        START,
        CALIBRATING,
        WAIT_FOR_RESPONSE,
        FINISHED,
        ERROR,
        NA
    }

    var state : State
        get() = _calibrationState.value!!
        set(value) {_calibrationState.value = value}

    var stateMsg : String
        get() = _calibrationStateMsg.value!!
        set(value) {_calibrationStateMsg.value = value}

    fun startCalibration()
    fun stopCalibration()
    fun runCalibration(config : DeviceData.Config)

    companion object{
        private var _calibrationState = MutableLiveData(State.NA)
        private var _calibrationStateMsg = MutableLiveData("")

        val calibrationState : LiveData<State>
            get() = _calibrationState

        val calibrationStateMsg : LiveData<String>
            get() = _calibrationStateMsg
    }
}