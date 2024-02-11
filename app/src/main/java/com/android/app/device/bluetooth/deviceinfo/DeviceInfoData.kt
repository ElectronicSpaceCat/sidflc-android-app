package com.android.app.device.bluetooth.deviceinfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import java.util.*

class DeviceInfoData {
    private val _versionModel = MutableLiveData("")
    private val _versionFirmware = MutableLiveData("")
    private val _versionSoftware = MutableLiveData("")
    private val _versionHardware = MutableLiveData("")
    private val _serialNumber = MutableLiveData("")
    private val _manufacturer = MutableLiveData("")

    /**
     * Get/Set for data: _versionFirmware
     */
    val versionFirmware: LiveData<String>
        get() = _versionFirmware

    fun setVersionFirmware(versionFirmware: String) {
        _versionFirmware.value = versionFirmware
    }

    /**
     * Get/Set for data: _versionSoftware
     */
    val versionSoftware: LiveData<String>
        get() = _versionSoftware

    fun setVersionSoftware(versionSoftware: String) {
        _versionSoftware.value = versionSoftware
    }

    /**
     * Get/Set for data: _versionHardware
     */
    val versionHardware: LiveData<String>
        get() = _versionHardware

    fun setVersionHardware(versionHardware: String) {
        _versionHardware.value = versionHardware
    }

    /**
     * Get/Set for data: _serialNumber
     */
    val serialNumber: LiveData<String>
        get() = _serialNumber

    fun setSerialNumber(serialNumber: String) {
        _serialNumber.value = serialNumber
    }

    /**
     * Get/Set for data: _manufacture
     */
    val manufacturer: LiveData<String>
        get() = _manufacturer

    fun setManufacturer(manufacturer: String) {
        _manufacturer.value = manufacturer
    }

    /**
     * Get/Set for data: _model
     */
    val versionModel: LiveData<String>
        get() = _versionModel

    fun setVersionModel(model: String) {
        _versionModel.value = model
    }

    companion object {
        private const val LBS_DI_SERVICE: String = "0000-1000-8000-00805F9B34FB"
        /** DEVICE_INFORMATION Service UUID.  */
        val LBS_UUID_DEVICE_INFORMATION_SERVICE: UUID = UUID.fromString("0000180A-${LBS_DI_SERVICE}")
        /** MODEL_ID characteristic UUID.  */
        val LBS_UUID_MODEL_ID_CHAR: UUID = UUID.fromString("00002A24-${LBS_DI_SERVICE}")
        /** VERSION_SOFTWARE characteristic UUID.  */
        val LBS_UUID_VERSION_SOFTWARE_CHAR: UUID = UUID.fromString("00002A28-${LBS_DI_SERVICE}")
        /** VERSION_FIRMWARE characteristic UUID.  */
        val LBS_UUID_VERSION_FIRMWARE_CHAR: UUID = UUID.fromString("00002A26-${LBS_DI_SERVICE}")
        /** VERSION_HARDWARE characteristic UUID.  */
        val LBS_UUID_VERSION_HARDWARE_CHAR: UUID = UUID.fromString("00002A27-${LBS_DI_SERVICE}")
        /** SERIAL_NUMBER characteristic UUID.  */
        val LBS_UUID_SERIAL_NUMBER_CHAR: UUID = UUID.fromString("00002A25-${LBS_DI_SERVICE}")
        /** MANUFACTURER characteristic UUID.  */
        val LBS_UUID_MANUFACTURER_CHAR: UUID = UUID.fromString("00002A29-${LBS_DI_SERVICE}")
    }
}