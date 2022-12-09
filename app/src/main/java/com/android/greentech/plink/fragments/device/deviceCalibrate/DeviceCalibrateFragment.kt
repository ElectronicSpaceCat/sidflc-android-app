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

package com.android.greentech.plink.fragments.device.deviceCalibrate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.android.greentech.plink.R
import com.android.greentech.plink.dataShared.DataShared
import com.android.greentech.plink.databinding.FragmentDeviceCalibrateBinding
import com.android.greentech.plink.device.sensor.ISensorCalibrate.State

class DeviceCalibrateFragment : Fragment() {
    private var _fragmentDeviceCalibrateBinding: FragmentDeviceCalibrateBinding? = null
    private val fragmentDeviceCalibrateBinding get() = _fragmentDeviceCalibrateBinding!!

    private var _prevSelectedSensor = DataShared.device.sensorSelected

    private enum class CalSelect {
        CAL_ALL,
        CAL_SENSOR_SHORT,
        CAL_SENSOR_LONG
    }

    private var _calSelect = CalSelect.CAL_ALL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _fragmentDeviceCalibrateBinding = FragmentDeviceCalibrateBinding.inflate(inflater, container, false)
        return fragmentDeviceCalibrateBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentDeviceCalibrateBinding.calibrationProgressBar.isIndeterminate = true

        /**
         * Observe sensorCarrierPosition calibration status
         */
        DataShared.device.sensorCarriagePosition.calibrationStateOnChange.observe(viewLifecycleOwner) { state ->
            when(state){
                State.PREPARE -> {
                    calPrepare()
                }
                State.START -> {
                    calStart()
                }
                State.FINISHED -> {
                    DataShared.device.sensorCarriagePosition.stopCalibration()
                    DataShared.device.sensorCarriagePosition.storeConfigData()

                    if(_calSelect == CalSelect.CAL_ALL) {
                        DataShared.device.sensorDeviceHeight.startCalibration()
                    }
                    else{
                        calFinished()
                    }
                }
                State.ERROR -> {
                    calError()
                }
                else -> {}
            }
        }

        /**
         * Observe sensorDeviceHeight calibration status
         */
        DataShared.device.sensorDeviceHeight.calibrationStateOnChange.observe(viewLifecycleOwner) { state ->
            when(state){
                State.PREPARE -> {
                    calPrepare()
                }
                State.START -> {
                    calStart()
                }
                State.FINISHED -> {
                    DataShared.device.sensorDeviceHeight.stopCalibration()
                    DataShared.device.sensorDeviceHeight.storeConfigData()

                    calFinished()
                }
                State.ERROR -> {
                    calError()
                }
                else -> {}
            }
        }

        /**
         * Observe sensorDeviceHeight calibration msg
         */
        DataShared.device.sensorDeviceHeight.calibrationStateMsgOnChange.observe(viewLifecycleOwner) { msg ->
            fragmentDeviceCalibrateBinding.calibrationInfo.text = msg
        }

        /**
         * Observe sensorCarrierPosition calibration msg
         */
        DataShared.device.sensorCarriagePosition.calibrationStateMsgOnChange.observe(viewLifecycleOwner) { msg ->
            fragmentDeviceCalibrateBinding.calibrationInfo.text = msg
        }

        fragmentDeviceCalibrateBinding.imageView.setOnLongClickListener {
            if(fragmentDeviceCalibrateBinding.calBtnAll.visibility == View.GONE){
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

        // Set up button onClickListeners
        fragmentDeviceCalibrateBinding.calBtnAll.setOnClickListener { onCalAllClicked() }
        fragmentDeviceCalibrateBinding.calBtnSensor1.setOnClickListener { onCalSensor1Clicked() }
        fragmentDeviceCalibrateBinding.calBtnSensor2.setOnClickListener { onCalSensor2Clicked() }
        fragmentDeviceCalibrateBinding.calBtnRestart.setOnClickListener { onRestartClicked() }
        fragmentDeviceCalibrateBinding.calBtnOk.setOnClickListener { onOkClicked() }

        fragmentDeviceCalibrateBinding.deviceStartCalibration.visibility = View.VISIBLE
        fragmentDeviceCalibrateBinding.deviceCalibrating.visibility = View.GONE
    }

    private fun calPrepare() {
        fragmentDeviceCalibrateBinding.deviceStartCalibration.visibility = View.GONE
        fragmentDeviceCalibrateBinding.calBtnOk.visibility = View.GONE
        fragmentDeviceCalibrateBinding.calBtnRestart.visibility = View.GONE

        fragmentDeviceCalibrateBinding.deviceCalibrating.visibility = View.VISIBLE

        fragmentDeviceCalibrateBinding.calibrationStatus.text = "Calibrating..."
        fragmentDeviceCalibrateBinding.calibrationStatus.visibility = View.VISIBLE
        fragmentDeviceCalibrateBinding.calibrationProgressBar.visibility = View.VISIBLE
        fragmentDeviceCalibrateBinding.calibrationInfo.visibility = View.VISIBLE
    }

    private fun calStart() {
        // Not implemented
    }

    private fun calFinished() {
        fragmentDeviceCalibrateBinding.calibrationStatus.text = "Calibrated"
        fragmentDeviceCalibrateBinding.calBtnOk.visibility = View.VISIBLE
        fragmentDeviceCalibrateBinding.calBtnRestart.visibility = View.VISIBLE
        fragmentDeviceCalibrateBinding.calibrationProgressBar.visibility = View.INVISIBLE
        fragmentDeviceCalibrateBinding.calibrationInfo.visibility = View.GONE
    }

    private fun calError() {
        DataShared.device.sensorCarriagePosition.stopCalibration()
        DataShared.device.sensorDeviceHeight.stopCalibration()

        fragmentDeviceCalibrateBinding.calibrationStatus.text = "Calibration Failed"
        fragmentDeviceCalibrateBinding.calBtnOk.visibility = View.VISIBLE
        fragmentDeviceCalibrateBinding.calBtnRestart.visibility = View.VISIBLE
        fragmentDeviceCalibrateBinding.calibrationProgressBar.visibility = View.INVISIBLE
    }

    private fun onCalAllClicked() {
        _calSelect = CalSelect.CAL_ALL
        DataShared.device.sensorCarriagePosition.startCalibration()
    }

    private fun onCalSensor1Clicked() {
        _calSelect = CalSelect.CAL_SENSOR_SHORT
        DataShared.device.sensorCarriagePosition.startCalibration()
    }

    private fun onCalSensor2Clicked() {
        _calSelect = CalSelect.CAL_SENSOR_LONG
        DataShared.device.sensorDeviceHeight.startCalibration()
    }

    private fun onRestartClicked() {
        when(_calSelect){
            CalSelect.CAL_SENSOR_SHORT,
            CalSelect.CAL_ALL -> {
                DataShared.device.sensorCarriagePosition.startCalibration()
            }
            CalSelect.CAL_SENSOR_LONG -> {
                DataShared.device.sensorDeviceHeight.startCalibration()
            }
        }
    }

    private fun onOkClicked() {
        Navigation.findNavController(requireActivity(), R.id.container_nav).navigateUp()
    }

    override fun onResume() {
        DataShared.device.setSensorEnable(false)
        super.onResume()
    }

    override fun onDestroyView() {
        // Selected the sensor that was active before entering the Calibration screen
        DataShared.device.setSensor(_prevSelectedSensor.value!!.id)

        // Ensure we stop any running cals on destroy
        DataShared.device.sensorCarriagePosition.stopCalibration()
        DataShared.device.sensorDeviceHeight.stopCalibration()

        DataShared.device.setSensorEnable(false)

        // Null the viewBinding
        _fragmentDeviceCalibrateBinding = null
        super.onDestroyView()
    }
}