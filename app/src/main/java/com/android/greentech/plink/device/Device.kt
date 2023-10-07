package com.android.greentech.plink.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.android.greentech.plink.R
import com.android.greentech.plink.device.bluetooth.DeviceBluetoothManager
import com.android.greentech.plink.device.bluetooth.pwrmonitor.PwrMonitorData
import com.android.greentech.plink.device.bluetooth.device.DeviceData
import com.android.greentech.plink.device.model.Model
import com.android.greentech.plink.device.model.ModelData
import com.android.greentech.plink.device.sensor.Sensor
import com.android.greentech.plink.device.springs.Spring
import kotlinx.coroutines.*
import no.nordicsemi.android.ble.livedata.state.BondState
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.log.Logger
import java.lang.Float.intBitsToFloat

class Device(context: Context) {
    // External data configurations
    enum class EXTDATA{
        FORCE_OFFSET,
        EFFICIENCY,
        FRICTION_COEFFICIENT,
        SPRING_ID
    }

    /**
     * TODO: - Need a clean way of updating spring, projectile, and model when they change.
     *       - I would like the spring, projectile, and model to be stored in a local database
     *         and accessed. This would allow for custom of each.
     *       - The model and default spring id will come from the firmware (set when programmed) but would like
     *         the ability to override the ids if the pcb is placed in a different case or springs are swapped out.
     *
     * NOTE: - Not sure if I should handle math on what happens if two different types of springs are put in the device.
     */
    /** Device model data */
    private var _model : MutableLiveData<ModelData>

    /** Ballistics data */
    private var _ballistics : DeviceBallistics

    /** BLE Device data */
    private var _bleDevice: BluetoothDevice? = null
    private var _bleDeviceManager = DeviceBluetoothManager(context)

    private val _prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private var _configIdx = 0
    private var _isInitialized = false

    /** Device sensors */
    val sensorCarriagePosition = Sensor(context, _bleDeviceManager, DeviceData.Sensor.Id.SHORT)
    val sensorDeviceHeight = Sensor(context, _bleDeviceManager, DeviceData.Sensor.Id.LONG)

    /** Active sensor (default to carriage position sensor) */
    var activeSensor : Sensor = sensorCarriagePosition

    val isInitialized : Boolean
        get() = _isInitialized

    /**
     * Get model as liveData
     */
    val modelOnChange : LiveData<ModelData>
        get() = _model

    /**
     * Set model data and store id to prefs
     */
    var model: ModelData
        get() = _model.value!!
        set(value) {
            // Set the new model
            _model.value = value

            // Set the calibration offset reference for the carrier position sensor
            activeSensor.offsetRef = model.getMaxCarriagePosition().toInt()

            // Store the model to prefs if new
            val idModel = _prefs.getString(model_pref_tag, Model.Name.V23.name)!!
            if(idModel != _model.value!!.name) {
                _prefs.edit().putString(model_pref_tag, _model.value!!.name).apply()
            }
        }

    /** Device Ballistics Data */
    val ballistics: DeviceBallistics
        get() = _ballistics

    /** Device bluetooth information */
    val name: String
        @SuppressLint("MissingPermission")
        get() {
            return if(_bleDeviceManager.bluetoothDevice == null){
                ""
            }
            else{
                _bleDeviceManager.bluetoothDevice!!.name
            }
        }
    val address: String
        get() {
            return if(_bleDeviceManager.bluetoothDevice == null){
                ""
            }
            else{
                _bleDeviceManager.bluetoothDevice!!.address
            }
        }
    val connectionState: LiveData<ConnectionState>
        get() = _bleDeviceManager.state
    val bondingState: LiveData<BondState>
        get() = _bleDeviceManager.bondingState

    /** Device model information */
    val manufacturer: LiveData<String>
        get() = _bleDeviceManager.deviceInfo.manufacturer
    val serial: LiveData<String>
        get() = _bleDeviceManager.deviceInfo.serialNumber
    val versionModel: LiveData<String>
        get() = _bleDeviceManager.deviceInfo.versionModel
    val versionHardware: LiveData<String>
        get() = _bleDeviceManager.deviceInfo.versionHardware

    /** Device software information */
    val versionFirmware: LiveData<String>
        get() = _bleDeviceManager.deviceInfo.versionFirmware
    val versionSoftware: LiveData<String>
        get() = _bleDeviceManager.deviceInfo.versionSoftware

    /** Device power management information */
    val batteryStatus: LiveData<PwrMonitorData.BattStatus>
        get() = _bleDeviceManager.powerMonitor.status
    val pwrSource: LiveData<PwrMonitorData.InputSource>
        get() = _bleDeviceManager.powerMonitor.inputSource
    val batteryLevel: LiveData<Int>
        get() = _bleDeviceManager.powerMonitor.batteryLevel

    /** Device data information */
    val sensorSelected: LiveData<DeviceData.Sensor>
        get() = _bleDeviceManager.deviceData.sensor
    val sensorStatus: LiveData<DeviceData.Status>
        get() = _bleDeviceManager.deviceData.status
    val sensorRangeRaw: LiveData<Int>
        get() = _bleDeviceManager.deviceData.range
    val sensorConfig: LiveData<DeviceData.Config>
        get() = _bleDeviceManager.deviceData.config
    val sensorEnabled: LiveData<Boolean>
        get() = _bleDeviceManager.deviceData.enable

    /**
     * Enable battery level notifications
     */
    fun enableBatteryLevelNotify(enable: Boolean) {
        _bleDeviceManager.enableBatteryLevelNotifications(enable)
    }

    /**
     * Request selected sensor
     *
     * @param id
     */
    fun setSensor(id: DeviceData.Sensor.Id) {
        _bleDeviceManager.setSensor(id)
    }

    /**
     * Enable sensor range sampling

     * @param enable
     */
    fun setSensorEnable(enable: Boolean) {
        _bleDeviceManager.setSensorEnable(enable)
    }

    /**
     * Send a configuration command
     *
     * @param target
     * @param command
     * @param id
     * @param value
     */
    fun sendConfigCommand(target : DeviceData.Config.Target, command: DeviceData.Config.Command, id: Int, value: Int) {
        _bleDeviceManager.setSensorConfigCommand(target, command, id, value)
    }

    /**
     * Send reset command to reset entire device
     * WARNING! this will disconnect the bluetooth connection!
     */
    fun resetDevice() {
        _bleDeviceManager.sensorReset(DeviceData.ResetCommand.RESET_DEVICE)
    }

    /**
     * Bond to the given peripheral
     *
     * Should only call this when a connection is established.
     */
    @SuppressLint("MissingPermission")
    fun ensureBond() {
        if (_bleDevice != null) {
            _bleDeviceManager.ensureBond().enqueue()
        }
    }

    /**
     * Remove bond information
     */
    fun removeBond() {
        if (_bleDevice != null) {
            _bleDeviceManager.removeBond().enqueue()
        }
    }

    @SuppressLint("MissingPermission")
    fun isBootloaderActive(): Boolean {
        return _bleDeviceManager.isBootloader()
    }

    /**
     * Connect to the given peripheral
     * (Primarily used for the backup bond-less bootloader)
     *
     * @param target the target device
     */
    @SuppressLint("MissingPermission")
    fun connect(context: Context, target: BluetoothDevice) {
        if (_bleDevice == null) {
            _bleDevice = target
            val logSession = Logger.newSession(context, null, target.address, target.name)
            _bleDeviceManager.setLogger(logSession)
        }

        _bleDeviceManager.connect(_bleDevice!!)
            .retry(3, 100)
            .useAutoConnect(false)
            .enqueue()
    }

    /**
     * Connect to the given peripheral
     * (Primarily used for auto-bonding)
     *
     * @param target the target device
     */
    @SuppressLint("MissingPermission")
    fun autoConnect(context: Context, target: BluetoothDevice) {
        if (_bleDevice == null) {
            _bleDevice = target
            val logSession = Logger.newSession(context, null, target.address, target.name)
            _bleDeviceManager.setLogger(logSession)
        }

        _bleDeviceManager.connect(_bleDevice!!)
            .useAutoConnect(true)
            .enqueue()
    }

    /**
     * Disconnect from peripheral
     */
    fun disconnect() {
        _bleDevice = null
        if (_bleDeviceManager.isConnected || _bleDeviceManager.isReady) {
            _bleDeviceManager.disconnect().enqueue()
        }
    }

    private fun getConfigs(config : Int){
        // Is sensor configs initialized?
        if(!_isInitialized){
            // Yes - Is config index within range?
            if(_configIdx < EXTDATA.values().lastIndex){
                // Yes - Is the config the same as the one we requested?
                if(_configIdx == config){
                    // Yes - Send for the next one
                    sendConfigCommand(DeviceData.Config.Target.EXT_STORE, DeviceData.Config.Command.GET, ++_configIdx, Int.MAX_VALUE)
                }
            }
            // No - Finished..
            else{
                _isInitialized = true
            }
        }
    }

    init {
        // Load model data from stored preferences
        val idModel = _prefs.getString(model_pref_tag, Model.Name.V24.name)!!
        val model = Model.getData(idModel)
        _model = MutableLiveData(model)

        // NOTE: Currently the selected spring is set in the Preference Settings screen
        val idSpring = _prefs.getString(
            context.getString(R.string.PREFERENCE_FILTER_SPRING_SELECTED),
            _model.value?.defaultSpringName?.name
        )!!
        val spring = Spring.getData(idSpring)
        _model.value!!.setSpring(spring)

        // Set model
        this.model = _model.value!!

        // Set the ballistics manager
        _ballistics = DeviceBallistics(this.model)

        /**
         * Observe the device model data and set the model if not the same
         * This would happen if connecting to another device with a different model.
         */
        versionModel.observe(context as LifecycleOwner) {
            // Don't update model if name is the same
            if(it == this.model.name) return@observe
            // Try to get valid model data for the name given
            val modelData = try {
                Model.getData(Model.Name.valueOf(it))
            }
            catch (e : IllegalArgumentException){
                null
            }
            // Set the model type if valid
            if (modelData != null) {
                this.model = modelData
            }
        }

        /**
         * Observe the device connection state
         */
        connectionState.observe(context as LifecycleOwner) {
            when(it.state){
                ConnectionState.State.DISCONNECTED -> {
                    _isInitialized = false
                    _configIdx = 0
                }
                else -> {}
            }
        }

        /**
         * Observe the selected sensor to update the activeSensor
         */
        sensorSelected.observe(context as LifecycleOwner) {
            when(it.id){
                DeviceData.Sensor.Id.SHORT,
                DeviceData.Sensor.Id.LONG-> {
                    // Switch sensor to selected
                    activeSensor = if(it.id == DeviceData.Sensor.Id.SHORT) {
                        sensorCarriagePosition
                    } else{
                        sensorDeviceHeight
                    }
                }
                else -> {
                    // Do nothing..
                }
            }
        }

        /**
         * Observe the sensor status. When ready,
         * get all device specific stored data.
         */
        sensorStatus.observe(context as LifecycleOwner) {
            when (it) {
                DeviceData.Status.READY -> {
                    if(_isInitialized) return@observe
                    // Send command to get a stored configuration which will trigger getting the rest
                    sendConfigCommand(DeviceData.Config.Target.EXT_STORE, DeviceData.Config.Command.GET, _configIdx, Int.MAX_VALUE)
                }
                else -> {}
            }
        }

        /** Observe the configuration data for external storage data */
        sensorConfig.observe(context as LifecycleOwner) {
            if(it.trgt != DeviceData.Config.Target.EXT_STORE) return@observe

            when (it.status) {
                DeviceData.Config.Status.OK,
                DeviceData.Config.Status.UPDATED,
                DeviceData.Config.Status.MISMATCH -> {
                    when(it.id){
                        EXTDATA.FORCE_OFFSET.ordinal -> {
                            _ballistics.forceOffset = intBitsToFloat(it.value).toDouble()
                        }
                        EXTDATA.EFFICIENCY.ordinal -> {
                            _ballistics.efficiency = intBitsToFloat(it.value).toDouble()
                        }
                        EXTDATA.FRICTION_COEFFICIENT.ordinal -> {
                            _ballistics.frictionCoefficient = intBitsToFloat(it.value).toDouble()
                        }
                    }
                }
                else -> {}
            }

            // Run config init
            getConfigs(it.id)
        }
    }

    companion object {
        private const val model_pref_tag = "model_id"
    }
}
