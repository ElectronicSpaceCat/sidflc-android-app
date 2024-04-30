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

package com.android.app.fragments.device.deviceModel

import android.content.res.XmlResourceParser
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.app.dataShared.DataShared
import com.android.app.databinding.FragmentDeviceModelBinding
import com.android.app.utils.converters.ConvertDispUnits
import com.android.app.utils.misc.Utils
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.util.*
import kotlin.math.roundToInt

class DeviceModelFragment : Fragment() {
    private var _deviceModelBinding: FragmentDeviceModelBinding? = null
    private val deviceModelBinding get() = _deviceModelBinding!!

    private var _lastPosition = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _deviceModelBinding = FragmentDeviceModelBinding.inflate(inflater, container, false)
        return deviceModelBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceModelBinding.modelCarriagePosMax.text = DataShared.device.model.getMaxCarriagePosition().roundToInt().toString()

        /**
         * Get the scale factor to get a close representation of
         * the the carriage position in relation to the physical model.
         */
        // Width of the actual drawable
        val drawableWidthMmStr = parseXmlForWidthValue(MODEL_DRAWABLE, MODEL_DRAWABLE_WIDTH_XML_ATTR)
        val drawableWidthMm = Utils.convertStrToDouble(drawableWidthMmStr).toFloat()

        // Width in Dp of container that holds the model drawable
        val widthDpStr = parseXmlForWidthValue(MODEL_LAYOUT, MODEL_LAYOUT_WIDTH_XML_ATTR)
        val widthDp = Utils.convertStrToDouble(widthDpStr).toFloat()

        // Convert to px
        val widthPx = ConvertDispUnits.dpToPx(requireContext(), widthDp)

        // Width in mm of container that holds the model drawable
        val widthMm = ConvertDispUnits.pxToMm(requireContext(), widthPx)
        // Scaling factor obtained by getting the ratio of the drawable container size to the drawable size
        val scale = (widthMm / drawableWidthMm)

        /**
         * Observe the carriage position for updating graphics
         */
        DataShared.device.sensorCarriagePosition.rangeFiltered.observe(viewLifecycleOwner) { position ->
            if(_lastPosition != position){
                _lastPosition = position

                // Convert carriage position from mm to an offset amount in pixels
                val offsetPX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, position.toFloat(), resources.displayMetrics)
                // Translate the image of the carriage by the offset with the scale applied
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

    private fun parseXmlForWidthValue(xml : String, attr : String) : String? {
        var str : String ?= "0.0"
        val parser : XmlResourceParser

        try{
            parser = resources.assets.openXmlResourceParser(xml)
        }
        catch (e : IOException) {
            return str
        }

        // Work with the input stream
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    // Get the value from the attribute
                    str = parser.getAttributeValue("http://schemas.android.com/apk/res/android", attr)
                    if (str != null && str.length > 1 && str[0] == '@') {
                        val id: Int = str.substring(1).toInt()
                        str = getString(id).removeSuffix("dip") // 'dip' is correct, not 'dp'
                    }
                    break
                }
            }
            event = parser.next()
        }

        return str
    }

    override fun onDestroyView() {
        _deviceModelBinding = null
        super.onDestroyView()
    }

    companion object{
        private const val MODEL_DRAWABLE = "res/drawable/ic_plink_v24_top_bottom.xml"
        private const val MODEL_DRAWABLE_WIDTH_XML_ATTR = "viewportWidth"

        private const val MODEL_LAYOUT = "res/layout/fragment_device_model.xml"
        private const val MODEL_LAYOUT_WIDTH_XML_ATTR = "layout_width"
    }
}