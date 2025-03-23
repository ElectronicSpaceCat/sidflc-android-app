package com.android.app.fragments.device.deviceConnected

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceConnectedBinding
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver
import androidx.navigation.findNavController

class DeviceConnectedFragment : Fragment() {
    private var _fragmentDeviceConnectedBinding: FragmentDeviceConnectedBinding? = null
    private val fragmentDeviceConnectedBinding get() = _fragmentDeviceConnectedBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _fragmentDeviceConnectedBinding = FragmentDeviceConnectedBinding.inflate(inflater, container, false)
        return fragmentDeviceConnectedBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        // If the DFU service then hide unrelated buttons
        if(DataShared.device.isBackupBootloader){
            fragmentDeviceConnectedBinding.btnCalibrate.visibility = View.GONE
            fragmentDeviceConnectedBinding.btnSensorTurner.visibility = View.GONE
            fragmentDeviceConnectedBinding.btnBallistics.visibility = View.GONE
        }

        // Set up button onClickListeners
        fragmentDeviceConnectedBinding.btnDeviceInfo.setOnClickListener { onInfoClicked() }
        fragmentDeviceConnectedBinding.btnDisconnect.setOnClickListener { onDisconnectClicked() }
        fragmentDeviceConnectedBinding.btnCalibrate.setOnClickListener { onCalibrateClicked() }
        fragmentDeviceConnectedBinding.btnSensorTurner.setOnClickListener { onTuneClicked() }
        fragmentDeviceConnectedBinding.btnBallistics.setOnClickListener { onBallisticsClicked() }
        fragmentDeviceConnectedBinding.btnHome.setOnClickListener { onHomeClicked() }
    }

    private fun onInfoClicked() {
        Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
            R.id.action_deviceConnectedFragment_to_deviceInfoFragment
        )
    }

    private fun onDisconnectClicked() {
        DataShared.device.removeBond()
        DataShared.device.disconnect()
    }

    private fun onCalibrateClicked() {
        Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
            R.id.action_deviceConnectedFragment_to_deviceCalibrateFragment
        )
    }

    private fun onTuneClicked() {
        Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
            R.id.action_deviceConnectedFragment_to_deviceSensorTunerFragment
        )
    }

    private fun onBallisticsClicked() {
        Navigation.findNavController(requireActivity(), R.id.container_nav).navigate(
            R.id.action_deviceConnectedFragment_to_deviceBallisticsFragment
        )
    }

    private fun onHomeClicked() {
        Navigation.findNavController(requireActivity(), R.id.container_nav).popBackStack(R.id.homeFragment, false)
    }

    override fun onDestroyView() {
        _fragmentDeviceConnectedBinding = null
        super.onDestroyView()
    }
}