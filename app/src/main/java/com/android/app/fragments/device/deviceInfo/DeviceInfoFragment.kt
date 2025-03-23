package com.android.app.fragments.device.deviceInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceInfoBinding
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class DeviceInfoFragment : Fragment() {
    private var _fragmentDeviceInfoBinding: FragmentDeviceInfoBinding? = null
    private val fragmentDeviceInfoBinding get() = _fragmentDeviceInfoBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentDeviceInfoBinding = FragmentDeviceInfoBinding.inflate(inflater, container, false)
        return fragmentDeviceInfoBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Device name and MAC address should already be loaded by the time this screen is loaded
        fragmentDeviceInfoBinding.deviceConnectedInfo.bleDeviceName.text = DataShared.device.name
        fragmentDeviceInfoBinding.deviceConnectedInfo.bleDeviceAddress.text = DataShared.device.address

        fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceManufacturer.visibility = View.GONE
        fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceModel.visibility = View.GONE
        fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceSerial.visibility = View.GONE
        fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceVersionHw.visibility = View.GONE
        fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceVersionFw.visibility = View.GONE

        // Set up observers for additional ble device information
        if(!DataShared.device.isBackupBootloader) {
//            DataShared.device.manufacturer.observe(viewLifecycleOwner) { str: String? ->
//                fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceManufacturer.visibility = View.VISIBLE
//                fragmentDeviceInfoBinding.deviceConnectedInfo.bleDeviceManufacturer.text = str
//            }
//            DataShared.device.versionModel.observe(viewLifecycleOwner) { str: String? ->
//                fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceModel.visibility = View.VISIBLE
//                fragmentDeviceInfoBinding.deviceConnectedInfo.bleDeviceModel.text = str
//            }
//            DataShared.device.serial.observe(viewLifecycleOwner) { str: String? ->
//                fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceSerial.visibility = View.VISIBLE
//                fragmentDeviceInfoBinding.deviceConnectedInfo.bleDeviceSerial.text = str
//            }
//            DataShared.device.versionHardware.observe(viewLifecycleOwner) { str: String? ->
//                fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceVersionHw.visibility = View.VISIBLE
//                fragmentDeviceInfoBinding.deviceConnectedInfo.bleDeviceVersionHw.text = str
//            }
            DataShared.device.versionFirmware.observe(viewLifecycleOwner) { str: String? ->
                fragmentDeviceInfoBinding.deviceConnectedInfo.itemDeviceVersionFw.visibility = View.VISIBLE
                fragmentDeviceInfoBinding.deviceConnectedInfo.bleDeviceVersionFw.text = str
            }
        }
    }

    override fun onDestroyView() {
        _fragmentDeviceInfoBinding = null
        super.onDestroyView()
    }
}