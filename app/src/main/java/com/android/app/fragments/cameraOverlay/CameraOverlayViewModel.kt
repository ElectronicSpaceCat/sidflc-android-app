package com.android.app.fragments.cameraOverlay

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.android.app.dataShared.DataShared
import com.android.app.utils.converters.ConvertLength.Unit
import com.android.app.utils.calculators.CalcBallistics
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

    private var _isPitchAtTargetDistanceCalculated = false
    private var _pitchAtTargetDistance = 0.0

    private var _impactData = CalcBallistics.ImpactData()

    val gyro = SensorGyro()

    val hitConfidence : LiveData<Double>
        get() = _hitConfidence
    val getAdjustedLaunchHeight: Double
        get() = DataShared.device.ballistics.adjustedLaunchHeight
    val adjustedTargetDistance: Double
        get() = DataShared.device.ballistics.adjustedTargetDistance
    val impactDistance : Double
        get() = ConvertLength.convert(
            ConvertLength.Unit.M, DataShared.targetDistance.unit, _impactData.distance)
    val impactHeight : Double
        get() = ConvertLength.convert(
            ConvertLength.Unit.M, DataShared.targetHeight.unit, _impactData.height)
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
                // Note: Force update the calc flag in the event the
                //       isCalculationPaused flag is the same for the next dataToGet
                _isCalculationPaused.value = !isCalculationPaused
                // Reset the flag for calculating the angle the target distance
                // was captured at.
                if(_dataToGet.value == DataType.TARGET_HEIGHT) {
                    _isPitchAtTargetDistanceCalculated = false
                }
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

    private fun calcPhoneHeight(pitch: Double, roll: Double){
        isRollInRange = roll in -1.5..1.5
        isPitchInRange = (90.0 - pitch) in -1.5..1.5

        isCalculationPaused = if(isPositionAutoMode){
            !(isRollInRange && isPitchInRange) || !DataShared.device.connectionState.value!!.isReady
        } else{
            !(isRollInRange && isPitchInRange)
        }

        if(!isCalculationPaused) {
            val deviceOffset = DataShared.deviceOffsetFromBase.getConverted(Unit.MM)
            val sensorDistance = DataShared.device.sensorDeviceHeight.rangeFiltered
            DataShared.phoneHeight.setValue(Unit.MM, CalcBallistics.getPhoneHeight(sensorDistance, deviceOffset))
        }
    }

    private fun calcTargetDistance(pitch: Double, roll: Double){
        isRollInRange = roll in -1.0..1.0
        isPitchInRange = pitch in 0.0..90.0
        isCalculationPaused = !(isRollInRange && isPitchInRange)

        if(!isCalculationPaused) {
            val phoneHeight = DataShared.phoneHeight.getConverted(Unit.M)
            val lensOffset = DataShared.lensOffsetFromBase.getConverted(Unit.M)
            // Calculate target distance
            DataShared.targetDistance.setValue(Unit.M, CalcBallistics.getTargetDistance(pitch, phoneHeight, lensOffset))
        }
    }

    private fun calcTargetHeight(pitch: Double, roll: Double){
        val phoneHeight = DataShared.phoneHeight.getConverted(Unit.M)
        val lensOffset = DataShared.lensOffsetFromBase.getConverted(Unit.M)
        val targetDistance = DataShared.targetDistance.getConverted(Unit.M)

        if(!_isPitchAtTargetDistanceCalculated) {
            _isPitchAtTargetDistanceCalculated = true
            _pitchAtTargetDistance = CalcBallistics.getPitchAtTargetDistance(phoneHeight, lensOffset, targetDistance)
        }

        isRollInRange = roll in -1.0..1.0
        isPitchInRange = pitch in _pitchAtTargetDistance..180.0
        isCalculationPaused = !(isRollInRange && isPitchInRange)

        if(!isCalculationPaused){
            DataShared.targetHeight.setValue(Unit.M, CalcBallistics.getTargetHeightWithPhoneBaseAsVertex(pitch, phoneHeight, lensOffset, targetDistance))
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
                calcPhoneHeight(pitch, roll)
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
        val deviceOffset = DataShared.deviceOffsetFromBase.getConverted(ConvertLength.Unit.MM)
        val phoneHeight = DataShared.phoneHeight.getConverted(ConvertLength.Unit.M)
        val targetDistance = DataShared.targetDistance.getConverted(ConvertLength.Unit.M)
        val targetHeight = DataShared.targetHeight.getConverted(ConvertLength.Unit.M)

        // Calculate the projectile impact data
        _impactData = DataShared.device.ballistics.calcImpactData(
            position,
            deviceOffset,
            phoneHeight,
            gyro.pitch,
            targetDistance
        )

        // Update the hit confidence
        _hitConfidence.postValue(
            DataShared.device.ballistics.calcHitConfidence(targetHeight, _impactData)
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

    fun onInactive() {
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
