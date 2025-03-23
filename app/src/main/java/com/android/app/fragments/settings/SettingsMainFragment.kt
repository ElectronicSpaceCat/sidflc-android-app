package com.android.app.fragments.settings

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.navigation.Navigation
import androidx.preference.*
import com.android.app.dataShared.DataShared
import com.android.app.utils.misc.Utils
import com.android.app.R
import androidx.navigation.findNavController

/** Fragment used to present the user with a gallery of photos taken */
class SettingsMainFragment : PreferenceFragmentCompat() {

    // These preferences require special handling
    private lateinit var editUnits: Preference
    private lateinit var lensOffset: EditTextPreference
    private lateinit var deviceOffset: EditTextPreference
    private lateinit var deviceSettings: Preference
    private lateinit var editProjectileList: Preference
    private lateinit var sensorCalDevice: Preference
    private lateinit var sensorCalPhone: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.prefs_main, rootKey)

        editUnits = findPreference(getString(R.string.PREFERENCE_FILTER_UNITS_EDIT))!!
        lensOffset = findPreference(getString(R.string.PREFERENCE_FILTER_LENS_OFFSET))!!
        deviceOffset = findPreference(getString(R.string.PREFERENCE_FILTER_DEVICE_OFFSET))!!
        deviceSettings = findPreference(getString(R.string.PREFERENCE_FILTER_DEVICE_SETTINGS))!!
        editProjectileList = findPreference(getString(R.string.PREFERENCE_FILTER_PROJECTILE_EDIT))!!
        sensorCalDevice = findPreference(getString(R.string.PREFERENCE_FILTER_SENSOR_CAL_DEVICE))!!
        sensorCalPhone = findPreference(getString(R.string.PREFERENCE_FILTER_SENSOR_CAL_PHONE))!!

        // Navigate to the units editor when preference clicked
        editUnits.setOnPreferenceClickListener {
            requireActivity().findNavController(R.id.container_nav).navigate(
                R.id.action_settingsFragment_to_unitEditorFragment
            )
            true
        }

        // Create generic text input listener with filters
        val textInputFilter = EditTextPreference.OnBindEditTextListener { editText: EditText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val filters = arrayOf(
                InputFilter.LengthFilter(7)
            )
            editText.filters = filters
            editText.setSelection(editText.text.length)
        }

        // Configure input dialog for the LensOffset
        lensOffset.setOnBindEditTextListener(textInputFilter)

        // Configure input dialog for the DeviceOffset
        deviceOffset.setOnBindEditTextListener(textInputFilter)

        // Navigate to the device settings when preference clicked
        deviceSettings.setOnPreferenceClickListener {
            if(!DataShared.device.connectionState.value!!.isReady){
                requireActivity().findNavController(R.id.container_nav).navigate(
                    R.id.deviceScannerFragment
                )
            }
            else {
                requireActivity().findNavController(R.id.container_nav).navigate(
                    R.id.deviceBallisticsFragment
                )
            }
            true
        }

        // Navigate to the projectile list editor when preference clicked
        editProjectileList.setOnPreferenceClickListener {
            requireActivity().findNavController(R.id.container_nav).navigate(
                R.id.action_settingsFragment_to_projectileEditFragment
            )
            true
        }

        // If device is connected then navigate to the device calibration screen
        // otherwise navigate to the device scanner screen
        sensorCalDevice.setOnPreferenceClickListener {
            if(!DataShared.device.connectionState.value!!.isReady){
                requireActivity().findNavController(R.id.container_nav).navigate(
                    R.id.deviceScannerFragment
                )
            }
            else {
                requireActivity().findNavController(R.id.container_nav).navigate(
                    R.id.deviceCalibrateFragment
                )
            }
            true
        }

        // Navigate to the phone calibration screen when preference clicked
        sensorCalPhone.setOnPreferenceClickListener {
            requireActivity().findNavController(R.id.container_nav).navigate(
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
        lensOffset.title = getString(R.string.pref_title_lens_offset) + " (" + DataShared.lensOffsetFromBase.unitStr() + ")"
        lensOffset.text = DataShared.lensOffsetFromBase.valueStr()
        DataShared.lensOffsetFromBase.unitOnChange.observe(viewLifecycleOwner){
            lensOffset.title = getString(R.string.pref_title_lens_offset) + " (" + DataShared.lensOffsetFromBase.unitStr() + ")"
        }
        lensOffset.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                if (lensOffset.text != newValue as String) {
                    val value = Utils.convertStrToDouble(newValue)
                    DataShared.lensOffsetFromBase.setValue(value)
                    DataShared.lensOffsetFromBase.storeToPrefs(requireContext())
                    retVal = true
                }
                retVal
            }

        /**
         * Device Offset
         */
        // Set the lensOffset title units appropriately
        deviceOffset.title = getString(R.string.pref_title_device_offset) + " (" + DataShared.deviceOffsetFromBase.unitStr() + ")"
        deviceOffset.text = DataShared.deviceOffsetFromBase.valueStr()
        DataShared.deviceOffsetFromBase.unitOnChange.observe(viewLifecycleOwner){
            deviceOffset.title = getString(R.string.pref_title_device_offset) + " (" + DataShared.deviceOffsetFromBase.unitStr() + ")"
        }
        deviceOffset.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _ : Preference, newValue: Any ->
                var retVal = false
                if (deviceOffset.text != newValue as String) {
                    val value = Utils.convertStrToDouble(newValue)
                    DataShared.deviceOffsetFromBase.setValue(value)
                    DataShared.deviceOffsetFromBase.storeToPrefs(requireContext())
                    retVal = true
                }
                retVal
            }
    }
}
