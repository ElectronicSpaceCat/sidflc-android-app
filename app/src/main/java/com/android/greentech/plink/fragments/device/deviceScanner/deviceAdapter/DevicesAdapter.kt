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
package com.android.greentech.plink.fragments.device.deviceScanner.deviceAdapter

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.text.TextUtils
import com.android.greentech.plink.R
import androidx.recyclerview.widget.DiffUtil
import com.android.greentech.plink.databinding.ItemDeviceBinding
import com.android.greentech.plink.fragments.device.deviceScanner.DeviceScannerFragment

class DevicesAdapter(
    activity: DeviceScannerFragment,
    devicesLiveData: DevicesLiveData
) :
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {
    private var devices: List<DiscoveredBluetoothDevice>? = null
    private lateinit var onItemClickListener: OnItemClickListener

    init {
        setHasStableIds(true)
        // Update list when new devices discovered
        devicesLiveData.observe(activity.viewLifecycleOwner) { newDevices ->
            val result = DiffUtil.calculateDiff(
                DeviceDiffCallback(devices, newDevices), false
            )
            devices = newDevices
            // Update the recycler view
            result.dispatchUpdatesTo(this)
        }
    }

    fun interface OnItemClickListener {
        fun onItemClick(device: DiscoveredBluetoothDevice)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        if (listener != null) {
            onItemClickListener = listener
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemDeviceBinding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(itemDeviceBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices?.get(position)
        if (device != null) {
            val deviceName = device.name
            if (!TextUtils.isEmpty(deviceName)){
                holder.itemDeviceBinding.deviceName.text = deviceName
            }
            else {
                holder.itemDeviceBinding.deviceName.setText(R.string.unknown_device)
            }
            holder.itemDeviceBinding.deviceAddress.text = device.address
            val rssiPercent = (100.0f * (127.0f + (device.rssi )) / (127.0f + 20.0f)).toInt()
            holder.itemDeviceBinding.rssi.setImageLevel(rssiPercent)
        }
    }

    override fun getItemId(position: Int): Long {
        return devices?.get(position).hashCode().toLong()
    }

    override fun getItemCount(): Int {
        if (devices != null) {
            return devices!!.size
        }
        return 0
    }

    inner class ViewHolder(val itemDeviceBinding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(itemDeviceBinding.root) {
        init {
            itemDeviceBinding.deviceContainer.setOnClickListener {
                devices?.get(this.adapterPosition)
                    ?.let { device -> onItemClickListener.onItemClick(device) }
            }
        }
    }
}
