package com.android.app.device.model

import com.android.app.device.springs.Spring

object Model {
    enum class Name{
        V23,
        V24
    }

    private val model : Map<String, ModelData> = mapOf(
        getName(Name.V23) to ModelData(
            getName(Name.V23),
            Spring.Name.MC9287K33,
            95.0,
            46.0,
            37.65,
            48.0,
            9.0,
            12.0,
            4.5,
            2.5,
            3.4875,
            95.822,
            9.98146),

        getName(Name.V24) to ModelData(
            getName(Name.V24),
            Spring.Name.MC9287K33,
            95.0,
            46.0,
            34.5,
            45.0,
            8.5,
            11.5,
            4.14,
            2.5,
            3.4875,
            95.822,
            9.98146)
    )

    /**
     * Get data for specified model
     *
     * @param name (model name)
     * @return modelData
     */
    fun getData(name: Name): ModelData? {
        return try{
            model.getValue(getName(name))
        } catch (e : Exception) {
            null
        }
    }

    /**
     * Get data for specified model
     *
     * @param name (model name string)
     * @return modelData
     */
    fun getData(name: String): ModelData? {
        return try{
            val id = Name.valueOf(name)
            model.getValue(getName(id))
        } catch (e : Exception) {
            null
        }
    }

    private fun getName(name : Name) : String{
        return name.name
    }
}
