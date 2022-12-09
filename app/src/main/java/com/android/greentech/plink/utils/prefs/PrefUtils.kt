package com.android.greentech.plink.utils.prefs

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.preference.PreferenceManager

object PrefUtils {
    /**
     * Converts a MutableList<String> to a delimited string sequence
     * and then stores the string to preferences.
     *
     * @param context :Context
     * @param key :String (Preference key)
     * @param array :MutableList<String>
     * @param delimiter :String
     */
    fun addStringArrayToPrefs(context: Context, key: String, array: MutableList<String>?, delimiter: String){
        var newStr: String? = null
        if(array != null){
            newStr = TextUtils.join(delimiter, array)
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putString(key, newStr)
        editor.apply()
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
        prefs.edit().remove(key).apply()
    }
}
