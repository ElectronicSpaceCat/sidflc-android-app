package com.android.app.fragments.device.deviceTuner.configAdapter

import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.app.R
import com.android.app.databinding.ItemConfigBinding
import com.android.app.device.Device
import com.android.app.device.bluetooth.device.DeviceData
import com.android.app.fragments.device.deviceTuner.DeviceTunerFragment
import com.android.app.fragments.dialogs.InputDialogFragment

class ConfigAdapter(activity: DeviceTunerFragment, device : Device): RecyclerView.Adapter<ConfigAdapter.ViewHolder>() {
    private var _activity = activity
    private var _device = device

    private inner class ConfigType(var name: String, var value: Int, var status: String)

    private var _configs = mutableListOf<ConfigType>()

    init {
        setHasStableIds(true)

        // Invalidate last config data to prevent the sensor config observer
        // from acting on the last value.
        _device.activeSensor.invalidateLastConfigData()

        /**
         * Observer selected sensor to update the sensor configurations in the list
         */
        _device.sensorSelected.observe(activity.viewLifecycleOwner) {
            // Clear out existing configurations
            clearConfigs()
            // Notify the adapter of change to the list to update it
            notifyConfigChange()
            // Add the configurations to adapter list and set status as Requested,
            // even though it is not an actual request call to the device
            _device.activeSensor.configs.forEach {
                addConfig(it.name, it.value, DeviceData.Config.Status.NA.name)
            }
        }

        /**
         * Observe the sensor config to update the item in the list
         */
        _device.sensorConfig.observe(activity.viewLifecycleOwner) {
            configModify(it.id, it.value, it.status.name)
        }
    }

    private fun addConfig(name: String, value: Int, status: String){
        _configs.add(ConfigType(name, value, status))
    }

    private fun configModify(id : Int, value : Int, status: String){
        if(id < _configs.size) {
            _configs[id].value = value
            _configs[id].status = status
            notifyItemChanged(id)
        }
    }

    private fun notifyConfigChange(){
        notifyItemRangeChanged(0, _configs.lastIndex)
    }

    private fun clearConfigs(){
        if(_configs.isEmpty()) return

        val indexSizePrev = _configs.lastIndex
        _configs.clear()
        notifyItemRangeRemoved(0, indexSizePrev)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemConfigBinding = ItemConfigBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(itemConfigBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = _configs[position]
        holder.itemConfigBinding.configId.text = position.toString()
        holder.itemConfigBinding.configName.text = config.name
        holder.itemConfigBinding.configValue.text = config.value.toString()
        holder.itemConfigBinding.configStatus.text = config.status
    }

    override fun getItemId(position: Int): Long {
        return _configs[position].hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return _configs.size
    }

    inner class ViewHolder(val itemConfigBinding: ItemConfigBinding) :
        RecyclerView.ViewHolder(itemConfigBinding.root) {
        init {
            // Set up the Edit button
            itemConfigBinding.iconEditConfig.setOnClickListener {
                showConfigEditorDialog(this.adapterPosition)
            }
            // Set up the Get button
            itemConfigBinding.iconGetConfig.setOnClickListener {
                getConfig(this.adapterPosition)
            }
            // Set up the Reset button
            itemConfigBinding.iconResetConfig.setOnClickListener {
                resetConfig(this.adapterPosition)
            }
        }
    }

    /**
     * Get the configuration
     */
    private fun getConfig(position: Int) {
        _device.sendConfigCommand(DeviceData.Config.Target.SENSOR, DeviceData.Config.Command.GET, position, Int.MAX_VALUE)

        // Set the sensor status to NA
        if(position < _configs.size) {
            configModify(position, _configs[position].value, DeviceData.Config.Status.NA.name)
        }
    }

    /**
     * Reset the configuration value to the app's initial hardcoded value.
     * If the hardcoded value equals Int.MAX_VALUE then use the factory default
     * value from the sensor.
     */
    private fun resetConfig(position: Int) {
        _device.sendConfigCommand(DeviceData.Config.Target.SENSOR, DeviceData.Config.Command.RESET, position, Int.MAX_VALUE)

        // Set the sensor status to NA
        if(position < _configs.size) {
            configModify(position, _configs[position].value, DeviceData.Config.Status.NA.name)
        }
    }

    private fun showConfigEditorDialog(position: Int) {
        val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    // Send configuration command
                    _device.sendConfigCommand(DeviceData.Config.Target.SENSOR, DeviceData.Config.Command.SET, position, value.toInt())
                    // Set the sensor status to NA
                    if(position < _configs.size) {
                        configModify(position, _configs[position].value, DeviceData.Config.Status.NA.name)
                    }
                }
                override fun onDialogNegativeClick(value: Number) {
                    // Do nothing..
                }
            }

        // Show the dialog
        InputDialogFragment(
            _activity.getString(R.string.config_input_title).plus(" ID: $position"),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
            _configs[position].value,
            10,
            0,
            listener
        ).show(_activity.parentFragmentManager, null)
    }
}