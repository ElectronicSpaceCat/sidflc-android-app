package com.android.app.fragments.device.deviceCalibrate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceCalibrateBinding
import com.android.app.device.bluetooth.device.DeviceData
import com.android.app.device.sensor.ISensorCalibrate
import com.android.app.device.sensor.ISensorCalibrate.State
import com.android.app.fragments.device.deviceCalibrate.DeviceCalibrateViewModel.CalSelect
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import androidx.navigation.findNavController
import androidx.core.view.isGone

class DeviceCalibrateFragment : Fragment() {
    private var _fragmentDeviceCalibrateBinding: FragmentDeviceCalibrateBinding? = null
    private val fragmentDeviceCalibrateBinding get() = _fragmentDeviceCalibrateBinding!!

    private lateinit var viewModel: DeviceCalibrateViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _fragmentDeviceCalibrateBinding = FragmentDeviceCalibrateBinding.inflate(inflater, container, false)
        return fragmentDeviceCalibrateBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[DeviceCalibrateViewModel::class.java]

        fragmentDeviceCalibrateBinding.calibrationProgressBar.isIndeterminate = true

        /**
         * Observe connection state navigate back to scanner page on disconnect
         */
        DataShared.device.connectionState.observe(viewLifecycleOwner) { state ->
            val navController = requireActivity().findNavController(R.id.container_nav)
            if (state.state != ConnectionState.State.READY) {
                val options = NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, false)
                    .setLaunchSingleTop(true)
                    .build()
                navController.navigate(R.id.deviceScannerFragment, null, options)
            }
        }

        /**
         * Observe sensorCarrierPosition calibration status
         */
        ISensorCalibrate.calibrationState.observe(viewLifecycleOwner) { state ->
            when(state){
                State.INIT -> {
                    fragmentDeviceCalibrateBinding.deviceStartCalibration.visibility = View.GONE
                    fragmentDeviceCalibrateBinding.calBtnOk.visibility = View.GONE
                    fragmentDeviceCalibrateBinding.calBtnRestart.visibility = View.GONE
                    fragmentDeviceCalibrateBinding.deviceCalibrating.visibility = View.VISIBLE
                    fragmentDeviceCalibrateBinding.calibrationStatus.text = "Initializing..."
                    fragmentDeviceCalibrateBinding.calibrationStatus.visibility = View.VISIBLE
                    fragmentDeviceCalibrateBinding.calibrationProgressBar.visibility = View.VISIBLE
                    fragmentDeviceCalibrateBinding.calibrationInfo.visibility = View.VISIBLE
                }
                State.PREPARE -> {
                    fragmentDeviceCalibrateBinding.calibrationStatus.text = "Preparing..."
                }
                State.START -> {
                    fragmentDeviceCalibrateBinding.calibrationStatus.text = "Calibrating..."
                }
                State.FINISHED -> {
                    viewModel.stopCalibration()
                    if(CalSelect.CAL_ALL == viewModel.calSelected
                        && DeviceData.Sensor.Id.LONG != viewModel.activeSensorId) {
                        viewModel.startCalibration(CalSelect.CAL_SENSOR_LONG)
                    }
                    else{
                        fragmentDeviceCalibrateBinding.calibrationStatus.text = "Calibrated"
                        fragmentDeviceCalibrateBinding.calBtnOk.visibility = View.VISIBLE
                        fragmentDeviceCalibrateBinding.calBtnRestart.visibility = View.VISIBLE
                        fragmentDeviceCalibrateBinding.calibrationProgressBar.visibility = View.INVISIBLE
                        fragmentDeviceCalibrateBinding.calibrationInfo.visibility = View.GONE
                    }
                }
                State.ERROR -> {
                    viewModel.stopCalibration()
                    fragmentDeviceCalibrateBinding.calibrationStatus.text = "Calibration Failed"
                    fragmentDeviceCalibrateBinding.calBtnOk.visibility = View.VISIBLE
                    fragmentDeviceCalibrateBinding.calBtnRestart.visibility = View.VISIBLE
                    fragmentDeviceCalibrateBinding.calibrationProgressBar.visibility = View.INVISIBLE
                }
                else -> {}
            }
        }

        /**
         * Observe calibration msg
         */
        ISensorCalibrate.calibrationStateMsg.observe(viewLifecycleOwner) { msg ->
            fragmentDeviceCalibrateBinding.calibrationInfo.text = msg
        }

        /**
         * Setup a long-click listener to open the option for individual sensor calibrations (engineer mode)
         */
        fragmentDeviceCalibrateBinding.imageView.setOnLongClickListener {
            if(fragmentDeviceCalibrateBinding.calBtnAll.isGone){
                fragmentDeviceCalibrateBinding.calBtnAll.visibility = View.VISIBLE
                fragmentDeviceCalibrateBinding.calBtnSensor1.visibility = View.GONE
                fragmentDeviceCalibrateBinding.calBtnSensor2.visibility = View.GONE
            }
            else{
                fragmentDeviceCalibrateBinding.calBtnAll.visibility = View.GONE
                fragmentDeviceCalibrateBinding.calBtnSensor1.visibility = View.VISIBLE
                fragmentDeviceCalibrateBinding.calBtnSensor2.visibility = View.VISIBLE
            }

            true
        }

        /**
         * Set up button onClickListeners
         */
        fragmentDeviceCalibrateBinding.calBtnAll.setOnClickListener { onCalAllClicked() }
        fragmentDeviceCalibrateBinding.calBtnSensor1.setOnClickListener { onCalSensor1Clicked() }
        fragmentDeviceCalibrateBinding.calBtnSensor2.setOnClickListener { onCalSensor2Clicked() }
        fragmentDeviceCalibrateBinding.calBtnRestart.setOnClickListener { onRestartClicked() }
        fragmentDeviceCalibrateBinding.calBtnOk.setOnClickListener { onOkClicked() }


        /**
         * Set view visibilities
         */
        fragmentDeviceCalibrateBinding.deviceStartCalibration.visibility = View.VISIBLE
        fragmentDeviceCalibrateBinding.deviceCalibrating.visibility = View.GONE
    }

    private fun onCalAllClicked() {
        viewModel.startCalibration(CalSelect.CAL_ALL)
    }

    // Currently hidden
    private fun onCalSensor1Clicked() {
        viewModel.startCalibration(CalSelect.CAL_SENSOR_SHORT)
    }

    // Currently hidden
    private fun onCalSensor2Clicked() {
        viewModel.startCalibration(CalSelect.CAL_SENSOR_LONG)
    }

    private fun onRestartClicked() {
        viewModel.startCalibration()
    }

    private fun onOkClicked() {
        requireActivity().findNavController(R.id.container_nav).navigateUp()
    }

    override fun onResume() {
        viewModel.onResume()
        super.onResume()
    }

    override fun onDestroyView() {
        viewModel.onDestroy()

        // Null the viewBinding
        _fragmentDeviceCalibrateBinding = null
        super.onDestroyView()
    }
}