package com.android.greentech.plink.fragments.device.deviceTuner

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
import com.android.greentech.plink.dataShared.DataShared
import com.android.greentech.plink.databinding.FragmentDeviceTunerBinding
import com.android.greentech.plink.device.bluetooth.sensor.SensorData
import com.android.greentech.plink.fragments.device.deviceTuner.configAdapter.ConfigAdapter
import com.android.greentech.plink.fragments.dialogs.InputDialogFragment
import com.android.greentech.plink.utils.plotter.DataPoint
import com.android.greentech.plink.utils.calculators.CumStdDev
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.roundToInt

class DeviceTunerFragment : Fragment() {
    private var _fragmentDeviceTunerBinding: FragmentDeviceTunerBinding? = null
    private val fragmentDeviceTunerBinding get() = _fragmentDeviceTunerBinding!!
    private var _adapter: ConfigAdapter? = null
    private var _queueSize = 25
    private val _arrayDequeue = ArrayDeque<Double>(MAX_BUFF_SIZE)
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

    private val _plotDataShortRange = PlotData()
    private val _plotDataLongRange = PlotData()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setDefaultPlotValues(SensorData.Sensor.Id.SHORT)
        setDefaultPlotValues(SensorData.Sensor.Id.LONG)

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
            when(DataShared.device.sensorSelected.value!!.id) {
                SensorData.Sensor.Id.SHORT -> {
                    fragmentDeviceTunerBinding.refData.text = _plotDataShortRange.ref.toString()
                    fragmentDeviceTunerBinding.incrementsValue.text = _plotDataShortRange.yIncrement.toString()
                    fragmentDeviceTunerBinding.sampleSizeData.text = DataShared.device.sensorCarriagePosition.sampleSize.toString()
                    _cumStdDeviation.reset()

                    fragmentDeviceTunerBinding.btnSensor1.isChecked = true
                    fragmentDeviceTunerBinding.btnSensor2.isChecked = false

                    fragmentDeviceTunerBinding.sensorType.text = DataShared.device.sensorCarriagePosition.type.name

                    fragmentDeviceTunerBinding.containerDeviceModel.visibility = View.VISIBLE
                }
                SensorData.Sensor.Id.LONG -> {
                    fragmentDeviceTunerBinding.refData.text = _plotDataLongRange.ref.toString()
                    fragmentDeviceTunerBinding.incrementsValue.text = _plotDataLongRange.yIncrement.toString()
                    fragmentDeviceTunerBinding.sampleSizeData.text = DataShared.device.sensorDeviceHeight.sampleSize.toString()
                    _cumStdDeviation.reset()

                    fragmentDeviceTunerBinding.btnSensor1.isChecked = false
                    fragmentDeviceTunerBinding.btnSensor2.isChecked = true

                    fragmentDeviceTunerBinding.containerDeviceModel.visibility = View.GONE

                    fragmentDeviceTunerBinding.sensorType.text = DataShared.device.sensorDeviceHeight.type.name
                }
                else -> {
                    fragmentDeviceTunerBinding.btnSensor1.isChecked = false
                    fragmentDeviceTunerBinding.btnSensor2.isChecked = false

                    fragmentDeviceTunerBinding.containerDeviceModel.visibility = View.GONE
                }
            }
            updatePlot()
        }

        /**
         * Handler for the short range sensor selector
         */
        fragmentDeviceTunerBinding.btnSensor1.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked && !DataShared.device.sensorCarriagePosition.isActive){
                fragmentDeviceTunerBinding.btnSensor2.isChecked = false
                DataShared.device.sensorCarriagePosition.select()
            }
        }

        /**
         * Handler for the long range sensor selector
         */
        fragmentDeviceTunerBinding.btnSensor2.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked && !DataShared.device.sensorDeviceHeight.isActive){
                fragmentDeviceTunerBinding.btnSensor1.isChecked = false
                DataShared.device.sensorDeviceHeight.select()
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
         * Observe the raw sensor range data and show it and the filtered version
         */
        DataShared.device.sensorRangeRaw.observe(viewLifecycleOwner) { value ->
            // Update the raw value
            fragmentDeviceTunerBinding.rawData.text = value.toString()

            fragmentDeviceTunerBinding.sdData.text =
                String.format(Locale.getDefault(), "%.1f", _cumStdDeviation.sd(value.toDouble()))

            // Update the filtered value
            when(DataShared.device.sensorSelected.value!!.id) {
                DataShared.device.sensorCarriagePosition.id -> {
                    _arrayDequeue.add(DataShared.device.sensorCarriagePosition.rangeFiltered.value!!)
                    fragmentDeviceTunerBinding.filteredData.text = String.format(
                        Locale.getDefault(),
                        "%.1f",
                        DataShared.device.sensorCarriagePosition.rangeFiltered.value!!)
                }
                DataShared.device.sensorDeviceHeight.id -> {
                    _arrayDequeue.add(DataShared.device.sensorDeviceHeight.rangeFiltered.value!!)
                    fragmentDeviceTunerBinding.filteredData.text = String.format(
                        Locale.getDefault(),
                        "%.1f",
                        DataShared.device.sensorDeviceHeight.rangeFiltered.value!!)
                }
                else -> {

                }
            }
            // Update the plotter
            if(_arrayDequeue.count() >= _queueSize){
                val data = (0 until _queueSize).map {
                    DataPoint(it.toDouble(), _arrayDequeue[it])
                }
                fragmentDeviceTunerBinding.graphViewPlotter.setData(data)
                fragmentDeviceTunerBinding.graphViewPlotter.draw()

                _arrayDequeue.removeFirst()
            }
        }

        /**
         * Handler for the plotter auto-scale enable button
         */
        fragmentDeviceTunerBinding.btnAutoScale.setOnClickListener {
            when(DataShared.device.sensorSelected.value!!.id) {
                DataShared.device.sensorCarriagePosition.id -> {
                    _plotDataShortRange.yMax = (DataShared.device.sensorCarriagePosition.rangeFiltered.value!! + _plotDataShortRange.yIncrement * 2).roundToInt()
                    _plotDataShortRange.yMin = (DataShared.device.sensorCarriagePosition.rangeFiltered.value!! - _plotDataShortRange.yIncrement * 2).roundToInt()
                    updatePlot()
                }
                DataShared.device.sensorDeviceHeight.id -> {
                    _plotDataLongRange.yMax = (DataShared.device.sensorDeviceHeight.rangeFiltered.value!! + _plotDataLongRange.yIncrement * 2).roundToInt()
                    _plotDataLongRange.yMin = (DataShared.device.sensorDeviceHeight.rangeFiltered.value!! - _plotDataLongRange.yIncrement * 2).roundToInt()
                    updatePlot()
                }
                else -> {

                }
            }
        }

        /**
         * Handler for the plotter reset button
         */
        fragmentDeviceTunerBinding.btnPlotReset.setOnClickListener {
            setDefaultPlotValues(DataShared.device.sensorSelected.value!!.id)
            updatePlot()
        }

        /**
         * Handler for the range enable switch
         */
        fragmentDeviceTunerBinding.switchSensorEnable.setOnCheckedChangeListener { _, isChecked ->
            DataShared.device.setSensorEnable(isChecked)
        }

        /**
         * Handler for the reset sensor default button
         */
        fragmentDeviceTunerBinding.btnResetSensorDefault.setOnClickListener {
            if(DataShared.device.sensorSelected.value!!.id.ordinal < DataShared.device.sensors.size){
                DataShared.device.sensors[DataShared.device.sensorSelected.value!!.id.ordinal].reset()
                clearPlot()
            }
        }

        /**
         * Handler for the reset sensor factory button
         */
        fragmentDeviceTunerBinding.btnResetSensorFactory.setOnClickListener {
            if(DataShared.device.sensorSelected.value!!.id.ordinal < DataShared.device.sensors.size){
                DataShared.device.sensors[DataShared.device.sensorSelected.value!!.id.ordinal].resetFactory()
                clearPlot()
            }
        }

        /**
         * Handler for the store configs button
         */
        fragmentDeviceTunerBinding.btnStoreConfigs.setOnClickListener {
            if(DataShared.device.sensorSelected.value!!.id.ordinal < DataShared.device.sensors.size){
                DataShared.device.sensors[DataShared.device.sensorSelected.value!!.id.ordinal].storeConfigData()
                Toast.makeText(context, "Data stored", Toast.LENGTH_SHORT).show()
            }
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
                    when(DataShared.device.sensorSelected.value!!.id) {
                        DataShared.device.sensorCarriagePosition.id -> {
                            _plotDataShortRange.yIncrement = value.toInt()
                        }
                        DataShared.device.sensorDeviceHeight.id -> {
                            _plotDataLongRange.yIncrement = value.toInt()
                        }
                        else -> {

                        }
                    }
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
                    when(DataShared.device.sensorSelected.value!!.id) {
                        DataShared.device.sensorCarriagePosition.id -> {
                            _plotDataShortRange.ref = value.toInt()
                        }
                        DataShared.device.sensorDeviceHeight.id -> {
                            _plotDataLongRange.ref = value.toInt()
                        }
                        else -> {

                        }
                    }
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
                    when(DataShared.device.sensorSelected.value!!.id) {
                        DataShared.device.sensorCarriagePosition.id -> {
                            DataShared.device.sensorCarriagePosition.setFilterSampleSize(size)
                            fragmentDeviceTunerBinding.sampleSizeData.text = DataShared.device.sensorCarriagePosition.sampleSize.toString()
                        }
                        DataShared.device.sensorDeviceHeight.id -> {
                            DataShared.device.sensorDeviceHeight.setFilterSampleSize(size)
                            fragmentDeviceTunerBinding.sampleSizeData.text = DataShared.device.sensorDeviceHeight.sampleSize.toString()
                        }
                        else -> {

                        }
                    }
                }
                override fun onDialogNegativeClick(value: Number) {
                    // Do nothing..
                }
            }

            val currentSampleSize = when(DataShared.device.sensorSelected.value!!.id) {
                DataShared.device.sensorCarriagePosition.id -> {
                    DataShared.device.sensorCarriagePosition.sampleSize
                }
                DataShared.device.sensorDeviceHeight.id -> {
                    DataShared.device.sensorDeviceHeight.sampleSize
                }
                else -> {
                    0
                }
            }

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
                    _arrayDequeue.clear()
                    _plotDataShortRange.xMax = _queueSize
                    _plotDataLongRange.xMax = _queueSize
                    fragmentDeviceTunerBinding.queueSizeValue.text = _queueSize.toString()

                    when(DataShared.device.sensorSelected.value!!.id) {
                        DataShared.device.sensorCarriagePosition.id -> {
                            setPlotterBounds(_plotDataShortRange)
                        }
                        DataShared.device.sensorDeviceHeight.id -> {
                            setPlotterBounds(_plotDataLongRange)
                        }
                        else -> {

                        }
                    }
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
            when(DataShared.device.sensorSelected.value!!.id) {
                DataShared.device.sensorCarriagePosition.id -> {
                    _plotDataShortRange.yMax += _plotDataShortRange.yIncrement
                    setPlotterBounds(_plotDataShortRange)
                }
                DataShared.device.sensorDeviceHeight.id -> {
                    _plotDataLongRange.yMax += _plotDataLongRange.yIncrement
                    setPlotterBounds(_plotDataLongRange)
                }
                else -> {

                }
            }
        }

        /**
         * Decreases the y upper bound of the graph by 1 increment
         */
        fragmentDeviceTunerBinding.ymaxDec.setOnClickListener {
            when(DataShared.device.sensorSelected.value!!.id) {
                DataShared.device.sensorCarriagePosition.id -> {
                    _plotDataShortRange.yMax -= _plotDataShortRange.yIncrement
                    setPlotterBounds(_plotDataShortRange)
                }
                DataShared.device.sensorDeviceHeight.id -> {
                    _plotDataLongRange.yMax -= _plotDataLongRange.yIncrement
                    setPlotterBounds(_plotDataLongRange)
                }
                else -> {

                }
            }
        }

        /**
         * Increases the y lower bound of the graph by 1 increment
         */
        fragmentDeviceTunerBinding.yminInc.setOnClickListener {
            when(DataShared.device.sensorSelected.value!!.id) {
                DataShared.device.sensorCarriagePosition.id -> {
                    _plotDataShortRange.yMin += _plotDataShortRange.yIncrement
                    setPlotterBounds(_plotDataShortRange)
                }
                DataShared.device.sensorDeviceHeight.id -> {
                    _plotDataLongRange.yMin += _plotDataLongRange.yIncrement
                    setPlotterBounds(_plotDataLongRange)
                }
                else -> {

                }
            }
        }

        /**
         * Decreases the y lower bound of the graph by 1 increment
         */
        fragmentDeviceTunerBinding.yminDec.setOnClickListener {
            when(DataShared.device.sensorSelected.value!!.id) {
                DataShared.device.sensorCarriagePosition.id -> {
                    _plotDataShortRange.yMin -= _plotDataShortRange.yIncrement
                    setPlotterBounds(_plotDataShortRange)
                }
                DataShared.device.sensorDeviceHeight.id -> {
                    _plotDataLongRange.yMin -= _plotDataLongRange.yIncrement
                    setPlotterBounds(_plotDataLongRange)
                }
                else -> {

                }
            }
        }
    }

    private fun setDefaultPlotValues(id : SensorData.Sensor.Id) {
        when(id) {
            SensorData.Sensor.Id.SHORT -> {
                _plotDataShortRange.ref = DataShared.device.model.getMaxCarriagePosition().toInt()
                _plotDataShortRange.xMin = 0
                _plotDataShortRange.xMax = _queueSize
                _plotDataShortRange.yMin = 0
                _plotDataShortRange.yMax = _plotDataShortRange.ref + 10
                _plotDataShortRange.xOffset = 70f
                _plotDataShortRange.xIncrement = 10
                _plotDataShortRange.yIncrement = 10
            }
            SensorData.Sensor.Id.LONG -> {
                _plotDataLongRange.ref = 0
                _plotDataLongRange.xMin = 0
                _plotDataLongRange.xMax = _queueSize
                _plotDataLongRange.yMin = 0
                _plotDataLongRange.yMax = 4000
                _plotDataLongRange.xOffset = 70f
                _plotDataLongRange.xIncrement = 10
                _plotDataLongRange.yIncrement = 500
            }
            else -> {}
        }
    }

    private fun updatePlot(){
        when(DataShared.device.sensorSelected.value!!.id) {
            DataShared.device.sensorCarriagePosition.id -> {
                setPlotterBounds(_plotDataShortRange)
            }
            DataShared.device.sensorDeviceHeight.id -> {
                setPlotterBounds(_plotDataLongRange)
            }
            else -> {

            }
        }
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
        _arrayDequeue.clear()
        _cumStdDeviation.reset()
        fragmentDeviceTunerBinding.graphViewPlotter.setData(listOf(DataPoint(0.0,0.0)))
        fragmentDeviceTunerBinding.graphViewPlotter.draw()
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

    companion object{
        const val MAX_BUFF_SIZE = 100
    }
}