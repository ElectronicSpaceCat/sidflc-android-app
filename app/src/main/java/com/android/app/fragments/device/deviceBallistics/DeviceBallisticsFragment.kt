/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.app.fragments.device.deviceBallistics

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceBallisticsBinding
import com.android.app.fragments.device.deviceBallistics.dataPointsAdapter.DataPointsAdapter
import com.android.app.fragments.settings.SettingsDeviceFragment
import com.android.app.utils.converters.ConvertLength
import com.android.app.utils.misc.Utils
import com.android.app.utils.plotter.CanvasPlotter
import com.android.app.utils.plotter.DataPoint
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import kotlin.math.roundToInt

class DeviceBallisticsFragment : Fragment() {
    private var _fragmentDeviceBallisticsBinding: FragmentDeviceBallisticsBinding? = null
    private val fragmentDeviceBallisticsBinding get() = _fragmentDeviceBallisticsBinding!!

    private var _settingsDeviceFragment : SettingsDeviceFragment ?= null
    private val settingsDeviceFragment get() = _settingsDeviceFragment!!

    private lateinit var _prefsListener : SharedPreferences.OnSharedPreferenceChangeListener

    private val _numDataPoints = ((DataShared.device.model.getMaxCarriagePosition().toInt() - POS_START) / POS_INCREMENTS)

    private var _testHeight = 0.0
    private var _testPitch = 0.0

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

        initPrefsListener(requireContext())

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
        _adapter.onRecDataChanged.observe(viewLifecycleOwner) {
            plotRecordedImpactData()
        }

        /**
         * Observe forceOffsetOnChange
         */
        DataShared.device.model.ballistics.forceOffsetOnChange.observe(viewLifecycleOwner) {
            plotCalculatedImpactData()
        }

        /**
         * Observe efficiencyOnChange
         */
        DataShared.device.model.ballistics.efficiencyOnChange.observe(viewLifecycleOwner) {
            plotCalculatedImpactData()
        }

        /**
         * Observe frictionCoefficientOnChange
         */
        DataShared.device.model.ballistics.frictionCoefficientOnChange.observe(viewLifecycleOwner) {
            plotCalculatedImpactData()
        }

        /**
         * Observe springOnChange
         */
        DataShared.device.model.springOnChange.observe(viewLifecycleOwner) {
            plotCalculatedImpactData()
        }

        /**
         * Observe projectileOnChange
         */
        DataShared.device.model.projectileOnChange.observe(viewLifecycleOwner) {
            plotCalculatedImpactData()
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

        setDefaultPlotterBounds()
        plotRecordedImpactData()
        plotCalculatedImpactData()
    }

    /**
     * Plots the calculated impact data using the carriage position range from 10mm to MaxCarriagePosition
     */
    private fun plotCalculatedImpactData() {
        val lensOffset = DataShared.lensOffset.getConverted(ConvertLength.Unit.MM)

        // Create the data set to plot
        var rowId = 0
        val data = mutableListOf<DataPoint>()
        for(idx in POS_START.. DataShared.device.model.getMaxCarriagePosition().toInt()) {
            val impactDistance = DataShared.device.model.ballistics.calcImpactData(
                idx.toDouble(),
                lensOffset,
                _testHeight,
                _testPitch,
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

        // Reset if size does not match
        if(dataPoints.lastIndex != _numDataPoints){
            val newData = (0.._numDataPoints).map { idx ->
                DataPointsAdapter.DataPoint((POS_START + idx * POS_INCREMENTS).toDouble(), 0.0, 0.0)
            }
            _adapter.setDataList(newData)
        }
        // Get stored data
        else {
            dataPoints.forEach {
                data.add(DataPoint(it.pos.toFloat(), it.rec.toFloat()))
            }
        }

        _plotters["rec"]?.setData(data)
        _plotters["rec"]?.draw()
    }

    private fun setDefaultPlotterBounds() {
        _plotBounds.xMin = POS_START.toFloat()
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
    }

    /**
     * Create listeners
     */
    private fun initPrefsListener(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.all.keys.forEach{ key ->
            preferencesHandler(context, prefs, key)
        }

        /**
         * Listen for preference changes
         */
        _prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { pref: SharedPreferences?, key: String? ->
            if (pref != null && key != null) {
                preferencesHandler(context, pref, key)
            }
        }
    }

    /**
     * Handler for preferences, if any are modified then notify a recalculation on the graphs.
     */
    private fun preferencesHandler(context: Context, pref: SharedPreferences, key: String) {
        when(key){
            context.getString(R.string.PREFERENCE_FILTER_TEST_HEIGHT) -> {
                _testHeight = Utils.convertStrToDouble(pref.getString(key, "0.0"))
                plotCalculatedImpactData()
            }
            context.getString(R.string.PREFERENCE_FILTER_TEST_PITCH) -> {
                _testPitch = Utils.convertStrToDouble(pref.getString(key, "0.0"))
                plotCalculatedImpactData()
            }
        }
    }

    override fun onResume() {
        PreferenceManager.getDefaultSharedPreferences(requireContext()).registerOnSharedPreferenceChangeListener(_prefsListener)
        super.onResume()
    }

    override fun onPause() {
        PreferenceManager.getDefaultSharedPreferences(requireContext()).unregisterOnSharedPreferenceChangeListener(_prefsListener)
        super.onPause()
    }

    override fun onDestroyView() {
        _plotters.clear()
        _adapter.storeDataToPrefs()

        val fragMan: FragmentManager = parentFragmentManager
        val fragTransaction: FragmentTransaction = fragMan.beginTransaction()
        fragTransaction.remove(settingsDeviceFragment)
        fragTransaction.commit()

        _settingsDeviceFragment = null
        _fragmentDeviceBallisticsBinding = null
        super.onDestroyView()
    }

    companion object {
        const val POS_START = 10
        const val POS_INCREMENTS = 5
    }
}