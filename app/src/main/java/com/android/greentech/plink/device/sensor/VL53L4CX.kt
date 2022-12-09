package com.android.greentech.plink.device.sensor

import com.android.greentech.plink.device.bluetooth.sensor.SensorData
import com.android.greentech.plink.device.sensor.ISensorCalibrate.State

open class VL53L4CX(
    override val sensor: Sensor,
    override val type: SensorData.Sensor.Type = SensorData.Sensor.Type.VL53L4CX
) : ISensor, SensorCalibrate() {

    enum class Config{
        POWER_LEVEL,
        PHS_CAL_PCH_PWR,
        TIME_BUDGET,
        OFFSET_MODE,
        DISTANCE_MODE,
        SMUDGE_CORR_EN,
        XTALK_COMP_EN,
        RECT_OF_INTEREST,
        CAL_REFSPAD,
        CAL_OFFSET_SIMPLE,
        CAL_OFFSET_ZERO,
        CAL_OFFSET_VCSEL,
        CAL_XTALK
    }

//    private var _sampleCount = 0
//    private val _maxCount = 35 // Run enough samples to level out the moving average

    override val configs: Array<ISensor.Config> = Array(Config.values().size) {
        ISensor.Config(Config.values()[it].name, Int.MAX_VALUE)
    }

    override fun runCalibration() {
        when (calibrationState) {
            State.PREPARE -> {
                if(sensor.isEnabled) {
                    sensor.enable(false)
                }
                else{
                    state = State.START
                }
                // Calling this will trigger a response which is used to drive the state machine
                sensor.setConfigCommand(SensorData.Config.Command.GET, Int.MAX_VALUE, Int.MAX_VALUE)
            }
            State.START -> {
                when(sensor.id){
                    SensorData.Sensor.Id.SHORT -> {
                        sensor.setConfigCommand(SensorData.Config.Command.SET, Config.POWER_LEVEL.ordinal, POWER_LEVEL_LOW)
                    }
                    SensorData.Sensor.Id.LONG -> {
                        sensor.setConfigCommand(SensorData.Config.Command.SET, Config.POWER_LEVEL.ordinal, POWER_LEVEL_DEFAULT)
                    }
                    else -> {}
                }

                state = State.WAIT_FOR_RESPONSE
            }
            State.WAIT_FOR_RESPONSE -> {
                when(sensor.lastConfigReceived.id) {
                    Config.POWER_LEVEL.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                when(sensor.id){
                                    SensorData.Sensor.Id.SHORT -> {
                                        sensor.setConfigCommand(SensorData.Config.Command.SET, Config.OFFSET_MODE.ordinal, OFFSET_CORRECTION_MODE_PERVCSEL)
                                    }
                                    SensorData.Sensor.Id.LONG -> {
                                        sensor.setConfigCommand(SensorData.Config.Command.SET, Config.OFFSET_MODE.ordinal, OFFSET_CORRECTION_MODE_STANDARD)
                                    }
                                    else -> {}
                                }
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.OFFSET_MODE.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                sensor.setConfigCommand(SensorData.Config.Command.SET, Config.PHS_CAL_PCH_PWR.ordinal, PHS_CAL_PCH_PWR)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.PHS_CAL_PCH_PWR.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                sensor.setConfigCommand(SensorData.Config.Command.SET, Config.SMUDGE_CORR_EN.ordinal, 0)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.SMUDGE_CORR_EN.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                sensor.setConfigCommand(SensorData.Config.Command.SET, Config.XTALK_COMP_EN.ordinal, 0)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.XTALK_COMP_EN.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                when(sensor.id){
                                    SensorData.Sensor.Id.SHORT -> {
                                        sensor.setConfigCommand(SensorData.Config.Command.SET, Config.RECT_OF_INTEREST.ordinal, SENSOR_SHORT_ROI)
                                    }
                                    SensorData.Sensor.Id.LONG -> {
                                        sensor.setConfigCommand(SensorData.Config.Command.RESET, Config.RECT_OF_INTEREST.ordinal, Int.MAX_VALUE)
                                    }
                                    else -> {}
                                }
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.RECT_OF_INTEREST.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                sensor.setConfigCommand(SensorData.Config.Command.SET, Config.CAL_REFSPAD.ordinal, 0)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.CAL_REFSPAD.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                sensor.setConfigCommand(SensorData.Config.Command.SET, Config.CAL_XTALK.ordinal, 0)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.CAL_XTALK.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                when(sensor.id){
                                    SensorData.Sensor.Id.SHORT -> {
                                        sensor.setConfigCommand(SensorData.Config.Command.SET, Config.CAL_OFFSET_VCSEL.ordinal, sensor.offsetRef)
                                    }
                                    SensorData.Sensor.Id.LONG -> {
                                        sensor.setConfigCommand(SensorData.Config.Command.SET, Config.CAL_OFFSET_ZERO.ordinal, 0)
                                    }
                                    else -> {}
                                }
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.CAL_OFFSET_ZERO.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                state = State.FINISHED
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.CAL_OFFSET_VCSEL.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                sensor.setConfigCommand(SensorData.Config.Command.SET, Config.TIME_BUDGET.ordinal, SENSOR_SHORT_TIME_BUDGET)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.TIME_BUDGET.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK,
                            SensorData.Config.Status.MISMATCH-> {
                                state = State.FINISHED
                            }
                            SensorData.Config.Status.UPDATED-> {
                                // Ignore..
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                }
            }
            State.ERROR -> {
                stateMsg = "Sensor: " + sensor.id.toString() + "Timeout"
            }
            else -> {}
        }
    }

    /** // Note: block has logic for setting a manual offset by doing our own averaging
    private fun runCalShortSimple () {
        when (sensor.calState) {
            SensorCalibrate.State.START -> {
                sensor.setConfigCommand(Config.POWER_LEVEL.ordinal, SensorData.Config.Command.SET, VCSEL_POWER_LEVEL)
                sensor.calState = SensorCalibrate.State.WAIT_FOR_RESPONSE
            }
            SensorCalibrate.State.CALIBRATING -> {
                if(_sampleCount >= _maxCount){
                    sensor.device.setSensorEnable(false)
                    val rangeDelta = (sensor.offsetRef - sensor.rangeFiltered.value!!)
                    val offset = sensor.getConfigByName(Config.OFFSET.name).value + rangeDelta
                    sensor.setConfigCommand(Config.OFFSET.ordinal, SensorData.Config.Command.SET, offset.roundToInt())
                    sensor.calState = SensorCalibrate.State.WAIT_FOR_RESPONSE
                }
                else {
                    _sampleCount += 1
                    sensor.calStateMsg = "Sensor: " + sensor.id.toString() + "\nSample: " + _sampleCount.toString() + "/" + _maxCount
                }
            }
            SensorCalibrate.State.WAIT_FOR_RESPONSE -> {
                when(sensor.lastConfigReceived.id) {
                    Config.POWER_LEVEL.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                _sampleCount = 0
                                sensor.device.setSensorEnable(true)
                                sensor.calState = SensorCalibrate.State.CALIBRATING
                            }
                            else -> {
                                sensor.calState = SensorCalibrate.State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.OFFSET.ordinal-> {
                        when(sensor.lastConfigReceived.status) {
                            SensorData.Config.Status.OK -> {
                                sensor.setConfigCommand(Config.CAL_XTALK.ordinal, SensorData.Config.Command.SET, 0)
                                sensor.calState = SensorCalibrate.State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                sensor.calState = SensorCalibrate.State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                    Config.CAL_XTALK.ordinal -> {
                        when(sensor.lastConfigReceived.status){
                            SensorData.Config.Status.OK -> {
                                sensor.calState = SensorCalibrate.State.FINISHED
                            }
                            else -> {
                                sensor.calState = SensorCalibrate.State.ERROR
                            }
                        }
                        setCalMessage(sensor.lastConfigReceived)
                    }
                }
            }
            else -> {}
        }
    }
*/

    private fun setCalMessage(config: SensorData.Config){
        if(config.id < Config.values().size){
            stateMsg = "Sensor: " + sensor.id.toString() + "\nConfig: " + Config.values()[config.id].name + " : " + config.status.name
        }
    }

    private

    companion object {
        const val OFFSET_CORRECTION_MODE_STANDARD = 1
        const val OFFSET_CORRECTION_MODE_PERVCSEL = 3
        const val POWER_LEVEL_LOW = 30 // Position sensor seems better at this level
        const val POWER_LEVEL_DEFAULT = 60
        const val SENSOR_SHORT_TIME_BUDGET = 35000 // Seems like reasonable reaction speed
        const val PHS_CAL_PCH_PWR = 2 // Supposedly helps
        const val SENSOR_SHORT_ROI = 101255430 // This sets a 4x4 (minimum allowed) out of 15x15 SPAD array
    }
}
