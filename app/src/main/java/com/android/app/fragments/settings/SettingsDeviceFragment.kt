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
import com.android.app.fragments.dialogs.ListDialogFragment
import com.android.app.utils.misc.Utils
import com.android.app.R
import com.android.app.device.model.ModelBallistics
import com.android.app.device.projectile.Projectile
import com.android.app.device.projectile.ProjectilePrefUtils
import java.util.Locale

/** Fragment used to present the user with a gallery of photos taken */
open class SettingsDeviceFragment : PreferenceFragmentCompat() {
    private lateinit var prefForceOffset: EditTextPreference
    private lateinit var prefEfficiency: EditTextPreference
    private lateinit var prefFrictionCoefficient: EditTextPreference
    private lateinit var prefSpringSelected: Preference
    private lateinit var prefProjectileSelected: Preference
    private lateinit var prefProjectileSelectedDrag: EditTextPreference

    lateinit var prefTestHeight: EditTextPreference
    lateinit var prefTestPitch: EditTextPreference

    private lateinit var _onChildAttachListener : RecyclerView.OnChildAttachStateChangeListener

    private var _isDeviceUserDataModified = false // Indicates if data stored in the device should be overridden

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_device, rootKey)

        /** Stored in Device User Configs */
        prefForceOffset = findPreference(getString(R.string.PREFERENCE_FILTER_FORCE_OFFSET))!!
        prefEfficiency = findPreference(getString(R.string.PREFERENCE_FILTER_EFFICIENCY))!!
        prefFrictionCoefficient = findPreference(getString(R.string.PREFERENCE_FILTER_FRICTION_COEFFICIENT))!!
        prefSpringSelected = findPreference(getString(R.string.PREFERENCE_FILTER_SPRING_SELECTED))!!
        /** Projectile data */
        prefProjectileSelected = findPreference(getString(R.string.PREFERENCE_FILTER_PROJECTILE_SELECTED_BALLISTICS))!!
        prefProjectileSelectedDrag = findPreference(getString(R.string.PREFERENCE_FILTER_PROJECTILE_SELECTED_DRAG))!!
        /** Test data */
        prefTestHeight = findPreference(getString(R.string.PREFERENCE_FILTER_TEST_HEIGHT))!!
        prefTestPitch = findPreference(getString(R.string.PREFERENCE_FILTER_TEST_PITCH))!!

        /** Set Id's to preferences with reset buttons */
        prefForceOffset.setViewId(PREF_ID_FORCE_OFFSET)
        prefEfficiency.setViewId(PREF_ID_EFFICIENCY)
        prefFrictionCoefficient.setViewId(PREF_ID_FRICTION_COEFFICIENT)
        prefSpringSelected.setViewId(PREF_ID_SPRING_SELECT)
        prefProjectileSelectedDrag.setViewId(PREF_ID_PROJECTILE_DRAG)

        /** The following setting not available to public - yet */
        prefFrictionCoefficient.isVisible = false

        _onChildAttachListener = object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                when(view.id){
                    PREF_ID_FORCE_OFFSET -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            prefForceOffset.callChangeListener(ModelBallistics.DEFAULT_FORCE_OFFSET.toString())
                        }
                    }
                    PREF_ID_EFFICIENCY -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            prefEfficiency.callChangeListener(ModelBallistics.DEFAULT_EFFICIENCY_FACTOR.toString())
                        }
                    }
                    PREF_ID_FRICTION_COEFFICIENT -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            prefFrictionCoefficient.callChangeListener(ModelBallistics.DEFAULT_FRICTION_COEFFICIENT.toString())
                        }
                    }
                    PREF_ID_SPRING_SELECT -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            prefSpringSelected.callChangeListener(DataShared.device.model.defaultSpring.name)
                        }
                    }
                    PREF_ID_PROJECTILE_DRAG -> {
                        view.findViewById<ImageButton>(R.id.btn_reset).setOnClickListener {
                            DataShared.device.model.projectile?.let {
                                // If projectile type is in the default list then reset drag to it's default, else set to 0.0
                                val projectile = Projectile.getData(it.name)
                                val drag = projectile?.drag ?: 0.0
                                prefProjectileSelectedDrag.callChangeListener(drag.toString())
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
        prefForceOffset.setOnBindEditTextListener(editTextListener)
        prefForceOffset.setOnPreferenceChangeListener { _ : Preference, newValue: Any ->
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
            prefForceOffset.text = String.format(
                Locale.getDefault(),
                "%.3f",
                it
            )
        }

        /**
         * Efficiency
         */
        prefEfficiency.setOnBindEditTextListener(editTextListener)
        prefEfficiency.setOnPreferenceChangeListener { _ : Preference, newValue: Any ->
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
            prefEfficiency.text = String.format(
                Locale.getDefault(),
                "%.3f",
                it
            )
        }

        /**
         * Friction Coefficient
         */
        prefFrictionCoefficient.setOnBindEditTextListener(editTextListener)
        prefFrictionCoefficient.setOnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                val value = Utils.convertStrToDouble(newValue as String)
                if (DataShared.device.model.ballistics.frictionCoefficient != value) {
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
            prefFrictionCoefficient.text = String.format(
                Locale.getDefault(),
                "%.3f",
                it
            )
        }

        /**
         * Spring Selected
         */
        prefSpringSelected.setOnPreferenceClickListener {
            val springSelectedListener = object : ListDialogFragment.OnItemSelectedListener {
                override fun onItemSelectedListener(name: String) {
                    prefSpringSelected.callChangeListener(name)
                }
            }

            val springs : Array<String> = Array(Spring.Name.entries.size) {
                Spring.Name.entries[it].name
            }

            // Set up the dialog and show
            ListDialogFragment(
                getString(R.string.header_spring),
                DataShared.device.model.spring?.name ?: "",
                springs,
                springSelectedListener
            ).show(requireActivity().supportFragmentManager, null)
            true
        }

        prefSpringSelected.setOnPreferenceChangeListener { _ : Preference, newValue: Any ->
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
                prefSpringSelected.summary = "not set"
            }
            else {
                prefSpringSelected.summary = it.name
            }
        }

        /**
         * Projectile Selected
         */
        prefProjectileSelected.setOnPreferenceClickListener {
            val projectileSelectedListener = object : ListDialogFragment.OnItemSelectedListener {
                override fun onItemSelectedListener(name: String) {
                    prefProjectileSelected.callChangeListener(name)
                }
            }

            val projectiles = ProjectilePrefUtils.getProjectileList(requireContext()).toTypedArray()
            val projectileNames : Array<String> = Array(projectiles.size) {
                projectiles[it].name
            }
            val projectileSelectedName = DataShared.device.model.projectile?.name ?: ""

            // Set up the dialog and show
            ListDialogFragment(
                getString(R.string.header_projectile),
                projectileSelectedName,
                projectileNames,
                projectileSelectedListener
            ).show(requireActivity().supportFragmentManager, null)
            true
        }

        prefProjectileSelected.setOnPreferenceChangeListener { _ : Preference, newValue: Any ->
            var retVal = false
            if (DataShared.device.model.projectile?.name != newValue as String) {
                val projectiles = ProjectilePrefUtils.getProjectileList(requireContext())
                projectiles.forEach {
                    if(it.name == newValue){
                        DataShared.device.model.setProjectile(it)
                        retVal = true
                    }
                }
            }
            retVal
        }

        DataShared.device.model.projectileOnChange.observe(viewLifecycleOwner) {
            if(null == it){
                prefProjectileSelected.summary = "not set"
                prefProjectileSelectedDrag.text = "0.0"
            }
            else {
                prefProjectileSelected.summary = it.name
                prefProjectileSelectedDrag.text = String.format(
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

        prefProjectileSelectedDrag.text = String.format(
            Locale.getDefault(),
            "%.3f",
            drag
        )

        prefProjectileSelectedDrag.setOnBindEditTextListener(editTextListener)
        prefProjectileSelectedDrag.setOnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                val value = Utils.convertStrToDouble(newValue as String)
                if (null != DataShared.device.model.projectile){
                    if (DataShared.device.model.projectile!!.drag != value) {
                        // Get projectiles from prefs
                        val projectiles = ProjectilePrefUtils.getProjectileList(requireContext())
                        // Find the matching projectile in list, set the drag, then store to prefs
                        projectiles.forEach {
                            if(it.name == DataShared.device.model.projectile?.name) {
                                it.setDrag(newValue.toDouble())
                                // Store to prefs
                                ProjectilePrefUtils.setProjectileList(requireContext(), projectiles.toList())
                                DataShared.device.model.setProjectile(it)
                                retVal = true
                            }
                        }
                    }
                }
                else{
                    prefProjectileSelectedDrag.text = "0.0"
                }
                retVal
            }

        /**
         * Test Pitch Setting Handler
         */
        prefTestPitch.title = getString(R.string.pref_title_pitch) + " (deg)"
        prefTestPitch.setOnBindEditTextListener(editTextListener)
//        prefTestPitch.setOnPreferenceChangeListener { _ : Preference, newValue: Any ->
//            var retVal = false
//            if (prefTestPitch.text != newValue as String) {
//                retVal = true
//            }
//            retVal
//        }

        /**
         * Test Height Setting Handler
         */
        prefTestHeight.title = getString(R.string.pref_title_height) + " (" + DataShared.phoneHeight.unitStr() + ")"
        prefTestHeight.setOnBindEditTextListener(editTextListener)
//        prefTestHeight.setOnPreferenceChangeListener { _ : Preference, newValue: Any ->
//            var retVal = false
//            if (prefTestHeight.text != newValue as String) {
//                retVal = true
//            }
//            retVal
//        }
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
