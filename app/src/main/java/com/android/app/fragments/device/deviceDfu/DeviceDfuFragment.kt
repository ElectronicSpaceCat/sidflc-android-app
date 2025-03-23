package com.android.app.fragments.device.deviceDfu

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import com.android.app.R
import com.android.app.databinding.FragmentDeviceDfuBinding
import com.android.app.fragments.device.deviceDfu.DeviceDfuViewModel.Companion.UpdateStatus
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import androidx.navigation.findNavController

class DeviceDfuFragment : Fragment() {
    private var _fragmentDfuBinding: FragmentDeviceDfuBinding? = null
    private val fragmentDfuBinding get() = _fragmentDfuBinding!!

    private lateinit var viewModel: DeviceDfuViewModel
    private lateinit var dfuProgressListener : DfuProgressListenerAdapter

    private var _forceBoot = false

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
        viewModel.networkStatus.observe(viewLifecycleOwner) { hasNetwork ->
            if(!this.isResumed) return@observe
            if (hasNetwork) {
                viewModel.checkFirmwareVersion(requireContext())
            } else {
                viewModel.updateStatus = UpdateStatus.NO_NETWORK
            }
        }

        /**
         * Observe the device connections state. Entering the bootloader will cause a
         * device disconnection event which should be ignored while DFU is in progress.
         */
        viewModel.deviceConnectionState.observe(viewLifecycleOwner) { state: ConnectionState ->
            // Return if update in progress
            if (viewModel.updateStatus == UpdateStatus.UPDATING) {
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
                    requireActivity().findNavController(R.id.container_nav)
                        .navigate(R.id.deviceScannerFragment, null, options)
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
                UpdateStatus.NO_NETWORK -> {
                    fragmentDfuBinding.dfuProgress.text = "Internet connection required\nto check for updates."
                    fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE
                    fragmentDfuBinding.dfuButton.visibility = View.INVISIBLE
                }
                UpdateStatus.CHECKING_FIRMWARE -> {
                    fragmentDfuBinding.dfuProgress.text = "Checking firmware version"
                    fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE
                    fragmentDfuBinding.dfuButton.visibility = View.INVISIBLE
                }
                UpdateStatus.ON_LATEST_FIRMWARE -> {
                    fragmentDfuBinding.dfuProgress.text = "Firmware up to date"
                    fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE
                    fragmentDfuBinding.dfuButton.text = "Check"
                    fragmentDfuBinding.dfuButton.visibility = View.VISIBLE
                }
                UpdateStatus.UPDATE_AVAILABLE -> {
                    fragmentDfuBinding.dfuProgress.text = "Firmware ${viewModel.latestVersion} available!"
                    fragmentDfuBinding.dfuButton.text = "Update"
                    fragmentDfuBinding.dfuButton.visibility = View.VISIBLE
                    fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE
                }
                UpdateStatus.DOWNLOADING -> {
                    fragmentDfuBinding.dfuProgress.text = "Downloading firmware…"
                    fragmentDfuBinding.dfuProgressBar.progress = 0
                    fragmentDfuBinding.dfuProgressBar.isIndeterminate = false
                    fragmentDfuBinding.dfuProgressBar.visibility = View.VISIBLE
                    fragmentDfuBinding.dfuButton.visibility = View.INVISIBLE
                }
                UpdateStatus.DOWNLOADED-> {
                    fragmentDfuBinding.dfuProgress.text = "Downloaded firmware…"
                    fragmentDfuBinding.dfuProgressBar.isIndeterminate = false
                    fragmentDfuBinding.dfuProgressBar.visibility = View.VISIBLE
                    fragmentDfuBinding.dfuButton.visibility = View.INVISIBLE
                }
                UpdateStatus.UPDATING -> {
                    fragmentDfuBinding.dfuProgressBar.visibility = View.VISIBLE
                }
                UpdateStatus.ERROR -> {
                    viewModel.cleanUp()
                    fragmentDfuBinding.dfuProgress.text = "Update error"
                    fragmentDfuBinding.dfuButton.text = "Retry"
                    fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE
                    fragmentDfuBinding.dfuButton.visibility = View.VISIBLE
                }
                UpdateStatus.NA -> {
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
        fragmentDfuBinding.dfuProgress.setOnLongClickListener { onUpdateStatusLongClicked() }

        // NOTE: Not sure if dfu notify channel is needed..
        //DfuServiceInitiator.createDfuNotificationChannel(requireContext())

        setupDfuProgressListener()
    }

    private fun onBtnDfuClicked() {
        when(viewModel.updateStatus){
            UpdateStatus.UPDATE_AVAILABLE -> {
                viewModel.startFirmwareUpdate(requireContext())
            }
            UpdateStatus.ON_LATEST_FIRMWARE -> {
                if(_forceBoot) {
                    viewModel.startFirmwareUpdate(requireContext())
                }
                else{
                    viewModel.checkFirmwareVersion(requireContext())
                }
            }
            UpdateStatus.NA -> {
                viewModel.checkFirmwareVersion(requireContext())
            }
            else -> {}
        }
    }

    private fun onUpdateStatusLongClicked() : Boolean {
        if(viewModel.updateStatus == UpdateStatus.ON_LATEST_FIRMWARE){
            _forceBoot = !_forceBoot
            if(_forceBoot){
                fragmentDfuBinding.dfuButton.text = "Force Update"
            }
            else{
                fragmentDfuBinding.dfuButton.text = "Check"
            }
        }
        return true
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
                fragmentDfuBinding.dfuButton.text = "Retry"
                fragmentDfuBinding.dfuButton.visibility = View.VISIBLE
                fragmentDfuBinding.dfuProgressBar.visibility = View.INVISIBLE
                viewModel.cleanUp()
            }

            override fun onDfuAborted(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(no.nordicsemi.android.dfu.R.string.dfu_status_aborted)
            }

            override fun onDeviceDisconnecting(deviceAddress: String?) {
                fragmentDfuBinding.dfuProgress.setText(no.nordicsemi.android.dfu.R.string.dfu_status_disconnecting)
            }

            override fun onDeviceConnecting(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(no.nordicsemi.android.dfu.R.string.dfu_status_connecting)
            }

            override fun onDeviceConnected(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(no.nordicsemi.android.dfu.R.string.dfu_status_connecting)
            }

            override fun onEnablingDfuMode(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(no.nordicsemi.android.dfu.R.string.dfu_status_switching_to_dfu)
            }

            override fun onDfuProcessStarting(deviceAddress: String) {
                fragmentDfuBinding.dfuProgressBar.progress = 0
                fragmentDfuBinding.dfuProgressBar.isIndeterminate = false
                fragmentDfuBinding.dfuProgress.setText(no.nordicsemi.android.dfu.R.string.dfu_status_starting)
            }

            override fun onDfuProcessStarted(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(no.nordicsemi.android.dfu.R.string.dfu_status_starting)
            }

            override fun onFirmwareValidating(deviceAddress: String) {
                fragmentDfuBinding.dfuProgress.setText(no.nordicsemi.android.dfu.R.string.dfu_status_validating)
            }

            override fun onProgressChanged(deviceAddress: String, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
                fragmentDfuBinding.dfuProgress.setText(no.nordicsemi.android.dfu.R.string.dfu_status_uploading)
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

    override fun onDestroyView() {
        DfuServiceListenerHelper.unregisterProgressListener(requireContext(), dfuProgressListener)
        _fragmentDfuBinding = null
        super.onDestroyView()
    }
}
