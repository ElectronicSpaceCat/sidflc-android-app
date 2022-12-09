/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.greentech.plink.fragments.device.deviceDfu

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.android.greentech.plink.R
import com.android.greentech.plink.databinding.FragmentDeviceDfuBinding
import com.android.greentech.plink.fragments.device.deviceDfu.DeviceDfuViewModel.Companion.UpdateStatus
import com.android.greentech.plink.utils.misc.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceListenerHelper

class DeviceDfuFragment : Fragment() {
    private var _fragmentDfuBinding: FragmentDeviceDfuBinding? = null
    private val fragmentDfuBinding get() = _fragmentDfuBinding!!

    private lateinit var viewModel: DeviceDfuViewModel
    private lateinit var dfuProgressListener : DfuProgressListenerAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            permissionsGranted(isGranted)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentDfuBinding = FragmentDeviceDfuBinding.inflate(inflater, container, false)
        return fragmentDfuBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[DeviceDfuViewModel::class.java]

        /**
         * Observe the network status
         */
        viewModel.networkStatus.observe(viewLifecycleOwner) {
            CoroutineScope(Dispatchers.IO).launch {
                delay(1000)
                try {
                    if (viewModel.networkStatus.value!!) {
                        viewModel.checkFirmwareVersion(requireContext())
                    } else {
                        viewModel.updateStatus = UpdateStatus.UPDATE_NO_NETWORK
                    }
                }
                catch (e : Exception){
                    // Do nothing..
                }
            }
        }

        /**
         * Observe the device connections state. Entering the bootloader will cause a
         * device disconnection event which should be ignored while DFU is in progress.
         */
        viewModel.deviceConnectionState.observe(viewLifecycleOwner) { state: ConnectionState ->
            // Return if update in progress and not the backup DFU mode
            if (viewModel.updateStatus == UpdateStatus.UPDATING && !viewModel.isBackupDFU(requireContext())) {
                return@observe
            }

            when(state.state) {
                // Did device disconnect?
                ConnectionState.State.DISCONNECTED -> {
                    // Yes - Go to the scanner page
                    val options = NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, false)
                        .setLaunchSingleTop(true)
                        .build()
                    Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(R.id.deviceScannerFragment, null, options)
                }
                ConnectionState.State.READY -> {
                    viewModel.checkFirmwareVersion(requireContext())
                }
                else -> {
                    // Do nothing..
                }
            }
        }

        /**
         * Observe if update status
         */
        viewModel.updateStatusLive.observe(viewLifecycleOwner) { status ->
            when(status!!){
                UpdateStatus.UPDATE_NO_NETWORK -> {
                    fragmentDfuBinding.dfuProgress.text = "Internet connection required\nto check for updates."
                    fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE
                    fragmentDfuBinding.dfuButton.visibility = View.INVISIBLE
                }
                UpdateStatus.UPDATED_CHECKING_FIRMWARE -> {
                    fragmentDfuBinding.dfuProgress.text = "Checking firmware version"
                    fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE
                    fragmentDfuBinding.dfuButton.visibility = View.INVISIBLE
                }
                UpdateStatus.UPDATE_ON_LATEST -> {
                    fragmentDfuBinding.dfuProgress.text = "Firmware is on latest"
                    fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE
                    fragmentDfuBinding.dfuButton.text = "Check"
                    fragmentDfuBinding.dfuButton.visibility = View.VISIBLE
                }
                UpdateStatus.UPDATE_AVAILABLE -> {
                    fragmentDfuBinding.dfuProgress.text = "Firmware ${viewModel.latestVersion} available!"
                    fragmentDfuBinding.dfuButton.text = "Update"
                    fragmentDfuBinding.dfuButton.visibility = View.VISIBLE
                    fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE

                    // Check if location permissions granted if update available
                    //checkLocationPermissionStatus()
                }
                UpdateStatus.UPDATE_DOWNLOADING -> {
                    fragmentDfuBinding.dfuProgress.text = "Downloading firmware…"
                    fragmentDfuBinding.dfuProgressBar.progress = 0
                    fragmentDfuBinding.dfuProgressBar.isIndeterminate = false
                    fragmentDfuBinding.dfuProgressBar.visibility = View.VISIBLE
                    fragmentDfuBinding.dfuButton.visibility = View.INVISIBLE
                }
                UpdateStatus.UPDATING -> {
                    fragmentDfuBinding.dfuProgressBar.visibility = View.VISIBLE
                }
                UpdateStatus.NA -> {
                    // Do nothing..
                }
            }
        }

        /**
         * Observe the file download progress
         */
        viewModel.downloadProgressLive.observe(viewLifecycleOwner) {
            fragmentDfuBinding.dfuProgressBar.progress = it
        }

        /**
         * Setup button listeners
         */
        fragmentDfuBinding.dfuButton.setOnClickListener { onBtnDfuClicked() }
        fragmentDfuBinding.locEnable.actionEnableLocation.setOnClickListener { onEnableLocationClicked() }
        fragmentDfuBinding.locPermissions.actionGrantLocationPermission.setOnClickListener { onGrantLocationPermissionClicked() }
        fragmentDfuBinding.locPermissions.actionPermissionSettings.setOnClickListener { onPermissionSettingsClicked() }

        // NOTE: Not sure if dfu notify channel is needed..
        //DfuServiceInitiator.createDfuNotificationChannel(requireContext())

        setupDfuProgressListener()
    }

    private fun onBtnDfuClicked() {
        when(viewModel.updateStatus){
            UpdateStatus.UPDATE_AVAILABLE -> {
                viewModel.startFirmwareUpdate(requireContext())
            }
            UpdateStatus.UPDATE_ON_LATEST,
            UpdateStatus.NA, -> {
                viewModel.checkFirmwareVersion(requireContext())
            }
            else -> {}
        }
    }

    private fun setupDfuProgressListener(){
        // Setup listener to inform user of the DFU states
        dfuProgressListener = object : DfuProgressListenerAdapter() {
            override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {
                if(message != null){
                    fragmentDfuBinding.dfuProgress.text = message
                }
                else{
                    fragmentDfuBinding.dfuProgress.text = "DFU ERROR: ".plus(error.toString())
                }
                fragmentDfuBinding.dfuButton.text = "Ok"

                viewModel.cleanUp()
            }

            override fun onDfuAborted(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(R.string.dfu_status_aborted)
            }

            override fun onDeviceDisconnecting(deviceAddress: String?) {
                fragmentDfuBinding.dfuProgress.setText(R.string.dfu_status_disconnecting)
            }

            override fun onDeviceConnected(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(R.string.dfu_status_connecting)
            }

            override fun onEnablingDfuMode(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(R.string.dfu_status_switching_to_dfu)
            }

            override fun onDfuProcessStarting(deviceAddress: String) {
                fragmentDfuBinding.dfuProgressBar.progress = 0
                fragmentDfuBinding.dfuProgressBar.isIndeterminate = false
                fragmentDfuBinding.dfuProgress.setText(R.string.dfu_status_starting)
            }

            override fun onFirmwareValidating(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(R.string.dfu_status_validating)
            }

            override fun onProgressChanged(deviceAddress: String, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
                fragmentDfuBinding.dfuProgress.setText(R.string.dfu_status_uploading)
                fragmentDfuBinding.dfuProgressBar.progress = percent
            }

            override fun onDfuCompleted(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.text = "Update complete!\n\nReconnecting.."
                fragmentDfuBinding.dfuProgressBar.progress = 100
                fragmentDfuBinding.dfuProgressBar.isIndeterminate = true

                viewModel.cleanUp()
            }
        }

        // Register the listener
        DfuServiceListenerHelper.registerProgressListener(requireContext(), dfuProgressListener)
    }

    private fun checkLocationPermissionStatus() {
        if(!Utils.isLocationPermissionsGranted(requireContext())){
            fragmentDfuBinding.locPermissions.root.visibility = View.VISIBLE
            fragmentDfuBinding.dfuButton.isEnabled = false
        }
        else {
            fragmentDfuBinding.locPermissions.root.visibility = View.GONE
            fragmentDfuBinding.dfuButton.isEnabled = false

            checkLocationStatus()
        }
    }

    private fun checkLocationStatus() {
        if(viewModel.updateStatus == UpdateStatus.UPDATE_AVAILABLE) {
            if (!Utils.isLocationEnabled(requireContext())) {
                fragmentDfuBinding.locEnable.root.visibility = View.VISIBLE
                fragmentDfuBinding.dfuButton.isEnabled = false
            } else {
                fragmentDfuBinding.locEnable.root.visibility = View.GONE
                fragmentDfuBinding.dfuButton.isEnabled = true
            }
        }
    }

    /**
     * Check if permission granted
     */
    private fun permissionsGranted(isGranted: Boolean) {
        if (isGranted) {
            checkLocationPermissionStatus()
        } else {
            Toast.makeText(
                activity,
                "Permissions not granted by the user." as CharSequence,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun onGrantLocationPermissionClicked() {
        if(Utils.isLocationPermissionsGranted(requireContext())) {
            checkLocationStatus()
        }
        else {
            Utils.markLocationPermissionRequested(requireActivity())
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun onEnableLocationClicked() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun onPermissionSettingsClicked() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", requireActivity().packageName, null)
        startActivity(intent)
    }

    /**
     * Broadcast receiver to monitor the changes in the location provider.
     */
    private val locationProviderChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            checkLocationStatus()
        }
    }

    override fun onResume() {
        checkLocationStatus()
        requireActivity().application.registerReceiver(locationProviderChangedReceiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
        super.onResume()
    }

    override fun onPause() {
        requireActivity().application.unregisterReceiver(locationProviderChangedReceiver)
        super.onPause()
    }

    override fun onDestroyView() {
        DfuServiceListenerHelper.unregisterProgressListener(requireContext(), dfuProgressListener)
        _fragmentDfuBinding = null
        super.onDestroyView()
    }
}
