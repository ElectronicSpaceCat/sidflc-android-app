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

package com.android.greentech.plink.fragments.device.deviceModel

import android.content.res.XmlResourceParser
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.greentech.plink.R
import com.android.greentech.plink.dataShared.DataShared
import com.android.greentech.plink.databinding.FragmentModelTunerBinding
import java.util.*
import kotlin.math.roundToInt

import com.android.greentech.plink.utils.misc.Utils
import org.xmlpull.v1.XmlPullParser
import java.io.IOException

class DeviceModelFragment : Fragment() {
    private var _deviceModelBinding: FragmentModelTunerBinding? = null
    private val deviceModelBinding get() = _deviceModelBinding!!

    private var _lastPosition = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _deviceModelBinding = FragmentModelTunerBinding.inflate(inflater, container, false)
        return deviceModelBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceModelBinding.modelCarriagePosMax.text = DataShared.device.model.getMaxCarriagePosition().roundToInt().toString()

        /**
         * Get the scale factor to get a close representation of
         * the the carrier position in relation to the physical model.
         */
        // Width of the actual drawable (height could also be used)
        val drawableWidthStr = parseXmlForWidthValue("res/drawable/ic_plink_v24_top_bottom.xml")
        val drawableWidthMm = Utils.convertStrToDouble(drawableWidthStr).toFloat()

        // Width in pixels of container that holds the model drawable
        val widthPx = requireContext().resources.getDimension(R.dimen.device_model_width)

        // Width in mm of container that holds the model drawable
        val widthMM = convertPxToMm(widthPx)
        // Scaling factor
        val scale = (widthMM / drawableWidthMm)

        /**
         * Observe the carriage position for updating graphics
         */
        DataShared.device.sensorCarriagePosition.rangeFiltered.observe(viewLifecycleOwner) { position ->
            if(_lastPosition != position){
                _lastPosition = position

                // Convert carrier position from mm to an offset amount in pixels
                val offsetPX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, position.toFloat(), resources.displayMetrics)
                // Translate the image of the carrier by the offset with the scale applied
                deviceModelBinding.plinkSlider.translationY = (-offsetPX * scale)

                val angle = DataShared.device.model.getSpringAngleAtPosition(position).toFloat()
                val angleAdj = (90f - DataShared.device.model.unloadedSpringAngle.toFloat()) + angle
                deviceModelBinding.springLeft.rotation = angleAdj
                deviceModelBinding.springRight.rotation = -angleAdj

                deviceModelBinding.modelCarriagePos.text = position.roundToInt().toString()
                deviceModelBinding.modelSpringAngle.text = String.format(
                    Locale.getDefault(),
                    "%.1f",
                    angle
                )
            }
        }

        /**
         * Set model name
         */
        deviceModelBinding.modelId.text = DataShared.device.model.name

        /**
         * Set spring name
         */
        deviceModelBinding.springId.text =
            if(DataShared.device.model.spring == null){
                "N/A"
            } else
            {
                DataShared.device.model.spring!!.name
            }
    }

    private fun parseXmlForWidthValue(xml : String) : String {
        var widthStr = "0.0"
        val parser : XmlResourceParser

        try{
            parser = resources.assets.openXmlResourceParser(xml)
        }
        catch (e : IOException) {
            return widthStr
        }

        // Work with the input stream
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    // Get the value from the attribute "android:viewportWidth"
                    widthStr = parser.getAttributeValue(2)
                    break
                }
            }
            event = parser.next()
        }

        return widthStr
    }

    private fun convertPxToMm(value : Float) : Float {
        return value / resources.displayMetrics.xdpi * 25.4f
    }

    override fun onDestroyView() {
        _deviceModelBinding = null
        super.onDestroyView()
    }
}