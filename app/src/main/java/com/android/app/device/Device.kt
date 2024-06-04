package com.android.app.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.android.app.R
import com.android.app.device.bluetooth.DeviceBluetoothManager
import com.android.app.device.bluetooth.pwrmonitor.PwrMonitorData
import com.android.app.device.bluetooth.device.DeviceData
import com.android.app.device.model.Model
import com.android.app.device.model.ModelBallistics
import com.android.app.device.model.ModelData
import com.android.app.device.sensor.Sensor
import com.android.app.device.springs.Spring
import no.nordicsemi.android.ble.livedata.state.BondState
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.log.Logger
import java.lang.Float.intBitsToFloat

class Device(context: Context) {
    // External data configurations
    enum class USERDATA{
        FORCE_OFFSET,
        EFFICIENCY,
        FRICTION_COEFFICIENT,
        SPRING_ID
    }

    /** Device model data */
    private var _model : MutableLiveData<ModelData>

    /** BLE Device data */
    private var _bleDeviceManager = DeviceBluetoothManager(context)
    private var _bleDevice: BluetoothDevice ?= null

    private val _prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val _prefsModelIdKey = context.getString(R.string.PREFERENCE_FILTER_DEVICE_MODEL_ID)

    private var _configIdx = 0
    private var _isInitialized = false

    /** Device sensors */
    val sensors : Array<Sensor> = Array(DeviceData.Sensor.Id.NUM_IDS.ordinal) {
        Sensor(context, _bleDeviceManager, DeviceData.Sensor.Id.entries[it])
    }

    /** Associate the sensors to a parameter */
    val sensorCarriagePosition = sensors[DeviceData.Sensor.Id.SHORT.ordinal]
    val sensorDeviceHeight = sensors[DeviceData.Sensor.Id.LONG.ordinal]

    /** Active sensor (default to carriage position sensor) */
    private var _activeSensor = sensorCarriagePosition
    val activeSensor : Sensor
        get() = _activeSensor

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
    var model : ModelData
        get() = _model.value!!
        set(value) {
            // Set the new model
            _model.value = value
            // Set the calibration offset reference for the carriage position sensor
            if(activeSensor.id == DeviceData.Sensor.Id.SHORT){
                activeSensor.targetReference = model.getMaxCarriagePosition().toInt()
                activeSensor.driftCompensationEnable = true
            }
            // Store the model to prefs if new
            val idModel = _prefs.getString(_prefsModelIdKey, Model.Name.V24.name)!!
            if(idModel != _model.value!!.name) {
                _prefs.edit().putString(_prefsModelIdKey, _model.value!!.name).apply()
            }
        }

    val ballistics : ModelBallistics
        get() = model.ballistics

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

    val isBackupBootloader: Boolean
        get() = _bleDeviceManager.isBackupBootloader

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
    fun sendConfigCommand(target : DeviceData.Config.Target, command: DeviceData.Config.Command, id: Int = Int.MAX_VALUE, value: Int = Int.MAX_VALUE) {
        _bleDeviceManager.sendSensorConfigCommand(target, command, id, value)
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
     * Remove bond information on the phone's side
     */
    fun removeBond() {
        if (_bleDevice != null) {
            _bleDeviceManager.removeBond().enqueue()
        }
    }

    /**
     * Connect to the given peripheral
     * (Primarily used for the backup bond-less bootloader)
     *
     * @param target the target device
     */
    @SuppressLint("MissingPermission")
    fun connect(context: Context, target: BluetoothDevice, autoConnect: Boolean) {
        if (_bleDevice == null) {
            _bleDevice = target
            val logSession = Logger.newSession(context, target.address, target.name)
            _bleDeviceManager.setLogger(logSession)
        }

        _bleDeviceManager.connect(_bleDevice!!)
            .retry(3, 100)
            .useAutoConnect(autoConnect)
            .enqueue()
    }

    /**
     * Disconnect from peripheral
     */
    fun disconnect() {
        _bleDevice = null
        _bleDeviceManager.disconnect().enqueue()
    }

    private fun getUserConfigurations(config : Int){
        // Is sensor configs initialized?
        if(!_isInitialized){
            // Yes - Is config index within range?
            if(_configIdx < USERDATA.entries.toTypedArray().lastIndex){
                // Yes - Is the config the same as the one we requested?
                if(_configIdx == config){
                    // Yes - Send for the next one
                    sendConfigCommand(DeviceData.Config.Target.USER, DeviceData.Config.Command.GET, ++_configIdx)
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
        // TODO: This should be stored in the device
        val idModel = _prefs.getString(_prefsModelIdKey, Model.Name.V24.name)!!
        val model = Model.getData(idModel)
        _model = MutableLiveData(model)

        // Set model
        this.model = _model.value!!

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
            if(it.id.ordinal < DeviceData.Sensor.Id.NUM_IDS.ordinal){
                _activeSensor = sensors[it.id.ordinal]
            }
        }

        /**
         * Observe the sensor status.
         * When ready and not initialized, get all device specific stored data.
         */
        sensorStatus.observe(context as LifecycleOwner) {
            when (it) {
                DeviceData.Status.READY -> {
                    if(_isInitialized) return@observe
                    // Send command to get a stored user configuration which will trigger getting the rest
                    sendConfigCommand(DeviceData.Config.Target.USER, DeviceData.Config.Command.GET, _configIdx)
                }
                else -> {}
            }
        }

        /** Observe the configuration data for external storage data */
        sensorConfig.observe(context as LifecycleOwner) {
            if(it.trgt != DeviceData.Config.Target.USER) return@observe

            when (it.status) {
                DeviceData.Config.Status.OK,
                DeviceData.Config.Status.UPDATED,
                DeviceData.Config.Status.MISMATCH -> {
                    when(it.id){
                        USERDATA.FORCE_OFFSET.ordinal -> {
                            this.model.ballistics.forceOffset = intBitsToFloat(it.value).toDouble()
                        }
                        USERDATA.EFFICIENCY.ordinal -> {
                            this.model.ballistics.efficiency = intBitsToFloat(it.value).toDouble()
                        }
                        USERDATA.FRICTION_COEFFICIENT.ordinal -> {
                            this.model.ballistics.frictionCoefficient = intBitsToFloat(it.value).toDouble()
                        }
                        USERDATA.SPRING_ID.ordinal -> {
                            this.model.spring = Spring.getData(it.value)
                        }
                    }
                }
                else -> {}
            }
            // Run config init
            getUserConfigurations(it.id)
        }
    }
}
