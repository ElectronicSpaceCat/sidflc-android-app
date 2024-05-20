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
     * Get sideHypotenuse
     * (This is for right triangle only)
     *
     * c = √(a² + b²)
     *
     * @param sideOpposite
     * @param sideAdjacent
     * @return sideC (Hypotenuse)
     */
    fun getHypotenuseGivenSideOppositeSideAdjacent(sideOpposite: Double, sideAdjacent: Double): Double {
        return sqrt(sideOpposite.pow(2.0) + sideAdjacent.pow(2.0))
    }

    /**
     * Get sideHypotenuse
     * (This is for right triangle only)
     *
     * c = a / sin(ά)
     *
     * c = b / sin(β)
     *
     * @param sideOpposite
     * @param angleOpposite
     * @return sideHypotenuse
     */
    fun getHypotenuseGivenSideOppositeAngleOpposite(sideOpposite: Double, angleOpposite: Double): Double {
        return (sideOpposite / sin(Math.toRadians(angleOpposite)))
    }

    /**
     * Get side from right triangle given angle opposite
     * of that side and the adjacent side
     * (This is for right triangle only)
     *
     * a = b * tan(ά)
     *
     * b = a * tan(β)
     *
     * @param sideOpposite
     * @param angleAdjacent
     * @return side
     */
    fun getSideGivenSideOppositeAngleAdjacent(sideOpposite: Double, angleAdjacent: Double): Double {
        return (sideOpposite * tan(Math.toRadians((angleAdjacent))))
    }

    /**
     * Get angleOpposite given sideHypotenuse and sideOpposite
     *
     * ά = asin(a/c)
     *
     * β = asin(b/c)
     *
     * @param sideHypotenuse
     * @param sideOpposite
     * @return angleOpposite
     */
    fun getAngleBetweenSideHypotenuseSideOpposite(sideHypotenuse: Double, sideOpposite: Double): Double {
        return Math.toDegrees(asin(sideOpposite / sideHypotenuse))
    }

    /**
     * Get angleOpposite given sideHypotenuse and sideAdjacent
     *
     * ά = acos(b/c)
     *
     * β = acos(a/c)
     *
     * @param sideHypotenuse
     * @param sideAdjacent
     * @return angleOpposite
     */
    fun getAngleBetweenSideHypotenuseSideAdjacent(sideHypotenuse: Double, sideAdjacent: Double): Double {
        return Math.toDegrees(acos(sideAdjacent / sideHypotenuse))
    }

    /**
     * Get angleOpposite given sideOpposite and sideAdjacent
     *
     * β = atan(b/a)
     *
     * α = atan(a/b)
     *
     * @param sideOpposite
     * @param sideAdjacent
     * @return angleOpposite
     */
    fun getAngleBetweenSideOppositeSideAdjacent(sideAdjacent: Double, sideOpposite: Double) : Double{
        return Math.toDegrees(atan(sideOpposite / sideAdjacent))
    }

    /**
     * Get sideOpposite given sideHypotenuse and angleOpposite
     *
     * a = c * sin(ά)
     *
     * b = c × sin(β)
     *
     * @param sideHypotenuse
     * @param angleOpposite
     * @return sideOpposite
     */
    fun getSideOppositeGivenSideHypotenuseAngleOpposite(sideHypotenuse: Double, angleOpposite: Double): Double {
        return (sideHypotenuse * sin(Math.toRadians((angleOpposite))))
    }

    /**
     * Get side given sideHypotenuse and angleAdjacent
     *
     * a = c × cos(β)
     *
     * b = c × cos(α)
     *
     * @param sideHypotenuse
     * @param angleAdjacent
     * @return sideAdjacent
     */
    fun getSideGivenSideHypotenuseAngleAdjacent(sideHypotenuse: Double, angleAdjacent: Double): Double {
        return (sideHypotenuse * cos(Math.toRadians((angleAdjacent))))
    }

    /**
     * Get sideOpposite given sideHypotenuse and angleOpposite
     *
     * a = c × sin(α)
     *
     * b = c × sin(β)
     *
     * @param sideHypotenuse
     * @param angleOpposite
     * @return sideOpposite
     */
    fun getSideGivenSideHypotenuseAngleOpposite(sideHypotenuse: Double, angleOpposite: Double): Double {
        return (sideHypotenuse * cos(Math.toRadians((angleOpposite))))
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
    fun getSideGivenSideHypotenuseSideAny(sideB: Double, sideC: Double): Double {
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
}