package com.android.app.fragments.device.deviceMonitor

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
import androidx.navigation.Navigation
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceMonitorBinding
import com.android.app.device.bluetooth.pwrmonitor.PwrMonitorData
import com.android.app.utils.converters.ConvertLength
import com.android.app.utils.misc.Utils
import no.nordicsemi.android.ble.livedata.state.BondState
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver

class DeviceMonitorFragment : Fragment() {
    private var _fragmentDeviceMonitorBinding: FragmentDeviceMonitorBinding? = null
    private val fragmentDeviceMonitorBinding get() = _fragmentDeviceMonitorBinding!!
    private var _sensorEnablePrev = false
    private var _disconnectCount = 0

    /**
     * Checks the bluetooth state and try to auto connect the
     * ble device when bluetooth is turned on and active.
     * The device should connect if it is on and bonded.
     * Otherwise the user will have to manually connect it from
     * the scanner fragment or through the Android Bluetooth settings.
     */
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED != intent.action) {
                return
            }
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                BluetoothAdapter.STATE_TURNING_OFF,
                BluetoothAdapter.STATE_OFF,
                BluetoothAdapter.STATE_TURNING_ON, -> {}
                BluetoothAdapter.STATE_ON -> {
                    connectDeviceIfMatch(requireContext())
                }
            }
        }
    }

    /**
     * Connects to the ble device if it is bonded and the
     * name matches a given UUID
     *
     * @param context
     */
    fun connectDeviceIfMatch(context: Context) {
        val device = Utils.getBondedDeviceByName(context, context.getString(R.string.app_name))
        if(device != null){
            DataShared.device.connect(context, device, true)
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

        /** Setup the button to acknowledge the device shutdown prompt */
        fragmentDeviceMonitorBinding.deviceShutdownPrompt.btnNoticeOk.setOnClickListener {
            fragmentDeviceMonitorBinding.deviceShutdownPrompt.root.visibility = View.GONE
        }

        /** Register Bluetooth receiver */
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            CoroutineScope(Dispatchers.IO).launch {
//                while (!Utils.hasPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)) {
//                    Thread.sleep(500)
//                }
//
//                requireContext().registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
//                connectDeviceIfMatch(requireContext())
//            }
//        } else {
//            requireContext().registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
//            connectDeviceIfMatch(requireContext())
//        }

        /**
         * Observe the device bond status
         */
        DataShared.device.bondingState.observe(viewLifecycleOwner) { bond ->
            if(bond.state != BondState.State.NOT_BONDED) {
                Toast.makeText(context, "Device: ${bond.state.name}", Toast.LENGTH_LONG).show()
            }
        }

        /**
         * Observe the device offset for setting the device height sensor target reference
         * which is used for calibration
         */
        DataShared.deviceOffsetFromBase.valueOnChange.observe(viewLifecycleOwner) {
            DataShared.device.sensorDeviceHeight.targetReference = DataShared.deviceOffsetFromBase.getConverted(ConvertLength.Unit.MM).toInt()
        }

        /**
         * Observe the device connections state
         */
        DataShared.device.connectionState.observe(viewLifecycleOwner) { state ->
            when (state.state) {
                ConnectionState.State.READY,
                ConnectionState.State.CONNECTING -> {
                    _disconnectCount = 0
                }
                ConnectionState.State.DISCONNECTED -> {
                    val connState = DataShared.device.connectionState.value!!
                    // Get disconnect reason
                    if (connState is ConnectionState.Disconnected) {
                        val stateWithReason: ConnectionState.Disconnected = connState
                        val reasonStr = when (stateWithReason.reason) {
                            ConnectionObserver.REASON_SUCCESS -> { "Success" }
                            ConnectionObserver.REASON_NOT_SUPPORTED -> { "Not Supported" }
                            ConnectionObserver.REASON_CANCELLED -> { "Cancelled" }
                            ConnectionObserver.REASON_TERMINATE_PEER_USER -> { "Peer Device" }
                            ConnectionObserver.REASON_TERMINATE_LOCAL_HOST -> { "Local Host" }
                            ConnectionObserver.REASON_LINK_LOSS -> { "Link Loss" }
                            ConnectionObserver.REASON_TIMEOUT -> { "Timeout" }
                            ConnectionObserver.REASON_UNKNOWN -> { "Unknown" }
                            else -> { "NA" }
                        }

                        // Handle Link Loss
                        if(stateWithReason.reason == ConnectionObserver.REASON_LINK_LOSS) {
                            // Ignore while in the DeviceInfo fragment (where the DFU logic resides, and will handle disconnects)
                            val navController = Navigation.findNavController(requireActivity(), R.id.container_nav)
                            if(navController.currentDestination?.id == R.id.deviceInfoFragment) {
                                return@observe
                            }
                            // Try a few connection attempts before clearing bonds on the phone
                            //  and closing the autoConnection.
                            if(++_disconnectCount >= MAX_CONNECTION_ATTEMPTS) {
                                DataShared.device.removeBond()
                                DataShared.device.disconnect()
                            }
                        }

                        // Handle all other disconnection reasons.
                        if(stateWithReason.reason != ConnectionObserver.REASON_UNKNOWN) {
                            Toast.makeText(context, "Disconnected: $reasonStr", Toast.LENGTH_LONG).show()
                        }
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
                        requireContext().getString(R.string.batt_charging),
                        Toast.LENGTH_SHORT,
                        Gravity.TOP or Gravity.CENTER, 0, 0)
                }
                PwrMonitorData.BattStatus.CHARGING_COMPLETE -> {
                    Utils.showToast(requireContext(),
                        requireContext().getString(R.string.batt_charging_complete),
                        Toast.LENGTH_SHORT,
                        Gravity.TOP or Gravity.CENTER, 0, 0)
                }
                PwrMonitorData.BattStatus.UNKNOWN -> {}
                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if(_sensorEnablePrev){
            DataShared.device.setSensorEnable(true)
        }

        requireContext().registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        connectDeviceIfMatch(requireContext())
    }

    override fun onPause() {
        super.onPause()
        _sensorEnablePrev = DataShared.device.sensorEnabled.value!!
        DataShared.device.setSensorEnable(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect device
        DataShared.device.disconnect()
        _fragmentDeviceMonitorBinding = null
        requireContext().unregisterReceiver(mReceiver)
    }

    companion object {
        const val MAX_CONNECTION_ATTEMPTS = 2
    }
}