package com.android.app.device.bluetooth.genericAttribute

import java.util.UUID

class GenericAttribute {
    companion object {
        private const val LBS_GENERIC_ATTR: String = "0000-1000-8000-00805F9B34FB"
        /** Generic Attribute Service UUID.  */
        val LBS_UUID_GENERIC_ATTR_SERVICE: UUID = UUID.fromString("00001801-${LBS_GENERIC_ATTR}")
        /** Generic Attribute Service UUID.  */
        val LBS_UUID_SERVICE_CHANGED_CHAR: UUID = UUID.fromString("00002A05-${LBS_GENERIC_ATTR}")
    }
}