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

package com.android.app.fragments.settings

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView
import com.android.app.dataShared.DataShared
import com.android.app.device.Device
import com.android.app.device.bluetooth.device.DeviceData
import com.android.app.device.springs.Spring
import com.android.app.fragments.dialogs.SpringSelectDialogFragment
import com.android.app.utils.misc.Utils
import com.android.app.R
import com.android.app.device.projectile.Projectile
import com.android.app.device.projectile.ProjectilePrefUtils
import com.android.app.fragments.dialogs.ProjectileSelectDialogFragment
import java.util.Locale

/** Fragment used to present the user with a gallery of photos taken */
class SettingsDeviceFragment : PreferenceFragmentCompat() {
    private lateinit var forceOffset: EditTextPreference
    private lateinit var efficiency: EditTextPreference
    private lateinit var frictionCoefficient: EditTextPreference
    private lateinit var springSelected: Preference
    private lateinit var projectileSelected: Preference
    private lateinit var projectileSelectedDrag: EditTextPreference
    private lateinit var testHeight: EditTextPreference
    private lateinit var testPitch: EditTextPreference

    private lateinit var _onChildAttachListener : RecyclerView.OnChildAttachStateChangeListener

    private var _isDeviceUserDataModified = false // Indicates if data stored in the device should be overridden

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_device, rootKey)

        /** Stored in Device User Configs */
        forceOffset = findPreference(getString(R.string.PREFERENCE_FILTER_FORCE_OFFSET))!!
        efficiency = findPreference(getString(R.string.PREFERENCE_FILTER_EFFICIENCY))!!
        frictionCoefficient = findPreference(getString(R.string.PREFERENCE_FILTER_FRICTION_COEFFICIENT))!!
        springSelected = findPreference(getString(R.string.PREFERENCE_FILTER_SPRING_SELECTED))!!
        /** Projectile data */
        projectileSelected = findPreference(getString(R.string.PREFERENCE_FILTER_PROJECTILE_SELECTED))!!
        projectileSelectedDrag = findPreference(getString(R.string.PREFERENCE_FILTER_PROJECTILE_SELECTED_DRAG))!!
        /** Test data */
        testHeight = findPreference(getString(R.string.PREFERENCE_FILTER_TEST_HEIGHT))!!
        testPitch = findPreference(getString(R.string.PREFERENCE_FILTER_TEST_PITCH))!!

        /** Set Id's to preferences with reset buttons */
        forceOffset.setViewId(PREF_ID_FORCE_OFFSET)
        efficiency.setViewId(PREF_ID_EFFICIENCY)
        frictionCoefficient.setViewId(PREF_ID_FRICTION_COEFFICIENT)
        springSelected.setViewId(PREF_ID_SPRING_SELECT)
        projectileSelectedDrag.setViewId(PREF_ID_PROJECTILE_DRAG)

        /** The following setting not available to public - yet */
        frictionCoefficient.isVisible = false

        _onChildAttachListener = object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                when(view.id){
                    PREF_ID_FORCE_OFFSET -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            DataShared.device.model.ballistics.resetForceOffset()
                        }
                    }
                    PREF_ID_EFFICIENCY -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            DataShared.device.model.ballistics.resetEfficiency()
                        }
                    }
                    PREF_ID_FRICTION_COEFFICIENT -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            DataShared.device.model.ballistics.resetFrictionCoefficient()
                        }
                    }
                    PREF_ID_SPRING_SELECT -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            DataShared.device.model.resetSpring()
                        }
                    }
                    PREF_ID_PROJECTILE_DRAG -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            if(null != DataShared.device.model.projectile){
                                if(null != Projectile.getData(DataShared.device.model.projectile!!.name)) {
                                    DataShared.device.model.projectile!!.setDrag(0.0)
                                }
                                else{
                                    DataShared.device.model.setProjectile(Projectile.getData(DataShared.device.model.projectile!!.name))
                                }
                                projectileSelectedDrag.callChangeListener(DataShared.device.model.projectile!!.drag.toString())
                            }
                        }
                    }
                }
            }
            override fun onChildViewDetachedFromWindow(view: View) {
                // Do nothing..
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Generic text input filter
        val editTextListener = EditTextPreference.OnBindEditTextListener { editText: EditText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val filters = arrayOf(
                InputFilter.LengthFilter(7)
            )
            editText.filters = filters
            editText.setSelection(editText.text.length)
        }

        /**
         * ForceOffset
         */
        forceOffset.setOnBindEditTextListener(editTextListener)

        forceOffset.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                val value = Utils.convertStrToDouble(newValue as String)
                if (DataShared.device.model.ballistics.forceOffset != value) {
                    DataShared.device.sendConfigCommand(
                        DeviceData.Config.Target.USER,
                        DeviceData.Config.Command.SET,
                        Device.USERDATA.FORCE_OFFSET.ordinal,
                        value.toFloat().toBits()
                    )
                    _isDeviceUserDataModified = true
                    retVal = true
                }
                retVal
            }

        DataShared.device.model.ballistics.forceOffsetOnChange.observe(viewLifecycleOwner) {
            forceOffset.text = String.format(
                Locale.getDefault(),
                "%.3f",
                it
            )
        }

        /**
         * Efficiency
         */
        efficiency.setOnBindEditTextListener(editTextListener)

        efficiency.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                val value = Utils.convertStrToDouble(newValue as String)
                if (DataShared.device.model.ballistics.efficiency != value) {
                    DataShared.device.sendConfigCommand(
                        DeviceData.Config.Target.USER,
                        DeviceData.Config.Command.SET,
                        Device.USERDATA.EFFICIENCY.ordinal,
                        value.toFloat().toBits()
                    )
                    _isDeviceUserDataModified = true
                    retVal = true
                }
                retVal
            }

        DataShared.device.model.ballistics.efficiencyOnChange.observe(viewLifecycleOwner) {
            efficiency.text = String.format(
                Locale.getDefault(),
                "%.3f",
                it
            )
        }

        /**
         * Friction Coefficient
         */
        frictionCoefficient.setOnBindEditTextListener(editTextListener)

        frictionCoefficient.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                val value = Utils.convertStrToDouble(newValue as String)
                if (DataShared.device.model.ballistics.frictionCoefficient != value) {
                    _isDeviceUserDataModified = true
                    DataShared.device.sendConfigCommand(
                        DeviceData.Config.Target.USER,
                        DeviceData.Config.Command.SET,
                        Device.USERDATA.FRICTION_COEFFICIENT.ordinal,
                        value.toFloat().toBits()
                    )
                    _isDeviceUserDataModified = true
                    retVal = true
                }
                retVal
            }

        DataShared.device.model.ballistics.frictionCoefficientOnChange.observe(viewLifecycleOwner) {
            frictionCoefficient.text = String.format(
                Locale.getDefault(),
                "%.3f",
                it
            )
        }

        /**
         * Spring Selected
         */
        springSelected.setOnPreferenceClickListener {
            val springSelectedListener = object : SpringSelectDialogFragment.OnItemSelectedListener {
                override fun onItemSelectedListener(name: String) {
                    springSelected.callChangeListener(name)
                }
            }

            // Set up the dialog and show
            SpringSelectDialogFragment(
                getString(R.string.header_spring),
                DataShared.device.model.spring?.name ?: "",
                springSelectedListener
            ).show(requireActivity().supportFragmentManager, null)
            true
        }

        springSelected.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
            var retVal = false
            if (DataShared.device.model.spring?.name != newValue as String) {
                val spring = Spring.getData(newValue)
                if(null != spring) {
                    DataShared.device.sendConfigCommand(
                        DeviceData.Config.Target.USER,
                        DeviceData.Config.Command.SET,
                        Device.USERDATA.SPRING_ID.ordinal,
                        Spring.Name.valueOf(spring.name).ordinal
                    )
                    _isDeviceUserDataModified = true
                    retVal = true
                }
            }
            retVal
        }

        DataShared.device.model.springOnChange.observe(viewLifecycleOwner) {
            if(null == it){
                springSelected.summary = "not set"
            }
            else {
                springSelected.summary = it.name
            }
        }

        /**
         * Projectile Selected
         */
        projectileSelected.setOnPreferenceClickListener {
            val projectileSelectedListener = object : ProjectileSelectDialogFragment.OnItemSelectedListener {
                override fun onItemSelectedListener(name: String) {
                    projectileSelected.callChangeListener(name)
                }
            }
            // Set up the dialog and show
            ProjectileSelectDialogFragment(
                getString(R.string.header_projectile),
                getString(R.string.PREFERENCE_FILTER_PROJECTILE_SELECTED),
                projectileSelectedListener
            ).show(requireActivity().supportFragmentManager, null)
            true
        }

        projectileSelected.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
            var retVal = false
            if (DataShared.device.model.projectile?.name != newValue as String) {
                val projectile = Projectile.getData(newValue)
                DataShared.device.model.setProjectile(projectile)
                retVal = true
            }
            retVal
        }

        DataShared.device.model.projectileOnChange.observe(viewLifecycleOwner) {
            if(null == it){
                projectileSelected.summary = "not set"
                projectileSelectedDrag.text = "0.0"
            }
            else {
                projectileSelected.summary = it.name
                projectileSelectedDrag.text = String.format(
                    Locale.getDefault(),
                    "%.3f",
                    it.drag
                )
            }
        }

        /**
         * Projectile Selected Drag
         */
        val drag = if (null != DataShared.device.model.projectile){
            DataShared.device.model.projectile!!.drag
        }
        else {
            0.0
        }

        projectileSelectedDrag.text = String.format(
            Locale.getDefault(),
            "%.3f",
            drag
        )

        projectileSelectedDrag.setOnBindEditTextListener(editTextListener)

        projectileSelectedDrag.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                val value = Utils.convertStrToDouble(newValue as String)
                if (null != DataShared.device.model.projectile){
                    if (DataShared.device.model.projectile!!.drag != value) {
                        // Get projectiles from prefs
                        val projectiles = ProjectilePrefUtils.getProjectileListFromPrefs(requireContext())
                        // Find the matching projectile in list, set the drag, then store to prefs
                        projectiles.forEach {
                            if(it.name == DataShared.device.model.projectile?.name) {
                                it.setDrag(newValue.toDouble())
                                // Store to prefs
                                ProjectilePrefUtils.setProjectileListPref(requireContext(), projectiles.toList())
                                DataShared.device.model.setProjectile(it)
                                retVal = true
                            }
                        }
                    }
                }
                else{
                    projectileSelectedDrag.text = "0.0"
                }
                retVal
            }

        /**
         * Test Height (based off of DataShared.deviceHeight units)
         */
        testHeight.title = getString(R.string.pref_title_height) + " (" + DataShared.deviceHeight.unitStr() + ")"
        testHeight.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                if (testHeight.text != newValue as String) {
                    retVal = true
                }
                retVal
            }

        /**
         * Test Pitch
         */
        testPitch.title = getString(R.string.pref_title_pitch) + " (deg)"
        testPitch.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                if (testPitch.text != newValue as String) {
                    retVal = true
                }
                retVal
            }
    }

    override fun onResume() {
        super.onResume()
        listView?.addOnChildAttachStateChangeListener(_onChildAttachListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        listView?.removeOnChildAttachStateChangeListener(_onChildAttachListener)

        // Send command to store the configurations if any changed
        if(_isDeviceUserDataModified) {
            DataShared.device.sendConfigCommand(
                DeviceData.Config.Target.USER,
                DeviceData.Config.Command.STORE
            )
        }
    }

    companion object {
        // Note: Set pref view ID's to anything but 0
        private const val PREF_ID_FORCE_OFFSET = 1
        private const val PREF_ID_EFFICIENCY = 2
        private const val PREF_ID_FRICTION_COEFFICIENT = 3
        private const val PREF_ID_SPRING_SELECT = 4
        private const val PREF_ID_PROJECTILE_DRAG = 5
    }
}
