package com.android.app.fragments.gyroCal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.android.app.R
import com.android.app.databinding.FragmentGyroCalBinding
import java.math.RoundingMode
import java.util.*
import androidx.navigation.findNavController

class GyroCalFragment : Fragment() {
    private var _fragmentGyroCalBinding: FragmentGyroCalBinding? = null
    private val fragmentGyroCalBinding get() = _fragmentGyroCalBinding!!

    private lateinit var viewModel: GyroCalViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGyroCalBinding = FragmentGyroCalBinding.inflate(inflater, container, false)
        return fragmentGyroCalBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[GyroCalViewModel::class.java]

        updateAppliedOffsetTextValues()

        // Set the Calibrate button onClick listener
        fragmentGyroCalBinding.btnCalGyro.setOnClickListener {
            viewModel.gyro.setPitchOffset()
            viewModel.gyro.setRollOffset()
            updateAppliedOffsetTextValues()
        }

        // Set the Reset button onClick listener
        fragmentGyroCalBinding.btnCalGyroReset.setOnClickListener {
            viewModel.gyro.resetRollOffset()
            viewModel.gyro.resetPitchOffset()
            updateAppliedOffsetTextValues()
        }

        // Set the Ok button onClick listener
        fragmentGyroCalBinding.btnCalGyroOk.setOnClickListener {
            requireActivity().findNavController(R.id.container_nav).popBackStack()
        }

        // Observe the roll value
        viewModel.gyro.rollOnChange.observe(viewLifecycleOwner) {
            fragmentGyroCalBinding.calRollValue.text = String.format(
                Locale.getDefault(),
                "%.1f",
                it.toBigDecimal().setScale(1, RoundingMode.HALF_UP)
            )
        }

        // Observe the pitch value
        viewModel.gyro.pitchOnChange.observe(viewLifecycleOwner) {
            fragmentGyroCalBinding.calPitchValue.text = String.format(
                Locale.getDefault(),
                "%.1f",
                it.toBigDecimal().setScale(1, RoundingMode.HALF_UP)
            )
        }
    }

    private fun updateAppliedOffsetTextValues(){
        fragmentGyroCalBinding.calRollAppliedOffsetValue.text = String.format(
            Locale.getDefault(),
            "%.1f",
            viewModel.gyro.rollOffset
        )

        fragmentGyroCalBinding.calPitchAppliedOffsetValue.text = String.format(
            Locale.getDefault(),
            "%.1f",
            viewModel.gyro.pitchOffset
        )
    }

    override fun onResume() {
        viewModel.onActive(requireContext())
        super.onResume()
    }

    override fun onPause() {
        viewModel.onInactive()
        super.onPause()
    }

    override fun onDestroyView() {
        viewModel.onDestroy(requireContext())
        _fragmentGyroCalBinding = null
        super.onDestroyView()
    }
}