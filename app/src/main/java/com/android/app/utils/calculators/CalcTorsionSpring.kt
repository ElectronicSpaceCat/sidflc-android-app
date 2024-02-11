package com.android.app.utils.calculators

import kotlin.math.pow

object CalcTorsionSpring {
    fun getAngle(activeCoils: Double) : Double {
        return FULL_ROTATION * (activeCoils % 1)
    }
    fun getWireDiameter(outerDiameter: Double, innerDiameter: Double) : Double{
        return ((outerDiameter - innerDiameter)/2.0)
    }
    fun getOuterDiameter(wireDiameter: Double, innerDiameter: Double) : Double{
        return ((2.0 * wireDiameter) - innerDiameter)
    }
    fun getInnerDiameter(wireDiameter: Double, outerDiameter: Double) : Double{
        return (outerDiameter - (2.0 * wireDiameter))
    }
    fun getMeanDiameterFromOuterDiameter(wireDiameter: Double, outerDiameter: Double) : Double{
        return (outerDiameter - wireDiameter)
    }
    fun getMeanDiameterFromInnerDiameter(wireDiameter: Double, innerDiameter: Double) : Double{
        return (innerDiameter + wireDiameter)
    }
    fun getCoilIndex(meanDiameter: Double, wireDiameter: Double) : Double{
        return (meanDiameter / wireDiameter)
    }
    fun getRatePerRotation(modulusOfElasticity: Double, wireDiameter: Double, meanDiameter: Double, activeCoils: Double) : Double{
        return ((modulusOfElasticity * 10.0.pow(6)) * wireDiameter.pow(4)) / (SPRING_FACTOR_PER_DEG * meanDiameter * activeCoils)
    }
    fun getRatePerDegree(modulusOfElasticity: Double, wireDiameter: Double, meanDiameter: Double, activeCoils: Double) : Double{
        return ((modulusOfElasticity * 10.0.pow(6)) * wireDiameter.pow(4)) / (SPRING_FACTOR_PER_ROTATION * meanDiameter * activeCoils)
    }
    fun getRatePerDegree(ratePerRotation: Double) : Double{
        return (ratePerRotation / FULL_ROTATION)
    }
    fun getCoilWireLength(meanDiameter: Double, activeCoils: Double) : Double{
        return (Math.PI * meanDiameter * activeCoils)
    }
    fun getWireLengthTotal(lengthLeg1: Double, lengthLeg2: Double, coilWireLength: Double) : Double{
        return (lengthLeg1 + lengthLeg2 + coilWireLength)
    }
    fun getActiveCoils(modulusOfElasticity: Double, springRate: Double, wireDiameter: Double, meanDiameter: Double) : Double{
        return ((modulusOfElasticity * 10.0.pow(6)) * wireDiameter.pow(4)) / (SPRING_FACTOR_PER_DEG * meanDiameter * springRate)
    }
    fun getTorqueAtDegreeTraveled(springRatePerDeg: Double, angle: Double) : Double{
        return (springRatePerDeg * angle)
    }
    fun getForceAtDegreeTraveled(springTorquePerDeg: Double, radius: Double) : Double{
        return (springTorquePerDeg / radius)
    }
    fun getForceAtDegreeTraveled(springRatePerDeg: Double, angle: Double, radius: Double) : Double{
        return ((springRatePerDeg * angle) / radius)
    }
}

/**
 * 10.8 = SPRING_FACTOR_PER_DEG
 * 3888 = SPRING_FACTOR_PER_ROTATION (10.8 * 360)
 *
 * "The 10.8 (or 3888) spring factor is greater than the theoretical factor
 * of 10.2 (or 3670) to allow for friction between adjacent spring coils,
 * and between the spring body and the arbor.
 *
 * This factor is based on experience and has been found to be satisfactory."
 */
const val FULL_ROTATION = (360.0)
const val SPRING_FACTOR_PER_DEG = (10.8)
const val SPRING_FACTOR_PER_ROTATION = (SPRING_FACTOR_PER_DEG * FULL_ROTATION)

