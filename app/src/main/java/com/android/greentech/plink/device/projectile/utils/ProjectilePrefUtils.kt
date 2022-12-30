package com.android.greentech.plink.device.projectile.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.android.greentech.plink.R
import com.android.greentech.plink.device.projectile.Projectile
import com.android.greentech.plink.device.projectile.ProjectileData
import com.android.greentech.plink.utils.prefs.PrefUtils
import com.android.greentech.plink.utils.misc.Utils

object ProjectilePrefUtils {
    /**
     * Get the current list of projectiles from preferences
     *
     * @param context :Context
     * @return MutableList<ProjectileData>
     */
    fun getProjectileListFromPrefs(context: Context): MutableList<ProjectileData> {
        val list: MutableList<ProjectileData> = mutableListOf()
        val setNames: List<String>? = PrefUtils.getStringArrayFromPrefs(
            context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_NAMES), ";")
        val setWeights: List<String>? = PrefUtils.getStringArrayFromPrefs(
            context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_WEIGHTS),";")
        val setDiameters: List<String>? = PrefUtils.getStringArrayFromPrefs(
            context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DIAMETERS),";")
        val setDrags: List<String>? = PrefUtils.getStringArrayFromPrefs(
            context, context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DRAGS),";")
        if (setNames != null && setWeights != null && setDiameters != null && setDrags != null
            && setNames.size == setWeights.size && setNames.size == setDiameters.size && setNames.size == setDrags.size) {
            for (i in setNames.indices) {
                list.add(
                    ProjectileData(
                        setNames[i],
                        Utils.convertStrToDouble(setWeights[i]),
                        Utils.convertStrToDouble(setDiameters[i]),
                        Utils.convertStrToDouble(setDrags[i]))
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
    fun setProjectileListPref(context: Context, list: List<ProjectileData>){
        var names: MutableList<String> ?= mutableListOf()
        var weights: MutableList<String> ?= mutableListOf()
        var diameters: MutableList<String> ?= mutableListOf()
        var drags: MutableList<String> ?= mutableListOf()

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
    fun setProjectileSelectedPref(context: Context, selected: String?){
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
    fun getProjectileSelectedData(context: Context) : ProjectileData?{
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
                    ProjectileData(names[idx], Utils.convertStrToDouble(weights[idx]), Utils.convertStrToDouble(diameters[idx]))
            }
        }
        return projectileData
    }

    fun setDefaultProjectilesPref(context: Context) : MutableList<ProjectileData> {
        // Init data lists
        val names: MutableList<String> = mutableListOf()
        val weights: MutableList<String> = mutableListOf()
        val diameters: MutableList<String> = mutableListOf()
        val drags: MutableList<String> = mutableListOf()

        // Create the default list
        val projectiles: MutableList<ProjectileData> = mutableListOf()
        projectiles.add(Projectile.getData(Projectile.Name.Penny)!!)
        projectiles.add(Projectile.getData(Projectile.Name.Nickel)!!)
        projectiles.add(Projectile.getData(Projectile.Name.Dime)!!)
        projectiles.add(Projectile.getData(Projectile.Name.Quarter)!!)

        // Build the string arrays
        projectiles.forEach { projectile ->
            names.add(projectile.name)
            weights.add(projectile.weight.toString())
            diameters.add(projectile.diameter.toString())
            drags.add(projectile.drag.toString())
        }

        // Override the names list in preferences
        PrefUtils.addStringArrayToPrefs(
            context,
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_NAMES),
            names,
            ";"
        )
        // Override the weights list in preferences
        PrefUtils.addStringArrayToPrefs(
            context,
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_WEIGHTS),
            weights,
            ";"
        )
        // Override the diameters list in preferences
        PrefUtils.addStringArrayToPrefs(
            context,
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DIAMETERS),
            diameters,
            ";"
        )
        // Override the diameters list in preferences
        PrefUtils.addStringArrayToPrefs(
            context,
            context.getString(R.string.PREFERENCE_FILTER_PROJECTILE_DRAGS),
            drags,
            ";"
        )

        // Set the selected projectile to the first element
        setProjectileSelectedPref(context, Projectile.Name.Quarter.name)

        return projectiles
    }
}