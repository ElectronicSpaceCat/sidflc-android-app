package com.android.app.fragments.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.android.app.device.springs.Spring

class SpringSelectDialogFragment(
    private val _listTitle: String,
    private val _selectedSpring: String,
    private val _listener: OnItemSelectedListener?
) : DialogFragment() {
    private var mEntries: Array<String> = emptyArray()
    private lateinit var mValue: String
    private var mClickedDialogEntryIndex : Int = 0

    interface OnItemSelectedListener {
        fun onItemSelectedListener(name: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Convert spring name list to array of strings
        val springs : Array<String> = Array(Spring.Name.entries.size) {
            Spring.Name.entries[it].name
        }

        // Set the entries to the name list
        mEntries = springs

        // Get spring name stored in preferences
        mValue = _selectedSpring

        // Get position in list if the name exists in it
        mClickedDialogEntryIndex = valueIndex

        // Create the list dialog
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle(_listTitle)
        dialog.setPositiveButton(null, null)
        dialog.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex, selectItemListener)

        return dialog.create()
    }

    private val valueIndex: Int
        get() = findIndexOfValue(mValue)

    private fun findIndexOfValue(value: String?): Int {
        if (value != null) {
            for (i in mEntries.indices.reversed()) {
                if (mEntries[i] == value) {
                    return i
                }
            }
        }
        return -1
    }

    private var selectItemListener = DialogInterface.OnClickListener { dialog, idx ->
        if (mClickedDialogEntryIndex != idx) {
            mClickedDialogEntryIndex = idx
            mValue = mEntries[mClickedDialogEntryIndex]

            _listener?.onItemSelectedListener(mValue)
        }
        dialog.dismiss()
    }
}