package com.android.app.device.projectile

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.android.app.R
import com.android.app.utils.converters.ConvertLength
import com.android.app.utils.prefs.PrefUtils
import com.android.app.utils.misc.Utils
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ProjectilePrefUtils {

    private const val REC_DATA_PREF_TAG = "_rec_data"

    @Serializable
    class RecData(var pitch : Double, var heightUnit : ConvertLength.Unit, var height : Double,  var recDistUnit : ConvertLength.Unit, val recDist : Array<Double>)

    /**
     * Get the current list of projectiles from preferences
     *
     * @param context :Context
     * @return MutableList<ProjectileData>
     */
    fun getProjectileList(context: Context): MutableList<ProjectileData> {
        val list: MutableList<ProjectileData> = mutableListOf()
        val names: List<String>? = PrefUtils.getStringArrayFromPrefs(context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_NAMES), ";")
        val weights: List<String>? = PrefUtils.getStringArrayFromPrefs(context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_WEIGHTS),";")
        val diameters: List<String>? = PrefUtils.getStringArrayFromPrefs(context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DIAMETERS),";")
        val drags: List<String>? = PrefUtils.getStringArrayFromPrefs(context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DRAGS),";")
        if (names != null && weights != null && diameters != null && drags != null
            && names.size == weights.size && names.size == diameters.size && names.size == drags.size) {
            for (i in names.indices) {
                list.add(
                    ProjectileData(
                        names[i],
                        Utils.convertStrToDouble(weights[i]),
                        Utils.convertStrToDouble(diameters[i]),
                        Utils.convertStrToDouble(drags[i])),
                )
            }
        }
        return list
    }

    /**
     * Overwrite projectile list data in preferences
     *
     * @param context :Context
     * @param list :String (selected projected)
     */
    fun setProjectileList(context: Context, list: List<ProjectileData>){
        var names: MutableList<String> ?= mutableListOf()
        var weights: MutableList<String> ?= mutableListOf()
        var diameters: MutableList<String> ?= mutableListOf()
        var drags: MutableList<String> ?= mutableListOf()

        // Compare previous projectile list with new and remove recorded data if
        // the projectile does not exist
        getProjectileList(context).forEach { prev ->
            var shouldRemoveOld = true
            for(idx in 0..list.size) {
                if(list[idx].name == prev.name) {
                    shouldRemoveOld = false
                    break
                }
            }
            if(shouldRemoveOld) {
                PrefUtils.removeStringArrayFromPrefs(context, prev.name + REC_DATA_PREF_TAG)
            }
        }
        // Add projectile data
        if(list.isNotEmpty()) {
            list.forEach { data ->
                names?.add(data.name)
                weights?.add(data.weight.toString())
                diameters?.add(data.diameter.toString())
                drags?.add(data.drag.toString())
            }
        }
        else{
            names = null
            weights = null
            diameters = null
            drags = null
        }
        // Update projectile names/weights list in preferences
        PrefUtils.addStringArrayToPrefs(context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_NAMES), names, ";")
        PrefUtils.addStringArrayToPrefs(context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_WEIGHTS), weights, ";")
        PrefUtils.addStringArrayToPrefs(context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DIAMETERS), diameters, ";")
        PrefUtils.addStringArrayToPrefs(context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DRAGS), drags, ";")
    }

    /**
     * Checks if selected projectile is in the list of projectiles
     * and then stores the selected projectile to preferences.
     *
     * If name is not in list it will not update the selected projectile.
     *
     * @param context :Context
     * @param selected :String (selected projected)
     *
     * @return True if success, False no success
     */
    fun setProjectileSelected(context: Context, selected: String?){
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = prefs.edit()
        var name : String? = null
        // Get name list
        val names = PrefUtils.getStringArrayFromPrefs(
            context,
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_NAMES),
            ";"
        )
        if(null != names){
            // If the selected projectile exists in the list then select that instead
            if(names.contains(selected)){
                val idx = names.indexOf(selected)
                name = names[idx]
            }
        }
        // if name = null, it will remove the preference
        editor.putString(context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_SELECTED), name)
        editor.apply()
    }

    /**
     * Returns the selected projectile data, null if nothing found in the list
     *
     * @param context the context.
     * @return ProjectileData
     */
    fun getProjectileSelected(context: Context) : ProjectileData?{
        var projectileData: ProjectileData?= null
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val projectile = prefs.getString(context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_SELECTED), "")

        val names = PrefUtils.getStringArrayFromPrefs(
            context,
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_NAMES),
            ";"
        )
        val weights = PrefUtils.getStringArrayFromPrefs(
            context,
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_WEIGHTS),
            ";"
        )
        val diameters = PrefUtils.getStringArrayFromPrefs(
            context,
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DIAMETERS),
            ";"
        )
        val drags = PrefUtils.getStringArrayFromPrefs(
            context,
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DRAGS),
            ";"
        )
        if(null != names && null != weights && null != diameters && null != drags
            && (names.size == weights.size)
            && (names.size == diameters.size)
            && (names.size == drags.size)){
            val idx = names.indexOf(projectile)
            if(-1 != idx){
                projectileData =
                    ProjectileData(names[idx], Utils.convertStrToDouble(weights[idx]), Utils.convertStrToDouble(diameters[idx]), Utils.convertStrToDouble(drags[idx]))
            }
        }
        return projectileData
    }

    fun setProjectileRecData(context: Context, projectileSelected: String?, recData: RecData) {
        getProjectileList(context).forEach {
            if(it.name == projectileSelected) {
                val json = Json.encodeToString(recData)
                val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
                editor.putString(it.name + REC_DATA_PREF_TAG, json).apply()
            }
        }
    }

    fun getProjectileRecData(context: Context, projectileSelected: String?) : RecData? {
        return try{
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val str = prefs.getString(projectileSelected + REC_DATA_PREF_TAG, "")
            val data = Json.decodeFromString<RecData>(str!!)
            data
        }
        catch (e : Exception) {
            null
        }
    }

    fun setDefaultProjectiles(context: Context) : MutableList<ProjectileData> {
        // Create the default list
        val projectiles: MutableList<ProjectileData> = mutableListOf()
        Projectile.Name.entries.forEach {
            projectiles.add(Projectile.getData(it.name)!!)
        }
        // Set the projectile list
        setProjectileList(context, projectiles)
        // Set the selected projectile to the first element
        setProjectileSelected(context, Projectile.Name.Quarter.name)

        return projectiles
    }
}