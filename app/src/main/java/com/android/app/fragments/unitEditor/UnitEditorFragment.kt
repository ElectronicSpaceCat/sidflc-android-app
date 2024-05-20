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

package com.android.app.fragments.unitEditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.android.app.databinding.FragmentUnitEditorBinding
import com.android.app.utils.converters.ConvertLength
import android.R.layout.simple_spinner_dropdown_item
import com.android.app.dataShared.DataShared

class UnitEditorFragment : Fragment() {

    private var _fragmentUnitEditorBinding: FragmentUnitEditorBinding? = null
    private val fragmentUnitEditorBinding get() = _fragmentUnitEditorBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentUnitEditorBinding = FragmentUnitEditorBinding.inflate(inflater, container, false)
        return fragmentUnitEditorBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Unit selections - Lengths
        val unitLengthsDefault : MutableList<String> = mutableListOf()
        ConvertLength.Unit.entries.forEach {
            unitLengthsDefault.add(it.name.lowercase())
        }

        // Unit selections - Lengths Reduced
        val unitLengthsReduced : MutableList<String> = mutableListOf()
        unitLengthsReduced.add(ConvertLength.Unit.MM.name.lowercase())
        unitLengthsReduced.add(ConvertLength.Unit.CM.name.lowercase())
        unitLengthsReduced.add(ConvertLength.Unit.IN.name.lowercase())

        // Set up simple spinner adapter with selection list
        val adapterLengthsLong = ArrayAdapter(requireContext(), simple_spinner_dropdown_item, unitLengthsDefault)
        val adapterLengthsShort = ArrayAdapter(requireContext(), simple_spinner_dropdown_item, unitLengthsReduced)

        // Assign the adapter to the spinners - lengths
        var idx = unitLengthsReduced.indexOf(DataShared.lensOffset.unitStr())
        fragmentUnitEditorBinding.lensOffsetUnitSelector.adapter = adapterLengthsShort
        fragmentUnitEditorBinding.lensOffsetUnitSelector.setSelection(0.coerceAtLeast(idx))

        idx = unitLengthsReduced.indexOf(DataShared.deviceOffset.unitStr())
        fragmentUnitEditorBinding.deviceOffsetUnitSelector.adapter = adapterLengthsShort
        fragmentUnitEditorBinding.deviceOffsetUnitSelector.setSelection(0.coerceAtLeast(idx))

        idx = unitLengthsReduced.indexOf(DataShared.carriagePosition.unitStr())
        fragmentUnitEditorBinding.carriagePositionUnitSelector.adapter = adapterLengthsShort
        fragmentUnitEditorBinding.carriagePositionUnitSelector.setSelection(0.coerceAtLeast(idx))

        idx = unitLengthsDefault.indexOf(DataShared.phoneHeight.unitStr())
        fragmentUnitEditorBinding.deviceHeightUnitSelector.adapter = adapterLengthsLong
        fragmentUnitEditorBinding.deviceHeightUnitSelector.setSelection(0.coerceAtLeast(idx))

        idx = unitLengthsDefault.indexOf(DataShared.targetDistance.unitStr())
        fragmentUnitEditorBinding.targetDistanceUnitSelector.adapter = adapterLengthsLong
        fragmentUnitEditorBinding.targetDistanceUnitSelector.setSelection(0.coerceAtLeast(idx))

        idx = unitLengthsDefault.indexOf(DataShared.targetHeight.unitStr())
        fragmentUnitEditorBinding.targetHeightUnitSelector.adapter = adapterLengthsLong
        fragmentUnitEditorBinding.targetHeightUnitSelector.setSelection(0.coerceAtLeast(idx))

        // Setup listener for LensOffset
        fragmentUnitEditorBinding.lensOffsetUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsReduced[p2].uppercase())
                if(selectedUnit != DataShared.lensOffset.unit){
                    DataShared.lensOffset.setUnit(selectedUnit)
                    DataShared.lensOffset.storeToPrefs(requireContext())
                }
            }
        }
        // Setup listener for DeviceOffset
        fragmentUnitEditorBinding.deviceOffsetUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsReduced[p2].uppercase())
                if(selectedUnit != DataShared.deviceOffset.unit){
                    DataShared.deviceOffset.setUnit(selectedUnit)
                    DataShared.deviceOffset.storeToPrefs(requireContext())
                }
            }
        }
        // Setup listener for CarrierPosition
        fragmentUnitEditorBinding.carriagePositionUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsReduced[p2].uppercase())
                if(selectedUnit != DataShared.carriagePosition.unit){
                    DataShared.carriagePosition.setUnit(selectedUnit)
                    DataShared.carriagePosition.storeToPrefs(requireContext())
                }
            }
        }
        // Setup listener for DeviceHeight
        fragmentUnitEditorBinding.deviceHeightUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsDefault[p2].uppercase())
                if(selectedUnit != DataShared.phoneHeight.unit){
                    DataShared.phoneHeight.setUnit(selectedUnit)
                    DataShared.phoneHeight.storeToPrefs(requireContext())
                }
            }
        }
        // Setup listener for TargetDistance
        fragmentUnitEditorBinding.targetDistanceUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsDefault[p2].uppercase())
                if(selectedUnit != DataShared.targetDistance.unit){
                    DataShared.targetDistance.setUnit(selectedUnit)
                    DataShared.targetDistance.storeToPrefs(requireContext())
                }
            }
        }
        // Setup listener for TargetHeight
        fragmentUnitEditorBinding.targetHeightUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsDefault[p2].uppercase())
                if(selectedUnit != DataShared.targetHeight.unit){
                    DataShared.targetHeight.setUnit(selectedUnit)
                    DataShared.targetHeight.storeToPrefs(requireContext())
                }
            }
        }
    }

    override fun onDestroyView() {
        _fragmentUnitEditorBinding = null
        super.onDestroyView()
    }
}
