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

package com.android.greentech.plink.fragments.unitEditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.android.greentech.plink.databinding.FragmentUnitEditorBinding
import com.android.greentech.plink.utils.converters.ConvertLength
import android.R.layout.simple_spinner_dropdown_item
import com.android.greentech.plink.dataShared.DataShared

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
        val unitLengthsLong : MutableList<String> = mutableListOf()
        ConvertLength.Unit.values().forEach {
            unitLengthsLong.add(it.name.lowercase())
        }

        // Unit selections - Lengths Reduced
        val unitLengthsShort : MutableList<String> = mutableListOf()
        unitLengthsShort.add(ConvertLength.Unit.MM.name.lowercase())
        unitLengthsShort.add(ConvertLength.Unit.CM.name.lowercase())
        unitLengthsShort.add(ConvertLength.Unit.IN.name.lowercase())

        // Set up simple spinner adapter with selection list
        val adapterLengthsLong = ArrayAdapter(requireContext(), simple_spinner_dropdown_item, unitLengthsLong)
        val adapterLengthsShort = ArrayAdapter(requireContext(), simple_spinner_dropdown_item, unitLengthsShort)

        // Assign the adapter to the spinners - lengths
        var idx = unitLengthsShort.indexOf(DataShared.lensOffset.unitStr())
        fragmentUnitEditorBinding.lensOffsetUnitSelector.adapter = adapterLengthsShort
        fragmentUnitEditorBinding.lensOffsetUnitSelector.setSelection(0.coerceAtLeast(idx))

        idx = unitLengthsShort.indexOf(DataShared.carriagePosition.unitStr())
        fragmentUnitEditorBinding.carriagePositionUnitSelector.adapter = adapterLengthsShort
        fragmentUnitEditorBinding.carriagePositionUnitSelector.setSelection(0.coerceAtLeast(idx))

        idx = unitLengthsLong.indexOf(DataShared.deviceHeight.unitStr())
        fragmentUnitEditorBinding.deviceHeightUnitSelector.adapter = adapterLengthsLong
        fragmentUnitEditorBinding.deviceHeightUnitSelector.setSelection(0.coerceAtLeast(idx))

        idx = unitLengthsLong.indexOf(DataShared.targetDistance.unitStr())
        fragmentUnitEditorBinding.targetDistanceUnitSelector.adapter = adapterLengthsLong
        fragmentUnitEditorBinding.targetDistanceUnitSelector.setSelection(0.coerceAtLeast(idx))

        idx = unitLengthsLong.indexOf(DataShared.targetHeight.unitStr())
        fragmentUnitEditorBinding.targetHeightUnitSelector.adapter = adapterLengthsLong
        fragmentUnitEditorBinding.targetHeightUnitSelector.setSelection(0.coerceAtLeast(idx))

        // Setup listener for LensOffset
        fragmentUnitEditorBinding.lensOffsetUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsShort[p2].uppercase())
                if(selectedUnit != DataShared.lensOffset.unit){
                    val valuePrev = DataShared.lensOffset.getValueFromPrefs(requireContext())
                    val valueNew = ConvertLength.convert(DataShared.lensOffset.unit, selectedUnit, valuePrev)
                    DataShared.lensOffset.storeValueToPrefs(requireContext(), valueNew)
                    DataShared.lensOffset.storeUnitToPrefs(requireContext(), selectedUnit)
                    DataShared.lensOffset.setUnit(selectedUnit)
                }
            }
        }
        // Setup listener for CarrierPosition
        fragmentUnitEditorBinding.carriagePositionUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsShort[p2].uppercase())
                if(selectedUnit != DataShared.carriagePosition.unit){
                    val valuePrev = DataShared.carriagePosition.getValueFromPrefs(requireContext())
                    val valueNew = ConvertLength.convert(DataShared.carriagePosition.unit, selectedUnit, valuePrev)
                    DataShared.carriagePosition.storeValueToPrefs(requireContext(), valueNew)
                    DataShared.carriagePosition.storeUnitToPrefs(requireContext(), selectedUnit)
                    DataShared.carriagePosition.setUnit(selectedUnit)
                }
            }
        }
        // Setup listener for DeviceHeight
        fragmentUnitEditorBinding.deviceHeightUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsLong[p2].uppercase())
                if(selectedUnit != DataShared.deviceHeight.unit){
                    val valuePrev = DataShared.deviceHeight.getValueFromPrefs(requireContext())
                    val valueNew = ConvertLength.convert(DataShared.deviceHeight.unit, selectedUnit, valuePrev)
                    DataShared.deviceHeight.storeValueToPrefs(requireContext(), valueNew)
                    DataShared.deviceHeight.storeUnitToPrefs(requireContext(), selectedUnit)
                    DataShared.deviceHeight.setUnit(selectedUnit)
                }
            }
        }
        // Setup listener for TargetDistance
        fragmentUnitEditorBinding.targetDistanceUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsLong[p2].uppercase())
                if(selectedUnit != DataShared.targetDistance.unit){
                    val valuePrev = DataShared.targetDistance.getValueFromPrefs(requireContext())
                    val valueNew = ConvertLength.convert(DataShared.targetDistance.unit, selectedUnit, valuePrev)
                    DataShared.targetDistance.storeValueToPrefs(requireContext(), valueNew)
                    DataShared.targetDistance.storeUnitToPrefs(requireContext(), selectedUnit)
                    DataShared.targetDistance.setUnit(selectedUnit)
                }
            }
        }
        // Setup listener for TargetHeight
        fragmentUnitEditorBinding.targetHeightUnitSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedUnit = ConvertLength.Unit.valueOf(unitLengthsLong[p2].uppercase())
                if(selectedUnit != DataShared.targetHeight.unit){
                    val valuePrev = DataShared.targetHeight.getValueFromPrefs(requireContext())
                    val valueNew = ConvertLength.convert(DataShared.targetHeight.unit, selectedUnit, valuePrev)
                    DataShared.targetHeight.storeValueToPrefs(requireContext(), valueNew)
                    DataShared.targetHeight.storeUnitToPrefs(requireContext(), selectedUnit)
                    DataShared.targetHeight.setUnit(selectedUnit)
                }
            }
        }
    }

    override fun onDestroyView() {
        _fragmentUnitEditorBinding = null
        super.onDestroyView()
    }
}
