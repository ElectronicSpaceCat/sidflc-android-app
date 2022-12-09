package com.android.greentech.plink.device.bluetooth.pwrmonitor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import java.util.*

class PwrMonitorData {
    private val _inputSource = MutableLiveData(InputSource.NA)
    private val _status = MutableLiveData(BattStatus.UNKNOWN)
    private val _batteryLevel = MutableLiveData(0)

    /**
     * Get/Set for data: _inputSource
     */
    val inputSource: LiveData<InputSource>
        get() = _inputSource

    fun setInputSource(inputSource: InputSource) {
        _inputSource.value = inputSource
    }

    /**
     * Get/Set for data: _status
     */
    val status: LiveData<BattStatus>
        get() = _status

    fun setStatus(status: BattStatus) {
        _status.value = status
    }

    /**
     * Get/Set for data: _level
     */
    val batteryLevel: LiveData<Int>
        get() = _batteryLevel

    fun setBatteryLevel(batteryLevel: Int) {
        _batteryLevel.value = batteryLevel
    }

    enum class InputSource {
        BATT,
        USB,
        NUM_SOURCE,
        NA
    }

    enum class BattStatus {
        OK,
        LOW,
        VERY_LOW,
        CHARGING,
        CHARGING_COMPLETE,
        NUM_STATUS,
        UNKNOWN,
    }

    companion object {
        private const val LBS_PWR_SERVICE: String = "1212-FADA-1523-785FEF15D223"
        /** PWR Service UUID.  */
        val LBS_UUID_PWR_SERVICE: UUID = UUID.fromString("0000F0DD-${LBS_PWR_SERVICE}")
        /** PWR_INPUT_SOURCE characteristic UUID.  */
        val LBS_UUID_PWR_SOURCE_CHAR: UUID = UUID.fromString("0000BEBA-${LBS_PWR_SERVICE}")
        /** PWR_STATUS characteristic UUID.  */
        val LBS_UUID_PWR_BATT_STATUS_CHAR: UUID = UUID.fromString("0000BEBB-${LBS_PWR_SERVICE}")
        /** PWR_STATUS characteristic UUID.  */
        val LBS_UUID_PWR_BATT_LEVEL_CHAR: UUID = UUID.fromString("0000BEBC-${LBS_PWR_SERVICE}")
    }
}