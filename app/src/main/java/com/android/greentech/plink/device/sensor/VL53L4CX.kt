package com.android.greentech.plink.device.sensor

import com.android.greentech.plink.device.bluetooth.device.DeviceData
import com.android.greentech.plink.device.sensor.ISensorCalibrate.State

open class VL53L4CX(override val sensor: Sensor) : ISensor {
    
    override val type: DeviceData.Sensor.Type = DeviceData.Sensor.Type.VL53L4CX
    
    enum class Config{
        POWER_LEVEL,
        PHS_CAL_PCH_PWR,
        TIME_BUDGET,
        OFFSET_MODE,
        DISTANCE_MODE,
        SMUDGE_CORR_MODE,
        XTALK_COMP_EN,
        RECT_OF_INTEREST,
        CAL_REFSPAD,
        CAL_OFFSET_SIMPLE,
        CAL_OFFSET_ZERO,
        CAL_OFFSET_VCSEL,
        CAL_XTALK // Note: This seems to goof up the sensor, datasheet recommends running it with target at 600mm
    }

    override val configs: Array<ISensor.Config> = Array(Config.values().size) {
        ISensor.Config(Config.values()[it].name, Int.MAX_VALUE)
    }

    override fun stopCalibration() {
        state = State.NA
    }
    override fun startCalibration() {
        sensor.resetFactory()
        state = State.INIT
        sensor.sendConfigCommand(DeviceData.Config.Command.GET, Int.MAX_VALUE, Int.MAX_VALUE)
    }

    override fun runCalibration(config : DeviceData.Config) {
        when (state) {
            State.INIT -> {
                if(sensor.isEnabled) {
                    sensor.enable(false)
                    // Calling this will trigger a response which is used to drive the state machine
                    sensor.sendConfigCommand(DeviceData.Config.Command.GET, Int.MAX_VALUE, Int.MAX_VALUE)
                }
                else{
                    state = State.PREPARE
                    sensor.loadConfigs()
                }
            }
            State.PREPARE -> {
                if(sensor.isInitialized) {
                    state = State.START
                    sensor.sendConfigCommand(DeviceData.Config.Command.GET, Int.MAX_VALUE, Int.MAX_VALUE)
                }
            }
            State.START -> {
                when(sensor.id){
                    DeviceData.Sensor.Id.SHORT -> {
                        sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.POWER_LEVEL.ordinal, POWER_LEVEL_LOW)
                    }
                    DeviceData.Sensor.Id.LONG -> {
                        sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.POWER_LEVEL.ordinal, POWER_LEVEL_DEFAULT)
                    }
                    else -> {}
                }
                setCalMessage(Config.POWER_LEVEL.ordinal)
                state = State.WAIT_FOR_RESPONSE
            }
            State.CALIBRATING -> {
                // Do nothing..
            }
            State.WAIT_FOR_RESPONSE -> {
                when(config.id) {
                    Config.POWER_LEVEL.ordinal-> {
                        setCalResponseMessage(config)
                        when(config.status) {
                            DeviceData.Config.Status.OK -> {
                                sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.PHS_CAL_PCH_PWR.ordinal, PHS_CAL_PCH_PWR)
                                setCalMessage(Config.PHS_CAL_PCH_PWR.ordinal)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                    }
                    Config.PHS_CAL_PCH_PWR.ordinal-> {
                        setCalResponseMessage(config)
                        when(config.status) {
                            DeviceData.Config.Status.OK -> {
                                when(sensor.id){
                                    DeviceData.Sensor.Id.SHORT -> {
                                        sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.OFFSET_MODE.ordinal, OFFSET_CORRECTION_MODE_PERVCSEL)
                                    }
                                    DeviceData.Sensor.Id.LONG -> {
                                        sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.OFFSET_MODE.ordinal, OFFSET_CORRECTION_MODE_PERVCSEL)
                                    }
                                    else -> {}
                                }
                                setCalMessage(Config.OFFSET_MODE.ordinal)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                    }
                    Config.OFFSET_MODE.ordinal-> {
                        setCalResponseMessage(config)
                        when(config.status) {
                            DeviceData.Config.Status.OK -> {
                                when(sensor.id){
                                    DeviceData.Sensor.Id.SHORT -> {
                                        sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.RECT_OF_INTEREST.ordinal, SENSOR_SHORT_ROI)
                                    }
                                    DeviceData.Sensor.Id.LONG -> {
                                        sensor.sendConfigCommand(DeviceData.Config.Command.RESET, Config.RECT_OF_INTEREST.ordinal, Int.MAX_VALUE)
                                    }
                                    else -> {}
                                }
                                setCalMessage(Config.RECT_OF_INTEREST.ordinal)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                    }
                    Config.RECT_OF_INTEREST.ordinal-> {
                        setCalResponseMessage(config)
                        when(config.status) {
                            DeviceData.Config.Status.OK -> {
                                sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.CAL_REFSPAD.ordinal, 0)
                                setCalMessage(Config.CAL_REFSPAD.ordinal)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                    }
                    Config.CAL_REFSPAD.ordinal-> {
                        setCalResponseMessage(config)
                        when(config.status) {
                            DeviceData.Config.Status.OK -> {
                                when(sensor.id){
                                    DeviceData.Sensor.Id.SHORT -> {
                                        sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.CAL_OFFSET_VCSEL.ordinal, sensor.targetReference)
                                        setCalMessage(Config.CAL_OFFSET_VCSEL.ordinal)
                                    }
                                    DeviceData.Sensor.Id.LONG -> {
                                        sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.CAL_OFFSET_ZERO.ordinal, 0)
                                        setCalMessage(Config.CAL_OFFSET_ZERO.ordinal)
                                    }
                                    else -> {}
                                }
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                    }
                    Config.CAL_OFFSET_ZERO.ordinal,
                    Config.CAL_OFFSET_VCSEL.ordinal-> {
                        setCalResponseMessage(config)
                        when(config.status) {
                            DeviceData.Config.Status.OK -> {
                                when(sensor.id){
                                    DeviceData.Sensor.Id.SHORT -> {
                                        sensor.sendConfigCommand(DeviceData.Config.Command.SET, Config.TIME_BUDGET.ordinal, SENSOR_SHORT_TIME_BUDGET)
                                    }
                                    DeviceData.Sensor.Id.LONG -> {
                                        sensor.sendConfigCommand(DeviceData.Config.Command.GET, Config.TIME_BUDGET.ordinal, Int.MAX_VALUE)
                                    }
                                    else -> {}
                                }
                                setCalMessage(Config.TIME_BUDGET.ordinal)
                                state = State.WAIT_FOR_RESPONSE
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                    }
                    Config.TIME_BUDGET.ordinal-> {
                        setCalResponseMessage(config)
                        when(config.status) {
                            DeviceData.Config.Status.OK,
                            DeviceData.Config.Status.MISMATCH-> {
                                sensor.storeConfigData()
                                stateMsg = "Sensor: " + sensor.id.toString() + "\nStoring data..."
                                state = State.WAIT_FOR_RESPONSE
                            }
                            DeviceData.Config.Status.UPDATED-> {
                                return
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }
                    }
                    STORE_DATA_CONFIG_ID-> { // Int.MAX_VALUE is the ID used for storing data
                        when(config.status) {
                            DeviceData.Config.Status.OK -> {
                                stateMsg = "Sensor: " + sensor.id.toString() + "\nData stored"
                                state = State.FINISHED
                            }
                            else -> {
                                state = State.ERROR
                            }
                        }

                    }
                }
            }
            State.FINISHED -> {
                // Do nothing..
            }
            State.ERROR -> {
                stateMsg = "Sensor: " + sensor.id.toString() + "\nTimeout"
            }
            State.NA -> {
                // Do nothing..
            }
        }
    }

    private fun setCalMessage(configId: Int){
        if(configId < Config.values().size){
            stateMsg = "Sensor: " + sensor.id.toString() + "\nConfig: " + Config.values()[configId].name
        }
    }

    private fun setCalResponseMessage(config: DeviceData.Config){
        if(config.id < Config.values().size){
            stateMsg = "Sensor: " + sensor.id.toString() + "\nConfig: " + Config.values()[config.id].name + " : " + config.status.name
        }
    }

    companion object {
        const val STORE_DATA_CONFIG_ID = 0xFF
        const val OFFSET_CORRECTION_MODE_STANDARD = 1
        const val OFFSET_CORRECTION_MODE_PERVCSEL = 3
        const val POWER_LEVEL_LOW = 30 // Carriage sensor seems better at this level
        const val POWER_LEVEL_DEFAULT = 60
        const val PHS_CAL_PCH_PWR = 2 // Supposedly helps

        const val SENSOR_SHORT_TIME_BUDGET = 55000 // Gives reasonable response speed
        const val SENSOR_SHORT_ROI = 101255430 // This sets a 4x4 (minimum allowed) out of 15x15 SPAD array
    }
}
