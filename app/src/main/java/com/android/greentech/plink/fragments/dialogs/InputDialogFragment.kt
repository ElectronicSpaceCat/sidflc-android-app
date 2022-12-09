package com.android.greentech.plink.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.widget.EditText
import android.widget.Toast
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.lang.NumberFormatException
import java.util.*
import kotlin.math.roundToInt

class InputDialogFragment(
    private val _title: String,
    private val _typeFlags : Int,
    private val _defaultValue: Number,
    private val _maxDigits: Int,
    private val _maxPrecision : Int,
    private val _listener: InputDialogListener
) : DialogFragment() {

    interface InputDialogListener {
        fun onDialogPositiveClick(value: Number)
        fun onDialogNegativeClick(value: Number)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle(_title)
        // Set an EditText view to get user input
        val input = EditText(context)
        // Set flags
        input.inputType = _typeFlags
        // Bring focus to the prompt
        input.requestFocus()
        // Set the default value shown
        val locVal = _defaultValue.toDouble()
        if(_maxPrecision > 0) {
            input.setText(String.format(Locale.getDefault(), "%.${_maxPrecision}f", locVal))
        }
        else{
            input.setText(locVal.roundToInt().toString())
        }

        // Set cursor to last character
        input.setSelection(input.text.length)
        // Apply filters
        val filters = arrayOf<InputFilter>(
            InputFilter.LengthFilter(_maxDigits)
        )
        input.filters = filters
        // Show hint when data in prompt is cleared
        input.hint = "Value"

        // Set view
        builder.setView(input)
        // Setup the positive button
        builder.setPositiveButton("Ok") { _, _ -> // Send the positive button event back to the host activity
            val text = input.text.toString()
            try {
                val num = text.toDouble()
                _listener.onDialogPositiveClick(num)
            } catch (e: NumberFormatException) {
                Toast.makeText(
                    context,
                    "Enter a valid number" as CharSequence,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        // Setup the negative button
        builder.setNegativeButton("Cancel") { dialog, _ -> // Send the negative button event back to the host activity
            _listener.onDialogNegativeClick(_defaultValue)
            dialog.dismiss()
        }

        // Show the input keyboard
        val dialog = builder.create()
        dialog.window!!.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)

        return dialog
    }
}