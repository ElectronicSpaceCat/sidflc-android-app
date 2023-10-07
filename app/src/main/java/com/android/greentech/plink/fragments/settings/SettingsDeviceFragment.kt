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

package com.android.greentech.plink.fragments.settings

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.preference.*
import com.android.greentech.plink.R
import com.android.greentech.plink.dataShared.DataShared
import com.android.greentech.plink.device.Device
import com.android.greentech.plink.device.bluetooth.device.DeviceData
import com.android.greentech.plink.device.springs.Spring
import com.android.greentech.plink.fragments.dialogs.SpringSelectDialogFragment
import com.android.greentech.plink.utils.misc.Utils
import com.android.greentech.plink.utils.textFilters.TextInputFilter
import java.util.*


/** Fragment used to present the user with a gallery of photos taken */
class SettingsDeviceFragment : PreferenceFragmentCompat() {
    // These preferences require special handling
    private lateinit var forceOffset: EditTextPreference
    private lateinit var efficiency: EditTextPreference
    private lateinit var frictionCoefficient: EditTextPreference
    private lateinit var editSpringList: Preference

    private var _dataWasModified = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.prefs_device, rootKey)

        forceOffset = findPreference(getString(R.string.PREFERENCE_FILTER_FORCE_OFFSET))!!
        efficiency = findPreference(getString(R.string.PREFERENCE_FILTER_EFFICIENCY))!!
        frictionCoefficient = findPreference(getString(R.string.PREFERENCE_FILTER_FRICTION_COEFFICIENT))!!
        editSpringList = findPreference(getString(R.string.PREFERENCE_FILTER_SPRING_SELECTED))!!

        /** The following setting not available to public - yet */
        frictionCoefficient.isVisible = false

        // Create generic text input listener with filters
        val forceOffsetFilter = EditTextPreference.OnBindEditTextListener { editText: EditText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val filters = arrayOf(
                InputFilter.LengthFilter(7)
            )
            editText.filters = filters
            editText.setSelection(editText.text.length)
        }

        // Configure input dialog for the forceOffset
        forceOffset.setOnBindEditTextListener(forceOffsetFilter)

        // Create generic text input listener with filters
        val efficiencyFilter = EditTextPreference.OnBindEditTextListener { editText: EditText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val filters = arrayOf(
                InputFilter.LengthFilter(7)
            )
            editText.filters = filters
            editText.setSelection(editText.text.length)
        }

        // Configure input dialog for the Efficiency
        efficiency.setOnBindEditTextListener(efficiencyFilter)

        // Create generic text input listener with filters
        val frictionCoefficientFilter = EditTextPreference.OnBindEditTextListener { editText: EditText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val filters = arrayOf(
                InputFilter.LengthFilter(7)
            )
            editText.filters = filters
            editText.setSelection(editText.text.length)
        }

        // Configure input dialog for the Friction Coefficient
        frictionCoefficient.setOnBindEditTextListener(frictionCoefficientFilter)

        // Navigate to the spring list editor when preference clicked
        editSpringList.setOnPreferenceClickListener {
            val springSelectedListener = object : SpringSelectDialogFragment.OnItemSelectedListener {
                override fun onItemSelectedListener(name: String) {
                    editSpringList.summary = name
                    val spring = Spring.getData(Spring.Name.valueOf(name))
                    DataShared.device.model.setSpring(spring)
                }
            }
            // Set up the dialog and show
            SpringSelectDialogFragment(
                "Spring",
                getString(R.string.PREFERENCE_FILTER_SPRING_SELECTED),
                springSelectedListener
            ).show(requireActivity().supportFragmentManager, null)
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Force Offset
         */
        forceOffset.text = String.format(
            Locale.getDefault(),
            "%.3f",
            DataShared.device.ballistics.forceOffset
        )
        forceOffset.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                if (forceOffset.text != newValue as String) {
                    if(TextInputFilter.isStrInRange(0.0, 2.0, newValue)) {
                        _dataWasModified = true
                        DataShared.device.sendConfigCommand(
                            DeviceData.Config.Target.EXT_STORE,
                            DeviceData.Config.Command.SET,
                            Device.EXTDATA.FORCE_OFFSET.ordinal,
                            Utils.convertStrToFloat(newValue).toBits()
                        )
                        retVal = true
                    }
                }
                retVal
            }

        /**
         * Efficiency Factor
         */
        efficiency.text = String.format(
            Locale.getDefault(),
            "%.3f",
            DataShared.device.ballistics.efficiency
        )
        efficiency.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                if (efficiency.text != newValue as String) {
                    if(TextInputFilter.isStrInRange(0.5, 1.0, newValue)) {
                        _dataWasModified = true
                        DataShared.device.sendConfigCommand(
                            DeviceData.Config.Target.EXT_STORE,
                            DeviceData.Config.Command.SET,
                            Device.EXTDATA.EFFICIENCY.ordinal,
                            Utils.convertStrToFloat(newValue).toBits()
                        )
                        retVal = true
                    }
                }
                retVal
            }

        /**
         * Friction Coefficient
         */
        frictionCoefficient.text = String.format(
            Locale.getDefault(),
            "%.3f",
            DataShared.device.ballistics.frictionCoefficient
        )
        frictionCoefficient.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                if (frictionCoefficient.text != newValue as String) {
                    if(TextInputFilter.isStrInRange(0.0, 1.0, newValue)) {
                        _dataWasModified = true
                        DataShared.device.sendConfigCommand(
                            DeviceData.Config.Target.EXT_STORE,
                            DeviceData.Config.Command.SET,
                            Device.EXTDATA.FRICTION_COEFFICIENT.ordinal,
                            Utils.convertStrToFloat(newValue).toBits()
                        )
                        retVal = true
                    }
                }
                retVal
            }

        /**
         * Selected Spring - set the summary
         */
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val prefsKey = requireContext().getString(R.string.PREFERENCE_FILTER_SPRING_SELECTED)

        editSpringList.summary = try {
            prefs.getString(prefsKey, Spring.Name.MC9287K33.name).toString()
        }
        catch (e : ClassCastException){
            "not set"
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if(!_dataWasModified) return

        // Send command to store the configurations if any changed
        DataShared.device.sendConfigCommand(
            DeviceData.Config.Target.EXT_STORE,
            DeviceData.Config.Command.STORE,
            Int.MAX_VALUE, // Ignored
            Int.MAX_VALUE) // Ignored
    }
}
