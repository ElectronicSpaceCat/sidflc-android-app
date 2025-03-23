package com.android.app.fragments.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.app.databinding.FragmentPrivacyBinding

class PrivacyFragment : Fragment() {
    private var _fragmentPrivacyBinding: FragmentPrivacyBinding? = null
    private val fragmentPrivacyBinding get() = _fragmentPrivacyBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentPrivacyBinding = FragmentPrivacyBinding.inflate(inflater, container, false)
        return fragmentPrivacyBinding.root
    }

    override fun onDestroyView() {
        _fragmentPrivacyBinding = null
        super.onDestroyView()
    }
}