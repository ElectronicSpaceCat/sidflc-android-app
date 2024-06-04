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
package com.android.app.device.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.android.app.device.bluetooth.device.DeviceData
import com.android.app.device.bluetooth.device.DeviceData.Companion.LBS_UUID_TOF_CONFIG_CHAR
import com.android.app.device.bluetooth.device.DeviceData.Companion.LBS_UUID_TOF_RANGE_CHAR
import com.android.app.device.bluetooth.device.DeviceData.Companion.LBS_UUID_TOF_RESET_CHAR
import com.android.app.device.bluetooth.device.DeviceData.Companion.LBS_UUID_TOF_SAMPLE_ENABLE_CHAR
import com.android.app.device.bluetooth.device.DeviceData.Companion.LBS_UUID_TOF_SELECT_CHAR
import com.android.app.device.bluetooth.device.DeviceData.Companion.LBS_UUID_TOF_SERVICE
import com.android.app.device.bluetooth.device.DeviceData.Companion.LBS_UUID_TOF_STATUS_CHAR
import com.android.app.device.bluetooth.device.callbacks.ConfigDataCmdCallback
import com.android.app.device.bluetooth.device.callbacks.ResetDataCmdCallback
import com.android.app.device.bluetooth.device.callbacks.SensorRangeDataCallback
import com.android.app.device.bluetooth.device.callbacks.SensorRangeEnableDataCallback
import com.android.app.device.bluetooth.device.callbacks.SensorSelectDataCallback
import com.android.app.device.bluetooth.device.callbacks.SensorStatusDataCallback
import com.android.app.device.bluetooth.deviceinfo.DeviceInfoData
import com.android.app.device.bluetooth.deviceinfo.DeviceInfoData.Companion.LBS_UUID_DEVICE_INFORMATION_SERVICE
import com.android.app.device.bluetooth.deviceinfo.DeviceInfoData.Companion.LBS_UUID_MANUFACTURER_CHAR
import com.android.app.device.bluetooth.deviceinfo.DeviceInfoData.Companion.LBS_UUID_MODEL_ID_CHAR
import com.android.app.device.bluetooth.deviceinfo.DeviceInfoData.Companion.LBS_UUID_SERIAL_NUMBER_CHAR
import com.android.app.device.bluetooth.deviceinfo.DeviceInfoData.Companion.LBS_UUID_VERSION_FIRMWARE_CHAR
import com.android.app.device.bluetooth.deviceinfo.DeviceInfoData.Companion.LBS_UUID_VERSION_HARDWARE_CHAR
import com.android.app.device.bluetooth.deviceinfo.DeviceInfoData.Companion.LBS_UUID_VERSION_SOFTWARE_CHAR
import com.android.app.device.bluetooth.deviceinfo.callbacks.FirmwareVersionDataCallback
import com.android.app.device.bluetooth.deviceinfo.callbacks.HardwareVersionDataCallback
import com.android.app.device.bluetooth.deviceinfo.callbacks.ManufacturerDataCallback
import com.android.app.device.bluetooth.deviceinfo.callbacks.ModelDataCallback
import com.android.app.device.bluetooth.deviceinfo.callbacks.SerialNumberDataCallback
import com.android.app.device.bluetooth.deviceinfo.callbacks.SoftwareVersionDataCallback
import com.android.app.device.bluetooth.dfu.DfuData.Companion.LBS_UUID_DFU_SERVICE
import com.android.app.device.bluetooth.pwrmonitor.PwrMonitorData
import com.android.app.device.bluetooth.pwrmonitor.PwrMonitorData.Companion.LBS_UUID_PWR_BATT_LEVEL_CHAR
import com.android.app.device.bluetooth.pwrmonitor.PwrMonitorData.Companion.LBS_UUID_PWR_BATT_STATUS_CHAR
import com.android.app.device.bluetooth.pwrmonitor.PwrMonitorData.Companion.LBS_UUID_PWR_SERVICE
import com.android.app.device.bluetooth.pwrmonitor.PwrMonitorData.Companion.LBS_UUID_PWR_SOURCE_CHAR
import com.android.app.device.bluetooth.pwrmonitor.callbacks.PwrBattStatusDataCallback
import com.android.app.device.bluetooth.pwrmonitor.callbacks.PwrBatteryLevelDataCallback
import com.android.app.device.bluetooth.pwrmonitor.callbacks.PwrInputSourceDataCallback
import com.android.app.utils.misc.Utils
import no.nordicsemi.android.ble.Request
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.ObservableBleManager
import no.nordicsemi.android.log.LogContract
import no.nordicsemi.android.log.LogSession
import no.nordicsemi.android.log.Logger

class DeviceBluetoothManager(context: Context) : ObservableBleManager(context) {
    // Characteristics - Device Information
    private var softwareVersionCharacteristic: BluetoothGattCharacteristic? = null
    private var firmwareVersionCharacteristic: BluetoothGattCharacteristic? = null
    private var hardwareVersionCharacteristic: BluetoothGattCharacteristic? = null
    private var serialNumberCharacteristic: BluetoothGattCharacteristic? = null
    private var manufacturerCharacteristic: BluetoothGattCharacteristic? = null
    private var modelCharacteristic: BluetoothGattCharacteristic? = null

    // Characteristics - ToF Power Monitor
    private var tofPowerSourceCharacteristic: BluetoothGattCharacteristic? = null
    private var tofBatteryStatusCharacteristic: BluetoothGattCharacteristic? = null
    private var tofBatteryLevelCharacteristic: BluetoothGattCharacteristic? = null

    // Characteristics - ToF Sensors
    private var tofRangeCharacteristic: BluetoothGattCharacteristic? = null
    private var tofSelectCharacteristic: BluetoothGattCharacteristic? = null
    private var tofConfigCharacteristic: BluetoothGattCharacteristic? = null
    private var tofStatusCharacteristic: BluetoothGattCharacteristic? = null
    private var tofSampleEnableCharacteristic: BluetoothGattCharacteristic? = null
    private var tofResetCharacteristic: BluetoothGattCharacteristic? = null

    private val _deviceInfo = DeviceInfoData()
    private val _pwrMonitorData = PwrMonitorData()
    private val _tofSensor = DeviceData()

    private var _logSession: LogSession? = null

    // Flags used to determine when device cache should be cleared on disconnect
    private var _supported = false

    private var _isBackupBootloader = false

    /**
     * Make the ensureBond method public
     */
    public override fun ensureBond(): Request {
        return super.ensureBond()
    }

    /**
     * Make the removeBond method public
     */
    public override fun removeBond(): Request {
        return super.removeBond()
    }

    override fun close() {
        super.cancelQueue()
    }

    /**
     * Sets the log session to be used for low level logging.
     * @param session the session, or null, if nRF Logger is not installed.
     */
    fun setLogger(session: LogSession?) {
        _logSession = session
    }

    override fun log(priority: Int, message: String) {
        // The priority is a Log.X constant, while the Logger accepts it's log levels.
        Logger.log(_logSession, LogContract.Log.Level.fromPriority(priority), message)
    }

    /**
     * Clear device cache on disconnect
     */
    override fun shouldClearCacheWhenDisconnected(): Boolean {
        return (!_supported || _isBackupBootloader)
    }

    /**
     * Overrides initialize
     */
    override fun initialize() {
        if (!isConnected) {
            log(Log.WARN, "ToF Device not connected")
            return
        }

        // NOTE: This is necessary to prevent backup
        //       bootloader connection from hanging.
        //       Basically bypasses discovering non-existing services..
        if(_isBackupBootloader){
            return
        }

        /**************************
         * ToF
         */
        // ToF Select
        readCharacteristic(tofSelectCharacteristic)
            .with(sensorSelectCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "ToF Select characteristic not found")
            }
            .enqueue()
        setIndicationCallback(tofSelectCharacteristic).with(sensorSelectCallback)
        enableIndications(tofSelectCharacteristic, true, "ToF Select")

        // ToF Status
        readCharacteristic(tofStatusCharacteristic)
            .with(sensorStatusCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "ToF status characteristic not found")
            }
            .enqueue()
        setIndicationCallback(tofStatusCharacteristic).with(sensorStatusCallback)
        enableIndications(tofStatusCharacteristic, true, "ToF Status")

        // ToF Config
        setIndicationCallback(tofConfigCharacteristic).with(sensorConfigCallback)
        enableIndications(tofConfigCharacteristic, true, "ToF Config")

        // ToF Ranging Enable
        readCharacteristic(tofSampleEnableCharacteristic)
            .with(sensorSampleEnableCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "ToF Enable characteristic not found")
            }
            .enqueue()
        setIndicationCallback(tofSampleEnableCharacteristic).with(sensorSampleEnableCallback)
        enableIndications(tofSampleEnableCharacteristic, true, "ToF Enable")

        // ToF Range Data
        setNotificationCallback(tofRangeCharacteristic).with(rangeCallback)
        enableNotification(tofRangeCharacteristic, true, "ToF Range Data")

        // ToF Reset
        readCharacteristic(tofResetCharacteristic)
            .with(sensorResetCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "ToF Reset characteristic not found")
            }
            .enqueue()
        setIndicationCallback(tofResetCharacteristic).with(sensorResetCallback)
        enableIndications(tofResetCharacteristic, true, "ToF Reset")

        /**************************
         * Power Monitor
         */
        // Power source characteristics setup
        readCharacteristic(tofPowerSourceCharacteristic)
            .with(pwrInputSourceCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "Power source characteristic not found")
            }
            .enqueue()
        setNotificationCallback(tofPowerSourceCharacteristic).with(pwrInputSourceCallback)
        enableNotification(tofPowerSourceCharacteristic, true, "Power source")

        // Battery status characteristics setup
        readCharacteristic(tofBatteryStatusCharacteristic)
            .with(pwrBattStatusCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "Battery status characteristic not found")
            }
            .enqueue()
        setNotificationCallback(tofBatteryStatusCharacteristic).with(pwrBattStatusCallback)
        enableNotification(tofBatteryStatusCharacteristic, true, "Battery status")

        // Battery level characteristics setup
        readCharacteristic(tofBatteryLevelCharacteristic)
            .with(pwrBatteryLevelCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "Battery level characteristic not found")
            }
            .enqueue()
        setNotificationCallback(tofBatteryLevelCharacteristic).with(pwrBatteryLevelCallback)
        enableNotification(tofBatteryLevelCharacteristic, false, "Battery Level")

        /**************************
         * Device Information
         */
        readCharacteristic(softwareVersionCharacteristic)
            .with(softwareVersionCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "Software version characteristic not found")
            }
            .enqueue()
        readCharacteristic(firmwareVersionCharacteristic)
            .with(firmwareVersionCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "Firmware version characteristic not found")
            }
            .enqueue()
        readCharacteristic(hardwareVersionCharacteristic)
            .with(hardwareVersionCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "Hardware version characteristic not found")
            }
            .enqueue()
        readCharacteristic(serialNumberCharacteristic)
            .with(serialNumberCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "Serial Number characteristic not found")
            }
            .enqueue()
        readCharacteristic(manufacturerCharacteristic)
            .with(manufacturerCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "Manufacturer characteristic not found")
            }
            .enqueue()
        readCharacteristic(modelCharacteristic)
            .with(modelCallback)
            .fail { _: BluetoothDevice?, _: Int ->
                log(Log.WARN, "Model characteristic not found")
            }
            .enqueue()
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        // Check if in bootloader, there should only be 3 services:
        // 1) Generic Access    (standard)
        // 2) Generic Attribute (standard)
        // 3) DFU
        _isBackupBootloader = gatt.services.size == 3 && null != gatt.getService(LBS_UUID_DFU_SERVICE)
        if(_isBackupBootloader) {
            return true
        }

        // ToF Service
        var service = gatt.getService(LBS_UUID_TOF_SERVICE)
        if (service != null) {
            // Characteristics - ToF
            tofRangeCharacteristic = service.getCharacteristic(LBS_UUID_TOF_RANGE_CHAR)
            tofSelectCharacteristic = service.getCharacteristic(LBS_UUID_TOF_SELECT_CHAR)
            tofConfigCharacteristic = service.getCharacteristic(LBS_UUID_TOF_CONFIG_CHAR)
            tofStatusCharacteristic = service.getCharacteristic(LBS_UUID_TOF_STATUS_CHAR)
            tofSampleEnableCharacteristic = service.getCharacteristic(LBS_UUID_TOF_SAMPLE_ENABLE_CHAR)
            tofResetCharacteristic = service.getCharacteristic(LBS_UUID_TOF_RESET_CHAR)
        }

        // Pwr Service
        service = gatt.getService(LBS_UUID_PWR_SERVICE)
        if (service != null) {
            // Characteristics - Pwr Monitor
            tofPowerSourceCharacteristic = service.getCharacteristic(LBS_UUID_PWR_SOURCE_CHAR)
            tofBatteryStatusCharacteristic = service.getCharacteristic(LBS_UUID_PWR_BATT_STATUS_CHAR)
            tofBatteryLevelCharacteristic = service.getCharacteristic(LBS_UUID_PWR_BATT_LEVEL_CHAR)
        }

        // Check if characteristics exists
        _supported =
          //  serviceChanged != null &&
            tofRangeCharacteristic != null &&
            tofSelectCharacteristic != null &&
            tofConfigCharacteristic != null &&
            tofStatusCharacteristic != null &&
            tofSampleEnableCharacteristic != null &&
            tofResetCharacteristic != null &&
            tofPowerSourceCharacteristic != null &&
            tofBatteryStatusCharacteristic != null &&
            tofBatteryLevelCharacteristic != null

        // Good so far?
        var writeRequest = false
        if (_supported) {
            // Yes - Check write permissions of each writable characteristic
            var rxProperties = tofSelectCharacteristic!!.properties
            writeRequest = rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
            if (writeRequest) {
                rxProperties = tofConfigCharacteristic!!.properties
                writeRequest = rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
            }
            if (writeRequest) {
                rxProperties = tofSampleEnableCharacteristic!!.properties
                writeRequest = rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
            }
            if (writeRequest) {
                rxProperties = tofResetCharacteristic!!.properties
                writeRequest = rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
            }
        }

        // Return True if device has supported services for the app OR is in the bootloader
        // The _supported flag is use to determine if the cached services should be cleared on disconnect
        // which it should if connected to the bootloader or app services are not supported.
        return (_supported && writeRequest)
    }

    override fun isOptionalServiceSupported(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(LBS_UUID_DEVICE_INFORMATION_SERVICE)
        if (service != null) {
            // Characteristics - Device Information
            softwareVersionCharacteristic = service.getCharacteristic(LBS_UUID_VERSION_SOFTWARE_CHAR)
            firmwareVersionCharacteristic = service.getCharacteristic(LBS_UUID_VERSION_FIRMWARE_CHAR)
            hardwareVersionCharacteristic = service.getCharacteristic(LBS_UUID_VERSION_HARDWARE_CHAR)
            serialNumberCharacteristic = service.getCharacteristic(LBS_UUID_SERIAL_NUMBER_CHAR)
            manufacturerCharacteristic = service.getCharacteristic(LBS_UUID_MANUFACTURER_CHAR)
            modelCharacteristic = service.getCharacteristic(LBS_UUID_MODEL_ID_CHAR)
        }
        return softwareVersionCharacteristic != null
                && firmwareVersionCharacteristic != null
                && hardwareVersionCharacteristic != null
                && serialNumberCharacteristic != null
                && manufacturerCharacteristic != null
                && modelCharacteristic != null
    }

    override fun onServicesInvalidated() {
        // Nullify characteristics - ToF
        tofRangeCharacteristic = null
        tofSelectCharacteristic = null
        tofConfigCharacteristic = null
        tofStatusCharacteristic = null
        tofSampleEnableCharacteristic = null
        tofResetCharacteristic = null
        // Nullify characteristics - Device Information
        softwareVersionCharacteristic = null
        firmwareVersionCharacteristic = null
        hardwareVersionCharacteristic = null
        serialNumberCharacteristic = null
        manufacturerCharacteristic = null
        modelCharacteristic = null
        // Characteristics - ToF Power Monitor
        tofPowerSourceCharacteristic = null
        tofBatteryStatusCharacteristic = null
        tofBatteryLevelCharacteristic = null
    }

    /**
     * TOF SENSOR CALLBACKS
     */
    private val sensorStatusCallback: SensorStatusDataCallback =
        object : SensorStatusDataCallback() {
            override fun onSensorStatusChanged(device: BluetoothDevice, status: Int) {
                log(
                    Log.VERBOSE, "ToF: " + _tofSensor.sensor.toString() + " Status set to: " + status
                )

                val locStatus = if(status < DeviceData.Status.entries.size){
                    DeviceData.Status.entries[status]
                } else{
                    DeviceData.Status.BOOTING
                }

                _tofSensor.setStatus(locStatus)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val rangeCallback: SensorRangeDataCallback = object : SensorRangeDataCallback() {
        override fun onRangeDataChanged(device: BluetoothDevice, range: Int) {
            log(
                Log.VERBOSE, "ToF: " + _tofSensor.sensor.toString() + " range: " + range
            )
            _tofSensor.setRange(range)
        }

        override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
            log(Log.WARN, "Invalid data received: $data")
        }
    }

    private val sensorSelectCallback: SensorSelectDataCallback =
        object : SensorSelectDataCallback() {
            override fun onSensorSelectChanged(device: BluetoothDevice, id: Int, type: Int) {
                log(
                    Log.VERBOSE, "ToF: " + _tofSensor.sensor.toString() + " id: " + id + " " + "type: " + type
                )

                val locId = if(id < DeviceData.Sensor.Id.entries.size){
                    DeviceData.Sensor.Id.entries[id]
                } else{
                    DeviceData.Sensor.Id.NA
                }

                val locType = if(type < DeviceData.Sensor.Type.entries.size){
                    DeviceData.Sensor.Type.entries[type]
                } else{
                    DeviceData.Sensor.Type.NA
                }

                _tofSensor.setSensor(DeviceData.Sensor(locId, locType))
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val sensorConfigCallback: ConfigDataCmdCallback =
        object : ConfigDataCmdCallback() {
            override fun onSensorConfigChanged(device: BluetoothDevice, trgt : Int, cmd : Int, id: Int, value: Int, status: Int) {
                log(
                    Log.VERBOSE,"ToF: " + _tofSensor.sensor.toString() + "trgt" + trgt + "cmd" + cmd +" id: " + id + " status: " + status +" value: " + value
                )

                val configTrgt = if(trgt < DeviceData.Config.Target.entries.size){
                    DeviceData.Config.Target.entries[trgt]
                } else{
                    DeviceData.Config.Target.NA
                }

                val configCmd = if(cmd < DeviceData.Config.Command.entries.size){
                    DeviceData.Config.Command.entries[cmd]
                } else{
                    DeviceData.Config.Command.NA
                }

                val configStatus = if(status < DeviceData.Config.Status.entries.size){
                    DeviceData.Config.Status.entries[status]
                } else{
                    DeviceData.Config.Status.INVALID
                }

                _tofSensor.setConfig(configTrgt, configCmd, id, value, configStatus)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val sensorSampleEnableCallback: SensorRangeEnableDataCallback =
        object : SensorRangeEnableDataCallback() {
            override fun onSampleEnableChanged(device: BluetoothDevice, enable: Boolean) {
                log(
                    Log.VERBOSE,"ToF: " + _tofSensor.sensor.toString() + " Enable set to: " + enable
                )
                _tofSensor.setEnable(enable)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val sensorResetCallback: ResetDataCmdCallback =
        object : ResetDataCmdCallback() {
            override fun onResetChanged(device: BluetoothDevice, command: Int) {
                log(
                    Log.VERBOSE,"ToF: " + _tofSensor.sensor.toString() + " reset: " + command
                )
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    /**
     * PWR MONITOR CALLBACKS
     */
    private val pwrInputSourceCallback: DataReceivedCallback =
        object : PwrInputSourceDataCallback() {
            override fun onPwrInputSourceChanged(device: BluetoothDevice, inputSource: Int) {
                log(Log.VERBOSE, "Pwr Input power source set to: $inputSource")

                val lInputSource = if(inputSource < PwrMonitorData.InputSource.entries.size){
                    PwrMonitorData.InputSource.entries[inputSource]
                } else{
                    PwrMonitorData.InputSource.NA
                }

                _pwrMonitorData.setInputSource(lInputSource)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val pwrBattStatusCallback: DataReceivedCallback =
        object : PwrBattStatusDataCallback() {
            override fun onPwrBattStatusChanged(device: BluetoothDevice, battStatus: Int) {
                log(Log.VERBOSE, "Pwr Battery status set to: $battStatus")

                val lBattStatus = if(battStatus < PwrMonitorData.BattStatus.entries.size){
                    PwrMonitorData.BattStatus.entries[battStatus]
                } else{
                    PwrMonitorData.BattStatus.UNKNOWN
                }

                _pwrMonitorData.setStatus(lBattStatus)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val pwrBatteryLevelCallback: DataReceivedCallback =
        object : PwrBatteryLevelDataCallback() {
            override fun onPwrBatteryLevelChanged(device: BluetoothDevice, batteryLevel: Int) {
                log(Log.VERBOSE, "Pwr Battery Level received: $batteryLevel%")
                _pwrMonitorData.setBatteryLevel(batteryLevel)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    /**
     * DEVICE INFORMATION CALLBACKS
     */
    private val firmwareVersionCallback: DataReceivedCallback =
        object : FirmwareVersionDataCallback() {
            override fun onFirmwareVersionChanged(device: BluetoothDevice, firmwareVersion: String?) {
                log(Log.VERBOSE, "Firmware version set to: $firmwareVersion")
                _deviceInfo.setVersionFirmware(firmwareVersion!!)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val softwareVersionCallback: DataReceivedCallback =
        object : SoftwareVersionDataCallback() {
            override fun onSoftwareVersionChanged(device: BluetoothDevice, softwareVersion: String?) {
                log(Log.VERBOSE, "Software version set to: $softwareVersion")
                _deviceInfo.setVersionSoftware(softwareVersion!!)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val hardwareVersionCallback: DataReceivedCallback =
        object : HardwareVersionDataCallback() {
            override fun onHardwareVersionChanged(device: BluetoothDevice, hardwareVersion: String?) {
                log(Log.VERBOSE, "Hardware version set to: $hardwareVersion")
                _deviceInfo.setVersionHardware(hardwareVersion!!)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val modelCallback: DataReceivedCallback = object : ModelDataCallback() {
        override fun onModelChanged(device: BluetoothDevice, model: String?) {
            log(Log.VERBOSE, "Model set to: $model")
            _deviceInfo.setVersionModel(model!!)
        }

        override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
            // Data can only invalid if we read them. We assume the app always sends correct data.
            log(Log.WARN, "Invalid data received: $data")
        }
    }

    private val serialNumberCallback: DataReceivedCallback =
        object : SerialNumberDataCallback() {
            override fun onSerialNumberChanged(device: BluetoothDevice, serialNumber: String?) {
                log(Log.VERBOSE, "Serial Number set to: $serialNumber")
                _deviceInfo.setSerialNumber(serialNumber!!)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    private val manufacturerCallback: DataReceivedCallback =
        object : ManufacturerDataCallback() {
            override fun onManufacturerChanged(device: BluetoothDevice, manufacturer: String?) {
                log(Log.VERBOSE, "Manufacturer set to: $manufacturer")
                _deviceInfo.setManufacturer(manufacturer!!)
            }

            override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
                // Data can only invalid if we read them. We assume the app always sends correct data.
                log(Log.WARN, "Invalid data received: $data")
            }
        }

    /**
     * Enable/Disable characteristic notifications
     */
    private fun enableNotification(characteristic: BluetoothGattCharacteristic?, enable : Boolean, name: String? = null) {
        var charName = name
        if(charName == null && characteristic != null) {
            charName = characteristic.uuid.toString()
        }
        if(enable){
            enableNotifications(characteristic)
                .done {
                    log(Log.INFO, "$charName : notifications enabled")
                }
                .fail { _: BluetoothDevice?, _: Int ->
                    log(Log.WARN, "$characteristic not found")
                }
                .enqueue()
        }
        else{
            disableNotifications(characteristic)
                .done {
                    log(Log.INFO, "$charName : notifications disabled")
                }
                .fail { _: BluetoothDevice?, _: Int ->
                    log(Log.WARN, "$characteristic not found")
                }
                .enqueue()
        }
    }

    /**
     * Enable/Disable characteristic indications
     */
    private fun enableIndications(characteristic: BluetoothGattCharacteristic?, enable : Boolean, name: String? = null) {
        var charName = name
        if(charName == null && characteristic != null) {
            charName = characteristic.uuid.toString()
        }
        if(enable){
            enableIndications(characteristic)
                .done {
                    log(Log.INFO, "$charName : indications enabled")
                }
                .fail { _: BluetoothDevice?, _: Int ->
                    log(Log.WARN, "$characteristic not found")
                }
                .enqueue()
        }
        else{
            disableIndications(characteristic)
                .done {
                    log(Log.INFO, "$charName : indications disabled")
                }
                .fail { _: BluetoothDevice?, _: Int ->
                    log(Log.WARN, "$characteristic not found")
                }
                .enqueue()
        }
    }

    /**
     * Select a sensor.
     *
     * @param id of the sensor
     */
    fun setSensor(id : DeviceData.Sensor.Id) {
        // Does characteristic exist?
        if (tofSelectCharacteristic == null) return

        // No need to change?
        if (_tofSensor.sensor.value!!.id == id) return
        log(Log.VERBOSE, "Request sensor: ".plus(id.ordinal.toString()))

        writeCharacteristic(
            tofSelectCharacteristic,
            byteArrayOf(id.ordinal.toByte()),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    /**
     * Sends a request to the device to enable/disable the ranging function.
     *
     * @param sampleEnabled true to turn the sampling on, false to turn sampling off.
     */
    fun setSensorEnable(sampleEnabled: Boolean) {
        // Does characteristic exist?
        if (tofSampleEnableCharacteristic == null) return

        // No need to change?
        if(_tofSensor.enable.value!! == sampleEnabled) return
        log(Log.VERBOSE, "ToF: " + _tofSensor.sensor.toString() + " sampleEnable requested: " + sampleEnabled)

        writeCharacteristic(
            tofSampleEnableCharacteristic,
            if (sampleEnabled) byteArrayOf(0x01) else byteArrayOf(0x00),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    /**
     * Sends a request to the device to set a configuration.
     * @param target
     * @param command
     * @param id for the sensor.
     * @param value for the sensor.
     */
    fun sendSensorConfigCommand(target: DeviceData.Config.Target, command: DeviceData.Config.Command, id: Int = Int.MAX_VALUE, value: Int = Int.MAX_VALUE) {
        // Does characteristic exist?
        if (tofConfigCharacteristic == null) return

        log(Log.VERBOSE, "ToF: " + _tofSensor.sensor.toString() + "target" + target + "cmd" + command + "config: " + id + " value: " + value)

        val data = ByteArray(7)

        // Byte 0 is the config target
        data[0] = target.ordinal.toByte()
        // Byte 1 is the config command
        data[1] = command.ordinal.toByte()
        // Byte 2 is the config id
        data[2] = id.toByte()
        // Bytes 3-6 is the config value
        Utils.intToBytes(data, 3, value)

        writeCharacteristic(
            tofConfigCharacteristic,
            data,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    /**
     * Send reset command
     *
     * @param option
     */
    fun sensorReset(option: DeviceData.ResetCommand) {
        // Does characteristic exist?
        if (tofResetCharacteristic == null) return

        // No need to change?
        log(Log.VERBOSE, "ToF: " + _tofSensor.sensor.toString() + " reset requested: " + option.ordinal)

        writeCharacteristic(
            tofResetCharacteristic,
            byteArrayOf(option.ordinal.toByte()),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).enqueue()
    }

    /**
     * Enable/Disable the Battery Level characteristic notifications
     */
    fun enableBatteryLevelNotifications(enable : Boolean) {
        enableNotification(tofBatteryLevelCharacteristic, enable, "Batter Level")
    }

    /**
     * Get bootloader status
     */
    val isBackupBootloader : Boolean
        get() = _isBackupBootloader

    /**
     * Get device sensor data
     */
    val deviceData : DeviceData
        get() = _tofSensor

    /**
     * Get device info data
     */
    val deviceInfo : DeviceInfoData
        get() = _deviceInfo

    /**
     * Get device power monitor data
     */
    val powerMonitor : PwrMonitorData
        get() = _pwrMonitorData
}