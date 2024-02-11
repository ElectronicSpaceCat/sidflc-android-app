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
package com.android.app.fragments.device.deviceScanner.deviceAdapter

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import com.android.app.device.bluetooth.dfu.DfuData
import no.nordicsemi.android.support.v18.scanner.ScanResult

//public class DiscoveredBluetoothDevice implements Parcelable {
class DiscoveredBluetoothDevice(scanResult: ScanResult) {
    val device: BluetoothDevice = scanResult.device
    private var lastScanResult: ScanResult? = null
    var name: String? = null
        private set
    var rssi = 0
        private set
    private var previousRssi = 0

    /**
     * Returns the highest recorded RSSI value during the scan.
     *
     * @return Highest RSSI value.
     */
    var highestRssi = -128
        private set
    val address: String
        get() = device.address
    val scanResult: ScanResult
        get() = lastScanResult!!

    val isBootloader : Boolean
        get() = scanResult.scanRecord?.serviceUuids?.contains(ParcelUuid(DfuData.LBS_UUID_DFU_SERVICE))!!

    /**
     * This method returns true if the RSSI range has changed.
     *
     * @return True, if the RSSI range has changed.
     */
    /* package */
    fun hasRssiLevelChanged(): Boolean {
        val newLevel = getRssiLevel(rssi)
        val oldLevel = getRssiLevel(previousRssi)
        return newLevel != oldLevel
    }

    /**
     * Updates the device values based on the scan result.
     *
     * @param scanResult the new received scan result.
     */
    fun update(scanResult: ScanResult) {
        lastScanResult = scanResult
        name = if (scanResult.scanRecord != null) scanResult.scanRecord!!.deviceName else null
        previousRssi = rssi
        rssi = scanResult.rssi

        if (highestRssi < rssi) highestRssi = rssi
    }

    fun matches(scanResult: ScanResult): Boolean {
        return device.address == scanResult.device.address
    }

    override fun hashCode(): Int {
        return device.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is DiscoveredBluetoothDevice) {
            return device.address == other.device.address
        }
        return super.equals(other)
    }
    //	// Parcelable implementation
    //
    //	private DiscoveredBluetoothDevice(final Parcel in) {
    //		device = in.readParcelable(BluetoothDevice.class.getClassLoader());
    //		lastScanResult = in.readParcelable(ScanResult.class.getClassLoader());
    //		name = in.readString();
    //		rssi = in.readInt();
    //		previousRssi = in.readInt();
    //		highestRssi = in.readInt();
    //	}
    //
    //	@Override // Note: Override from Parcelable
    //	public void writeToParcel(final Parcel parcel, final int flags) {
    //		parcel.writeParcelable(device, flags);
    //		parcel.writeParcelable(lastScanResult, flags);
    //		parcel.writeString(name);
    //		parcel.writeInt(rssi);
    //		parcel.writeInt(previousRssi);
    //		parcel.writeInt(highestRssi);
    //	}
    //
    //	@Override // Note: Override from Parcelable
    //	public int describeContents() {
    //		return 0;
    //	}
    //
    //	// Note: Required for Parcelable to create object in order to pass into bundles
    //	public static final Creator<DiscoveredBluetoothDevice> CREATOR = new Creator<DiscoveredBluetoothDevice>() {
    //		@Override
    //		public DiscoveredBluetoothDevice createFromParcel(final Parcel source) {
    //			return new DiscoveredBluetoothDevice(source);
    //		}
    //
    //		@Override
    //		public DiscoveredBluetoothDevice[] newArray(final int size) {
    //			return new DiscoveredBluetoothDevice[size];
    //		}
    //	};
    /**
     * Updates the device rssi level which is used to set the signal bar strength image selector.
     *
     * @param rssiValue the rssi value to translate into a signal bar level.
     */
    private fun getRssiLevel(rssiValue: Int): Int {
        return if (rssiValue <= 10) 0 else if (rssiValue <= 28) 1 else if (rssiValue <= 45) 2 else if (rssiValue <= 65) 3 else 4
    }

    init {
        update(scanResult)
    }
}