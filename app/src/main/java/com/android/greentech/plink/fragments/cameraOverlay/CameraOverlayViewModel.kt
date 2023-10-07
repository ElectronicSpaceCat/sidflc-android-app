/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.greentech.plink.fragments.cameraOverlay

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.android.greentech.plink.dataShared.DataShared
import com.android.greentech.plink.utils.converters.ConvertLength.Unit
import com.android.greentech.plink.utils.calculators.CalcBallistics
import com.android.greentech.plink.utils.calculators.CalcTrig
import com.android.greentech.plink.utils.converters.ConvertLength
import com.android.greentech.plink.utils.sensors.SensorGyro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class DataType{
    DEVICE_HEIGHT,
    TARGET_DISTANCE,
    TARGET_HEIGHT,
    NONE,
    NA
}

class CameraOverlayViewModel(application: Application) : AndroidViewModel(application) {
    private val _isCalculationPaused = MutableLiveData(true)
    private val _dataToGet = MutableLiveData(DataType.NA)
    private var _totalLengthTarget = 0.0
    private var _totalLengthEst = 0.0

    private val carriagePosMerger = MediatorLiveData<Double>()

    val gyro = SensorGyro()

    private var _isPositionAutoMode = MutableLiveData(true)
    val isPositionAutoModeOnChange : LiveData<Boolean>
        get() = _isPositionAutoMode

    var isPositionAutoMode : Boolean
        get() = _isPositionAutoMode.value!!
        set(value) {
            if(_isPositionAutoMode.value != value ){
                _isPositionAutoMode.value = value
            }
        }

    private var _isEngViewActive = MutableLiveData(false)
    val isEngViewActiveOnChange : LiveData<Boolean>
        get() = _isEngViewActive

    var isEngViewActive : Boolean
        get() = _isEngViewActive.value!!
        set(value) {
            if(_isEngViewActive.value != value ){
                _isEngViewActive.value = value
            }
        }

    var impactDistance = 0.0
    var impactHeight = 0.0

    private var _hitConfidence = MutableLiveData(0.0)
    val hitConfidence : LiveData<Double>
        get() = _hitConfidence

    var isRollInRange = false
    var isPitchInRange = false

    var dataToGet : DataType
        get() = _dataToGet.value!!
        set(value) {
            if(_dataToGet.value != value ){
                _dataToGet.value = value
                // Note: Need to force update the calc flag in the event the
                //  isCalculationPaused is the same for the next dataToGet
                _isCalculationPaused.value = !isCalculationPaused
            }
        }

    val dataToGetLive: LiveData<DataType>
        get() = _dataToGet

    var isCalculationPaused: Boolean
        get() = _isCalculationPaused.value!!
        set(value) {
            if(_isCalculationPaused.value != value ){
                _isCalculationPaused.value = value
            }
        }

    val isCalculationPausedLive: LiveData<Boolean>
        get() = _isCalculationPaused

    private fun calcDeviceHeight(pitch: Double, roll: Double){
        isRollInRange = roll in -1.5..1.5
        isPitchInRange = (90.0 - pitch) in -1.5..1.5

        if(isPositionAutoMode){
            isCalculationPaused = !(isRollInRange && isPitchInRange) || !DataShared.device.connectionState.value!!.isReady
        }
        else{
            isCalculationPaused = !(isRollInRange && isPitchInRange)
        }

        if(!isCalculationPaused) {
            DataShared.deviceHeight.setValue(Unit.MM, DataShared.device.sensorDeviceHeight.rangeFiltered.value!!)
        }
    }

    private fun calcTargetDistance(pitch: Double, roll: Double){
        isRollInRange = roll in -1.0..1.0
        isPitchInRange = pitch in 0.0..90.0
        isCalculationPaused = !(isRollInRange && isPitchInRange)

        if(!isCalculationPaused) {
            // Convert to common unit
            val deviceHeight = DataShared.deviceHeight.getConverted(Unit.M)
            val lensOffset = DataShared.lensOffset.getConverted(Unit.M)
            val caseBodyLength = ConvertLength.convert(Unit.MM, Unit.M, DataShared.device.model.caseBodyLength)
            // Get total lens height which is offset by the phone's pitch
            val lensHeight = (deviceHeight + CalcTrig.getSideAGivenSideCAngleA(caseBodyLength + lensOffset, pitch))
            // Calculate target distance
            DataShared.targetDistance.setValue(Unit.M, CalcBallistics.getTargetDistance(lensHeight, pitch))
        }
    }

    private fun calcTargetHeight(pitch: Double, roll: Double){
        // Convert to common unit
        val deviceHeight = DataShared.deviceHeight.getConverted(Unit.M)
        val lensOffset = DataShared.lensOffset.getConverted(Unit.M)
        val caseBodyLength = ConvertLength.convert(Unit.MM, Unit.M, DataShared.device.model.caseBodyLength)
        // Get total lens height
        val lensHeight = (deviceHeight + CalcTrig.getSideAGivenSideCAngleA(caseBodyLength + lensOffset, pitch))
        // Calculate target angle
        val targetDistance = DataShared.targetDistance.getConverted(Unit.M)
        val angleAtTrgtDist = CalcTrig.getAngleBGivenSideASideB(lensHeight, targetDistance)

        isRollInRange = roll in -1.0..1.0
        isPitchInRange = pitch in angleAtTrgtDist..180.0
        isCalculationPaused = !(isRollInRange && isPitchInRange)

        if(!isCalculationPaused){
            // Get angle from target distance and lens height
            DataShared.targetHeight.setValue(Unit.M, CalcBallistics.getTargetHeight(lensHeight, angleAtTrgtDist, pitch))
        }
    }

    private fun calcDefaultBounds(pitch: Double, roll: Double) {
        isRollInRange = roll in -1.0..1.0
        isPitchInRange = pitch in 0.0..90.0
        isCalculationPaused = !(isRollInRange && isPitchInRange)
    }

    private fun calcData(pitch: Double, roll: Double) {
        when(dataToGet){
            DataType.DEVICE_HEIGHT -> {
                calcDeviceHeight(pitch, roll)
            }
            DataType.TARGET_DISTANCE -> {
                calcTargetDistance(pitch, roll)
            }
            DataType.TARGET_HEIGHT -> {
                calcTargetHeight(pitch, roll)
            }
            DataType.NONE,
            DataType.NA-> {
                calcDefaultBounds(pitch, roll)
            }
        }
    }

    private fun isReadyToFire() : Boolean {
        return (!isCalculationPaused
                && dataToGet == DataType.NONE
                && DataShared.device.connectionState.value!!.isReady)
    }

    /**
     * This should be called on a background thread since it blocks
     */
    private fun calcBallistics(position : Double) {
        // Get impact data
        val impactData = DataShared.device.ballistics.getImpactData(
            position,
            gyro.pitch,
            DataShared.deviceHeight.getConverted(Unit.M),
            DataShared.targetDistance.getConverted(Unit.M),
            DataShared.lensOffset.getConverted(Unit.MM)
        )

        // Convert impactDistance to same units as targetDistance
        impactDistance = ConvertLength.convert(Unit.M, DataShared.targetDistance.unit, impactData.distance)
        // Convert impactHeight to same units as targetHeight
        impactHeight = ConvertLength.convert(Unit.M, DataShared.targetHeight.unit, impactData.height)

        // Target distance + target height
        _totalLengthEst = if(impactDistance >= DataShared.targetDistance.value && DataShared.targetHeight.value > 0.0){
            DataShared.targetDistance.value + impactHeight
        } else{
            impactDistance
        }

        // Get the totalLengthTarget
        _totalLengthTarget = (DataShared.targetDistance.value + DataShared.targetHeight.value)

        // Calculate the hit confidence percentage based on the total length
        _hitConfidence.postValue(if(_totalLengthEst == 0.0 || _totalLengthTarget == 0.0){
            0.0 // Return 0 if no target or estimate length = 0
        } else{
            // Flip the percentage calculation if the estimate length is above or below the target length
            if (_totalLengthEst > _totalLengthTarget) {
                (_totalLengthTarget / _totalLengthEst) * 100.0
            } else {
                (_totalLengthEst / _totalLengthTarget) * 100.0
            }
        })
    }

    fun onActive(context: Context) {
        gyro.onActive(context)
        gyro.setOnSensorChangedListener { _, pitch, roll -> calcData(pitch, roll) }

        // Is sensor ready?
        if(DataShared.device.connectionState.value?.isReady!!){
            if(dataToGet == DataType.NONE || dataToGet == DataType.DEVICE_HEIGHT) {
                DataShared.device.setSensorEnable(true)
            }
        }
    }

    fun onInactive(){
        gyro.onInactive()
        DataShared.device.setSensorEnable(false)
    }

    fun onDestroy(){
        gyro.onDestroy()
    }

    init {
        // Merged carriage position liveData sources (auto and manual modes)
        carriagePosMerger.addSource(DataShared.carriagePosition.valueOnChange) {
            if (isPositionAutoMode) {
                carriagePosMerger.value = it
            }
        }

        carriagePosMerger.addSource(DataShared.carriagePositionOverride.valueOnChange) {
            if (!isPositionAutoMode) {
                carriagePosMerger.value = it
            }
        }

        // Launch background coroutine to process the ballistics data
        // which will update the hitConfidence liveData.
        CoroutineScope(Dispatchers.IO).launch {
            carriagePosMerger.asFlow().collect {
                if(isReadyToFire()) {
                    calcBallistics(it)
                }
            }
        }
    }
}
