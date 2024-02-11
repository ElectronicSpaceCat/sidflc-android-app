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
    private val _settingKey: String,
    private val _listener: OnItemSelectedListener
) : DialogFragment() {
    private var mEntries: Array<String> = emptyArray()
    private lateinit var mValue: String
    private var mClickedDialogEntryIndex : Int = 0
    private lateinit var prefs: SharedPreferences

    interface OnItemSelectedListener {
        fun onItemSelectedListener(name: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Convert spring name list to array of strings
        val springs : Array<String> = Array(Spring.Name.values().size) {
            Spring.Name.values()[it].name
        }

        // Set the entries to the name list
        mEntries = springs

        // Get spring name stored in preferences
        mValue = prefs.getString(_settingKey, Spring.Name.MC9287K33.name).toString()

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

            prefs.edit().putString(_settingKey, mValue).apply()

            _listener.onItemSelectedListener(mValue)
        }
        dialog.dismiss()
    }
}