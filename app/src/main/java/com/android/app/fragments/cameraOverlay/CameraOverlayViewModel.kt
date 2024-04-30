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
package com.android.app.fragments.cameraOverlay

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.android.app.dataShared.DataShared
import com.android.app.utils.converters.ConvertLength.Unit
import com.android.app.utils.calculators.CalcBallistics
import com.android.app.utils.calculators.CalcTrig
import com.android.app.utils.converters.ConvertLength
import com.android.app.utils.sensors.SensorGyro
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
    private val carriagePosMerger = MediatorLiveData<Double>()
    private var _isPositionAutoMode = MutableLiveData(true)
    private var _isEngViewActive = MutableLiveData(false)
    private val _hitConfidence = MutableLiveData(0.0)

    val gyro = SensorGyro()

    val hitConfidence : LiveData<Double>
        get() = _hitConfidence
    val getAdjustedLaunchHeight: Double
        get() = DataShared.device.ballistics.adjustedLaunchHeight
    val adjustedTargetDistance: Double
        get() = DataShared.device.ballistics.adjustedTargetDistance
    val impactDistance : Double
        get() = ConvertLength.convert(
            ConvertLength.Unit.M, DataShared.targetDistance.unit, DataShared.device.ballistics.impactData.distance)
    val impactHeight : Double
        get() = ConvertLength.convert(
            ConvertLength.Unit.M, DataShared.targetHeight.unit, DataShared.device.ballistics.impactData.height)
    val velocity : Double
        get() = DataShared.device.ballistics.projectileBallistics.velocity
    val netPotentialEnergy : Double
        get() = DataShared.device.ballistics.projectileBallistics.netPotentialEnergy


    val isPositionAutoModeOnChange : LiveData<Boolean>
        get() = _isPositionAutoMode

    var isPositionAutoMode : Boolean
        get() = _isPositionAutoMode.value!!
        set(value) {
            if(_isPositionAutoMode.value != value ){
                _isPositionAutoMode.value = value
            }
        }

    val isEngViewActiveOnChange : LiveData<Boolean>
        get() = _isEngViewActive

    var isEngViewActive : Boolean
        get() = _isEngViewActive.value!!
        set(value) {
            if(_isEngViewActive.value != value ){
                _isEngViewActive.value = value
            }
        }

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

        isCalculationPaused = if(isPositionAutoMode){
            !(isRollInRange && isPitchInRange) || !DataShared.device.connectionState.value!!.isReady
        } else{
            !(isRollInRange && isPitchInRange)
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

    /**
     * Called periodically by gyro.setOnSensorChangedListener()
     */
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
        // Convert data to the necessary units for ballistic calculations
        val lensOffset = DataShared.lensOffset.getConverted(ConvertLength.Unit.MM)
        val height = DataShared.deviceHeight.getConverted(ConvertLength.Unit.M)
        val targetDistance = DataShared.targetDistance.getConverted(ConvertLength.Unit.M)
        val targetHeight = DataShared.targetHeight.getConverted(ConvertLength.Unit.M)

        // Calculate the projectile impact data
        DataShared.device.ballistics.calcImpactData(
            position,
            lensOffset,
            height,
            gyro.pitch,
            targetDistance
        )

        // Update the hit confidence
        _hitConfidence.postValue(
            DataShared.device.ballistics.calcHitConfidence(targetDistance, targetHeight)
        )
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

    companion object {
        private const val DEFAULT_DELTA_TIME_SECONDS = 0.01
    }
}
