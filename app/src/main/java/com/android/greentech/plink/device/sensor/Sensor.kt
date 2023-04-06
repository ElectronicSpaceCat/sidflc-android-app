package com.android.greentech.plink.device.sensor

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.android.greentech.plink.device.bluetooth.DeviceBluetoothManager
import com.android.greentech.plink.device.bluetooth.device.DeviceData
import com.android.greentech.plink.utils.calculators.CalcFilters
import no.nordicsemi.android.ble.livedata.state.ConnectionState

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
    private var _filter = CalcFilters.MovingAverage(_sampleSize)
//    private var _filter = CalcFilters.KalmanFilter(0.1, 15.0)
    private var _rangeFiltered = MutableLiveData(0.0)

    var offsetRef : Int = 0

    val sampleSize: Int
        get() = _sampleSize

    val rangeFiltered: LiveData<Double>
        get() = _rangeFiltered

    val isEnabled : Boolean
        get() = _isEnabled

    val isInitialized : Boolean
        get() = _isInitialized

    val type : DeviceData.Sensor.Type
        get() = _sensor.type

    val configs: Array<ISensor.Config>
        get() = _sensor.configs

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
            DeviceData.Sensor.Type.VL53L4CD ->
                VL53L4CD(this)
            DeviceData.Sensor.Type.VL53L4CX ->
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
     * Call this to start the process of loading in configuration data
     */
    fun loadConfigs(){
        if(_isInitialized) return
        setConfigCommand(DeviceData.Config.Command.GET, _configIdx, Int.MAX_VALUE)
    }

    /**
     * Grab the configurations for the sensor when it comes online
     *
     * This should be kicked off by sending a GET command for index 0
     * somewhere else in code.
     */
    private fun getConfigs(config : Int){
        if(_isInitialized) return

        // Is config index within range?
        if(_configIdx < _sensor.configs.lastIndex){
            // Yes - Is the config the same as the one we requested?
            if(_configIdx == config){
                // Yes - Send for the next one
                setConfigCommand(DeviceData.Config.Command.GET, ++_configIdx, Int.MAX_VALUE)
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
        device.deviceData.setConfig(DeviceData.Config.Target.SENSOR, DeviceData.Config.Command.NA, 0xFF, Int.MAX_VALUE, DeviceData.Config.Status.NA)
    }

    /**
     * Send a configuration command
     * @param command
     * @param id
     * @param value
     */
    fun setConfigCommand(command: DeviceData.Config.Command, id: Int, value: Int) {
        device.setSensorConfigCommand(DeviceData.Config.Target.SENSOR, command, id, value)
    }

    /**
     * Send special command to store configuration data
     * into permanent storage internal to the device
     */
    fun storeConfigData() {
        device.setSensorConfigCommand(DeviceData.Config.Target.SENSOR,  DeviceData.Config.Command.STORE, Int.MAX_VALUE, Int.MAX_VALUE)
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
     *
     * Super - Sets the sensor type in persistent storage
     *
     * @param sensor DeviceData.Sensor
     */
    private fun onSensorUpdate(sensor: DeviceData.Sensor) {
        if(this.type != sensor.type) {
            setSensorType(sensor.type)
        }
    }

    /**
     * Called when sensor ranging enable state changes.
     *
     * Super - None
     *
     * @param enable
     */
    private fun onSensorEnableUpdate(enable : Boolean) {
        _isEnabled = enable
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
     *
     * Super - Resets the configuration init data when the sensor is booting
     *
     * @param status
     */
    private fun onStatusUpdate(status : DeviceData.Status) {
        when (status) {
            DeviceData.Status.BOOTING -> {
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

    companion object{
        private const val DEFAULT_SAMPLE_SIZE = 5
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
}