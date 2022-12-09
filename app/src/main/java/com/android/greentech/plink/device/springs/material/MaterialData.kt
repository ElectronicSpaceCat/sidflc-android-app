package com.android.greentech.plink.device.springs.material

class MaterialData(
    name: String,
    density: Double,
    modulusOfElasticity: Double,
    modulusOfTorsion: Double) {
    private val _name : String = name
    private val _density: Double = density
    private val _modulusOfElasticity: Double = modulusOfElasticity
    private val _modulusOfTorsion: Double = modulusOfTorsion

    val getName: String
        get() = _name
    val getDensity: Double
        get() = _density
    val getModulusOfElasticity: Double
        get() = _modulusOfElasticity
    val getModulusOfTorsion: Double
        get() = _modulusOfTorsion
}