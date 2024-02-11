package com.android.app.device.springs

import com.android.app.device.springs.material.Material
import com.android.app.device.springs.material.MaterialData
import com.android.app.utils.calculators.CalcTorsionSpring

/**
 * Spring data
 *
 * @constructor
 * @param name
 * @param wireDiameter (mm)
 * @param outerDiameter (mm)
 * @param activeCoils
 * @param legLength1 (mm)
 * @param legLength2 (mm)
 * @param material
 */
class SpringData(
    name: String = "Spring",
    wireDiameter: Double,
    outerDiameter: Double,
    activeCoils: Double,
    legLength1: Double,
    legLength2: Double,
    material: Material.Name) {

    private val _name = name
    private val _wireDiameter = wireDiameter
    private val _outerDiameter = outerDiameter
    private val _activeCoils = activeCoils
    private val _legLength1 = legLength1
    private val _legLength2 = legLength2

    // Calculated parameters
    private val _innerDiameter : Double = CalcTorsionSpring.getInnerDiameter(wireDiameter, outerDiameter)
    private val _meanDiameter : Double = CalcTorsionSpring.getMeanDiameterFromOuterDiameter(wireDiameter, outerDiameter)
    private val _material : MaterialData = Material.getMaterialData(material)
    private val _coilSpringIndex : Double = CalcTorsionSpring.getCoilIndex(_meanDiameter, wireDiameter)
    private val _springRatePerDeg : Double = CalcTorsionSpring.getRatePerDegree(_material.getModulusOfElasticity, wireDiameter, _meanDiameter, _activeCoils)

    val name: String
        get() = _name
    val wireDiameter
        get() =  _wireDiameter
    val innerDiameter
        get() =  _innerDiameter
    val outerDiameter
        get() =  _outerDiameter
    val meanDiameter
        get() =  _meanDiameter
    val activeCoils
        get() =  _activeCoils
    val material
        get() =  _material

    /**
     * Get the net force at degree
     *
     * @param angle
     * @param radius
     * @return netForce
     */
    fun getForceAtDegree(angle : Double, radius: Double) : Double{
        return CalcTorsionSpring.getForceAtDegreeTraveled(_springRatePerDeg, angle, radius)
    }
}
