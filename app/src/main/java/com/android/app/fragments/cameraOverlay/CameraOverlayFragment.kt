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

package com.android.app.fragments.cameraOverlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentCameraOverlayBinding
import com.android.app.device.bluetooth.pwrmonitor.PwrMonitorData
import com.android.app.device.bluetooth.device.DeviceData
import com.android.app.device.projectile.ProjectilePrefUtils
import com.android.app.fragments.dialogs.ListDialogFragment
import com.android.app.utils.converters.ConvertLength
import com.android.app.utils.converters.ConvertLength.Unit
import com.android.app.utils.converters.LengthData
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import java.util.*
import kotlin.math.roundToInt

/** Fragment used to overlay the user interface over the camera preview */
class CameraOverlayFragment internal constructor() : Fragment() {
    private var _fragmentCameraOverlayBinding: FragmentCameraOverlayBinding? = null
    private val fragmentCameraOverlayBinding get() = _fragmentCameraOverlayBinding!!

    private lateinit var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener

    private var _shouldClearEngData = true

    private lateinit var viewModel: CameraOverlayViewModel
    private lateinit var btnPhoneHeight: BallisticsButton<LengthData>
    private lateinit var btnTargetDistance: BallisticsButton<LengthData>
    private lateinit var btnTargetHeight: BallisticsButton<LengthData>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _fragmentCameraOverlayBinding = FragmentCameraOverlayBinding.inflate(inflater, container, false)
        return fragmentCameraOverlayBinding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CameraOverlayViewModel::class.java]

        /**
         * Update UI components that are dependent on sharedPreferences
         */
        preferencesInit(requireContext())

        /**
         * Init the engineering view units
         */
        initEngViewUnits()

        /**
         * Set up the carriage position bars
         */
        fragmentCameraOverlayBinding.carriagePositionSeekBarAuto.isIndeterminate = false
        fragmentCameraOverlayBinding.carriagePositionSeekBarAuto.setOnTouchListener { _, _ -> true }

        /**
         * Set up the estimated hit confidence indicator bar
         */
        fragmentCameraOverlayBinding.hitConfidenceBar.max = 100
        fragmentCameraOverlayBinding.hitConfidenceBar.isIndeterminate = false

        /**
         * Setup the ballistics button for Device Height
         */
        btnPhoneHeight = object : BallisticsButton<LengthData>("PH", "Phone Height",
            fragmentCameraOverlayBinding.phoneHeight,
            DataShared.phoneHeight,
            View.GONE) {
            override fun onButtonClick() {
                super.onButtonClick()
                if (btnPhoneHeight.acquisitionMode == Mode.AUTO) {
                    if (viewModel.dataToGet != DataType.DEVICE_HEIGHT) {
                        btnPhoneHeight.dataStatus = DataStatus.NOT_SET
                        viewModel.dataToGet = DataType.DEVICE_HEIGHT
                        if (btnTargetDistance.acquisitionMode == Mode.AUTO &&
                            (btnTargetDistance.dataStatus == DataStatus.NOT_SET || btnTargetDistance.lockStatus == LockStatus.UNLOCKED)
                        ) {
                            btnTargetDistance.enable(false)
                        }
                        if (btnTargetHeight.acquisitionMode == Mode.AUTO &&
                            (btnTargetHeight.dataStatus == DataStatus.NOT_SET || btnTargetHeight.lockStatus == LockStatus.UNLOCKED)
                        ) {
                            btnTargetHeight.enable(false)
                        }
                    } else if (!viewModel.isCalculationPaused) {
                        btnPhoneHeight.dataStatus = DataStatus.SET
                        if (btnTargetDistance.acquisitionMode == Mode.AUTO &&
                            (btnTargetDistance.dataStatus == DataStatus.NOT_SET || btnTargetDistance.lockStatus == LockStatus.UNLOCKED)
                        ) {
                            viewModel.dataToGet = DataType.TARGET_DISTANCE
                            btnTargetDistance.enable(true)
                        } else if (btnTargetHeight.acquisitionMode == Mode.AUTO &&
                            (btnTargetHeight.dataStatus == DataStatus.NOT_SET || btnTargetHeight.lockStatus == LockStatus.UNLOCKED)
                        ) {
                            btnTargetHeight.dataStatus = DataStatus.NOT_SET
                            viewModel.dataToGet = DataType.TARGET_HEIGHT
                            btnTargetHeight.enable(true)
                        } else {
                            viewModel.dataToGet = DataType.NONE
                        }
                    }
                }
            }

            override fun onAcquisitionModeClick() {
                super.onAcquisitionModeClick()
                if (btnPhoneHeight.acquisitionMode == Mode.AUTO) {
                    viewModel.dataToGet = DataType.DEVICE_HEIGHT
                    if (DataShared.device.connectionState.value?.isReady!!) {
                        btnPhoneHeight.dataStatus = DataStatus.NOT_SET
                    } else {
                        btnPhoneHeight.enable(false)
                    }

                    if (btnTargetDistance.acquisitionMode == Mode.AUTO &&
                        (btnTargetDistance.dataStatus == DataStatus.NOT_SET || btnTargetDistance.lockStatus == LockStatus.UNLOCKED)
                    ) {
                        btnTargetDistance.enable(false)
                    }
                    if (btnTargetHeight.acquisitionMode == Mode.AUTO &&
                        (btnTargetHeight.dataStatus == DataStatus.NOT_SET || btnTargetHeight.lockStatus == LockStatus.UNLOCKED)
                    ) {
                        btnTargetHeight.enable(false)
                    }
                } else {
                    if (btnTargetDistance.acquisitionMode == Mode.AUTO && btnTargetDistance.dataStatus == DataStatus.NOT_SET) {
                        btnTargetDistance.enable(true)
                        viewModel.dataToGet = DataType.TARGET_DISTANCE

                        btnTargetHeight.enable(false)
                    } else if (btnTargetHeight.acquisitionMode == Mode.AUTO && btnTargetHeight.dataStatus == DataStatus.NOT_SET) {
                        btnTargetHeight.enable(true)
                        viewModel.dataToGet = DataType.TARGET_HEIGHT
                    } else if (btnTargetDistance.dataStatus == DataStatus.SET && btnTargetHeight.dataStatus == DataStatus.SET) {
                        viewModel.dataToGet = DataType.NONE
                    }
                }
            }

            override fun onButtonClickDisabled() {
                if (btnPhoneHeight.dataStatus == DataStatus.NOT_SET && !DataShared.device.connectionState.value?.isReady!!) {
                    Toast.makeText(requireContext(),
                        "Device connection required",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        /**
         * Setup the ballistics button for Target Distance
         */
        btnTargetDistance = object : BallisticsButton<LengthData>("TD", "Target Distance",
            fragmentCameraOverlayBinding.targetDistance,
            DataShared.targetDistance,
            View.VISIBLE) {
            override fun onButtonClick() {
                super.onButtonClick()
                if (btnTargetDistance.acquisitionMode == Mode.AUTO && btnPhoneHeight.dataStatus == DataStatus.SET) {
                    if (viewModel.dataToGet != DataType.TARGET_DISTANCE) {
                        viewModel.dataToGet = DataType.TARGET_DISTANCE
                        btnTargetDistance.dataStatus = DataStatus.NOT_SET

                        if (btnTargetHeight.acquisitionMode == Mode.AUTO &&
                            (btnTargetHeight.dataStatus == DataStatus.NOT_SET || btnTargetHeight.lockStatus == LockStatus.UNLOCKED)
                        ) {
                            btnTargetHeight.enable(false)
                        }
                    } else if (!viewModel.isCalculationPaused) {
                        btnTargetDistance.dataStatus = DataStatus.SET
                        if (btnTargetHeight.acquisitionMode == Mode.AUTO &&
                            (btnTargetHeight.dataStatus == DataStatus.NOT_SET || btnTargetHeight.lockStatus == LockStatus.UNLOCKED)
                        ) {
                            viewModel.dataToGet = DataType.TARGET_HEIGHT
                            btnTargetHeight.enable(true)
                        } else {
                            viewModel.dataToGet = DataType.NONE
                        }
                    }
                }
            }

            override fun onAcquisitionModeClick() {
                super.onAcquisitionModeClick()
                if (btnTargetDistance.acquisitionMode == Mode.AUTO) {
                    if (btnPhoneHeight.dataStatus == DataStatus.NOT_SET) {
                        btnTargetDistance.enable(false)
                    } else {
                        btnTargetDistance.dataStatus = DataStatus.NOT_SET
                        viewModel.dataToGet = DataType.TARGET_DISTANCE
                    }

                    if (btnTargetHeight.acquisitionMode == Mode.AUTO &&
                        (btnTargetHeight.dataStatus == DataStatus.NOT_SET || btnTargetHeight.lockStatus == LockStatus.UNLOCKED)
                    ) {
                        btnTargetHeight.enable(false)
                    }
                } else if (btnTargetHeight.acquisitionMode == Mode.AUTO && btnTargetHeight.dataStatus == DataStatus.NOT_SET
                    && btnPhoneHeight.dataStatus == DataStatus.SET
                ) {
                    btnTargetHeight.enable(true)
                    viewModel.dataToGet = DataType.TARGET_HEIGHT
                } else if (btnPhoneHeight.dataStatus == DataStatus.SET && btnTargetHeight.dataStatus == DataStatus.SET) {
                    viewModel.dataToGet = DataType.NONE
                }
            }

            override fun onButtonClickDisabled() {
                if (btnPhoneHeight.dataStatus == DataStatus.NOT_SET) {
                    Toast.makeText(requireContext(),
                        "Device height (DH) required",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        /**
         * Setup the ballistics button for Target Height
         */
        btnTargetHeight = object : BallisticsButton<LengthData>("TH", "Target Height",
            fragmentCameraOverlayBinding.targetHeight,
            DataShared.targetHeight,
            View.VISIBLE) {
            override fun onButtonClick() {
                super.onButtonClick()
                if (btnTargetHeight.acquisitionMode == Mode.AUTO && btnPhoneHeight.dataStatus == DataStatus.SET && btnTargetDistance.dataStatus == DataStatus.SET) {
                    if (viewModel.dataToGet != DataType.TARGET_HEIGHT) {
                        viewModel.dataToGet = DataType.TARGET_HEIGHT
                        btnTargetHeight.dataStatus = DataStatus.NOT_SET
                    } else if (!viewModel.isCalculationPaused) {
                        btnTargetHeight.dataStatus = DataStatus.SET
                        viewModel.dataToGet = DataType.NONE
                    }
                }
            }

            override fun onAcquisitionModeClick() {
                super.onAcquisitionModeClick()
                if (btnTargetHeight.acquisitionMode == Mode.AUTO) {
                    if (btnPhoneHeight.dataStatus == DataStatus.NOT_SET || btnTargetDistance.dataStatus == DataStatus.NOT_SET) {
                        btnTargetHeight.enable(false)
                    } else {
                        btnTargetHeight.dataStatus = DataStatus.NOT_SET
                        btnTargetHeight.enable(true)
                        viewModel.dataToGet = DataType.TARGET_HEIGHT
                    }
                } else if (btnPhoneHeight.dataStatus == DataStatus.SET && btnTargetDistance.dataStatus == DataStatus.SET) {
                    btnTargetHeight.dataStatus = DataStatus.SET
                    viewModel.dataToGet = DataType.NONE
                }
            }

            override fun onButtonClickDisabled() {
                if (btnTargetDistance.dataStatus == DataStatus.NOT_SET) {
                    Toast.makeText(requireContext(),
                        "Target distance (TD) required\nfor target height (TH)",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        /**
         * OnClick for selecting the projectile
         */
        fragmentCameraOverlayBinding.projectileSelected.setOnClickListener {
            val projectiles = ProjectilePrefUtils.getProjectileList(requireContext()).toTypedArray()
            val projectileNames : Array<String> = Array(projectiles.size) {
                projectiles[it].name
            }

            val projectileSelected = ProjectilePrefUtils.getProjectileSelected(requireContext())
            val projectileSelectedName = projectileSelected?.name ?: ""

            val projectileSelectedListener = object : ListDialogFragment.OnItemSelectedListener {
                override fun onItemSelectedListener(name: String) {
                    ProjectilePrefUtils.setProjectileSelected(requireContext(), name)
                }
            }

            ListDialogFragment(
                getString(R.string.header_projectile),
                projectileSelectedName,
                projectileNames,
                projectileSelectedListener
            ).show(requireActivity().supportFragmentManager, null)
        }

        /**
         * OnLongClick for opening Projectile Editor screen
         */
        fragmentCameraOverlayBinding.projectileSelected.setOnLongClickListener {
            Navigation.findNavController(requireActivity(), R.id.container_nav)
                .navigate(R.id.projectileEditFragment)
            true
        }

        /**
         * OnClick shortcut for opening the Scanner screen
         */
        fragmentCameraOverlayBinding.deviceConnectionStatus.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.container_nav)
                .navigate(R.id.deviceScannerFragment)
        }

        /**
         * OnLongClick for opening Engineering view.
         */
        fragmentCameraOverlayBinding.hitConfidence.setOnLongClickListener {
            viewModel.isEngViewActive = !viewModel.isEngViewActive
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            prefs.edit()
                .putBoolean(requireContext().getString(R.string.PREFERENCE_FILTER_ENGINEERING_VIEW),
                    viewModel.isEngViewActive)
                .apply()

            true
        }

        /**
         * Observe the carriage position unit type
         */
        DataShared.carriagePosition.unitOnChange.observe(viewLifecycleOwner) { unit ->
            // Set unit text
            fragmentCameraOverlayBinding.carriagePositionUnit.text =
                ("(").plus(DataShared.carriagePosition.unitStr() + ")")
            // Set data precision
            when (unit) {
                Unit.MM -> {
                    DataShared.carriagePosition.precision = 0
                }
                Unit.CM -> {
                    DataShared.carriagePosition.precision = 1
                }
                Unit.IN -> {
                    DataShared.carriagePosition.precision = 2
                }
                else -> {}
            }

            // Copy the unit type from carriagePosition
            DataShared.carriagePositionOverride.setUnit(unit)
            DataShared.carriagePositionOverride.precision = DataShared.carriagePosition.precision
        }

        /**
         * Observe position auto mode
         */
        viewModel.isPositionAutoModeOnChange.observe(viewLifecycleOwner) {
            // Set the carriage max position which depends on the unit selected
            val maxPos = ConvertLength.convert(
                Unit.MM,
                DataShared.carriagePosition.unit,
                DataShared.device.model.getMaxCarriagePosition()
            )

            val str = String.format(
                Locale.getDefault(),
                "%.${DataShared.carriagePosition.precision}f",
                maxPos
            )

            fragmentCameraOverlayBinding.carriagePositionMax.text = str
            fragmentCameraOverlayBinding.carriagePositionOverrideMax.text = str

            when (it) {
                false -> { // Manual mode
                    // Set the position value to match the manual seek bar value
                    fragmentCameraOverlayBinding.carriagePositionValueOverride.text = DataShared.carriagePositionOverride.valueStr()
                    // Setup periodic task
                    runCarriageOverrideTask()
                    fragmentCameraOverlayBinding.carriagePositionSeekBarManual.isEnabled = true
                    fragmentCameraOverlayBinding.carriagePosManual.visibility = View.VISIBLE
                    fragmentCameraOverlayBinding.carriagePositionSeekBarAuto.max = DataShared.device.model.getMaxCarriagePosition().toInt()
                    fragmentCameraOverlayBinding.carriagePositionSeekBarManual.max = DataShared.device.model.getMaxCarriagePosition().toInt()
                    fragmentCameraOverlayBinding.carriagePositionSeekBarManual.progress = DataShared.carriagePositionOverride.getConverted(Unit.MM).toInt()
                }
                true -> { // Auto mode
                    fragmentCameraOverlayBinding.carriagePositionSeekBarManual.isEnabled = false
                    fragmentCameraOverlayBinding.carriagePosManual.visibility = View.GONE
                    fragmentCameraOverlayBinding.carriagePositionSeekBarAuto.max = DataShared.device.model.getMaxCarriagePosition().toInt()
                    fragmentCameraOverlayBinding.carriagePositionSeekBarManual.max = DataShared.device.model.getMaxCarriagePosition().toInt()
                    fragmentCameraOverlayBinding.carriagePositionSeekBarAuto.progress = DataShared.carriagePosition.getConverted(Unit.MM).toInt()
                    fragmentCameraOverlayBinding.carriagePositionSeekBarManual.progress = DataShared.carriagePosition.getConverted(Unit.MM).toInt()
                }
            }
        }

        /**
         * Observe engineering view enable
         */
        viewModel.isEngViewActiveOnChange.observe(viewLifecycleOwner) {
            if (it) {
                fragmentCameraOverlayBinding.engineerView.visibility = View.VISIBLE
            } else {
                fragmentCameraOverlayBinding.engineerView.visibility = View.INVISIBLE
            }
        }

        /**
         * OnLongClick for switching from manual to auto carriage position.
         */
        fragmentCameraOverlayBinding.carriagePosition.setOnLongClickListener {
            viewModel.isPositionAutoMode = !viewModel.isPositionAutoMode
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            prefs.edit().putBoolean(requireContext().getString(R.string.PREFERENCE_FILTER_CARRIAGE_POSITION_MODE), viewModel.isPositionAutoMode).apply()
            true
        }

        /**
         * Set onClick for sensors available prompt
         */
        fragmentCameraOverlayBinding.sensorsAvailablePrompt.btnSensorsOk.setOnClickListener {
            fragmentCameraOverlayBinding.sensorsAvailablePrompt.root.visibility = View.GONE
        }

        /**
         * Observe the dataToGet for enabling/disabling the button data
         * NOTE: This only updates when value is different than last
         */
        viewModel.dataToGetLive.observe(viewLifecycleOwner) { data ->
            // Update the button visuals and select sensor if needed
            when (data!!) {
                DataType.DEVICE_HEIGHT -> {
                    fragmentCameraOverlayBinding.reticleDialPitch.setImageLevel(1)
                    if (DataShared.device.connectionState.value?.isReady!!
                        && btnPhoneHeight.acquisitionMode == BallisticsButton.Mode.AUTO
                    ) {
                        DataShared.device.setSensor(DeviceData.Sensor.Id.LONG)
                        DataShared.device.setSensorEnable(true)
                    }
                }
                DataType.TARGET_HEIGHT -> {
                    fragmentCameraOverlayBinding.reticleDialPitch.setImageLevel(2)
                    DataShared.device.setSensorEnable(false)
                }
                DataType.TARGET_DISTANCE -> {
                    DataShared.device.setSensorEnable(false)
                }
                DataType.NONE -> {
                    fragmentCameraOverlayBinding.reticleDialPitch.setImageLevel(0)
                    if (DataShared.device.connectionState.value?.isReady!!) {
                        DataShared.device.setSensor(DeviceData.Sensor.Id.SHORT)
                        DataShared.device.setSensorEnable(true)
                    }
                }
                DataType.NA -> {
                    DataShared.device.setSensorEnable(false)
                }
            }

            // Clear the carriage position data
            if (data != DataType.NONE) {
                fragmentCameraOverlayBinding.carriagePositionSeekBarAuto.progress = 0
                if (viewModel.isPositionAutoMode) {
                    fragmentCameraOverlayBinding.carriagePositionSeekBarManual.progress = 0
                    fragmentCameraOverlayBinding.carriagePositionValue.text = getString(R.string.value_unknown)
                }
            }

            // Clear the ballistics visual data
            clearEngData()
            clearEngBallisticsData()
        }

        /**
         * Observe the calculation flag which drives visual effects
         * NOTE: This only updates when value is different than last
         */
        viewModel.isCalculationPausedLive.observe(viewLifecycleOwner) { paused ->
            // Update the button visuals
            when (viewModel.dataToGet) {
                DataType.DEVICE_HEIGHT -> {
                    btnPhoneHeight.dataUpdateEnable = !paused
                }
                DataType.TARGET_DISTANCE -> {
                    btnTargetDistance.dataUpdateEnable = !paused
                }
                DataType.TARGET_HEIGHT -> {
                    btnTargetHeight.dataUpdateEnable = !paused
                }
                DataType.NONE,
                DataType.NA, -> {
                }
            }

            // Clear the engineer data
            clearEngData()
            clearEngBallisticsData()
        }

        /**
         * Observe the roll value to update visuals
         */
        viewModel.gyro.rollOnChange.observe(viewLifecycleOwner) { deg ->
            fragmentCameraOverlayBinding.reticleDialRoll.rotation = deg.toFloat()
            fragmentCameraOverlayBinding.rollData.text = String.format(
                Locale.getDefault(),
                "%d°",
                deg.roundToInt()
            )
            // Update roll text color
            fragmentCameraOverlayBinding.rollData.isEnabled = viewModel.isRollInRange
        }

        /**
         * Observe the pitch value to update visuals
         */
        viewModel.gyro.pitchOnChange.observe(viewLifecycleOwner) { deg ->
            fragmentCameraOverlayBinding.reticleDialPitch.rotation = deg.toFloat()
            fragmentCameraOverlayBinding.pitchData.text = String.format(
                Locale.getDefault(),
                "%d°",
                deg.roundToInt()
            )
            // Update pitch text color
            fragmentCameraOverlayBinding.pitchData.isEnabled = viewModel.isPitchInRange
        }

        /**
         * Observe carriage position sensor and update the shared data for carriage position
         */
        DataShared.device.sensorCarriagePosition.rangeFilteredLive.observe(viewLifecycleOwner) {
            if (viewModel.dataToGet == DataType.NONE
                // Only update the value when sensor is ready
                && DataShared.device.connectionState.value!!.isReady
            ) {
                DataShared.carriagePosition.setValue(Unit.MM, it.roundToInt().toDouble())
            }
        }

        /**
         * Observe carriage position
         */
        DataShared.carriagePosition.valueOnChange.observe(viewLifecycleOwner) {
            if (viewModel.isPositionAutoMode) {
                updateEngData(DataShared.carriagePosition.getConverted(Unit.MM))
                fragmentCameraOverlayBinding.carriagePositionSeekBarManual.progress = DataShared.carriagePosition.getConverted(Unit.MM).toInt()
            }
            fragmentCameraOverlayBinding.carriagePositionValue.text = DataShared.carriagePosition.valueStr()
            fragmentCameraOverlayBinding.carriagePositionSeekBarAuto.progress = DataShared.carriagePosition.getConverted(Unit.MM).toInt()
        }

        /**
         * Observe carriage position override
         */
        DataShared.carriagePositionOverride.valueOnChange.observe(viewLifecycleOwner) {
            if (!viewModel.isPositionAutoMode) {
                updateEngData(DataShared.carriagePositionOverride.getConverted(Unit.MM))
                fragmentCameraOverlayBinding.carriagePositionValueOverride.text = DataShared.carriagePositionOverride.valueStr()
            }
        }

        /**
         * Observe if ballisticsDataReady to update the visual data
         */
        viewModel.hitConfidence.observe(viewLifecycleOwner) {
            if(viewModel.isCalculationPaused) return@observe
            updateEngBallisticsData()
        }

        /**
         * Observe the power source to set the indicator if USB, BATTERY, or NONE
         */
        DataShared.device.pwrSource.observe(viewLifecycleOwner) { source ->
            if (source == PwrMonitorData.InputSource.USB) {
                fragmentCameraOverlayBinding.usb.visibility = View.VISIBLE
            } else {
                fragmentCameraOverlayBinding.usb.visibility = View.INVISIBLE
            }
        }

        /**
         * Observe the battery status and set the indicator icon
         */
        DataShared.device.batteryStatus.observe(viewLifecycleOwner) { status ->
            when(status){
                PwrMonitorData.BattStatus.OK,
                PwrMonitorData.BattStatus.LOW,
                PwrMonitorData.BattStatus.CHARGING_COMPLETE, -> {
                    fragmentCameraOverlayBinding.battery.setImageLevel(0)
                }
                PwrMonitorData.BattStatus.VERY_LOW -> {
                    fragmentCameraOverlayBinding.battery.setImageLevel(1)
                }
                PwrMonitorData.BattStatus.CHARGING -> {
                    fragmentCameraOverlayBinding.battery.setImageLevel(2)
                }
                PwrMonitorData.BattStatus.UNKNOWN -> {
                    fragmentCameraOverlayBinding.battery.setImageLevel(3)
                }
                else -> {}
            }
        }

        /**
         * Observe the device connection status
         */
        DataShared.device.connectionState.observe(viewLifecycleOwner) { state ->
            when (state.state) {
                ConnectionState.State.CONNECTING -> {
                    // NOTE: Will hit this when set to autoConnect even if user turned off device
                    fragmentCameraOverlayBinding.deviceInfo.text = getString(R.string.state_disconnected)
                    fragmentCameraOverlayBinding.deviceInfo.isEnabled = false
                }
                ConnectionState.State.INITIALIZING -> {
                    fragmentCameraOverlayBinding.deviceInfo.text = getString(R.string.state_initializing)
                    fragmentCameraOverlayBinding.deviceInfo.isEnabled = true
                }
                ConnectionState.State.READY -> {
                    fragmentCameraOverlayBinding.deviceInfo.text = getString(R.string.state_connected)
                    fragmentCameraOverlayBinding.deviceInfo.isEnabled = true
                    fragmentCameraOverlayBinding.battery.visibility = View.VISIBLE
                    resetBallisticButtons()
                }
                ConnectionState.State.DISCONNECTING -> {
                    fragmentCameraOverlayBinding.deviceInfo.text = getString(R.string.state_disconnecting)
                }
                ConnectionState.State.DISCONNECTED -> {
                    fragmentCameraOverlayBinding.deviceInfo.text = getString(R.string.state_disconnected)
                    fragmentCameraOverlayBinding.deviceInfo.isEnabled = false
                    fragmentCameraOverlayBinding.battery.visibility = View.INVISIBLE
                    fragmentCameraOverlayBinding.usb.visibility = View.INVISIBLE
                    DataShared.carriagePosition.setValue(0.0)
                    fragmentCameraOverlayBinding.carriagePositionSeekBarAuto.progress = 0
                    fragmentCameraOverlayBinding.carriagePositionValue.text = getString(R.string.value_unknown)
                    if(viewModel.isPositionAutoMode){
                        fragmentCameraOverlayBinding.carriagePositionSeekBarManual.progress = 0
                        fragmentCameraOverlayBinding.carriagePositionValue.text = getString(R.string.value_unknown)
                        clearEngData()
                        clearEngBallisticsData()
                    }
                    resetBallisticButtons()
                }
                else -> {}
            }
        }
    }

    /**
     * Reset the ballistic buttons
     */
    private fun resetBallisticButtons() {
        if (btnPhoneHeight.acquisitionMode == BallisticsButton.Mode.MANUAL) {
            if (btnTargetDistance.acquisitionMode == BallisticsButton.Mode.AUTO
                && btnTargetDistance.dataStatus != BallisticsButton.DataStatus.SET) {

                btnTargetDistance.dataStatus = BallisticsButton.DataStatus.NOT_SET
                btnTargetDistance.enable(true)
                btnTargetHeight.enable(false)
                viewModel.dataToGet = DataType.TARGET_DISTANCE
            }
            else if (btnTargetHeight.acquisitionMode == BallisticsButton.Mode.AUTO
                && btnTargetHeight.dataStatus != BallisticsButton.DataStatus.SET ) {

                btnTargetHeight.dataStatus = BallisticsButton.DataStatus.NOT_SET
                btnTargetHeight.enable(true)
                viewModel.dataToGet = DataType.TARGET_HEIGHT
            }
            else if(!viewModel.isPositionAutoMode || DataShared.device.connectionState.value?.isReady!!){
                viewModel.dataToGet = DataType.NONE
            }
            else {
                viewModel.dataToGet = DataType.NA
            }
        }
        else {
            btnPhoneHeight.dataStatus = BallisticsButton.DataStatus.NOT_SET

            if(DataShared.device.connectionState.value?.isReady!!){
                btnPhoneHeight.enable(true)
                viewModel.dataToGet = DataType.DEVICE_HEIGHT
            }
            else{
                btnPhoneHeight.enable(false)
                viewModel.dataToGet = DataType.NA
            }
            btnTargetDistance.enable(false)
            btnTargetHeight.enable(false)
        }
    }

    /**
     * Run a periodic scheduled task to emulate the
     * carriage position when in override mode
     */
    private fun runCarriageOverrideTask() {
        Timer(LOOPER_TASK, false).schedule(object : TimerTask() {
            override fun run() {
                if(_fragmentCameraOverlayBinding == null){
                    this.cancel()
                }
                else if(!viewModel.isPositionAutoMode){
                    DataShared.carriagePositionOverride.postValue(Unit.MM, fragmentCameraOverlayBinding.carriagePositionSeekBarManual.progress.toDouble())
                }
                else{
                    this.cancel()
                    viewLifecycleOwner.lifecycleScope.launch {
                        clearEngData()
                        clearEngBallisticsData()
                    }
                }
            }
        },0, 20)
    }

    /**
     * Function used to setup a listener for onPreferenceChanges
     */
    private fun preferencesInit(context: Context) {
        // Init preference dependent data
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.all.keys.forEach{ key ->
            preferencesHandler(context, prefs, key)
        }

        // Setup an on-change listener
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { pref: SharedPreferences?, key: String? ->
            if (pref != null && key != null) {
                preferencesHandler(context, pref, key)
            }
        }
    }

    /**
     * Handler for preferences
     */
    private fun preferencesHandler(context: Context, pref: SharedPreferences?, key: String) {
        when(key){
            /** Preference - Selected Projectile */
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_SELECTED) -> {
                val projectile = ProjectilePrefUtils.getProjectileSelected(context)
                DataShared.device.model.setProjectile(projectile)
                if(projectile != null){
                    fragmentCameraOverlayBinding.projectileSelectedName.text = projectile.name
                }
                else{
                    fragmentCameraOverlayBinding.projectileSelectedName.text = getString(R.string.value_unknown)
                }
            }
            /** Preference - carriage Position Mode */
            context.getString(R.string.PREFERENCE_FILTER_CARRIAGE_POSITION_MODE) -> {
                viewModel.isPositionAutoMode = pref!!.getBoolean(key, true)
            }
            /** Preference - Engineering View Active */
            context.getString(R.string.PREFERENCE_FILTER_ENGINEERING_VIEW) -> {
                viewModel.isEngViewActive = pref!!.getBoolean(key, true)
            }
        }
    }

    private fun updateEngData(position : Double) {
        // If engineering view is visible then display the data
        if(fragmentCameraOverlayBinding.engineerView.visibility == View.VISIBLE) {
            fragmentCameraOverlayBinding.engineerData.phoneHeightData.text = DataShared.phoneHeight.valueStr()

            fragmentCameraOverlayBinding.engineerData.targetDistanceData.text = DataShared.targetDistance.valueStr()

            fragmentCameraOverlayBinding.engineerData.targetHeightData.text = DataShared.targetHeight.valueStr()

            fragmentCameraOverlayBinding.engineerData.springAngleData.text = String.format(
                Locale.getDefault(),
                "%.2f",
                DataShared.device.model.getSpringAngleAtPosition(position)
            )
            fragmentCameraOverlayBinding.engineerData.energyStoredData.text = String.format(
                Locale.getDefault(),
                "%.2f",
                DataShared.device.model.getPotentialEnergyAtPosition(position)
            )
        }
    }

    private fun updateEngBallisticsData() {
        // Update the hit confidence progress bar
        fragmentCameraOverlayBinding.hitConfidenceBar.progress = viewModel.hitConfidence.value!!.toInt()
        // Update the hit confidence text value
        fragmentCameraOverlayBinding.hitConfidenceValue.text = String.format(
            Locale.getDefault(),
            "%.1f%%",
            viewModel.hitConfidence.value!!
        )

        // If engineering view is visible then display the data
        if(fragmentCameraOverlayBinding.engineerView.visibility == View.VISIBLE) {
            fragmentCameraOverlayBinding.engineerData.launchHeightData.text = String.format(
                Locale.getDefault(),
                "%.3f",
                ConvertLength.convert(Unit.M,
                    DataShared.phoneHeight.unit,
                    viewModel.getAdjustedLaunchHeight)
            )
            fragmentCameraOverlayBinding.engineerData.targetDistanceAdjData.text = String.format(
                Locale.getDefault(),
                "%.3f",
                ConvertLength.convert(Unit.M,
                    DataShared.targetDistance.unit,
                    viewModel.adjustedTargetDistance)
            )
            fragmentCameraOverlayBinding.engineerData.velocityData.text = String.format(
                Locale.getDefault(),
                "%.3f",
                viewModel.velocity
            )
            fragmentCameraOverlayBinding.engineerData.impactDistanceData.text = String.format(
                Locale.getDefault(),
                "%.3f",
                viewModel.impactDistance
            )
            fragmentCameraOverlayBinding.engineerData.impactHeightData.text = String.format(
                Locale.getDefault(),
                "%.3f",
                viewModel.impactHeight
            )
            fragmentCameraOverlayBinding.engineerData.netEnergyStoredData.text = String.format(
                Locale.getDefault(),
                "%.2f",
                viewModel.netPotentialEnergy
            )
        }
    }

    private fun clearEngData() {
        fragmentCameraOverlayBinding.hitConfidenceBar.progress = 0
        fragmentCameraOverlayBinding.hitConfidenceValue.text = getString(R.string.value_unknown)

        if(fragmentCameraOverlayBinding.engineerView.visibility == View.VISIBLE){
            fragmentCameraOverlayBinding.engineerData.launchHeightData.text = getString(R.string.value_unknown)
            fragmentCameraOverlayBinding.engineerData.targetDistanceAdjData.text = getString(R.string.value_unknown)
            fragmentCameraOverlayBinding.engineerData.velocityData.text = getString(R.string.value_unknown)
            fragmentCameraOverlayBinding.engineerData.impactDistanceData.text = getString(R.string.value_unknown)
            fragmentCameraOverlayBinding.engineerData.impactHeightData.text = getString(R.string.value_unknown)
            fragmentCameraOverlayBinding.engineerData.netEnergyStoredData.text = getString(R.string.value_unknown)
        }
    }

    private fun clearEngBallisticsData() {
        if(fragmentCameraOverlayBinding.engineerView.visibility == View.VISIBLE) {
            fragmentCameraOverlayBinding.engineerData.phoneHeightData.text = getString(R.string.value_unknown)
            fragmentCameraOverlayBinding.engineerData.targetDistanceData.text = getString(R.string.value_unknown)
            fragmentCameraOverlayBinding.engineerData.targetHeightData.text = getString(R.string.value_unknown)
            fragmentCameraOverlayBinding.engineerData.springAngleData.text = getString(R.string.value_unknown)
            fragmentCameraOverlayBinding.engineerData.energyStoredData.text = getString(R.string.value_unknown)
        }
    }

    private fun initEngViewUnits(){
        fragmentCameraOverlayBinding.engineerData.phoneHeightUnit.text = ("(").plus(DataShared.phoneHeight.unitStr() + ")")
        fragmentCameraOverlayBinding.engineerData.launchHeightUnit.text = ("(").plus(DataShared.phoneHeight.unitStr() + ")")
        fragmentCameraOverlayBinding.engineerData.targetDistanceUnit.text = ("(").plus(DataShared.targetDistance.unitStr() + ")")
        fragmentCameraOverlayBinding.engineerData.targetDistanceAdjUnit.text = ("(").plus(DataShared.targetDistance.unitStr() + ")")
        fragmentCameraOverlayBinding.engineerData.targetHeightUnit.text = ("(").plus(DataShared.targetHeight.unitStr() + ")")
        fragmentCameraOverlayBinding.engineerData.springAngleUnit.text = "(deg)"
        fragmentCameraOverlayBinding.engineerData.potentialEnergyUnit.text = "(N-mm)"
        fragmentCameraOverlayBinding.engineerData.netPotentialEnergyUnit.text = "(N-mm)"
        fragmentCameraOverlayBinding.engineerData.velocityUnit.text = "(m/s)"
        fragmentCameraOverlayBinding.engineerData.impactDistanceUnit.text = ("(").plus(DataShared.targetDistance.unitStr() + ")")
        fragmentCameraOverlayBinding.engineerData.impactHeightUnit.text = ("(").plus(DataShared.targetHeight.unitStr() + ")")
    }

    override fun onResume() {
        viewModel.onActive(requireContext())
        // Register the onPrefsChange listener
        PreferenceManager.getDefaultSharedPreferences(requireContext()).registerOnSharedPreferenceChangeListener(prefsListener)

        // Check if sensors are available, if not then show prompt
        if(!viewModel.gyro.accelAvailable || !viewModel.gyro.magAvailable){
            var value = if(viewModel.gyro.accelAvailable) 1 else 0
            fragmentCameraOverlayBinding.sensorsAvailablePrompt.statusIconAccel.setImageLevel(value)

            value = if(viewModel.gyro.magAvailable) 1 else 0
            fragmentCameraOverlayBinding.sensorsAvailablePrompt.statusIconMag.setImageLevel(value)
            fragmentCameraOverlayBinding.sensorsAvailablePrompt.root.visibility = View.VISIBLE
        }
        else{
            fragmentCameraOverlayBinding.sensorsAvailablePrompt.root.visibility = View.GONE
        }

        super.onResume()
    }

    override fun onPause() {
        viewModel.onInactive()
        // Unregister the onPrefsChange listener
        PreferenceManager.getDefaultSharedPreferences(requireContext()).unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onPause()
    }

    override fun onDestroyView() {
        viewModel.onDestroy()
        _fragmentCameraOverlayBinding = null
        super.onDestroyView()
    }

    companion object {
        private const val LOOPER_TASK = "carriagePositionOverrideTask"
    }
}