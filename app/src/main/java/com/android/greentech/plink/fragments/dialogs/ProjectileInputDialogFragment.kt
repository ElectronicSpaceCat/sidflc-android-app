package com.android.greentech.plink.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class ProjectileInputDialogFragment(
    private val _title: String,
    private val _Name: String,
    private val _Weight: Double,
    private val _Diameter: Double,
    private val _Drag: Double,
    private val _listener: ProjectileInputDialogListener
) : DialogFragment() {
    interface ProjectileInputDialogListener {
        fun onDialogPositiveClick(name: String, weight: Double, diameter: Double, drag: Double)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(_title)

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        /** Add a TextView for the projectile name */
        val projectileName = EditText(context)
        projectileName.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        projectileName.hint = "Name"
        projectileName.setText(_Name)
        projectileName.setSelection(projectileName.text.length)
        val projectileNameFilters = arrayOf<InputFilter>(
            InputFilter.LengthFilter(12)
        )
        projectileName.filters = projectileNameFilters
        projectileName.requestFocus()

        layout.addView(projectileName)

        /** Add a TextView for the projectile weight */
        val projectileWeight = EditText(context)
        projectileWeight.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        projectileWeight.hint = "Weight"
        projectileWeight.setText(_Weight.toString())
        projectileWeight.setSelection(projectileWeight.text.length)
        val projectileWeightFilters = arrayOf<InputFilter>(
            InputFilter.LengthFilter(7)
        )
        projectileWeight.filters = projectileWeightFilters
        layout.addView(projectileWeight)

        /** Add a TextView for the projectile diameter */
        val projectileDiameter = EditText(context)
        projectileDiameter.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        projectileDiameter.hint = "Diameter"
        projectileDiameter.setText(_Diameter.toString())
        projectileDiameter.setSelection(projectileDiameter.text.length)
        val projectileDiameterFilters = arrayOf<InputFilter>(
            InputFilter.LengthFilter(7)
        )
        projectileDiameter.filters = projectileDiameterFilters
        layout.addView(projectileDiameter)

        /** Add a TextView for the projectile drag */
        val projectileDrag = EditText(context)
        projectileDrag.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        projectileDrag.hint = "Drag"
        projectileDrag.setText(_Drag.toString())
        projectileDrag.setSelection(projectileDrag.text.length)
        val projectileDragFilters = arrayOf<InputFilter>(
            InputFilter.LengthFilter(7)
        )
        projectileDrag.filters = projectileDragFilters
        layout.addView(projectileDrag)

        builder.setView(layout)

        // Setup the positive button
        builder.setPositiveButton("Ok") { _, _ -> // Send the positive button event back to the host activity
            val name = projectileName.text.toString()
            val weight: Double = try {
                projectileWeight.text.toString().toDouble()
            } catch (e: NumberFormatException) {
                _Weight
            }
            val diameter: Double = try {
                projectileDiameter.text.toString().toDouble()
            } catch (e: NumberFormatException) {
                _Diameter
            }
            val drag: Double = try {
                projectileDrag.text.toString().toDouble()
            } catch (e: NumberFormatException) {
                _Drag
            }
            _listener.onDialogPositiveClick(name, weight, diameter, drag)
        }

        // Setup the negative button
        builder.setNegativeButton("Cancel") { dialog, _ -> // Send the negative button event back to the host activity
            dialog.dismiss()
        }

        val dialog = builder.create()

        // Show the input keyboard
        dialog.window!!.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)

        return dialog
    }
}
