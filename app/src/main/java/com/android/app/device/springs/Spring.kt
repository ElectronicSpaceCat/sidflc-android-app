package com.android.app.device.springs

import com.android.app.device.springs.material.Material

object Spring {
    enum class Name{
        MC9287K33,
        MC9287K141,
        MC9287K83,
        MC9287K155
    }

    /**
     * Map of default springs.
     * NOTE: Make sure entered data is in (mm)
     */
    private val springs : Map<String, SpringData> = mapOf(
        getName(Name.MC9287K33) to SpringData(
            getName(Name.MC9287K33),
            1.016,
            7.8486,
            3.25,
            31.75,
            31.75,
            Material.Name.STAINLESS_302_304),

        getName(Name.MC9287K141) to SpringData(
            getName(Name.MC9287K141),
            1.143,
            9.0678,
            3.25,
            31.75,
            31.75,
            Material.Name.STAINLESS_302_304),

        getName(Name.MC9287K83) to SpringData(
            getName(Name.MC9287K83),
            1.2192,
            9.525,
            3.25,
            31.75,
            31.75,
            Material.Name.STAINLESS_302_304),

        getName(Name.MC9287K155) to SpringData(
            getName(Name.MC9287K155),
            1.2954,
            10.3632,
            2.25,
            50.8,
            50.8,
            Material.Name.STAINLESS_302_304)
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
        return try {
            val id = Name.valueOf(name)
            springs.getValue(getName(id))
        } catch (e : Exception) {
            null
        }
    }

    /**
     * Get data for specified spring
     *
     * @param id (spring id)
     * @return springData
     */
    fun getData(id: Int): SpringData? {
        return if(id < Name.entries.size) {
            springs.getValue(Name.entries[id].name)
        }
        else {
            null
        }
    }

    private fun getName(name : Name) : String {
        return name.name
    }
}
