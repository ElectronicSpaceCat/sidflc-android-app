package com.android.app.fragments.device.deviceSensorTuner

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceSensorTunerBinding
import com.android.app.device.bluetooth.device.DeviceData
import com.android.app.fragments.device.deviceSensorTuner.configAdapter.ConfigAdapter
import com.android.app.fragments.dialogs.InputDialogFragment
import com.android.app.utils.plotter.DataPoint
import com.android.app.utils.calculators.CumStdDev
import com.android.app.utils.plotter.CanvasPlotter
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import java.util.*
import kotlin.collections.ArrayDeque

class DeviceSensorTunerFragment : Fragment() {
    private var _fragmentDeviceSensorTunerBinding: FragmentDeviceSensorTunerBinding? = null
    private val fragmentDeviceSensorTunerBinding get() = _fragmentDeviceSensorTunerBinding!!

    private var _adapter: ConfigAdapter? = null
    private var _queueSize = 25
    private val _arrayDequeue = ArrayDeque<Double>()
    private val _cumStdDeviation = CumStdDev()
    private var _sensorEnablePrev = false

    private inner class PlotData {
        var ref : Float = 0f
        var xMin : Float = 0f
        var xMax : Float = 0f
        var yMin : Float = 0f
        var yMax : Float = 0f
        var xAxisHeight : Float = 0f
        var yAxisWidth : Float = 0f
        var xIncrement : Float = 0f
        var yIncrement : Float = 0f
    }

    private val _plotterData = arrayOf(
        PlotData(),
        PlotData()
    )

    private var _activePlotterData : PlotData = _plotterData[0]

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _fragmentDeviceSensorTunerBinding = FragmentDeviceSensorTunerBinding.inflate(inflater, container, false)
        return fragmentDeviceSensorTunerBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentDeviceSensorTunerBinding.graphViewBox.plotType = CanvasPlotter.PlotType.LAYOUT
        val theme = resources.newTheme()
        theme.applyStyle(R.style.Theme_PlinkApp, true)
        fragmentDeviceSensorTunerBinding.graphViewBox.colorRefLine = resources.getColor(R.color.textprimary, theme)

        fragmentDeviceSensorTunerBinding.graphViewPlotter.plotType = CanvasPlotter.PlotType.PLOT

        // Configure the recycler view
        fragmentDeviceSensorTunerBinding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        fragmentDeviceSensorTunerBinding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL)
        )

        // Configure the recycler view animator
        val animator = fragmentDeviceSensorTunerBinding.recyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        _adapter = ConfigAdapter(this, DataShared.device)

        // Set recyclerView adapter to adapter instance
        fragmentDeviceSensorTunerBinding.recyclerView.adapter = _adapter

        // Init the queue size value
        fragmentDeviceSensorTunerBinding.queueSizeValue.text = _queueSize.toString()

        // Initialize the plotter data
        DeviceData.Sensor.Id.entries.forEach {
            setDefaultPlotterData(it)
        }

        // Init the sensor to enabled
        DataShared.device.setSensorEnable(true)

        /**
         * Observe connection state navigate back to scanner page on disconnect
         */
        DataShared.device.connectionState.observe(viewLifecycleOwner) { state ->
            val navController = Navigation.findNavController(requireActivity(), R.id.container_nav)
            if (state.state == ConnectionState.State.DISCONNECTED) {
                val options = NavOptions.Builder()
                    .setPopUpTo(R.id.homeFragment, false)
                    .setLaunchSingleTop(true)
                    .build()
                navController.navigate(R.id.deviceScannerFragment, null, options)
            }
        }

        /**
         * Observe the selected sensor
         */
        DataShared.device.sensorSelected.observe(viewLifecycleOwner) {
            when(it.id) {
                DeviceData.Sensor.Id.SHORT -> {
                    fragmentDeviceSensorTunerBinding.btnSensor1.isChecked = true
                    fragmentDeviceSensorTunerBinding.btnSensor2.isChecked = false
                    fragmentDeviceSensorTunerBinding.switchDriftCompEnable.isEnabled = true
                    fragmentDeviceSensorTunerBinding.containerDeviceModel.visibility = View.VISIBLE
                }
                DeviceData.Sensor.Id.LONG -> {
                    fragmentDeviceSensorTunerBinding.btnSensor1.isChecked = false
                    fragmentDeviceSensorTunerBinding.btnSensor2.isChecked = true
                    fragmentDeviceSensorTunerBinding.switchDriftCompEnable.isEnabled = false
                    fragmentDeviceSensorTunerBinding.containerDeviceModel.visibility = View.GONE
                }
                else -> {
                    fragmentDeviceSensorTunerBinding.btnSensor1.isChecked = false
                    fragmentDeviceSensorTunerBinding.btnSensor2.isChecked = false
                    fragmentDeviceSensorTunerBinding.switchDriftCompEnable.isEnabled = false
                    fragmentDeviceSensorTunerBinding.containerDeviceModel.visibility = View.GONE
                }
            }
            // Set the active plotter data to match active sensor
            _activePlotterData = _plotterData[it.id.ordinal]
            setPlotterBounds(_activePlotterData)

            fragmentDeviceSensorTunerBinding.refData.text = _activePlotterData.ref.toString()
            fragmentDeviceSensorTunerBinding.incrementsValue.text = _activePlotterData.yIncrement.toString()
            fragmentDeviceSensorTunerBinding.sampleSizeData.text = DataShared.device.activeSensor.sampleSize.toString()
            fragmentDeviceSensorTunerBinding.sensorType.text = DataShared.device.activeSensor.type.name
            fragmentDeviceSensorTunerBinding.switchDriftCompEnable.isChecked = DataShared.device.activeSensor.driftCompensationEnable
            // Reset standard deviation
            _cumStdDeviation.reset()
            // Trigger a sensor load config
            DataShared.device.activeSensor.loadConfigs()
            // Reset plot
            clearPlot()
        }

        /**
         * Handler for the short range sensor selector
         */
        fragmentDeviceSensorTunerBinding.btnSensor1.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked &&
                DataShared.device.sensorSelected.value!!.id != DeviceData.Sensor.Id.SHORT){

                fragmentDeviceSensorTunerBinding.btnSensor2.isChecked = false
                DataShared.device.setSensor(DeviceData.Sensor.Id.SHORT)
            }
        }

        /**
         * Handler for the long range sensor selector
         */
        fragmentDeviceSensorTunerBinding.btnSensor2.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked &&
                DataShared.device.sensorSelected.value!!.id != DeviceData.Sensor.Id.LONG){

                fragmentDeviceSensorTunerBinding.btnSensor1.isChecked = false
                DataShared.device.setSensor(DeviceData.Sensor.Id.LONG)
            }
        }

        /**
         * Observe the sensor enabled status
         */
        DataShared.device.sensorEnabled.observe(viewLifecycleOwner) { enabled ->
            fragmentDeviceSensorTunerBinding.switchSensorEnable.isChecked = enabled
        }

        /**
         * Observe the power source to set the indicator if USB, BATTERY, or NONE
         */
        DataShared.device.pwrSource.observe(viewLifecycleOwner) { source ->
            fragmentDeviceSensorTunerBinding.pwrSrc.text = source.name
        }

        /**
         * Observe battery status
         */
        DataShared.device.batteryStatus.observe(viewLifecycleOwner) { status ->
            fragmentDeviceSensorTunerBinding.battStatus.text = status.name
        }

        /**
         * Observe battery level
         */
        DataShared.device.batteryLevel.observe(viewLifecycleOwner) { level ->
            fragmentDeviceSensorTunerBinding.battLevel.text = level.toString()
        }

        /**
         * Observe the sensor status
         */
        DataShared.device.sensorStatus.observe(viewLifecycleOwner) { status ->
            fragmentDeviceSensorTunerBinding.sensorStatus.text = status.name
        }

        /**
         * Observe the raw sensor range data
         */
        DataShared.device.sensorRangeRaw.observe(viewLifecycleOwner) { value ->
            // Make sure sensor configs are read before updating graph
            if(!DataShared.device.activeSensor.isInitialized) return@observe
            // Don't update data if not enabled
            if(!fragmentDeviceSensorTunerBinding.switchSensorEnable.isChecked) return@observe

            // Update the raw value
            fragmentDeviceSensorTunerBinding.rawData.text = value.toString()
            // Update the standard deviation
            _cumStdDeviation.sd(value.toDouble())
            // Reset SD is out of normal range
            if(_cumStdDeviation.sd > SD_RESET_THRESHOLD){
                _cumStdDeviation.reset()
            }
            fragmentDeviceSensorTunerBinding.sdData.text = String.format(Locale.getDefault(), "%.1f", _cumStdDeviation.sd)

            // Update the filtered value
            fragmentDeviceSensorTunerBinding.filteredData.text = String.format(
                Locale.getDefault(),
                "%.1f",
                DataShared.device.activeSensor.rangeFiltered.value!!)

            // Update the drift offset value
            fragmentDeviceSensorTunerBinding.driftData.text = String.format(
                Locale.getDefault(),
                "%.1f",
                DataShared.device.activeSensor.driftOffset)

            // Update the data queue
            if(_arrayDequeue.count() >= _queueSize){
                _arrayDequeue.removeFirst()
            }
            _arrayDequeue.add(DataShared.device.activeSensor.rangeFiltered.value!!)

            // Update the plotter
            val data = (0 until _queueSize).map {
                DataPoint(it.toFloat(), _arrayDequeue[it].toFloat())
            }
            updatePlot(data)
        }

        /**
         * Handler for the plotter auto-scale enable button
         */
        fragmentDeviceSensorTunerBinding.btnAutoScale.setOnClickListener {
            _activePlotterData.yMax = (DataShared.device.activeSensor.rangeFiltered.value!! + _activePlotterData.yIncrement * 2).toFloat()
            _activePlotterData.yMin = (DataShared.device.activeSensor.rangeFiltered.value!! - _activePlotterData.yIncrement * 2).toFloat()
            setPlotterBounds(_activePlotterData)
        }

        /**
         * Handler for the plotter reset button
         */
        fragmentDeviceSensorTunerBinding.btnPlotReset.setOnClickListener {
            setDefaultPlotterData(DataShared.device.sensorSelected.value!!.id)
            setPlotterBounds(_activePlotterData)
        }

        /**
         * Handler for the range enable switch
         */
        fragmentDeviceSensorTunerBinding.switchSensorEnable.setOnCheckedChangeListener { _, isChecked ->
            DataShared.device.setSensorEnable(isChecked)
            if(!isChecked){
                resetQueueData()
                _cumStdDeviation.reset()
            }
        }

        /**
         * Handler for the drift compensation enable switch
         */
        fragmentDeviceSensorTunerBinding.switchDriftCompEnable.setOnCheckedChangeListener { _, isChecked ->
            DataShared.device.activeSensor.driftCompensationEnable = isChecked
        }

        /**
         * Handler for the reset sensor default button
         */
        fragmentDeviceSensorTunerBinding.btnResetSensorDefault.setOnClickListener {
            DataShared.device.activeSensor.reset()
            clearPlot()
        }

        /**
         * Handler for the reset sensor factory button
         */
        fragmentDeviceSensorTunerBinding.btnResetSensorFactory.setOnClickListener {
            DataShared.device.activeSensor.resetFactory()
            clearPlot()
        }

        /**
         * Handler for the store configs button
         */
        fragmentDeviceSensorTunerBinding.btnStoreConfigs.setOnClickListener {
            DataShared.device.activeSensor.storeConfigData()
            Toast.makeText(context, "Data stored", Toast.LENGTH_SHORT).show()
        }

        /**
         * Handler for the std dev reset button
         */
        fragmentDeviceSensorTunerBinding.btnResetSd.setOnClickListener {
            _cumStdDeviation.reset()
        }

        fragmentDeviceSensorTunerBinding.btnEditIncrements.setOnClickListener {
            val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    _activePlotterData.yIncrement = value.toFloat()
                    fragmentDeviceSensorTunerBinding.graphViewBox.yIncrement = value.toFloat()
                    fragmentDeviceSensorTunerBinding.incrementsValue.text = value.toInt().toString()
                    setPlotterBounds(_activePlotterData)
                }
                override fun onDialogNegativeClick(value: Number) {
                    // Do nothing..
                }
            }
            InputDialogFragment(
                "Set Increments",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                fragmentDeviceSensorTunerBinding.graphViewBox.yIncrement,
                4,
                0,
                listener).show(parentFragmentManager, null)
        }

        fragmentDeviceSensorTunerBinding.btnEditRef.setOnClickListener {
            val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    _activePlotterData.ref = value.toFloat()
                    fragmentDeviceSensorTunerBinding.graphViewBox.ref = value.toFloat()
                    fragmentDeviceSensorTunerBinding.refData.text = value.toInt().toString()
                    setPlotterBounds(_activePlotterData)
                }
                override fun onDialogNegativeClick(value: Number) {
                    // Do nothing..
                }
            }
            InputDialogFragment(
                "Set Reference",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                fragmentDeviceSensorTunerBinding.graphViewBox.ref,
                4,
                0,
                listener).show(parentFragmentManager, null)
        }

        fragmentDeviceSensorTunerBinding.btnEditSampleSize.setOnClickListener {
            val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    // Prevent zero as a sample size
                    val size = Integer.max(1, value.toInt())
                    // Update sample size
                    DataShared.device.activeSensor.setFilterSampleSize(size)
                    fragmentDeviceSensorTunerBinding.sampleSizeData.text = DataShared.device.activeSensor.sampleSize.toString()
                }
                override fun onDialogNegativeClick(value: Number) {
                    // Do nothing..
                }
            }

            val currentSampleSize = DataShared.device.activeSensor.sampleSize

            // Prompt user input dialog to enter the sample size value
            InputDialogFragment(
                "Set Sample Size",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                currentSampleSize,
                3,
                0,
                listener).show(parentFragmentManager, null)
        }

        fragmentDeviceSensorTunerBinding.btnEditQueueSize.setOnClickListener {
            val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    // Cap the value to MAX_BUFF_SIZE if higher
                    _queueSize = Integer.min(value.toInt(), MAX_BUFF_SIZE)
                    resetQueueData()
                    _activePlotterData.xMax = _queueSize.toFloat()
                    fragmentDeviceSensorTunerBinding.queueSizeValue.text = _queueSize.toString()
                    setPlotterBounds(_activePlotterData)
                }
                override fun onDialogNegativeClick(value: Number) {
                    // Do nothing..
                }
            }
            InputDialogFragment(
                "Set Queue Size",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                _queueSize,
                3,
                0,
                listener).show(parentFragmentManager, null)
        }

        /**
         * Increases the y upper bound of the graph by 1 increment
         */
        fragmentDeviceSensorTunerBinding.ymaxInc.setOnClickListener {
            _activePlotterData.yMax += _activePlotterData.yIncrement
            setPlotterBounds(_activePlotterData)
        }

        /**
         * Decreases the y upper bound of the graph by 1 increment
         */
        fragmentDeviceSensorTunerBinding.ymaxDec.setOnClickListener {
            _activePlotterData.yMax -= _activePlotterData.yIncrement
            setPlotterBounds(_activePlotterData)
        }

        /**
         * Increases the y lower bound of the graph by 1 increment
         */
        fragmentDeviceSensorTunerBinding.yminInc.setOnClickListener {
            _activePlotterData.yMin += _activePlotterData.yIncrement
            setPlotterBounds(_activePlotterData)
        }

        /**
         * Decreases the y lower bound of the graph by 1 increment
         */
        fragmentDeviceSensorTunerBinding.yminDec.setOnClickListener {
            _activePlotterData.yMin -= _activePlotterData.yIncrement
            setPlotterBounds(_activePlotterData)
        }
    }

    private fun setDefaultPlotterData(id : DeviceData.Sensor.Id) {
        when(val idIdx = id.ordinal) {
            DeviceData.Sensor.Id.SHORT.ordinal -> {
                _plotterData[idIdx].ref = DataShared.device.model.getMaxCarriagePosition().toFloat()
                _plotterData[idIdx].xMin = 0f
                _plotterData[idIdx].xMax = (_queueSize - 1).toFloat()
                _plotterData[idIdx].yMin = 0f
                _plotterData[idIdx].yMax = _activePlotterData.ref + 10
                _plotterData[idIdx].yAxisWidth = 60f
                _plotterData[idIdx].xIncrement = 10f
                _plotterData[idIdx].yIncrement = 10f
            }
            DeviceData.Sensor.Id.LONG.ordinal -> {
                _plotterData[idIdx].ref = 0f
                _plotterData[idIdx].xMin = 0f
                _plotterData[idIdx].xMax = (_queueSize - 1).toFloat()
                _plotterData[idIdx].yMin = 0f
                _plotterData[idIdx].yMax = 2500f
                _plotterData[idIdx].yAxisWidth = 60f
                _plotterData[idIdx].xIncrement = 10f
                _plotterData[idIdx].yIncrement = 500f
            }
            else -> {}
        }
    }

    private fun updatePlot(data: List<DataPoint>){
        fragmentDeviceSensorTunerBinding.graphViewPlotter.setData(data)
        fragmentDeviceSensorTunerBinding.graphViewPlotter.draw()
    }

    private fun setPlotterBounds(data : PlotData) {
        // Plotter Box
        fragmentDeviceSensorTunerBinding.graphViewBox.xMin = data.xMin
        fragmentDeviceSensorTunerBinding.graphViewBox.xMax = data.xMax
        fragmentDeviceSensorTunerBinding.graphViewBox.yMin = data.yMin
        fragmentDeviceSensorTunerBinding.graphViewBox.yMax = data.yMax

        fragmentDeviceSensorTunerBinding.graphViewBox.xIncrement = data.xIncrement
        fragmentDeviceSensorTunerBinding.graphViewBox.yIncrement = data.yIncrement
        fragmentDeviceSensorTunerBinding.graphViewBox.xAxisHeight = data.xAxisHeight
        fragmentDeviceSensorTunerBinding.graphViewBox.yAxisWidth = data.yAxisWidth
        fragmentDeviceSensorTunerBinding.graphViewBox.ref = data.ref
        fragmentDeviceSensorTunerBinding.graphViewBox.drawAxisX = false
        fragmentDeviceSensorTunerBinding.graphViewBox.updateBounds()
        fragmentDeviceSensorTunerBinding.graphViewBox.draw()

        // Plotter
        fragmentDeviceSensorTunerBinding.graphViewPlotter.xMin = data.xMin
        fragmentDeviceSensorTunerBinding.graphViewPlotter.xMax = data.xMax
        fragmentDeviceSensorTunerBinding.graphViewPlotter.yMin = data.yMin
        fragmentDeviceSensorTunerBinding.graphViewPlotter.yMax = data.yMax

        fragmentDeviceSensorTunerBinding.graphViewPlotter.xIncrement = data.xIncrement
        fragmentDeviceSensorTunerBinding.graphViewPlotter.yIncrement = data.yIncrement
        fragmentDeviceSensorTunerBinding.graphViewPlotter.xAxisHeight = data.xAxisHeight
        fragmentDeviceSensorTunerBinding.graphViewPlotter.yAxisWidth = data.yAxisWidth
        fragmentDeviceSensorTunerBinding.graphViewPlotter.updateBounds()
    }

    private fun clearPlot() {
        resetQueueData()
        updatePlot(listOf(DataPoint(0f,0f)))
    }

    private fun resetQueueData() {
        _arrayDequeue.clear()
        for (idx in 0.._queueSize) {
            _arrayDequeue.add(0.0)
        }
    }

    override fun onResume() {
        DataShared.device.setSensorEnable(_sensorEnablePrev)
        DataShared.device.enableBatteryLevelNotify(true)
        super.onResume()
    }

    override fun onPause() {
        _sensorEnablePrev = DataShared.device.sensorEnabled.value!!
        DataShared.device.enableBatteryLevelNotify(false)
        super.onPause()
    }

    override fun onDestroyView() {
        DataShared.device.setSensorEnable(false)

        _adapter = null
        _fragmentDeviceSensorTunerBinding = null
        super.onDestroyView()
    }

    companion object{
        const val MAX_BUFF_SIZE = 100
        const val SD_RESET_THRESHOLD = 10.0
    }
}