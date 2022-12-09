package com.android.greentech.plink.device.springs

import com.android.greentech.plink.device.springs.material.Material

object Spring {
    enum class Name{
        MC9287K33,
        MC9287K141,
        MC9287K83,
        MC9287K155
    }

    private val springs : Map<String, SpringData> = mapOf(
        getName(Name.MC9287K33) to SpringData(getName(Name.MC9287K33),    1.016,  7.8486, 3.25, 31.75,31.75, Material.Name.STAINLESS_302_304),
        getName(Name.MC9287K141) to SpringData(getName(Name.MC9287K141),  0.045, 0.357, 3.25, 1.25,1.25, Material.Name.STAINLESS_302_304),
        getName(Name.MC9287K83) to SpringData(getName(Name.MC9287K83),    0.048, 0.375, 3.25, 1.25,1.25, Material.Name.STAINLESS_302_304),
        getName(Name.MC9287K155) to SpringData(getName(Name.MC9287K155),  0.051, 0.408, 2.25, 2.0, 2.0,  Material.Name.STAINLESS_302_304)
    )

    /**
     * Get data for specified spring
     *
     * @param name (spring name)
     * @return springData
     */
    fun getData(name: Name): SpringData? {
        return try{
            springs.getValue(getName(name))
        } catch (e : Exception) {
            null
        }
    }

    /**
     * Get data for specified spring
     *
     * @param name (spring name string)
     * @return springData
     */
    fun getData(name: String): SpringData? {
        return try{
            val id = Name.valueOf(name)
            springs.getValue(getName(id))
        } catch (e : Exception) {
            null
        }
    }

    private fun getName(name : Name) : String {
        return name.name
    }
}
