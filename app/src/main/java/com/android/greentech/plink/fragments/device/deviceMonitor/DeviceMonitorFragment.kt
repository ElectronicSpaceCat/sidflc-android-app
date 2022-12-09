package com.android.greentech.plink.fragments.device.deviceMonitor

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.android.greentech.plink.R
import com.android.greentech.plink.dataShared.DataShared
import com.android.greentech.plink.databinding.FragmentDeviceMonitorBinding
import com.android.greentech.plink.device.bluetooth.pwrmonitor.PwrMonitorData
import com.android.greentech.plink.utils.misc.Utils
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class DeviceMonitorFragment : Fragment() {
    private var _fragmentDeviceMonitorBinding: FragmentDeviceMonitorBinding? = null
    private val fragmentDeviceMonitorBinding get() = _fragmentDeviceMonitorBinding!!

    private var _sensorEnablePrev = false

    /**
     * Checks the bluetooth state and try to auto connect the
     * ble device when bluetooth is turned on and active.
     * The device should connect if it is on and bonded.
     * Otherwise the user will have to manually connect it from
     * the scanner fragment or through the Android Bluetooth settings.
     */
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_TURNING_OFF,
                    BluetoothAdapter.STATE_OFF,
                    BluetoothAdapter.STATE_TURNING_ON, -> {}
                    BluetoothAdapter.STATE_ON -> {
                        // Attempt to connect device
                        autoConnectDeviceIfMatch(requireContext())
                    }
                }
            }
        }
    }

    /**
     * Connects to the ble device if it is bonded and the
     * name matches our device name
     *
     * @param context
     */
    fun autoConnectDeviceIfMatch(context: Context) {
        val device = Utils.getBondedDevice(context, context.getString(R.string.app_name))
        if(device != null){
            DataShared.device.autoConnect(context, device)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _fragmentDeviceMonitorBinding = FragmentDeviceMonitorBinding.inflate(inflater, container, false)
        return fragmentDeviceMonitorBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentDeviceMonitorBinding.deviceShutdownPrompt.btnNoticeOk.setOnClickListener {
            fragmentDeviceMonitorBinding.deviceShutdownPrompt.root.visibility = View.GONE
        }

        /** Register broadcast receiver to watch bluetooth state */
        requireContext().registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        /** Attempt to connect device if bluetooth is enabled and device is advertising */
        autoConnectDeviceIfMatch(requireContext())

//        /**
//         * Observe the device bond status
//         */
//        DataShared.device.bondingState.observe(viewLifecycleOwner) { bond ->
//            if(bond.state != BondState.State.NOT_BONDED) {
//                Toast.makeText(context, "Device: ".plus(bond.state.name), Toast.LENGTH_LONG).show()
//            }
//        }

        /**
         * Observe the device connections state
         */
        DataShared.device.connectionState.observe(viewLifecycleOwner) { state ->
            val navController = Navigation.findNavController(requireActivity(), R.id.container_nav)

            when (state.state) {
//                ConnectionState.State.READY -> {
//                    if(navController.currentDestination?.id != R.id.homeFragment && navController.currentDestination?.id != R.id.deviceScannerFragment) {
//                        Toast.makeText(context, "Device: Connected", Toast.LENGTH_SHORT).show()
//                    }
//                }
                ConnectionState.State.DISCONNECTED -> {
//                    if(navController.currentDestination?.id != R.id.homeFragment && navController.currentDestination?.id != R.id.deviceScannerFragment) {
//                        Toast.makeText(context, "Device: Disconnected", Toast.LENGTH_SHORT).show()
//                    }
//                    val connState = DataShared.device.connectionState.value!!
//                    // Get disconnect reason if any
//                    val disconnectReason = if (connState is ConnectionState.Disconnected) {
//                        connState.reason
//                    } else {
//                        ConnectionObserver.REASON_SUCCESS
//                    }
//
//                    if (connState is ConnectionState.Disconnected) {
//                        val stateWithReason: ConnectionState.Disconnected = connState
//                        // Determine disconnection reason
//                        when (stateWithReason.reason) {
//                            ConnectionObserver.REASON_NOT_SUPPORTED -> { }
//                            ConnectionObserver.REASON_CANCELLED -> { }
//                            ConnectionObserver.REASON_SUCCESS -> { }
//                            ConnectionObserver.REASON_TERMINATE_PEER_USER -> { }
//                            ConnectionObserver.REASON_TIMEOUT -> { }
//                            ConnectionObserver.REASON_UNKNOWN -> { }
//                            ConnectionObserver.REASON_TERMINATE_LOCAL_HOST -> { }
//                            ConnectionObserver.REASON_LINK_LOSS -> {}
//                            else -> { }
//                        }
//                    }
//
//                    Toast.makeText(context, "Disconnect reason id: ".plus(disconnectReason.toString()), Toast.LENGTH_LONG).show()


                    // Navigate back to scanner page
                    // Note: deviceDfuFragment handles disconnects differently
                    if(navController.currentDestination?.id == R.id.deviceConnectedFragment
                            || navController.currentDestination?.id == R.id.deviceCalibrateFragment
                            || navController.currentDestination?.id == R.id.deviceTunerFragment) {

                        val options = NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, false)
                            .setLaunchSingleTop(true)
                            .build()
                        navController.navigate(R.id.deviceScannerFragment, null, options)
                    }
                }
                else -> { }
            }
        }

        /**
         * Observe the battery status and set the indicator
         */
        DataShared.device.batteryStatus.observe(viewLifecycleOwner) { status ->
            when(status){
                PwrMonitorData.BattStatus.OK -> {

                }
                PwrMonitorData.BattStatus.LOW -> {
                    Utils.showToast(requireContext(),
                        requireContext().getString(R.string.batt_low),
                        Toast.LENGTH_LONG,
                        Gravity.TOP or Gravity.CENTER, 0, 0)
                }
                PwrMonitorData.BattStatus.VERY_LOW -> {
                    Utils.showToast(requireContext(),
                        requireContext().getString(R.string.batt_very_low),
                        Toast.LENGTH_LONG,
                        Gravity.TOP or Gravity.CENTER, 0, 0)

                    // Show prompt when very low battery
                    fragmentDeviceMonitorBinding.deviceShutdownPrompt.root.visibility = View.VISIBLE
                }
                PwrMonitorData.BattStatus.CHARGING -> {
                    Utils.showToast(requireContext(),
                        requireContext().getString(R.string.batt_charging), Toast.LENGTH_SHORT,
                        Gravity.TOP or Gravity.CENTER, 0, 0)
                }
                PwrMonitorData.BattStatus.CHARGING_COMPLETE -> {
                    Utils.showToast(requireContext(), requireContext().getString(R.string.batt_charging_complete),
                        Toast.LENGTH_SHORT,
                        Gravity.TOP or Gravity.CENTER, 0, 0)
                }
                PwrMonitorData.BattStatus.UNKNOWN -> {}
                else -> {}
            }
        }
    }

    override fun onResume() {
        if(_sensorEnablePrev){
            DataShared.device.setSensorEnable(true)
        }
        super.onResume()
    }

    override fun onPause() {
        _sensorEnablePrev = DataShared.device.sensorEnabled.value!!
        DataShared.device.setSensorEnable(false)
        super.onPause()
    }

    override fun onDestroy() {
        // Disconnect device
        DataShared.device.disconnect()
        requireContext().unregisterReceiver(mReceiver)
        _fragmentDeviceMonitorBinding = null
        super.onDestroy()
    }
}