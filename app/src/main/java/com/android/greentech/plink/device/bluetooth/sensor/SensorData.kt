package com.android.greentech.plink.device.bluetooth.sensor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * The data in the class must reference the
 * data from the nRF firmware.
 */
class SensorData {
    // From device
    private val _sensor = MutableLiveData(Sensor(Sensor.Id.NA, Sensor.Type.NA))
    private val _range = MutableLiveData(0xFFFF)
    private val _status = MutableLiveData(Status.NA)
    private val _enable = MutableLiveData(false)
    private val _config = MutableLiveData(Config(Config.Command.NA, 0xFF, Int.MAX_VALUE, Config.Status.INVALID))

    class Sensor(var id: Id, var type: Type){
        enum class Id {
            SHORT,
            LONG,
            NUM_IDS,
            NA
        }

        enum class Type {
            VL6180X,
            VL53L0X,
            VL53L4CD,
            VL53L4CX,
            NUM_TYPES,
            NA
        }
    }

    /**
     * Get/Set for data: _sensor
     */
    val sensor: MutableLiveData<Sensor>
        get() = _sensor

    fun setSensor(sensor: Sensor) {
        _sensor.value = sensor
    }

    /**
     * Get/Set for data: _range
     */
    val range: MutableLiveData<Int>
        get() = _range

    fun setRange(range: Int) {
        _range.value = range
    }

    /**
     * Get/Set for data: _status
     */
    val status: LiveData<Status>
        get() = _status

    fun setStatus(status: Status) {
        _status.value = status
    }

    /**
     * Get/Set for data: _samplingEnabled
     */
    val enable: LiveData<Boolean>
        get() = _enable

    fun setEnable(enable: Boolean) {
        _enable.value = enable
    }

    /**
     * Get/Set for data: _config
     */
    val config: LiveData<Config>
        get() = _config

    fun setConfig(cmd : Config.Command, id: Int, value: Int, status: Config.Status) {
        _config.value = Config(cmd, id, value, status)
    }

    class Config(var cmd : Command, var id: Int, var value: Int, var status: Status){
        enum class Command{
            GET,
            SET,
            RESET,
            STORE,
            NUM_COMMANDS,
            NA
        }

        enum class Status{
            OK,
            UPDATED,
            MISMATCH,
            ERROR,
            INVALID,
            NUM_STATUS,
            NA,
        }
    }

    enum class Status {
        BOOTING,
        READY,
        STANDBY,
        ERROR,
        NUM_STATUS,
        NA
    }

    enum class ResetCommand {
        RESET_DEVICE,
        RESET_SENSOR,
        RESET_SENSOR_FACTORY,
        NUM_OPTIONS,
        NA
    }

    companion object {
        private const val LBS_TOF_SERVICE: String = "1212-EFDE-1523-785FEF13D123"
        /** TOF Service UUID.  */
        val LBS_UUID_TOF_SERVICE: UUID = UUID.fromString("0000F00D-${LBS_TOF_SERVICE}")
        /** TOF_RANGE characteristic UUID.  */
        val LBS_UUID_TOF_RANGE_CHAR: UUID = UUID.fromString("0000BEAA-${LBS_TOF_SERVICE}")
        /** TOF_SELECT characteristic UUID.  */
        val LBS_UUID_TOF_SELECT_CHAR: UUID = UUID.fromString("0000BEAB-${LBS_TOF_SERVICE}")
        /** TOF_OFFSET characteristic UUID.  */
        val LBS_UUID_TOF_CONFIG_CHAR: UUID = UUID.fromString("0000BEAC-${LBS_TOF_SERVICE}")
        /** TOF_STATUS characteristic UUID.  */
        val LBS_UUID_TOF_STATUS_CHAR: UUID = UUID.fromString("0000BEAD-${LBS_TOF_SERVICE}")
        /** TOF_SAMPLE_ENABLE characteristic UUID.  */
        val LBS_UUID_TOF_SAMPLE_ENABLE_CHAR: UUID = UUID.fromString("0000BEAE-${LBS_TOF_SERVICE}")
        /** TOF_RESET characteristic UUID.  */
        val LBS_UUID_TOF_RESET_CHAR: UUID = UUID.fromString("0000BEAF-${LBS_TOF_SERVICE}")
    }
}
