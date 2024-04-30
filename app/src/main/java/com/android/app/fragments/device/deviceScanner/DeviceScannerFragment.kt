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

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceScannerBinding
import com.android.app.fragments.device.deviceScanner.deviceAdapter.DevicesAdapter
import com.android.app.fragments.device.deviceScanner.deviceAdapter.DiscoveredBluetoothDevice
import com.android.app.utils.misc.Utils.isLocationPermissionDeniedForever
import com.android.app.utils.misc.Utils.isLocationPermissionsGranted
import com.android.app.utils.misc.Utils.markLocationPermissionRequested
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.Timer
import java.util.TimerTask

internal enum class ScanState {
    SCAN_NO_BLUETOOTH,
    SCAN_DEVICE_NOT_SUPPORTED,
    SCAN_DEVICE_CONNECTING,
    SCAN_DEVICE_CONNECTED,
    SCAN_DEVICES_NOT_FOUND,
    SCAN_DEVICES_FOUND
}

class DeviceScannerFragment : Fragment(), DevicesAdapter.OnItemClickListener {
    /** Android ViewBinding */
    private var _fragmentDeviceScannerBinding: FragmentDeviceScannerBinding? = null
    private val fragmentDeviceScannerBinding get() = _fragmentDeviceScannerBinding!!

    private lateinit var viewModel: DeviceScannerViewModel

    private var scanState: ScanState = ScanState.SCAN_NO_BLUETOOTH

    private var deviceSelected : DiscoveredBluetoothDevice?= null

    private var kickTimer = false

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            permissionsGranted(isGranted)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _fragmentDeviceScannerBinding = FragmentDeviceScannerBinding.inflate(inflater, container, false)
        return fragmentDeviceScannerBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[DeviceScannerViewModel::class.java]

        // Initialize the scanner search-by filters
        viewModel.filterByUuid(true)
        viewModel.filterByDistance(false)

        // Create view model containing utility methods for scanning
        viewModel.deviceScannerState.observe(viewLifecycleOwner) { state: DeviceScannerStateLiveData ->
            startScan(state)
        }

        // Configure the recycler view
        fragmentDeviceScannerBinding.recyclerViewBleDevices.layoutManager = LinearLayoutManager(requireActivity())
        fragmentDeviceScannerBinding.recyclerViewBleDevices.addItemDecoration(
            DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL)
        )

        // Configure the recycler view animator
        val animator = fragmentDeviceScannerBinding.recyclerViewBleDevices.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        // Set the recycler view adapter
        val adapter = DevicesAdapter(this, viewModel.devices)
        adapter.setOnItemClickListener(this)
        fragmentDeviceScannerBinding.recyclerViewBleDevices.adapter = adapter

        // Set setOnClickListener for each layout items
        fragmentDeviceScannerBinding.bluetoothOff.actionEnableBluetooth.setOnClickListener { onEnableBluetoothClicked() }
        fragmentDeviceScannerBinding.noDevices.actionEnableLocation.setOnClickListener { onEnableLocationClicked() }
        fragmentDeviceScannerBinding.noLocationPermission.actionGrantLocationPermission.setOnClickListener { onGrantLocationPermissionClicked() }
        fragmentDeviceScannerBinding.noLocationPermission.actionPermissionSettings.setOnClickListener { onPermissionSettingsClicked() }
        fragmentDeviceScannerBinding.deviceNotSupported.actionClearCache.setOnClickListener { onClearCacheClicked() }
        fragmentDeviceScannerBinding.connecting.setOnLongClickListener { onConnectingClicked() }

        // Set observer to watch the device connection state
        DataShared.device.connectionState.observe(viewLifecycleOwner) {
            viewModel.refresh()
        }

        // Setup a timer for rescanning periodically
        Timer("RescanTimeoutTask", false).schedule(object : TimerTask() {
            override fun run() {
                if(_fragmentDeviceScannerBinding == null) {
                    this.cancel()
                }
                if(kickTimer){
                    kickTimer = false
                }
                else{
                    clear()
                    viewModel.refresh()
                }
            }
        },0, 2500)
    }

    override fun onItemClick(device: DiscoveredBluetoothDevice) {
        connectDevice(device)
    }

    @SuppressLint("MissingPermission")
    private fun connectDevice(target: DiscoveredBluetoothDevice){
        deviceSelected = target
        DataShared.device.disconnect()
        DataShared.device.connect(requireActivity(), target.device, !target.isBootloader)
        Toast.makeText(requireActivity(),
            "Device selected: " + target.name,
            Toast.LENGTH_SHORT
        ).show()
    }

    @SuppressLint("MissingPermission")
    private fun onEnableBluetoothClicked() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(intent)
        requireContext().sendBroadcast(intent)

//        // Use this block if not wanting to prompt user to turn on bluetooth
//        val manager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        if(!manager.adapter.isEnabled){
//            manager.adapter.enable()
//        }
    }

    private fun onEnableLocationClicked() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun onGrantLocationPermissionClicked() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.refresh()
        } else {
            markLocationPermissionRequested(requireActivity())
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun onPermissionSettingsClicked() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", requireActivity().packageName, null)
        startActivity(intent)
    }

    /**
     * Disconnect from the device which will trigger a clear cache on disconnect
     * when device services are not supported AND is not in the bootloader
     */
    private fun onClearCacheClicked() {
        DataShared.device.disconnect()
    }

    /**
     * Cancel connection if the connecting text is clicked
     */
    private fun  onConnectingClicked() : Boolean {
        DataShared.device.disconnect()
        return true
    }

    /**
     * Call refresh to trigger a rescan of devices
     */
    private fun onScanClicked() {
        viewModel.deviceScannerState.clearRecords()
    }

    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    @SuppressLint("MissingPermission")
    private fun startScan(state: DeviceScannerStateLiveData) {
        // Check device connection status
        val connState = DataShared.device.connectionState.value!!

        // Determine which scan state to be in
        if (!state.isBluetoothEnabled()) {
            scanState = ScanState.SCAN_NO_BLUETOOTH
        }
        else if (connState.isReady) {
            scanState = ScanState.SCAN_DEVICE_CONNECTED
        }
        else if (connState.isConnected) {
            scanState = ScanState.SCAN_DEVICE_CONNECTING
        }
        else if (state.hasRecords()) {
            scanState = ScanState.SCAN_DEVICES_FOUND
            kickTimer = true
        }
        else {
            // NOTE: Disconnect reason REASON_SUCCESS is set if user disconnected the device
            var disconnectReason = ConnectionObserver.REASON_SUCCESS

            // Get disconnect reason if any
            if (connState is ConnectionState.Disconnected) {
                disconnectReason = connState.reason
            }

            when (disconnectReason) {
                ConnectionObserver.REASON_NOT_SUPPORTED -> {
                    scanState = ScanState.SCAN_DEVICE_NOT_SUPPORTED
                }
                else -> {
                    scanState = ScanState.SCAN_DEVICES_NOT_FOUND
                }
            }
        }

        // Handle UI elements
        when (scanState) {
            ScanState.SCAN_NO_BLUETOOTH -> {
                fragmentDeviceScannerBinding.stateScanning.visibility = View.INVISIBLE
                fragmentDeviceScannerBinding.connecting.visibility = View.GONE
                fragmentDeviceScannerBinding.recyclerViewBleDevices.visibility = View.GONE
                fragmentDeviceScannerBinding.noDevices.root.visibility = View.GONE
                fragmentDeviceScannerBinding.noDevices.noLocation.visibility = View.GONE
                fragmentDeviceScannerBinding.bluetoothOff.root.visibility = View.VISIBLE
                fragmentDeviceScannerBinding.deviceNotSupported.root.visibility = View.GONE
                fragmentDeviceScannerBinding.noLocationPermission.root.visibility = View.GONE
            }
            ScanState.SCAN_DEVICE_NOT_SUPPORTED -> {
                fragmentDeviceScannerBinding.stateScanning.visibility = View.INVISIBLE
                fragmentDeviceScannerBinding.connecting.visibility = View.GONE
                fragmentDeviceScannerBinding.recyclerViewBleDevices.visibility = View.GONE
                fragmentDeviceScannerBinding.noDevices.root.visibility = View.GONE
                fragmentDeviceScannerBinding.noDevices.noLocation.visibility = View.GONE
                fragmentDeviceScannerBinding.bluetoothOff.root.visibility = View.GONE
                fragmentDeviceScannerBinding.deviceNotSupported.root.visibility = View.VISIBLE
                fragmentDeviceScannerBinding.noLocationPermission.root.visibility = View.GONE
            }
            ScanState.SCAN_DEVICE_CONNECTING -> {
                fragmentDeviceScannerBinding.stateScanning.visibility = View.VISIBLE
                fragmentDeviceScannerBinding.connecting.visibility = View.VISIBLE
                fragmentDeviceScannerBinding.recyclerViewBleDevices.visibility = View.GONE
                fragmentDeviceScannerBinding.noDevices.root.visibility = View.GONE
                fragmentDeviceScannerBinding.noDevices.noLocation.visibility = View.GONE
                fragmentDeviceScannerBinding.bluetoothOff.root.visibility = View.GONE
                fragmentDeviceScannerBinding.deviceNotSupported.root.visibility = View.GONE
                fragmentDeviceScannerBinding.noLocationPermission.root.visibility = View.GONE
            }
            ScanState.SCAN_DEVICE_CONNECTED -> {
                // If not the bootloader service and not bonded then ensure a bond
                if(deviceSelected != null){
                    if(!deviceSelected?.isBootloader!! && deviceSelected?.device?.bondState == BluetoothDevice.BOND_NONE){
                        DataShared.device.ensureBond()
                    }
                }
                // Go to deviceConnected fragment
                Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                    R.id.action_deviceScannerFragment_to_deviceConnectedFragment
                )

                viewModel.stopScan()
            }
            ScanState.SCAN_DEVICES_NOT_FOUND -> {
                viewModel.startScan()
                fragmentDeviceScannerBinding.stateScanning.visibility = View.VISIBLE
                fragmentDeviceScannerBinding.connecting.visibility = View.GONE
                fragmentDeviceScannerBinding.recyclerViewBleDevices.visibility = View.INVISIBLE
                fragmentDeviceScannerBinding.bluetoothOff.root.visibility = View.GONE
                fragmentDeviceScannerBinding.deviceNotSupported.root.visibility = View.GONE

                if (!isLocationPermissionsGranted(requireActivity())) {
                    fragmentDeviceScannerBinding.noDevices.root.visibility = View.GONE
                    fragmentDeviceScannerBinding.noDevices.noLocation.visibility = View.GONE
                    fragmentDeviceScannerBinding.noLocationPermission.root.visibility = View.VISIBLE
                    val deniedForever = isLocationPermissionDeniedForever(requireActivity())
                    fragmentDeviceScannerBinding.noLocationPermission.actionGrantLocationPermission.visibility =
                        if (deniedForever) View.GONE else View.VISIBLE
                    fragmentDeviceScannerBinding.noLocationPermission.actionPermissionSettings.visibility =
                        if (deniedForever) View.VISIBLE else View.GONE

                }else if(!state.isLocationEnabled()){
                    fragmentDeviceScannerBinding.noDevices.root.visibility = View.VISIBLE
                    fragmentDeviceScannerBinding.noDevices.noLocation.visibility = View.VISIBLE
                    fragmentDeviceScannerBinding.noLocationPermission.root.visibility = View.GONE
                }
                else {
                    fragmentDeviceScannerBinding.noDevices.root.visibility = View.VISIBLE
                    fragmentDeviceScannerBinding.noDevices.noLocation.visibility = View.GONE
                    fragmentDeviceScannerBinding.noLocationPermission.root.visibility = View.GONE
                }
            }
            ScanState.SCAN_DEVICES_FOUND -> {
                fragmentDeviceScannerBinding.stateScanning.visibility = View.INVISIBLE
                fragmentDeviceScannerBinding.connecting.visibility = View.GONE
                fragmentDeviceScannerBinding.recyclerViewBleDevices.visibility = View.VISIBLE
                fragmentDeviceScannerBinding.noDevices.root.visibility = View.GONE
                fragmentDeviceScannerBinding.noDevices.noLocation.visibility = View.GONE
                fragmentDeviceScannerBinding.bluetoothOff.root.visibility = View.GONE
                fragmentDeviceScannerBinding.deviceNotSupported.root.visibility = View.GONE
                fragmentDeviceScannerBinding.noLocationPermission.root.visibility = View.GONE
            }
        }
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private fun stopScan() {
        viewModel.stopScan()
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private fun clear() {
        viewModel.devices.clear()
        viewModel.deviceScannerState.clearRecords()
    }

    /**
     * Check if permission granted
     */
    private fun permissionsGranted(isGranted: Boolean) {
        if (isGranted) {
            viewModel.refresh()
        } else {
            Toast.makeText(
                requireActivity(),
                "Permissions not granted by the user." as CharSequence,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
        clear()
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    override fun onDestroyView() {
        _fragmentDeviceScannerBinding = null
        super.onDestroyView()
    }
}