package com.android.app.fragments.about

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.app.databinding.FragmentAboutBinding

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