package com.android.app.fragments.device.deviceBallistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceBallisticsBinding
import com.android.app.device.projectile.ProjectileData
import com.android.app.device.projectile.ProjectilePrefUtils
import com.android.app.fragments.device.deviceBallistics.dataPointsAdapter.DataPointsAdapter
import com.android.app.fragments.settings.SettingsDeviceFragment
import com.android.app.utils.converters.ConvertLength
import com.android.app.utils.misc.Utils
import com.android.app.utils.plotter.CanvasPlotter
import com.android.app.utils.plotter.DataPoint
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import kotlin.math.roundToInt
import androidx.navigation.findNavController

/**
 * TODO: Need way to remove projectile test data if associated projectile does not exist
 */

class DeviceBallisticsFragment : Fragment() {
    private var _fragmentDeviceBallisticsBinding: FragmentDeviceBallisticsBinding? = null
    private val fragmentDeviceBallisticsBinding get() = _fragmentDeviceBallisticsBinding!!
    private var _settingsDeviceFragment : SettingsDeviceFragment ?= null
    private val settingsDeviceFragment get() = _settingsDeviceFragment!!
    private val _numDataPoints = ((DataShared.device.model.getMaxCarriagePosition().toInt() - DataShared.device.model.getMinCarriagePosition().toInt()) / POS_INCREMENTS) + 1
    private var _recData = ProjectilePrefUtils.RecData(0.0, DataShared.phoneHeight.unit, 0.0, DataShared.targetDistance.unit, Array(_numDataPoints) { 0.0 })
    private var _projectileSelectedPrev : ProjectileData ?= null

    private inner class PlotData {
        var xMin : Float = 0f
        var xMax : Float = 0f
        var yMin : Float = 0f
        var yMax : Float = 0f
        var xAxisHeight : Float = 0f
        var yAxisWidth : Float = 0f
        var xIncrement : Float = 0f
        var yIncrement : Float = 0f
    }

    private var _plotBounds = PlotData()
    private var _plotters = mutableMapOf<String, CanvasPlotter>()
    private lateinit var _adapter : DataPointsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentDeviceBallisticsBinding = FragmentDeviceBallisticsBinding.inflate(inflater, container, false)
        return fragmentDeviceBallisticsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create the ballistics settings fragment
        val fragMan: FragmentManager = parentFragmentManager
        val fragTransaction: FragmentTransaction = fragMan.beginTransaction()

        _settingsDeviceFragment = SettingsDeviceFragment()
        fragTransaction.add(fragmentDeviceBallisticsBinding.fragmentContainerView.id, settingsDeviceFragment)
        fragTransaction.commit()

        // Create the recycler adapters
        registerAdapter(fragmentDeviceBallisticsBinding.dataPoints)

        // Create plotters
        _plotters["box"] = fragmentDeviceBallisticsBinding.graphViewBox
        _plotters["box"]?.plotType = CanvasPlotter.PlotType.LAYOUT

        _plotters["cal"] = fragmentDeviceBallisticsBinding.graphViewPlotter1
        _plotters["cal"]?.plotType = CanvasPlotter.PlotType.PLOT
        val theme = resources.newTheme()
        theme.applyStyle(R.style.Theme_PlinkApp, true)
        _plotters["cal"]?.colorDataLine = resources.getColor(R.color.textprimary, theme)

        _plotters["rec"] = fragmentDeviceBallisticsBinding.graphViewPlotter2
        _plotters["rec"]?.plotType = CanvasPlotter.PlotType.PLOT

        setDefaultPlotterBounds()

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

        /**
         * Set the distance units for associated texts
         */
        ("Distance " + "(" +  DataShared.targetDistance.unitStr() + ") / Pos (mm)").also {
            fragmentDeviceBallisticsBinding.ballisticsLegend.text = it
        }

        ("Calc " + "(" +  DataShared.targetDistance.unitStr() + ")").also {
            fragmentDeviceBallisticsBinding.dataPointsHeader.calculatedHeader.text = it
        }

        ("Rec " + "(" +  DataShared.targetDistance.unitStr() + ")").also {
            fragmentDeviceBallisticsBinding.dataPointsHeader.recordedHeader.text = it
        }

        fragmentDeviceBallisticsBinding.dataPointsHeader.posHeader.text = "Pos (mm)"

        /**
         * Observe the recorded impact data changed
         */
        val listener = object : DataPointsAdapter.OnRecDataChanged {
            override fun onRecDataChanged(position: Int, value: Double) {
                if(position < _recData.recDist.size) {
                    _recData.recDist[position] = value
                    plotRecordedImpactData()
                }
            }
        }
        _adapter.setOnRecDataChangedListener(listener)

        /**
         * Observe forceOffsetOnChange
         */
        DataShared.device.model.ballistics.forceOffsetOnChange.observe(viewLifecycleOwner) {
            if(!this.isResumed) return@observe
            plotCalculatedImpactData()
        }

        /**
         * Observe efficiencyOnChange
         */
        DataShared.device.model.ballistics.efficiencyOnChange.observe(viewLifecycleOwner) {
            if(!this.isResumed) return@observe
            plotCalculatedImpactData()
        }

        /**
         * Observe frictionCoefficientOnChange
         */
        DataShared.device.model.ballistics.frictionCoefficientOnChange.observe(viewLifecycleOwner) {
            if(!this.isResumed) return@observe
            plotCalculatedImpactData()
        }

        /**
         * Observe springOnChange
         */
        DataShared.device.model.springOnChange.observe(viewLifecycleOwner) {
            if(!this.isResumed) return@observe
            plotCalculatedImpactData()
        }

        /**
         * NOTE: Set observers with references to SettingsDeviceFragment after it
         *       has been created by this fragment, otherwise there will be errors.
         */
        lifecycleScope.launch {
            settingsDeviceFragment.lifecycle.withCreated {
                /**
                 * Observe projectileOnChange
                 */
                DataShared.device.model.projectileOnChange.observe(viewLifecycleOwner) {
                    // If projectile changed, store the recorded test data
                    if(_projectileSelectedPrev != it) {
                        storeRecordedDataFromPrefs(_projectileSelectedPrev)
                        _projectileSelectedPrev = it
                    }
                    loadRecordedDataFromPrefs(it)
                    plotCalculatedImpactData()
                    plotRecordedImpactData()
                }

                /**
                 * Observe Test Pitch
                 */
                settingsDeviceFragment.prefTestPitch.setOnPreferenceChangeListener { _, newValue ->
                    var retVal = false
                    if (settingsDeviceFragment.prefTestPitch.text != newValue as String) {
                        _recData.pitch = Utils.convertStrToDouble(newValue)
                        plotCalculatedImpactData()
                        retVal = true
                    }
                    retVal
                }

                /**
                 * Observe Test Height
                 */
                settingsDeviceFragment.prefTestHeight.setOnPreferenceChangeListener { _, newValue ->
                    var retVal = false
                    if (settingsDeviceFragment.prefTestHeight.text != newValue as String) {
                        _recData.height = Utils.convertStrToDouble(newValue)
                        plotCalculatedImpactData()
                        retVal = true
                    }
                    retVal
                }
            }
        }

        /**
         * Handler for the plotter reset button
         */
        fragmentDeviceBallisticsBinding.btnPlotReset.setOnClickListener {
            setDefaultPlotterBounds()
            plotCalculatedImpactData()
        }

        /**
         * Navigate Up
         */
        fragmentDeviceBallisticsBinding.navUp.setOnClickListener {
            _plotBounds.yMin += _plotBounds.yIncrement
            _plotBounds.yMax += _plotBounds.yIncrement
            updateAllPlots()
        }

        /**
         * Navigate Down
         */
        fragmentDeviceBallisticsBinding.navDown.setOnClickListener {
            _plotBounds.yMin -= _plotBounds.yIncrement
            _plotBounds.yMax -= _plotBounds.yIncrement
            updateAllPlots()
        }

        /**
         * Navigate Left
         */
        fragmentDeviceBallisticsBinding.navLeft.setOnClickListener {
            _plotBounds.xMin -= _plotBounds.yIncrement
            _plotBounds.xMax -= _plotBounds.yIncrement
            updateAllPlots()
        }

        /**
         * Navigate Right
         */
        fragmentDeviceBallisticsBinding.navRight.setOnClickListener {
            _plotBounds.xMin += _plotBounds.yIncrement
            _plotBounds.xMax += _plotBounds.yIncrement
            updateAllPlots()
        }

        /**
         * Graph Zoom Out
         */
        fragmentDeviceBallisticsBinding.zoomOut.setOnClickListener {
            _plotBounds.xMin += _plotBounds.xIncrement
            _plotBounds.xMax -= _plotBounds.xIncrement
            _plotBounds.yMin += _plotBounds.xIncrement
            _plotBounds.yMax -= _plotBounds.xIncrement
            updateAllPlots()
        }

        /**
         * Graph Zoom In
         */
        fragmentDeviceBallisticsBinding.zoomIn.setOnClickListener {
            _plotBounds.xMin -= _plotBounds.xIncrement
            _plotBounds.xMax += _plotBounds.xIncrement
            _plotBounds.yMin -= _plotBounds.xIncrement
            _plotBounds.yMax += _plotBounds.xIncrement
            updateAllPlots()
        }
    }

    /**
     * Plots the calculated impact data using the carriage position range from 10mm to MaxCarriagePosition
     */
    private fun plotCalculatedImpactData() {
        val lensOffset = DataShared.lensOffsetFromBase.getConverted(ConvertLength.Unit.MM)

        // Create the data set to plot
        var rowId = 0
        val data = mutableListOf<DataPoint>()
        val posRange = (DataShared.device.model.getMinCarriagePosition().toInt().. DataShared.device.model.getMaxCarriagePosition().toInt())
        for(idx in posRange) {
            val impactDistance = DataShared.device.model.ballistics.calcImpactData(
                idx.toDouble(),
                lensOffset,
                _recData.height,
                _recData.pitch,
                0.0
            )

            val convertedDistance = ConvertLength.convert(ConvertLength.Unit.M, DataShared.targetDistance.unit, impactDistance.distance)
            // Add plot data point
            data.add(DataPoint(idx.toFloat(), convertedDistance.toFloat()))
            // Add data to list view on every position increment step
            if(0 == idx % POS_INCREMENTS) {
                _adapter.setPos(rowId, idx.toDouble(), false)
                _adapter.setCal(rowId, convertedDistance, true)
                rowId++
            }
        }

        _plotBounds.xMax = data.last().xVal
        _plotBounds.yMax = (data[0].yVal + POS_INCREMENTS).roundToInt().toFloat()

        _plotters["cal"]?.setData(data)
        updateAllPlots()
    }

    /**
     * Plots the recorded impact data
     */
    private fun plotRecordedImpactData() {
        val dataPoints = _adapter.getData()
        val data = mutableListOf<DataPoint>()

        dataPoints.forEach {
            data.add(DataPoint(it.pos.toFloat(), it.rec.toFloat()))
        }

        _plotters["rec"]?.setData(data)
        _plotters["rec"]?.draw()
    }

    private fun setDefaultPlotterBounds() {
        _plotBounds.xMin = DataShared.device.model.getMinCarriagePosition().toFloat()
        _plotBounds.xMax = 50f
        _plotBounds.yMin = 0f
        _plotBounds.yMax = 30f
        _plotBounds.xAxisHeight = 30f
        _plotBounds.yAxisWidth = 60f
        _plotBounds.xIncrement = 5f
        _plotBounds.yIncrement = 5f
    }

    private fun updateAllPlots() {
        _plotters.forEach {
            it.value.xMin = _plotBounds.xMin
            it.value.xMax = _plotBounds.xMax
            it.value.yMin = _plotBounds.yMin
            it.value.yMax = _plotBounds.yMax
            it.value.xIncrement = _plotBounds.xIncrement
            it.value.yIncrement = _plotBounds.yIncrement
            it.value.xAxisHeight = _plotBounds.xAxisHeight
            it.value.yAxisWidth = _plotBounds.yAxisWidth
            it.value.updateBounds()
            it.value.draw()
        }
    }

    private fun registerAdapter(recyclerView: RecyclerView) {
        val animator = recyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        // Set recyclerView adapter to adapter instance
        _adapter = DataPointsAdapter(this)
        recyclerView.adapter = _adapter

        // Configure the recycler view
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.addItemDecoration(
            DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL)
        )

        // Create zeroed list in the adapter if the size does not match
        val newData = (0..< _recData.recDist.size).map { idx ->
            DataPointsAdapter.DataPoint((DataShared.device.model.getMinCarriagePosition() + idx * POS_INCREMENTS), 0.0, 0.0)
        }
        _adapter.setDataList(newData)
    }

    private fun loadRecordedDataFromPrefs(projectile: ProjectileData?) {
        // Get recorded data from prefs
        val data = ProjectilePrefUtils.getProjectileRecData(requireContext(), projectile?.name)
        // Update units to match the current in settings
        _recData.heightUnit = DataShared.phoneHeight.unit
        _recData.recDistUnit = DataShared.targetDistance.unit
        // Set data
        if(null != data) {
            _recData.pitch = data.pitch
            _recData.height = ConvertLength.convert(data.heightUnit, _recData.heightUnit, data.height)
            _recData.recDist.forEachIndexed { idx, _ ->
                if(idx < data.recDist.size){
                    _recData.recDist[idx] = ConvertLength.convert(data.recDistUnit, _recData.recDistUnit, data.recDist[idx])
                }
                else {
                    _recData.recDist[idx] = 0.0
                }
            }
        }
        else {
            _recData.pitch = 0.0
            _recData.height = 0.0
            _recData.recDist.fill(0.0)
        }
        // Set the preferences when recorded data loaded
        try {
            settingsDeviceFragment.prefTestPitch.text = _recData.pitch.toString()
            settingsDeviceFragment.prefTestHeight.text = _recData.height.toString()
        }
        catch (e : Exception) {
            // Ignore...
        }

        // Set the recorded data in the adapter
        val dataPoints = _adapter.getData()
        dataPoints.forEachIndexed { idx, _ ->
            if(idx < _recData.recDist.size){
                _adapter.setRec(idx, _recData.recDist[idx], true)
            }
            else {
                _adapter.setRec(idx, 0.0, true)
            }
        }
    }

    private fun storeRecordedDataFromPrefs(projectile: ProjectileData?) {
        projectile?.let {
            ProjectilePrefUtils.setProjectileRecData(requireContext(), it.name, _recData)
        }
    }

    override fun onDestroyView() {
        _plotters.clear()

        // Store projectile recorded data
        storeRecordedDataFromPrefs(DataShared.device.model.projectile)

        // Set the projectile back to the selected
        val projectile = ProjectilePrefUtils.getProjectileSelected(requireContext())
        DataShared.device.model.projectile = projectile

        val fragMan: FragmentManager = parentFragmentManager
        val fragTransaction: FragmentTransaction = fragMan.beginTransaction()
        fragTransaction.remove(settingsDeviceFragment)
        fragTransaction.commit()

        _settingsDeviceFragment = null
        _fragmentDeviceBallisticsBinding = null
        super.onDestroyView()
    }

    companion object {
        const val POS_INCREMENTS = 5
    }
}