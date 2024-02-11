package com.android.app.fragments.device.deviceTuner

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceTunerBinding
import com.android.app.device.bluetooth.device.DeviceData
import com.android.app.fragments.device.deviceTuner.configAdapter.ConfigAdapter
import com.android.app.fragments.dialogs.InputDialogFragment
import com.android.app.utils.plotter.DataPoint
import com.android.app.utils.calculators.CumStdDev
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.roundToInt

class DeviceTunerFragment : Fragment() {
    private var _fragmentDeviceTunerBinding: FragmentDeviceTunerBinding? = null
    private val fragmentDeviceTunerBinding get() = _fragmentDeviceTunerBinding!!
    private var _adapter: ConfigAdapter? = null
    private var _queueSize = 25
    private val _arrayDequeue = ArrayDeque<Double>()
    private val _cumStdDeviation = CumStdDev()
    private var _sensorEnablePrev = false

    private inner class PlotData {
        var ref : Int = 0
        var xMin : Int = 0
        var xMax : Int = 0
        var yMin : Int = 0
        var yMax : Int = 0
        var xOffset : Float = 0f // Offsets the x values
        var yOffset : Float = 0f // Offsets the y values
        var xIncrement : Int = 0
        var yIncrement : Int = 0
    }

    private val _plotterData = arrayOf(PlotData(), PlotData())
    private var _activePlotterData : PlotData = _plotterData[0]

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _fragmentDeviceTunerBinding = FragmentDeviceTunerBinding.inflate(inflater, container, false)
        return fragmentDeviceTunerBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure the recycler view
        fragmentDeviceTunerBinding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        fragmentDeviceTunerBinding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL)
        )

        // Configure the recycler view animator
        val animator = fragmentDeviceTunerBinding.recyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        _adapter = ConfigAdapter(this, DataShared.device)

        // Set recyclerView adapter to adapter instance
        fragmentDeviceTunerBinding.recyclerView.adapter = _adapter

        // Init the queue size value
        fragmentDeviceTunerBinding.queueSizeValue.text = _queueSize.toString()

        // Init the sensor to enabled
        DataShared.device.setSensorEnable(true)

        /**
         * Observe the selected sensor
         */
        DataShared.device.sensorSelected.observe(viewLifecycleOwner) {
            when(it.id) {
                DeviceData.Sensor.Id.SHORT -> {
                    fragmentDeviceTunerBinding.btnSensor1.isChecked = true
                    fragmentDeviceTunerBinding.btnSensor2.isChecked = false
                    fragmentDeviceTunerBinding.switchDriftCompEnable.isEnabled = true
                    fragmentDeviceTunerBinding.containerDeviceModel.visibility = View.VISIBLE
                }
                DeviceData.Sensor.Id.LONG -> {
                    fragmentDeviceTunerBinding.btnSensor1.isChecked = false
                    fragmentDeviceTunerBinding.btnSensor2.isChecked = true
                    fragmentDeviceTunerBinding.switchDriftCompEnable.isEnabled = false
                    fragmentDeviceTunerBinding.containerDeviceModel.visibility = View.GONE
                }
                else -> {
                    fragmentDeviceTunerBinding.btnSensor1.isChecked = false
                    fragmentDeviceTunerBinding.btnSensor2.isChecked = false
                    fragmentDeviceTunerBinding.switchDriftCompEnable.isEnabled = false
                    fragmentDeviceTunerBinding.containerDeviceModel.visibility = View.GONE
                }
            }
            // Set the active plotter data to match active sensor
            _activePlotterData = _plotterData[it.id.ordinal]

            fragmentDeviceTunerBinding.refData.text = _activePlotterData.ref.toString()
            fragmentDeviceTunerBinding.incrementsValue.text = _activePlotterData.yIncrement.toString()
            fragmentDeviceTunerBinding.sampleSizeData.text = DataShared.device.activeSensor.sampleSize.toString()
            fragmentDeviceTunerBinding.sensorType.text = DataShared.device.activeSensor.type.name
            fragmentDeviceTunerBinding.switchDriftCompEnable.isChecked = DataShared.device.activeSensor.driftCompensationEnable
            // Reset standard deviation
            _cumStdDeviation.reset()

            // Init the dequeue
            resetQueueData()

            // Trigger a sensor load config
            DataShared.device.activeSensor.loadConfigs()

            updatePlot()
        }

        /**
         * Handler for the short range sensor selector
         */
        fragmentDeviceTunerBinding.btnSensor1.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked &&
                DataShared.device.sensorSelected.value!!.id != DeviceData.Sensor.Id.SHORT){

                fragmentDeviceTunerBinding.btnSensor2.isChecked = false
                DataShared.device.setSensor(DeviceData.Sensor.Id.SHORT)
            }
        }

        /**
         * Handler for the long range sensor selector
         */
        fragmentDeviceTunerBinding.btnSensor2.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked &&
                DataShared.device.sensorSelected.value!!.id != DeviceData.Sensor.Id.LONG){

                fragmentDeviceTunerBinding.btnSensor1.isChecked = false
                DataShared.device.setSensor(DeviceData.Sensor.Id.LONG)
            }
        }

        /**
         * Observe the sensor enabled status
         */
        DataShared.device.sensorEnabled.observe(viewLifecycleOwner) { enabled ->
            fragmentDeviceTunerBinding.switchSensorEnable.isChecked = enabled
        }

        /**
         * Observe the power source to set the indicator if USB, BATTERY, or NONE
         */
        DataShared.device.pwrSource.observe(viewLifecycleOwner) { source ->
            fragmentDeviceTunerBinding.pwrSrc.text = source.name
        }

        /**
         * Observe battery status
         */
        DataShared.device.batteryStatus.observe(viewLifecycleOwner) { status ->
            fragmentDeviceTunerBinding.battStatus.text = status.name
        }

        /**
         * Observe battery level
         */
        DataShared.device.batteryLevel.observe(viewLifecycleOwner) { level ->
            fragmentDeviceTunerBinding.battLevel.text = level.toString()
        }

        /**
         * Observe the sensor status
         */
        DataShared.device.sensorStatus.observe(viewLifecycleOwner) { status ->
            fragmentDeviceTunerBinding.sensorStatus.text = status.name
        }

        /**
         * Observe the raw sensor range data
         */
        DataShared.device.sensorRangeRaw.observe(viewLifecycleOwner) { value ->
            // Don't update data if not enabled
            if(!fragmentDeviceTunerBinding.switchSensorEnable.isChecked) return@observe

            // Update the raw value
            fragmentDeviceTunerBinding.rawData.text = value.toString()
            // Update the standard deviation
            _cumStdDeviation.sd(value.toDouble())
            // Reset SD is out of normal range
            if(_cumStdDeviation.sd > SD_RESET_THRESHOLD){
                _cumStdDeviation.reset()
            }
            fragmentDeviceTunerBinding.sdData.text = String.format(Locale.getDefault(), "%.1f", _cumStdDeviation.sd)

            // Update the filtered value
            fragmentDeviceTunerBinding.filteredData.text = String.format(
                Locale.getDefault(),
                "%.1f",
                DataShared.device.activeSensor.rangeFiltered.value!!)

            // Update the drift offset value
            fragmentDeviceTunerBinding.driftData.text = String.format(
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
                DataPoint(it.toDouble(), _arrayDequeue[it])
            }
            fragmentDeviceTunerBinding.graphViewPlotter.setData(data)
            fragmentDeviceTunerBinding.graphViewPlotter.draw()
        }

        /**
         * Handler for the plotter auto-scale enable button
         */
        fragmentDeviceTunerBinding.btnAutoScale.setOnClickListener {
            _activePlotterData.yMax = (DataShared.device.activeSensor.rangeFiltered.value!! + _activePlotterData.yIncrement * 2).roundToInt()
            _activePlotterData.yMin = (DataShared.device.activeSensor.rangeFiltered.value!! - _activePlotterData.yIncrement * 2).roundToInt()
            updatePlot()
        }

        /**
         * Handler for the plotter reset button
         */
        fragmentDeviceTunerBinding.btnPlotReset.setOnClickListener {
            setDefaultPlotterData(DataShared.device.sensorSelected.value!!.id)
            updatePlot()
        }

        /**
         * Handler for the range enable switch
         */
        fragmentDeviceTunerBinding.switchSensorEnable.setOnCheckedChangeListener { _, isChecked ->
            DataShared.device.setSensorEnable(isChecked)
            if(!isChecked){
                resetQueueData()
                _cumStdDeviation.reset()
            }
        }

        /**
         * Handler for the drift compensation enable switch
         */
        fragmentDeviceTunerBinding.switchDriftCompEnable.setOnCheckedChangeListener { _, isChecked ->
            DataShared.device.activeSensor.driftCompensationEnable = isChecked
        }

        /**
         * Handler for the reset sensor default button
         */
        fragmentDeviceTunerBinding.btnResetSensorDefault.setOnClickListener {
            DataShared.device.activeSensor.reset()
            clearPlot()
        }

        /**
         * Handler for the reset sensor factory button
         */
        fragmentDeviceTunerBinding.btnResetSensorFactory.setOnClickListener {
            DataShared.device.activeSensor.resetFactory()
            clearPlot()
        }

        /**
         * Handler for the store configs button
         */
        fragmentDeviceTunerBinding.btnStoreConfigs.setOnClickListener {
            DataShared.device.activeSensor.storeConfigData()
            Toast.makeText(context, "Data stored", Toast.LENGTH_SHORT).show()
        }

        /**
         * Handler for the std dev reset button
         */
        fragmentDeviceTunerBinding.btnResetSd.setOnClickListener {
            _cumStdDeviation.reset()
        }

        fragmentDeviceTunerBinding.btnEditIncrements.setOnClickListener {
            val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    _activePlotterData.yIncrement = value.toInt()
                    fragmentDeviceTunerBinding.graphViewBox.yIncrement = value.toInt()
                    fragmentDeviceTunerBinding.incrementsValue.text = value.toInt().toString()
                    updatePlot()
                }
                override fun onDialogNegativeClick(value: Number) {
                    // Do nothing..
                }
            }
            InputDialogFragment(
                "Set Increments",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                fragmentDeviceTunerBinding.graphViewBox.yIncrement,
                4,
                0,
                listener).show(parentFragmentManager, null)
        }

        fragmentDeviceTunerBinding.btnEditRef.setOnClickListener {
            val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    _activePlotterData.ref = value.toInt()
                    fragmentDeviceTunerBinding.graphViewBox.ref = value.toInt()
                    fragmentDeviceTunerBinding.refData.text = value.toInt().toString()
                    updatePlot()
                }
                override fun onDialogNegativeClick(value: Number) {
                    // Do nothing..
                }
            }
            InputDialogFragment(
                "Set Reference",
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                fragmentDeviceTunerBinding.graphViewBox.ref,
                4,
                0,
                listener).show(parentFragmentManager, null)
        }

        fragmentDeviceTunerBinding.btnEditSampleSize.setOnClickListener {
            val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    // Prevent zero as a sample size
                    val size = Integer.max(1, value.toInt())
                    // Update sample size
                    DataShared.device.activeSensor.setFilterSampleSize(size)
                    fragmentDeviceTunerBinding.sampleSizeData.text = DataShared.device.activeSensor.sampleSize.toString()
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

        fragmentDeviceTunerBinding.btnEditQueueSize.setOnClickListener {
            val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    // Cap the value to MAX_BUFF_SIZE if higher
                    _queueSize = Integer.min(value.toInt(), MAX_BUFF_SIZE)
                    resetQueueData()
                    _activePlotterData.xMax = _queueSize
                    fragmentDeviceTunerBinding.queueSizeValue.text = _queueSize.toString()
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
        fragmentDeviceTunerBinding.ymaxInc.setOnClickListener {
            _activePlotterData.yMax += _activePlotterData.yIncrement
            setPlotterBounds(_activePlotterData)
        }

        /**
         * Decreases the y upper bound of the graph by 1 increment
         */
        fragmentDeviceTunerBinding.ymaxDec.setOnClickListener {
            _activePlotterData.yMax -= _activePlotterData.yIncrement
            setPlotterBounds(_activePlotterData)
        }

        /**
         * Increases the y lower bound of the graph by 1 increment
         */
        fragmentDeviceTunerBinding.yminInc.setOnClickListener {
            _activePlotterData.yMin += _activePlotterData.yIncrement
            setPlotterBounds(_activePlotterData)
        }

        /**
         * Decreases the y lower bound of the graph by 1 increment
         */
        fragmentDeviceTunerBinding.yminDec.setOnClickListener {
            _activePlotterData.yMin -= _activePlotterData.yIncrement
            setPlotterBounds(_activePlotterData)
        }
    }

    private fun setDefaultPlotterData(id : DeviceData.Sensor.Id) {
        when(val idIdx = id.ordinal) {
            DeviceData.Sensor.Id.SHORT.ordinal -> {
                _plotterData[idIdx].ref = DataShared.device.model.getMaxCarriagePosition().toInt()
                _plotterData[idIdx].xMin = 0
                _plotterData[idIdx].xMax = _queueSize
                _plotterData[idIdx].yMin = 0
                _plotterData[idIdx].yMax = _activePlotterData.ref + 10
                _plotterData[idIdx].xOffset = 70f
                _plotterData[idIdx].xIncrement = 10
                _plotterData[idIdx].yIncrement = 10

                _activePlotterData = _plotterData[idIdx]
            }
            DeviceData.Sensor.Id.LONG.ordinal -> {
                _plotterData[idIdx].ref = 0
                _plotterData[idIdx].xMin = 0
                _plotterData[idIdx].xMax = _queueSize
                _plotterData[idIdx].yMin = 0
                _plotterData[idIdx].yMax = 2500
                _plotterData[idIdx].xOffset = 70f
                _plotterData[idIdx].xIncrement = 10
                _plotterData[idIdx].yIncrement = 500

                _activePlotterData = _plotterData[idIdx]
            }
            else -> {}
        }
    }

    private fun updatePlot(){
        setPlotterBounds(_activePlotterData)
    }

    private fun setPlotterBounds(data : PlotData) {
        // Plotter Box
        fragmentDeviceTunerBinding.graphViewBox.setDataBounds(
            data.xMin,
            data.xMax,
            data.yMin,
            data.yMax
        )
        fragmentDeviceTunerBinding.graphViewBox.xIncrement = data.xIncrement
        fragmentDeviceTunerBinding.graphViewBox.yIncrement = data.yIncrement
        fragmentDeviceTunerBinding.graphViewBox.xOffset = data.xOffset
        fragmentDeviceTunerBinding.graphViewBox.yOffset = data.yOffset
        fragmentDeviceTunerBinding.graphViewBox.ref = data.ref
        fragmentDeviceTunerBinding.graphViewBox.draw()

        // Plotter
        fragmentDeviceTunerBinding.graphViewPlotter.setDataBounds(
            data.xMin,
            data.xMax,
            data.yMin,
            data.yMax
        )
        fragmentDeviceTunerBinding.graphViewPlotter.xOffset = data.xOffset
        fragmentDeviceTunerBinding.graphViewPlotter.yOffset = data.yOffset
        fragmentDeviceTunerBinding.graphViewPlotter.draw()
    }

    private fun clearPlot() {
        resetQueueData()
        fragmentDeviceTunerBinding.graphViewPlotter.setData(listOf(DataPoint(0.0,0.0)))
        fragmentDeviceTunerBinding.graphViewPlotter.draw()
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
        _fragmentDeviceTunerBinding = null
        super.onDestroyView()
    }

    init {
        // Initialize the plotter data
        DeviceData.Sensor.Id.entries.forEach {
            setDefaultPlotterData(it)
        }
    }

    companion object{
        const val MAX_BUFF_SIZE = 100
        const val SD_RESET_THRESHOLD = 10.0
    }
}