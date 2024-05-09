package com.android.app.fragments.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class ListDialogFragment(
    private val _listTitle: String,
    private val _selectedName: String,
    private val _list: Array<String>,
    private val _listener: OnItemSelectedListener?
) : DialogFragment() {
    private lateinit var mValue: String
    private var mClickedDialogEntryIndex : Int = 0

    interface OnItemSelectedListener {
        fun onItemSelectedListener(name: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mValue = _selectedName

        // Get position in list if the name exists in it
        mClickedDialogEntryIndex = valueIndex

        // Create the list dialog
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle(_listTitle)
        dialog.setPositiveButton(null, null)
        dialog.setSingleChoiceItems(_list, mClickedDialogEntryIndex, selectItemListener)

        return dialog.create()
    }

    private val valueIndex: Int
        get() = findIndexOfValue(mValue)

    private fun findIndexOfValue(value: String?): Int {
        if (value != null) {
            for (i in _list.indices.reversed()) {
                if (_list[i] == value) {
                    return i
                }
            }
        }
        return -1
    }

    private var selectItemListener = DialogInterface.OnClickListener { dialog, idx ->
        if (mClickedDialogEntryIndex != idx) {
            mClickedDialogEntryIndex = idx
            mValue = _list[mClickedDialogEntryIndex]

            _listener?.onItemSelectedListener(mValue)
        }
        dialog.dismiss()
    }
}