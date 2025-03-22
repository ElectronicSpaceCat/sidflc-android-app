package com.android.app.device.springs.material

object Material {
    enum class Name{
        MUSIC_WIRE,
        HARD_DRAWN_MB,
        OIL_TEMP_MB,
        OIL_TEMP_CHROME_SILICON,
        OIL_TEMP_CHROME_VANADIUM,
        STAINLESS_302_304,
        STAINLESS_316,
        STAINLESS_17_7_PH,
        PHOSPHOR_BRONZE,
        BERYLLIUM_COPPER,
        MONEL_400,
        MONEL_K_500,
        INCONEL_600,
        NCONEL_718,
        NCONEL_X750,
        ELGILOY,
        NISPAN_C,
        HASTELLOY_C276
    }

    private val materials : Map<String, MaterialData> = mapOf(
        getName(Name.MUSIC_WIRE) to MaterialData(getName(Name.MUSIC_WIRE),                              0.284, 30.0,11.5),
        getName(Name.HARD_DRAWN_MB) to MaterialData(getName(Name.HARD_DRAWN_MB),                        0.284, 30.0,11.5),
        getName(Name.OIL_TEMP_MB) to MaterialData(getName(Name.OIL_TEMP_MB),                            0.284, 30.0,11.5),
        getName(Name.OIL_TEMP_CHROME_SILICON) to MaterialData(getName(Name.OIL_TEMP_CHROME_SILICON),    0.284, 30.0,11.5),
        getName(Name.OIL_TEMP_CHROME_VANADIUM) to MaterialData(getName(Name.OIL_TEMP_CHROME_VANADIUM),  0.284, 30.0,11.5),
        getName(Name.STAINLESS_302_304) to MaterialData(getName(Name.STAINLESS_302_304),                0.286, 0.19292,0.0689),
        getName(Name.STAINLESS_316) to MaterialData(getName(Name.STAINLESS_316),                        0.286, 28.0,10.0),
        getName(Name.STAINLESS_17_7_PH) to MaterialData(getName(Name.STAINLESS_17_7_PH),                0.282, 29.5,11.0),
        getName(Name.PHOSPHOR_BRONZE) to MaterialData(getName(Name.PHOSPHOR_BRONZE),                    0.32,  15.0,6.25),
        getName(Name.BERYLLIUM_COPPER) to MaterialData(getName(Name.BERYLLIUM_COPPER),                  0.298, 18.5,7.0),
        getName(Name.MONEL_400) to MaterialData(getName(Name.MONEL_400),                                0.319, 26.0,9.5),
        getName(Name.MONEL_K_500) to MaterialData(getName(Name.MONEL_K_500),                            0.306, 26.0,9.5),
        getName(Name.INCONEL_600) to MaterialData(getName(Name.INCONEL_600),                            0.304, 31.0,11.0),
        getName(Name.NCONEL_718) to MaterialData(getName(Name.NCONEL_718),                              0.298, 29.0,11.2),
        getName(Name.NCONEL_X750) to MaterialData(getName(Name.NCONEL_X750),                            0.298, 31.0,12.0),
        getName(Name.ELGILOY) to MaterialData(getName(Name.ELGILOY),                                    0.294, 32.0,12.0),
        getName(Name.NISPAN_C) to MaterialData(getName(Name.NISPAN_C),                                  0.294, 25.0,9.5),
        getName(Name.HASTELLOY_C276) to MaterialData(getName(Name.HASTELLOY_C276),                      0.294, 30.7,11.8)
    )

    /**
     * Get material data for specified material type
     *
     * @param name (material)
     * @return materialData
     */
    fun getMaterialData(name: Name): MaterialData {
        return materials.getValue(getName(name))
    }

    private fun getName(name : Name) : String {
        return name.name
    }
}
