package com.android.greentech.plink.device.sensor

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.android.greentech.plink.device.bluetooth.DeviceBluetoothManager
import com.android.greentech.plink.device.bluetooth.sensor.SensorData
import com.android.greentech.plink.utils.calculators.CalcFilters
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class Sensor(
    context: Context,
    private val device: DeviceBluetoothManager,
    val id: SensorData.Sensor.Id) {
    private lateinit var _sensor : ISensor

    private val _sensorTag = "sensor_".plus(id.toString() + "_")
    private val _typeTag = _sensorTag + "type"
    private val _filterTag = _sensorTag + "filter_size"

    private val _prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private var _configIdx = 0
    private var _isInitialized = false
    private var _isEnabled = false
    private var _isSensorActive =  false
    private var _sampleSize = DEFAULT_SAMPLE_SIZE
    private var _filter = CalcFilters.MovingAverage(_sampleSize)
//    private var _filter = CalcFilters.KalmanFilter(0.1, 15.0)
    private var _rangeFiltered = MutableLiveData(0.0)

    var offsetRef : Int = 0

    val sampleSize: Int
        get() = _sampleSize

    val rangeFiltered: LiveData<Double>
        get() = _rangeFiltered

    val lastConfigReceived : SensorData.Config
        get() = device.sensorData.config.value!!

    val isActive : Boolean
        get() = _isSensorActive

    val isEnabled : Boolean
        get() = _isEnabled

    val isInitialized : Boolean
        get() = _isInitialized

    val type : SensorData.Sensor.Type
        get() = _sensor.type

    val configs: Array<ISensor.Config>
        get() = _sensor.configs

    val calibrationInProgress : Boolean
        get() = _sensor.calibrationInProgress

    val calibrationState : ISensorCalibrate.State
        get() = _sensor.calibrationState

    val calibrationStateOnChange : LiveData<ISensorCalibrate.State>
        get() = _sensor.calibrationStateOnChange

    val calibrationStateMsg : String
        get() = _sensor.calibrationStateMsg

    val calibrationStateMsgOnChange : LiveData<String>
        get() = _sensor.calibrationStateMsgOnChange

    /**
     * Select the sensor
     */
    fun select() {
        device.setSensor(id)
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
        device.sensorReset(SensorData.ResetCommand.RESET_SENSOR)
    }

    /**
     * Reset the sensor to factory defaults
     */
    fun resetFactory() {
        device.sensorReset(SensorData.ResetCommand.RESET_SENSOR_FACTORY)
    }

    /**
     * Set the sensor type
     * @param type SensorData.Sensor.Type
     */
    private fun setSensorType(type: SensorData.Sensor.Type) {
        _sensor = when (type) {
            SensorData.Sensor.Type.VL53L4CD ->
                VL53L4CD(this)
            SensorData.Sensor.Type.VL53L4CX ->
                VL53L4CX(this)
            else -> {
                VL53L4CX(this)
            }
        }
    }

    fun setFilterSampleSize(size: Int) {
        _sampleSize = Integer.max(1, size)
        _filter = CalcFilters.MovingAverage(_sampleSize)
        _filter.reset(_rangeFiltered.value!!)
        _prefs.edit().putInt(_filterTag, _sampleSize).apply()
    }

    /**
     * Grab the configurations for the sensor when it comes online
     *
     * This should be kicked off by sending a GET command for index 0
     * somewhere else in code.
     */
    private fun getConfigs(config : Int){
        // Is sensor configs initialized?
        if(!_isInitialized){
            // Yes - Is config index within range?
            if(_configIdx < _sensor.configs.lastIndex){
                // Yes - Is the config the same as the one we requested?
                if(_configIdx == config){
                    // Yes - Send for the next one
                    setConfigCommand(SensorData.Config.Command.GET, ++_configIdx, Int.MAX_VALUE)
                }
            }
            // No - Finished..
            else{
                _isInitialized = true
            }
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
     * Get a cached configuration index by name.
     * @param name String
     */
    fun getConfigIdxByName(name : String) : Int {
        for (idx in 0.._sensor.configs.lastIndex) {
            if(_sensor.configs[idx].name == name){
                return idx
            }
        }
        return -1
    }

    /**
     * Get a cached configuration by index.
     * @param idx Int
     */
    fun getConfigByIdx(idx : Int) : ISensor.Config {
        return if(idx < _sensor.configs.size){
            _sensor.configs[idx]
        }
        else {
            ISensor.Config("NA", Int.MAX_VALUE)
        }
    }

    /**
     * Get a cached configuration by name.
     * @param name String
     */
    fun getConfigByName(name : String) : ISensor.Config {
        for (idx in 0.._sensor.configs.lastIndex) {
            if(_sensor.configs[idx].name == name){
                return _sensor.configs[idx]
            }
        }
        return ISensor.Config("NA", Int.MAX_VALUE)
    }

    /**
     * Invalidate the LiveData cached value. This can help in
     * the case when adding new observers and not having it trigger
     * on a valid config response.
     */
    fun invalidateLastConfigData() {
        device.sensorData.setConfig(SensorData.Config.Target.SENSOR, SensorData.Config.Command.NA, 0xFF, Int.MAX_VALUE, SensorData.Config.Status.NA)
    }

    /**
     * Send a configuration command
     * @param command
     * @param id
     * @param value
     */
    fun setConfigCommand(command: SensorData.Config.Command, id: Int, value: Int) {
        device.setSensorConfigCommand(SensorData.Config.Target.SENSOR, command, id, value)
    }

    /**
     * Send special command to store configuration data
     * into permanent storage internal to the device
     */
    fun storeConfigData() {
        device.setSensorConfigCommand(SensorData.Config.Target.SENSOR,  SensorData.Config.Command.STORE, 0xFF, Int.MAX_VALUE)
    }

    /**
     * Called when the active sensor changes
     *
     * Super - Sets the sensor type in persistent storage
     *
     * @param sensor SensorData.Sensor
     */
    private fun onSensorUpdate(sensor: SensorData.Sensor) {
        if(_sensor.type != sensor.type){
            setSensorType(sensor.type)
            // Store the current type
            _prefs.edit().putInt(_typeTag, type.ordinal).apply()
        }
    }

    /**
     * Called when sensor ranging enable state changes.
     *
     * Super - None
     *
     * @param enable
     */
    @Suppress("UNUSED_PARAMETER")
    private fun onSensorEnableUpdate(enable : Boolean) {

    }

    /**
     * Called when new range data is received.
     *
     * Super - Sets the filter window size
     *
     * @param range
     */
    private fun onRangeUpdate(range: Int) {
        _rangeFiltered.value = _filter.getAverage(range.toDouble())
        //_rangeFiltered.value = _filter.filter(range.toDouble())
        runCalibration()
    }

    /**
     * Called when there is a configuration response
     * to a configuration command.
     *
     * Super - Stores the configuration response data
     * depending on the status and boot configuration status.
     *
     * @param config
     */
    private fun onConfigUpdate(config : SensorData.Config) {
        // Ignore configurations not intended for the sensor
        if(config.trgt != SensorData.Config.Target.SENSOR) return

        when (config.status) {
            SensorData.Config.Status.OK,
            SensorData.Config.Status.UPDATED,
            SensorData.Config.Status.MISMATCH -> {
                storeConfig(config.id, config.value)
            }
            else -> {}
        }

        // Run config init
        getConfigs(config.id)
        // Run the calibration routine
        runCalibration()
    }

    /**
     * Called when there is a status update from the active sensor.
     *
     * Super - Resets the configuration routine when the sensor is booting
     * and runs the routine when the sensor is in a ready state.
     *
     * @param status
     */
    private fun onStatusUpdate(status : SensorData.Status) {
        when (status) {
            SensorData.Status.READY -> {
                // Leave off where we were in the boot config
                // process if it was interrupted.
                if(!_isInitialized) {
                    setConfigCommand(SensorData.Config.Command.GET, _configIdx, Int.MAX_VALUE)
                }
            }
            SensorData.Status.BOOTING -> {
                _isInitialized = false
                _configIdx = 0
            }
            else -> {}
        }
    }

    /**
     * Called when there is a connection state change from the device
     *
     * Super - Resets the configured flag when disconnected.
     *
     * @param connection
     */
    private fun onConnectionStateUpdate(connection: ConnectionState) {
        when(connection.state){
            ConnectionState.State.DISCONNECTED -> {
                _isInitialized = false
                _configIdx = 0
            }
            else -> {}
        }
    }

    private fun runCalibration() {
        if(_isInitialized && _sensor.calibrationInProgress) {
            _sensor.runCalibration()
        }
    }

    fun startCalibration() {
        device.setSensor(id)
        _sensor.startCalibration()
    }

    fun stopCalibration() {
        _sensor.stopCalibration()
    }

    companion object{
        private const val DEFAULT_SAMPLE_SIZE = 5
    }

    init {
        /**
         * Get the sensor type from storage and initialize the type
         */
        val locTypeInt = _prefs.getInt(_typeTag, 0)
        val locType = if(locTypeInt < SensorData.Sensor.Type.values().size){
            SensorData.Sensor.Type.values()[locTypeInt]
        }
        else{
            SensorData.Sensor.Type.values()[0]
        }
        // Set the type
        setSensorType(locType)

        /**
         * Load the sample window size for the range moving average
         */
        _sampleSize = _prefs.getInt(_filterTag, DEFAULT_SAMPLE_SIZE)

        /**
         * Observe the sensor selected to determine if this sensor is active.
         */
        device.sensorData.sensor.observe(context as LifecycleOwner) { sensor ->
            _isSensorActive = (id == sensor.id)
            if (_isSensorActive) {
                onSensorUpdate(sensor)
            }
        }

        /**
         * Observe the sensor status to determine when to configure the sensors.
         */
        device.sensorData.status.observe(context as LifecycleOwner) { status ->
            if (_isSensorActive) {
                onStatusUpdate(status)
            }
        }

        /**
         * Observe the sensor config to update configs for each sensors and store them
         * when data changes.
         */
        device.sensorData.config.observe(context as LifecycleOwner) { config ->
            if (_isSensorActive) {
                onConfigUpdate(config)
            }
        }

        /**
         * Observe the sensor selected to determine if this sensor is active.
         */
        device.sensorData.enable.observe(context as LifecycleOwner) { enable ->
            _isEnabled = enable
            if (_isSensorActive) {
                onSensorEnableUpdate(enable)
            }
        }

        /**
         * Observe the sensor range data.
         */
        device.sensorData.range.observe(context as LifecycleOwner) { range ->
            if (_isSensorActive) {
                onRangeUpdate(range)
            }
        }

        /**
         * Observe the connection state of the device and
         * set the configuration state to not configured.
         */
        device.state.observe(context as LifecycleOwner) { connection ->
            onConnectionStateUpdate(connection)
        }
    }
}