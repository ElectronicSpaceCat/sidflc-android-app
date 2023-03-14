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
import com.android.greentech.plink.device.bluetooth.sensor.SensorData
import com.android.greentech.plink.device.model.Model
import com.android.greentech.plink.device.model.ModelData
import com.android.greentech.plink.device.sensor.Sensor
import com.android.greentech.plink.device.springs.Spring
import no.nordicsemi.android.ble.livedata.state.BondState
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.log.Logger

class Device(context: Context) {
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

    /** Device Sensor array */
    val sensors = arrayOf(
        Sensor(context, _bleDeviceManager, SensorData.Sensor.Id.SHORT),
        Sensor(context, _bleDeviceManager, SensorData.Sensor.Id.LONG)
    )

    /** Provide sensor accessors by name */
    val sensorCarriagePosition = sensors[SensorData.Sensor.Id.SHORT.ordinal]
    val sensorDeviceHeight = sensors[SensorData.Sensor.Id.LONG.ordinal]

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
            sensors[SensorData.Sensor.Id.SHORT.ordinal].offsetRef = model.getMaxCarriagePosition().toInt()

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

    /** Device sensor information */
    val sensorSelected: LiveData<SensorData.Sensor>
        get() = _bleDeviceManager.sensorData.sensor
    val sensorStatus: LiveData<SensorData.Status>
        get() = _bleDeviceManager.sensorData.status
    val sensorRangeRaw: LiveData<Int>
        get() = _bleDeviceManager.sensorData.range
    val sensorConfig: LiveData<SensorData.Config>
        get() = _bleDeviceManager.sensorData.config
    val sensorEnabled: LiveData<Boolean>
        get() = _bleDeviceManager.sensorData.enable

    /**
     * Enable battery level notifications
     */
    fun enableBatteryLevelNotify(enable: Boolean) {
        _bleDeviceManager.enableBatteryLevelNotifications(enable)
    }

    /**
     * Get the active sensor
     *
     * @return SensorData.Sensor.Id or null
     */
    fun getActiveSensor() : Sensor? {
        return if(sensorSelected.value!!.id.ordinal < SensorData.Sensor.Id.NUM_IDS.ordinal){
            sensors[sensorSelected.value!!.id.ordinal]
        } else{
            null
        }
    }

    /**
     * Request selected sensor
     *
     * @param id
     */
    fun setSensor(id: SensorData.Sensor.Id) {
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
    fun sendConfigCommand(target : SensorData.Config.Target, command: SensorData.Config.Command, id: Int, value: Int) {
        _bleDeviceManager.setSensorConfigCommand(target, command, id, value)
    }

    /**
     * Send reset command to reset entire device
     * WARNING! this will disconnect the bluetooth connection!
     */
    fun resetDevice() {
        _bleDeviceManager.sensorReset(SensorData.ResetCommand.RESET_DEVICE)
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

    /**
     * Connect to the given peripheral
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
        reconnect()
    }

    /**
     * Connect to the given peripheral
     * (Primarily for use by the auto-bonding)
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
        autoConnect()
    }

    /**
     * Reconnects to previously connected device
     * If this device was not supported, its services were cleared on disconnection, so
     * reconnection may help
     */
    private fun autoConnect() {
        if (_bleDevice != null) {
            _bleDeviceManager.connect(_bleDevice!!)
                .useAutoConnect(true)
                .enqueue()
        }
    }

    /**
     * Reconnects to previously connected device
     * If this device was not supported, its services were cleared on disconnection, so
     * reconnection may help
     */
    private fun reconnect() {
        if (_bleDevice != null) {
            _bleDeviceManager.connect(_bleDevice!!)
                .retry(3, 100)
                .useAutoConnect(false)
                .enqueue()
        }
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
        _ballistics = DeviceBallistics(context, this.model)

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
    }

    companion object {
        private const val model_pref_tag = "model_id"
    }
}
