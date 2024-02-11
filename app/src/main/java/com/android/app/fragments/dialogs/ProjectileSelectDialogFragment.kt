package com.android.app.fragments.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.android.app.utils.prefs.PrefUtils

class ProjectileSelectDialogFragment(
    private val _listTitle: String,
    private val _settingKey: String,
) : DialogFragment() {
    private var mEntries: Array<String> = emptyArray()
    private lateinit var mValue: String
    private var mClickedDialogEntryIndex : Int = 0
    private lateinit var prefs: SharedPreferences

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val data = PrefUtils.getStringArrayFromPrefs(requireContext(), requireContext().getString(
            com.android.app.R.string.PREFERENCE_FILTER_PROJECTILE_NAMES), ";")
        if(!data.isNullOrEmpty()){
            mEntries = data.toTypedArray()
        }
        mValue = prefs.getString(_settingKey, "").toString()

        mClickedDialogEntryIndex = valueIndex

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
        }
        dialog.dismiss()
    }
}