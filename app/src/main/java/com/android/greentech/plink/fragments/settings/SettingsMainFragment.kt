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
class SettingsMainFragment : PreferenceFragmentCompat() {
    // These preferences require special handling
    private lateinit var editUnits: Preference
    private lateinit var lensOffset: EditTextPreference
    private lateinit var deviceSettings: Preference
    private lateinit var editProjectileList: Preference
    private lateinit var sensorCalDevice: Preference
    private lateinit var sensorCalPhone: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.prefs_main, rootKey)

        editUnits = findPreference(getString(R.string.PREFERENCE_FILTER_UNITS_EDIT))!!
        lensOffset = findPreference(getString(R.string.PREFERENCE_FILTER_LENS_OFFSET))!!
        deviceSettings = findPreference(getString(R.string.PREFERENCE_FILTER_DEVICE_SETTINGS))!!
        editProjectileList = findPreference(getString(R.string.PREFERENCE_FILTER_PROJECTILE_EDIT))!!
        sensorCalDevice = findPreference(getString(R.string.PREFERENCE_FILTER_SENSOR_CAL_DEVICE))!!
        sensorCalPhone = findPreference(getString(R.string.PREFERENCE_FILTER_SENSOR_CAL_PHONE))!!

        // Navigate to the units editor when preference clicked
        editUnits.setOnPreferenceClickListener {
            Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                R.id.action_settingsFragment_to_unitEditorFragment
            )
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

        // Navigate to the device settings when preference clicked
        deviceSettings.setOnPreferenceClickListener {
            if(!DataShared.device.connectionState.value!!.isReady){
                Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                    R.id.deviceScannerFragment
                )
            }
            else {
                Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                    R.id.settingsDeviceFragment
                )
            }
            true
        }

        // Navigate to the projectile list editor when preference clicked
        editProjectileList.setOnPreferenceClickListener {
            Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
                R.id.action_settingsFragment_to_projectileEditFragment
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
    }
}
