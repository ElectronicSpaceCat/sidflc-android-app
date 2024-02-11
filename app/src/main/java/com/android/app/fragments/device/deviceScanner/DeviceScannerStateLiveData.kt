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
package com.android.app.fragments.device.deviceScanner

import androidx.lifecycle.LiveData

/**
 * This class keeps the current state of the scanner.
 */
class DeviceScannerStateLiveData/* package */(
    private var bluetoothEnabled: Boolean,
    private var locationEnabled: Boolean
) : LiveData<DeviceScannerStateLiveData>() {
    private var scanningStarted = false
    private var hasRecords = false

    init {
        postValue(this)
    }

    /* package */
    fun refresh() {
        postValue(this)
    }

    /* package */
    fun scanningStarted() {
        scanningStarted = true
        postValue(this)
    }

    /* package */
    fun scanningStopped() {
        scanningStarted = false
        postValue(this)
    }

    /* package */
    fun bluetoothEnabled() {
        bluetoothEnabled = true
        postValue(this)
    }

    /* package */
    fun bluetoothDisabled() {
        bluetoothEnabled = false
        hasRecords = false
        postValue(this)
    }

    /* package */
    fun setLocationEnabled(enabled: Boolean) {
        locationEnabled = enabled
        postValue(this)
    }

    /* package */
    fun recordFound() {
        hasRecords = true
        postValue(this)
    }

    /**
     * Notifies the observer that scanner has no records to show.
     */
    fun clearRecords() {
        hasRecords = false
        postValue(this)
    }

    /**
     * Returns whether scanning is in progress.
     */
    fun isScanning(): Boolean {
        return scanningStarted
    }

    /**
     * Returns whether any records matching filter criteria has been found.
     */
    fun hasRecords(): Boolean {
        return hasRecords
    }

    /**
     * Returns whether Bluetooth adapter is enabled.
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothEnabled
    }

    /**
     * Returns whether Location is enabled.
     */
    fun isLocationEnabled(): Boolean {
        return locationEnabled
    }
}