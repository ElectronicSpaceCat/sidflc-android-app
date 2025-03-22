package com.android.app.utils.prefs

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.preference.PreferenceManager
import androidx.core.content.edit

object PrefUtils {
    /**
     * Converts a MutableList<String> to a delimited string sequence
     * and then stores the string to preferences.
     * Passing 'array' as null removes data from the list.
     *
     * @param context :Context
     * @param key :String (Preference key)
     * @param array :MutableList<String>?
     * @param delimiter :String
     */
    fun addStringArrayToPrefs(context: Context, key: String, array: List<String>?, delimiter: String){
        var newStr: String? = null
        if(array != null){
            newStr = TextUtils.join(delimiter, array)
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            putString(key, newStr)
        }
    }

    /**
     * Gets a delimited string sequence from preferences and returns it as a List<String>
     *
     * @param context :Context
     * @param key :String (Preference key)
     * @param delimiter :String
     */
    fun getStringArrayFromPrefs(context: Context, key : String, delimiter: String) : List<String>?{
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val str = prefs.getString(key, null)
        if(str != null){
            return str.split(delimiter)
        }
        return null
    }

    /**
     * Remove a delimited string sequence from preferences
     *
     * @param context :Context
     * @param key :String (Preference key)
     */
    fun removeStringArrayFromPrefs(context: Context, key : String){
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit { remove(key) }
    }
}
