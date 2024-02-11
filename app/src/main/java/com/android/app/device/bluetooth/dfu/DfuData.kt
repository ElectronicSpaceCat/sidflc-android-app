package com.android.app.device.bluetooth.dfu

import java.util.*

/**
 * The data in the class must reference the
 * data from the nRF firmware.
 */
class DfuData {
    companion object {
        private const val LBS_DFU_SERVICE: String = "0000-1000-8000-00805F9B34FB"
        /** DFU Service UUID.  */
        val LBS_UUID_DFU_SERVICE: UUID = UUID.fromString("0000FE59-${LBS_DFU_SERVICE}")
    }
}
