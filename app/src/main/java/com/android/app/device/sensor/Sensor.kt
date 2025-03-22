package com.android.app.device.sensor

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.android.app.device.bluetooth.DeviceBluetoothManager
import com.android.app.device.bluetooth.device.DeviceData
import com.android.app.utils.calculators.CalcFilters
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import kotlin.math.abs
import androidx.core.content.edit

class Sensor(
    context: Context,
    private val device: DeviceBluetoothManager,
    val id: DeviceData.Sensor.Id) {

    private var _sensor : ISensor = VL53L4CD(this) // Set a default type

    private val _sensorTag = "sensor_".plus(id.toString() + "_")
    private val _filterTag = _sensorTag + "filter_size"

    private val _prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private var _configIdx = 0
    private var _isInitialized = false // Indicates when configurations are cached (not required for normal operation)
    private var _isEnabled = false

    private var _isActive = false

    private var _sampleSize = DEFAULT_SAMPLE_SIZE
    private var _rangeAvg = CalcFilters.MovingAverage(_sampleSize)
    private var _rangeAvgErr = CalcFilters.MovingAverage(_sampleSize) // Moving average of the (previous - current) range average
//    private var _rangeKalman = CalcFilters.KalmanFilter(1.0, 5.0)
//    private var _uncertainty = 0.0

    private var _rangeFiltered = MutableLiveData(0.0) // Range value after filtering and drift compensation (if enabled)

    private var _driftOffset : Double = 0.0 // Used for drift compensation
    private var _driftCompensationEnable = false // Enable sensor drift compensation
    private var _driftCompReset = false

    var targetReference : Int = 0 // Target reference distance in mm

    val sampleSize: Int
        get() = _sampleSize

    val rangeFilteredLive: LiveData<Double>
        get() = _rangeFiltered

    val rangeFiltered: Double
        get() = _rangeFiltered.value!!

    val isEnabled : Boolean
        get() = _isEnabled

    val isInitialized : Boolean
        get() = _isInitialized

    val type : DeviceData.Sensor.Type
        get() = _sensor.type

    val configs: Array<ISensor.Config>
        get() = _sensor.configs

    val driftOffset : Double
        get() = _driftOffset

    var driftCompensationEnable : Boolean
        get() = _driftCompensationEnable
        set(value) {
            _driftCompensationEnable = value
            _driftOffset = 0.0
        }

    /**
     * Select the sensor enable
     */
    fun enable(enable: Boolean) {
        device.setSensorEnable(enable)
    }

    /**
     * Reset the sensor
     */
    fun reset() {
        device.sensorReset(DeviceData.ResetCommand.RESET_SENSOR)
    }

    /**
     * Reset the sensor to factory defaults
     */
    fun resetFactory() {
        device.sensorReset(DeviceData.ResetCommand.RESET_SENSOR_FACTORY)
    }

    /**
     * Set the sensor type
     * @param type DeviceData.Sensor.Type
     */
    private fun setSensorType(type: DeviceData.Sensor.Type) {
        _sensor = when (type) {
            DeviceData.Sensor.Type.VL53L4CD -> {
                VL53L4CD(this)
            }
            else -> {
                VL53L4CX(this)
            }
        }
    }

    fun setFilterSampleSize(size: Int) {
        _sampleSize = Integer.max(1, size)
        _rangeAvg = CalcFilters.MovingAverage(_sampleSize)
        _rangeAvgErr = CalcFilters.MovingAverage(_sampleSize)
        _rangeAvg.reset(_rangeFiltered.value!!)
        _rangeAvgErr.reset()
        _prefs.edit { putInt(_filterTag, _sampleSize) }
    }

    /**
     * Call this to start the process of loading in configuration data
     */
    fun loadConfigs(){
        if(isInitialized) return
        sendConfigCommand(DeviceData.Config.Command.GET, _configIdx, Int.MAX_VALUE)
    }

    /**
     * Grab the configurations for the sensor when it comes online
     *
     * This should be kicked off by sending a GET config command somewhere else in code.
     */
    private fun getConfigs(config : Int){
        if(isInitialized) return

        // Is config index within range?
        if(_configIdx < _sensor.configs.lastIndex){
            // Yes - Is the config the same as the one we requested?
            if(_configIdx == config){
                // Yes - Send for the next one
                sendConfigCommand(DeviceData.Config.Command.GET, ++_configIdx, Int.MAX_VALUE)
            }
        }
        // No - Finished..
        else{
            _isInitialized = true
        }
    }

    /**
     * Store a configuration
     */
    private fun storeConfig(id: Int, value: Int){
        if(id < _sensor.configs.size){
            if(value != _sensor.configs[id].value){
                _sensor.configs[id].value = value
            }
        }
    }

    /**
     * Invalidate the LiveData cached value. This can help in
     * the case when adding new observers and not having it trigger
     * on a valid config response.
     */
    fun invalidateLastConfigData() {
        device.deviceData.setConfig(DeviceData.Config.Target.SENSOR, DeviceData.Config.Command.NA, 0xFF, Int.MAX_VALUE, DeviceData.Config.Status.NA)
    }

    /**
     * Send a configuration command
     * @param command
     * @param id
     * @param value
     */
    fun sendConfigCommand(command: DeviceData.Config.Command, id: Int, value: Int) {
        device.sendSensorConfigCommand(DeviceData.Config.Target.SENSOR, command, id, value)
    }

    /**
     * Send special command to store configuration data
     * into permanent storage internal to the device
     */
    fun storeConfigData() {
        device.sendSensorConfigCommand(DeviceData.Config.Target.SENSOR,  DeviceData.Config.Command.STORE)
    }

    /**
     * Start calibration routine
     */
    fun startCalibration() {
        _sensor.startCalibration()
    }

    /**
     * Stop calibration routine
     */
    fun stopCalibration() {
        _sensor.stopCalibration()
    }

    /**
     * Called when the active sensor changes
     * @param sensor DeviceData.Sensor
     */
    private fun onSensorUpdate(sensor: DeviceData.Sensor) {
        if(this.type != sensor.type) {
            setSensorType(sensor.type)
        }
    }

    /**
     * Called when sensor ranging enable state changes.
     * @param enable
     */
    private fun onSensorEnableUpdate(enable : Boolean) {
        _isEnabled = enable
        _driftCompReset = true
    }

    /**
     * Called when new range data is received
     * If the autoRangeDriftAdjustEnable is True then
     * the offsetRef will be used to determine the drift offset
     * and remove it from the filtered range value.
     * @param range
     */
    private fun onRangeUpdate(range: Int) {
        // Apply the moving average to reduce jitter
        _rangeAvg.getAverage(range.toDouble())
        _rangeAvgErr.getAverage(abs(_rangeAvg.averagePrev - _rangeAvg.average))
        // Only calculate drift offset if target reference not zero
        if(targetReference != 0) {
            // Set the drift compensation reset flag if average range below target reference
            if (_rangeAvg.average < targetReference) {
                _driftCompReset = true
            }
            // Calculate applied drift offset if error withing threshold
            if(_rangeAvgErr.average < RANGE_ERR_AVG_THRESHOLD) {
                // Reset the drift offset if range above target reference and flag was set
                if (_driftCompReset && _rangeAvg.average > targetReference) {
                    _driftCompReset = false
                    _driftOffset = 0.0
                }
                // Calculate the drift offset
                if (_rangeAvg.average > (targetReference + _driftOffset + 0.5)) {
                    _driftOffset = (_rangeAvg.average - targetReference)
                }
            }
            // Apply drift compensation if enabled
            if(driftCompensationEnable){
                _rangeFiltered.value = (_rangeAvg.average - _driftOffset)
                return
            }
        }
        // Update the filtered range value
        _rangeFiltered.value = _rangeAvg.average
    }

    /**
     * Called when there is a configuration response
     * to a configuration command.
     * @param config
     */
    private fun onConfigUpdate(config : DeviceData.Config) {
        // Ignore configurations not intended for the sensor
        if(config.trgt != DeviceData.Config.Target.SENSOR) return

        when (config.status) {
            DeviceData.Config.Status.OK,
            DeviceData.Config.Status.UPDATED,
            DeviceData.Config.Status.MISMATCH -> {
                storeConfig(config.id, config.value)
            }
            else -> {}
        }

        // Run configuration updates
        getConfigs(config.id)
        // Run calibration if needed
        _sensor.runCalibration(config)
    }

    /**
     * Called when there is a status update from the active sensor.
     * @param status
     */
    private fun onStatusUpdate(status : DeviceData.Status) {
        when (status) {
            DeviceData.Status.BOOTING -> {
                dataInit()
            }
            else -> {}
        }
    }

    /**
     * Called when there is a connection state change from the device
     * @param connection
     */
    private fun onConnectionStateUpdate(connection: ConnectionState) {
        when(connection.state){
            ConnectionState.State.DISCONNECTED -> {
                dataInit()
            }
            else -> {}
        }
    }

    /**
     * Initialize data
     */
    private fun dataInit() {
        _isInitialized = false
        _configIdx = 0
    }

    init {
        /**
         * Load the sample window size for the range moving average
         */
        _sampleSize = _prefs.getInt(_filterTag, DEFAULT_SAMPLE_SIZE)

        /**
         * Observe the sensor selected
         */
        device.deviceData.sensor.observe(context as LifecycleOwner) {
            _isActive = (it.id == this.id)
            if(!_isActive) return@observe
            onSensorUpdate(it)
        }

        /**
         * Observe the sensor status
         */
        device.deviceData.status.observe(context as LifecycleOwner) {
            if(!_isActive) return@observe
            onStatusUpdate(it)
        }

        /**
         * Observe the config data
         */
        device.deviceData.config.observe(context as LifecycleOwner) {
            if(!_isActive) return@observe
            onConfigUpdate(it)
        }

        /**
         * Observe the sensor enable
         */
        device.deviceData.enable.observe(context as LifecycleOwner) {
            if(!_isActive) return@observe
            onSensorEnableUpdate(it)
        }

        /**
         * Observe the sensor range data
         */
        device.deviceData.range.observe(context as LifecycleOwner) {
            if(!_isActive) return@observe
            onRangeUpdate(it)
        }

        /**
         * Observe the connection state of the device and
         * set the configuration state to not configured.
         */
        device.state.observe(context as LifecycleOwner) {
            if(!_isActive) return@observe
            onConnectionStateUpdate(it)
        }
    }

    companion object{
        private const val DEFAULT_SAMPLE_SIZE = 7
        private const val RANGE_ERR_AVG_THRESHOLD = 1 // Error needs to be within threshold before calculating drift offset, ie the signal is stabilized
    }
}