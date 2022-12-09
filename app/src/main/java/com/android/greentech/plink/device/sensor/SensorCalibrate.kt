package com.android.greentech.plink.device.sensor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.greentech.plink.device.sensor.ISensorCalibrate.State

abstract class SensorCalibrate : ISensorCalibrate{
    private var _calibrationState = MutableLiveData(State.NA)
    private var _calibrationStateMsg = MutableLiveData("")
    private var _calibrationInProgress = false

    override val calibrationInProgress : Boolean
        get() = _calibrationInProgress

    override val calibrationState : State
        get() = _calibrationState.value!!

    override val calibrationStateOnChange : LiveData<State>
        get() = _calibrationState

    override val calibrationStateMsg : String
        get() = _calibrationStateMsg.value!!

    override val calibrationStateMsgOnChange : LiveData<String>
        get() = _calibrationStateMsg


    protected var state : State
        get() = _calibrationState.value!!
        set(value) {_calibrationState.value = value}

    protected var stateMsg : String
        get() = _calibrationStateMsg.value!!
        set(value) {_calibrationStateMsg.value = value}

    override fun startCalibration() {
        _calibrationInProgress = true
        state = State.PREPARE
        runCalibration()
    }

    override fun stopCalibration() {
        _calibrationInProgress = false
        state = State.NA
    }

    override fun runCalibration() {}
}
