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
import androidx.navigation.Navigation
import androidx.preference.*
import com.android.greentech.plink.dataShared.DataShared
import com.android.greentech.plink.device.springs.Spring
import com.android.greentech.plink.fragments.dialogs.SpringSelectDialogFragment
import com.android.greentech.plink.utils.textFilters.TextInputFilter
import com.android.greentech.plink.utils.misc.Utils
import com.android.greentech.plink.R
import java.util.*

/** Fragment used to present the user with a gallery of photos taken */
class SettingsFragment : PreferenceFragmentCompat() {
    // These preferences require special handling
    private lateinit var editUnits: Preference
    private lateinit var sensorCalDevice: Preference
    private lateinit var sensorCalPhone: Preference
    private lateinit var editProjectileList: Preference
    private lateinit var editSpringList: Preference
    private lateinit var lensOffset: EditTextPreference
    private lateinit var forceOffset: EditTextPreference
//    private lateinit var frictionCoefficient: EditTextPreference
    private lateinit var efficiency: EditTextPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        editUnits = findPreference(getString(R.string.PREFERENCE_FILTER_UNITS_EDIT))!!
        sensorCalDevice = findPreference(getString(R.string.PREFERENCE_FILTER_SENSOR_CAL_DEVICE))!!
        sensorCalPhone = findPreference(getString(R.string.PREFERENCE_FILTER_SENSOR_CAL_PHONE))!!
        editProjectileList = findPreference(getString(R.string.PREFERENCE_FILTER_PROJECTILE_EDIT))!!
        editSpringList = findPreference(getString(R.string.PREFERENCE_FILTER_SPRING_SELECTED))!!
        lensOffset = findPreference(getString(R.string.PREFERENCE_FILTER_LENS_OFFSET))!!
        forceOffset = findPreference(getString(R.string.PREFERENCE_FILTER_FORCE_OFFSET))!!
//        frictionCoefficient = findPreference(getString(R.string.PREFERENCE_FILTER_FRICTION_COEFFICIENT))!!
        efficiency = findPreference(getString(R.string.PREFERENCE_FILTER_EFFICIENCY))!!

        // Navigate to the units editor when preference clicked
        editUnits.setOnPreferenceClickListener {
            Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                R.id.action_settingsFragment_to_unitEditorFragment
            )
            true
        }

        // If device is connected then navigate to the device calibration screen
        // otherwise navigate to the device scanner screen
        sensorCalDevice.setOnPreferenceClickListener {
            if(!DataShared.device.connectionState.value!!.isReady){
                Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                    R.id.deviceScannerFragment
                )
            }
            else {
                Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                    R.id.deviceCalibrateFragment
                )
            }
            true
        }

        // Navigate to the phone calibration screen when preference clicked
        sensorCalPhone.setOnPreferenceClickListener {
            Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                R.id.action_settingsFragment_to_gyroCalFragment
            )
            true
        }

        // Navigate to the projectile list editor when preference clicked
        editProjectileList.setOnPreferenceClickListener {
            Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                R.id.action_settingsFragment_to_projectileEditFragment
            )
            true
        }

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

        // Create generic text input listener with filters
        val lensOffsetFilter = EditTextPreference.OnBindEditTextListener { editText: EditText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val filters = arrayOf(
                InputFilter.LengthFilter(7)
            )
            editText.filters = filters
            editText.setSelection(editText.text.length)
        }

        // Configure input dialog for the LensOffset
        lensOffset.setOnBindEditTextListener(lensOffsetFilter)

        // Create generic text input listener with filters
        val forceOffsetFilter = EditTextPreference.OnBindEditTextListener { editText: EditText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val filters = arrayOf(
                InputFilter.LengthFilter(7),
                TextInputFilter.MinMax(0, 2)
            )
            editText.filters = filters
            editText.setSelection(editText.text.length)
        }

        // Configure input dialog for the forceOffset
        forceOffset.setOnBindEditTextListener(forceOffsetFilter)

//        // Create generic text input listener with filters
//        val frictionCoefficientFilter = EditTextPreference.OnBindEditTextListener { editText: EditText ->
//            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
//            val filters = arrayOf(
//                InputFilter.LengthFilter(7),
//                TextInputFilter.MinMax(0, 1)
//            )
//            editText.filters = filters
//            editText.setSelection(editText.text.length)
//        }
//
//        // Configure input dialog for the Friction Coefficient
//        frictionCoefficient.setOnBindEditTextListener(frictionCoefficientFilter)

        // Create generic text input listener with filters
        val efficiencyFilter = EditTextPreference.OnBindEditTextListener { editText: EditText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val filters = arrayOf(
                InputFilter.LengthFilter(7),
                TextInputFilter.MinMax(0, 1)
            )
            editText.filters = filters
            editText.setSelection(editText.text.length)
        }

        // Configure input dialog for the Efficiency
        efficiency.setOnBindEditTextListener(efficiencyFilter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Lens Offset
         */
        // Set the lensOffset title units appropriately
        lensOffset.title = getString(R.string.pref_title_lens_offset) + " (" + DataShared.lensOffset.unitStr() + ")"
        lensOffset.text = DataShared.lensOffset.valueStr()
        DataShared.lensOffset.unitOnChange.observe(viewLifecycleOwner){
            lensOffset.title = getString(R.string.pref_title_lens_offset) + " (" + DataShared.lensOffset.unitStr() + ")"
        }
        lensOffset.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                if (lensOffset.text != newValue as String) {
                    val value = Utils.convertStrToDouble(newValue)
                    DataShared.lensOffset.setValue(value)
                    DataShared.lensOffset.storeToPrefs(requireContext())
                    retVal = true
                }
                retVal
            }

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
                    val value = Utils.convertStrToDouble(newValue)
                    DataShared.device.ballistics.forceOffset = value
                    retVal = true
                }
                retVal
            }

//        /**
//         * Friction Coefficient
//         */
//        frictionCoefficient.text = String.format(
//            Locale.getDefault(),
//            "%.3f",
//            DataShared.device.ballistics.frictionCoefficient
//        )
//        frictionCoefficient.onPreferenceChangeListener =
//            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
//                var retVal = false
//                if (frictionCoefficient.text != newValue as String) {
//                    val value = Utils.convertStrToDouble(newValue)
//                    DataShared.device.ballistics.frictionCoefficient = value
//                    retVal = true
//                }
//                retVal
//            }

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
                    val value = Utils.convertStrToDouble(newValue)
                    DataShared.device.ballistics.efficiency = value
                    retVal = true
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
}
