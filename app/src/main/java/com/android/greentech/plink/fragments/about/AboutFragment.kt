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

package com.android.greentech.plink.fragments.about

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.greentech.plink.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
    private var _fragmentAboutBinding: FragmentAboutBinding? = null
    private val fragmentAboutBinding get() = _fragmentAboutBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentAboutBinding = FragmentAboutBinding.inflate(inflater, container, false)
        return fragmentAboutBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val manager: PackageManager = requireActivity().packageManager
        val info: PackageInfo = manager.getPackageInfo(requireActivity().packageName, PackageManager.GET_ACTIVITIES)

        fragmentAboutBinding.appVersion.text = info.versionName
    }

    override fun onDestroyView() {
        _fragmentAboutBinding = null
        super.onDestroyView()
    }
}