package com.android.greentech.plink.device.projectile

object Projectile {
    enum class Name{
        Penny,
        Nickel,
        Dime,
        Quarter
    }

    private val projectiles : Map<String, ProjectileData> = mapOf(
        getName(Name.Penny) to ProjectileData(getName(Name.Penny),
            2.5,
            19.05,
            0.0),

        getName(Name.Nickel) to ProjectileData(getName(Name.Nickel),
            5.0,
            21.21,
            0.0),

        getName(Name.Dime) to ProjectileData(getName(Name.Dime),
            2.268,
            17.91,
            0.0),

        getName(Name.Quarter) to ProjectileData(getName(Name.Quarter),
            5.67,
            24.26,
            0.04)
    )

    /**
     * Get data for specified projectile
     *
     * @param name (projectile name)
     * @return projectileData
     */
    fun getData(name: Name): ProjectileData? {
        return try{
            projectiles.getValue(getName(name))
        } catch (e : Exception) {
            null
        }
    }

    /**
     * Get data for specified projectile
     *
     * @param name (projectile name string)
     * @return projectileData
     */
    fun getData(name: String): ProjectileData? {
        return try{
            val id = Name.valueOf(name)
            projectiles.getValue(getName(id))
        } catch (e : Exception) {
            null
        }
    }

    private fun getName(name : Name) : String{
        return name.name
    }
}
