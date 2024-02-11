package com.android.app.utils.calculators

import kotlin.math.*

/** REFERENCE TRIANGLE
 *
 *           |\
 *           |  \
 *           | β  \
 *           |      \
 *           |        \  C
 *         A |          \
 *           |            \
 *           |__ γ       ά  \
 *           |__|_____________\
 *                   B
 */
object CalcTrig {
    /**
     * Get sideC (hypotenuse)
     * (This is for right triangle only)
     *
     * c = √(a² + b²)
     *
     * @param sideA
     * @param sideB
     * @return sideC (Hypotenuse)
     */
    fun getHypotenuse(sideA: Double, sideB: Double): Double {
        return sqrt(sideA.pow(2.0) + sideB.pow(2.0))
    }

    /**
     * Get sideA or sideB from from right triangle given
     * angle opposite of that side and the adjacent side
     * (This is for right triangle only)
     *
     * a = b * tan(ά)
     *
     * @param sideB
     * @param angleA
     * @return sideA
     */
    fun getSideGiven1Side1Angle(sideB: Double, angleA: Double): Double {
        return (sideB * tan(Math.toRadians((angleA))))
    }


    /**
     * Find angle ά given side A and side C (Hypotenuse) of right triangle
     *
     * ά = asin(a/c)
     *
     * @param sideA
     * @param sideC
     */
    fun getAngleAGivenSideASideC(sideA: Double, sideC: Double): Double {
        return Math.toDegrees(asin(sideA / sideC))
    }

    /**
     * Get sideA from right triangle given sideC and angleA
     *
     * a = c * sin(ά)
     *
     * @param sideC
     * @param angleA
     * @return sideA
     */
    fun getSideAGivenSideCAngleA(sideC: Double, angleA: Double): Double {
        return (sideC * sin(Math.toRadians((angleA))))
    }

    /**
     * Get sideB from right triangle given sideC and angleA
     *
     * a = c * cos(ά)
     *
     * @param sideC
     * @param angleA
     * @return sideA
     */
    fun getSideBGivenSideCAngleA(sideC: Double, angleA: Double): Double {
        return (sideC * cos(Math.toRadians((angleA))))
    }

    /**
     * Get sideA from right triangle given sideC and angleB
     *
     * a = c * cos(β)
     *
     * @param sideC
     * @param angleB
     * @return sideA
     */
    fun getSideAGivenSideCAngleB(sideC: Double, angleB: Double): Double {
        return (sideC * cos(Math.toRadians((angleB))))
    }

    /**
     * Get side given the hypotenuse and another side
     *
     * a = √(c² - b²)
     *
     * @param sideB
     * @param sideC
     * @return sideA (Hypotenuse)
     */
    fun getSideGivenHypotenuse(sideB: Double, sideC: Double): Double {
        return sqrt(sideC.pow(2.0) - sideB.pow(2.0))
    }

    /**
     * Find 3rd side given 2 sides and the angle between them
     *
     * c = √[a² + b² - 2ab * cos(γ)]
     *
     * @param sideA
     * @param sideB
     * @param angleAB
     * @return sideC
     */
    fun getSideGiven2Sides1Angle(sideA: Double, sideB: Double, angleAB: Double): Double {
        return (sqrt(sideA.pow(2) + sideB.pow(2) - 2 * sideA * sideB * cos(Math.toRadians(angleAB))))
    }

    /**
     * Find side given angle opposite of side to be found and
     * 1 other side with angle opposite of that side
     *
     * a / sin(ά) = b / sin(β) = c / sin(γ)
     *
     * Ex. a = (b * sin(ά))/sin(β)
     *
     * @param sideB
     * @param angleA (angle opposite of side to be found)
     * @param angleB
     * @return sideA
     */
    fun getSideGiven1Side2Angles(sideB: Double, angleA: Double, angleB: Double): Double {
        return ((sideB * sin(Math.toRadians(angleA)))/sin(Math.toRadians(angleB)))
    }

    /**
     * Find angle given other 2 angles
     *
     * ά + β + γ = 180°
     *
     * @param angleA
     * @param angleB
     * @return angleC (Degrees)
     */
    fun getAngleGiven2Angles(angleA: Double, angleB: Double): Double {
        return (180.0 - angleA - angleB)
    }

    /**
     * Find angle given 3 sides
     *
     * ά = acos((b² + c² - a²)/(2bc))
     *
     * @param sideA (Side opposite of angle to be found)
     * @param sideB
     * @param sideC
     * @return angle (ά) (Degrees)
     */
    fun getAngleGiven3Sides(sideA: Double, sideB: Double, sideC: Double): Double {
        return Math.toDegrees(acos((sideB.pow(2) + sideC.pow(2) - sideA.pow(2)) / (2 * sideB * sideC)))
    }

    /**
     * Find angle given sideA and sideB
     *
     * β = atan(b / a)
     *
     * @param sideA
     * @param sideB
     * @return angle (β) (Degrees)
     */
    fun getAngleBGivenSideASideB(sideA: Double, sideB: Double) : Double{
        return Math.toDegrees(atan(sideB / sideA))
    }
}