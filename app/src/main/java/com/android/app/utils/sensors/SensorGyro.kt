package com.android.app.utils.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.android.app.utils.calculators.CalcFilters

class SensorGyro : SensorEventListener {
    companion object {
        private const val TAG_SMPL_SIZE = "gyro_sample_size"
        private const val TAG_AZIMUTH = "gyro_azimuth"
        private const val TAG_PITCH = "gyro_pitch"
        private const val TAG_ROLL = "gyro_roll"

        private const val IMU_WINDOW_SIZE = 15 // Moving average

        private const val AZIMUTH = 0
        private const val ROLL = 1
        private const val PITCH = 2

        private var sensorManager: SensorManager? = null
        private var mAccelerometer: Sensor? = null
        private var mMagnetometer: Sensor? = null

        private var _accelAvailable = false
        private var _magAvailable = false

        private var accels: FloatArray = FloatArray(3)
        private var mags: FloatArray = FloatArray(3)

        private val _rawValues = FloatArray(3)

        private val _azimuth = MutableLiveData(0.0)
        private val _pitch = MutableLiveData(0.0)
        private val _roll = MutableLiveData(0.0)

        private var _sampleWindowSize = 0

        private lateinit var _avgAzimuth: CalcFilters.MovingAverage
        private lateinit var _avgPitch: CalcFilters.MovingAverage
        private lateinit var _avgRoll: CalcFilters.MovingAverage

        private var _rollOffset = 0.0
        private var _pitchOffset = 0.0
        private var _azimuthOffset = 0.0

        private var _onSensorChangedListener: OnSensorChangedListener? = null
    }

    fun interface OnSensorChangedListener {
        fun onSensorChanged(roll: Double, pitch: Double, azimuth : Double)
    }

    fun setOnSensorChangedListener(listener: OnSensorChangedListener?) {
        if(_onSensorChangedListener == null){
            _onSensorChangedListener = listener
        }
    }

    val accelAvailable : Boolean
        get() = _accelAvailable

    val magAvailable: Boolean
        get() = _magAvailable

    val rollOnChange : LiveData<Double>
        get() = _roll
    val roll : Double
        get() = _roll.value!!

    val pitchOnChange : LiveData<Double>
        get() = _pitch
    val pitch : Double
        get() = _pitch.value!!

    val azimuthOnChange : LiveData<Double>
        get() = _azimuth
    val azimuth : Double
        get() = _azimuth.value!!

    val rollOffset : Double
        get() = _rollOffset

    val pitchOffset : Double
        get() = _pitchOffset

    val azimuthOffset : Double
        get() = _azimuthOffset

    val sampleWindowSize : Int
        get() = _sampleWindowSize

    fun storeSampleWindowSize(context: Context, size : Int) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putInt(TAG_SMPL_SIZE, size).apply()
        setSampleWindowSize(size)
    }

    private fun setSampleWindowSize(size: Int){
        _avgAzimuth = CalcFilters.MovingAverage(size)
        _avgPitch = CalcFilters.MovingAverage(size)
        _avgRoll = CalcFilters.MovingAverage(size)
    }

    private fun loadSampleWindowSize(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val windowSize = prefs.getInt(TAG_SMPL_SIZE, IMU_WINDOW_SIZE)
        if(_sampleWindowSize != windowSize) {
            _sampleWindowSize = windowSize
            setSampleWindowSize(_sampleWindowSize)
        }
    }

    private fun loadCalOffsets(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        _azimuthOffset = prefs.getFloat(TAG_AZIMUTH, 0f).toDouble()
        _pitchOffset = prefs.getFloat(TAG_PITCH, 0f).toDouble()
        _rollOffset = prefs.getFloat(TAG_ROLL, 0f).toDouble()
    }

    fun setAzimuthOffset() {
        _azimuthOffset = Math.toDegrees(_rawValues[AZIMUTH].toDouble()) * -1.0
    }

    fun resetAzimuthOffset() {
        _azimuthOffset = 0.0
        _avgAzimuth.reset(Math.toDegrees(_rawValues[AZIMUTH].toDouble()))
    }

    fun setPitchOffset() {
        _pitchOffset = Math.toDegrees(_rawValues[PITCH].toDouble()) * -1.0
    }

    fun resetPitchOffset() {
        _pitchOffset = 0.0
        _avgPitch.reset(Math.toDegrees(_rawValues[PITCH].toDouble()))
    }

    fun setRollOffset() {
        _rollOffset = Math.toDegrees(_rawValues[ROLL].toDouble()) * -1.0
    }

    fun resetRollOffset() {
        _rollOffset = 0.0
        _avgRoll.reset(Math.toDegrees(_rawValues[ROLL].toDouble()))
    }

    fun storeAzimuthOffset(context: Context) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putFloat(TAG_AZIMUTH, _azimuthOffset.toFloat()).apply()
    }

    fun storePitchOffset(context: Context) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putFloat(TAG_PITCH, _pitchOffset.toFloat()).apply()
    }

    fun storeRollOffset(context: Context) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putFloat(TAG_ROLL, _rollOffset.toFloat()).apply()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        when (sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                when(accuracy){
                    SensorManager.SENSOR_STATUS_UNRELIABLE -> {}
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {}
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {}
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {}
                    else -> return
                }
                //println("Accel accuracy: ".plus(accuracy.toString()))
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                when(accuracy){
                    SensorManager.SENSOR_STATUS_UNRELIABLE -> {}
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {}
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {}
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {}
                    else -> return
                }
                //println("Mag accuracy: ".plus(accuracy.toString()))
            }
            else -> return // If sensor not one of concern, then leave function
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accels = event.values.clone()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mags = event.values.clone()
            }
            else -> return // If sensor not one of concern, then leave function
        }

        // Gravity rotational data
        val gravity = FloatArray(9)
        // Magnetic rotational data
        val magnetic = FloatArray(9)
        SensorManager.getRotationMatrix(gravity, magnetic, accels, mags)

        val outGravity = FloatArray(9)

        SensorManager.remapCoordinateSystem(
            gravity,
            SensorManager.AXIS_Y,
            SensorManager.AXIS_MINUS_X,
            outGravity
        )

        SensorManager.getOrientation(outGravity, _rawValues)

        // Moving average - Azimuth
        _azimuth.value = _avgAzimuth.getAverage(
            Math.toDegrees(
                _rawValues[AZIMUTH].toDouble()
            )
        ) + _azimuthOffset

        // Moving average - Pitch
        _pitch.value = (_avgPitch.getAverage(
            Math.toDegrees(
                _rawValues[PITCH].toDouble()
            )
        ) + _pitchOffset)

        // Moving average - Roll
        _roll.value = (_avgRoll.getAverage(
            Math.toDegrees(
                _rawValues[ROLL].toDouble()
            )
        ) + _rollOffset)

        // Call the listener
        _onSensorChangedListener?.onSensorChanged(azimuth, pitch, roll)
    }

    fun onActive(context: Context, sampleRate : Int = SensorManager.SENSOR_DELAY_GAME)  {
        // Get sensor manager
        if(sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        }
        // Get accelerometer and magnetometer
        if(sensorManager != null){
            mAccelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            mMagnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        }
        // Register accelerometer to sensor manager
        if(mAccelerometer != null){
            _accelAvailable = sensorManager!!.registerListener(this, mAccelerometer, sampleRate)
        }
        // Register magnetometer to sensor manager
        if(mMagnetometer != null) {
            _magAvailable = sensorManager!!.registerListener(this, mMagnetometer, sampleRate)
        }

        // Load sensor offsets from prefs
        loadCalOffsets(context)

        // Load sample window size from prefs
        loadSampleWindowSize(context)
    }

    fun onInactive(){
        sensorManager?.unregisterListener(this)
    }

    fun onDestroy(){
        _onSensorChangedListener = null
        sensorManager = null
        mAccelerometer = null
        mMagnetometer = null
    }
}
