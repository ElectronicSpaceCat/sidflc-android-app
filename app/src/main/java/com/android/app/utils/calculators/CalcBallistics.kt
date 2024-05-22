package com.android.app.utils.calculators

import kotlin.math.*

object CalcBallistics {
    const val ACCELERATION_OF_GRAVITY = 9.80665

    /**
     * Get phone height
     *
     * @param sensorDistance value (mm)
     * @param deviceOffset (mm)
     * @return phoneHeight (mm)
     */
    fun getPhoneHeight(sensorDistance: Double, deviceOffset : Double): Double {
        return (sensorDistance - deviceOffset)
    }

    /**
     * Get target distance
     *
     * @param pitch (deg)
     * @param phoneHeight (m)
     * @param lensOffset (m)
     * @return targetDistance (m)
     */
    fun getTargetDistance(pitch: Double, phoneHeight : Double, lensOffset: Double): Double {
        val lensHeight = (phoneHeight + CalcTrig.getSideOppositeGivenSideHypotenuseAngleOpposite(lensOffset, pitch))
        val distance1 = CalcTrig.getSideGivenSideHypotenuseAngleAdjacent(lensOffset, (90.0 - pitch))
        val distance2 = CalcTrig.getSideGivenSideOppositeAngleAdjacent(lensHeight, pitch)
        return (distance1 + distance2)
    }

    /**
     * Get the pitch angle that the target distance was calculated
     *
     * The equation is: y = d*cos(x) - k * sin²(x) - p * sin(x)
     * where d = targetDistance, k = lensOffset, p = phoneHeight
     *
     * Since solving for the angle is a hard af, just approximated it numerically
     * and then interpolate between the points where y crossed zero.
     *
     * @param phoneHeight (m)
     * @param lensOffset (m)
     * @param targetDistance (deg)
     * @return targetHeight (m)
     */
    fun getPitchAtTargetDistance(phoneHeight : Double, lensOffset: Double, targetDistance: Double) : Double {
        val circleCenter = Point(0.0, phoneHeight)
        val point = Point(targetDistance, 0.0)
        val tangents = CalcGeometry.getTangentPointsOfLine(circleCenter, lensOffset, point)!!
        return CalcTrig.getAngleBetweenSideHypotenuseSideAdjacent(lensOffset, tangents.t1.x)
    }

    /**
     * Get target height when rotated about the wrist
     *
     * @param pitch (deg)
     * @param phoneHeight (m)
     * @param lensOffset (m)
     * @param targetDistance (m)
     * @return targetHeight (m)
     */
    fun getTargetHeightRotatedAboutWrist(pitch: Double, phoneHeight: Double, lensOffset: Double, targetDistance: Double): Double {
        // Set a center point
        val cX = 0.0
        val cY = phoneHeight
        // Point where the line is tangential to the circle
        val tX : Double
        val tY : Double
        if(pitch > 90.0) {
            val pitchAdj = (180.0 - pitch)
            tX = CalcTrig.getSideOppositeGivenSideHypotenuseAngleOpposite(lensOffset, (pitchAdj - 90.0))
            tY = CalcTrig.getSideOppositeGivenSideHypotenuseAngleOpposite(lensOffset, pitchAdj) + cY
        }
        else {
            tX = CalcTrig.getSideOppositeGivenSideHypotenuseAngleOpposite(lensOffset, (90.0 - pitch))
            tY = CalcTrig.getSideOppositeGivenSideHypotenuseAngleOpposite(lensOffset, pitch) + cY
        }
        // Slope of the radial line
        val rSlope = CalcLinear.getSlope(cX, cY, tX, tY)
        // Slope of the tangential line (reciprocal of rSlope)
        val tSlope = -(1/rSlope)
        val tInterceptY = CalcLinear.getInterceptY(tX, tY, tSlope)
        // y = mx + b )
        return ((tSlope * targetDistance) + tInterceptY)
    }

    /**
     * Get target height when rotated about the lens
     *
     * @param pitch (deg)
     * @param pitchAtTargetDistance (deg)
     * @param phoneHeight (m)
     * @param lensOffset (m)
     * @param targetDistance (m)
     * @return targetHeight (m)
     */
    fun getTargetHeightRotatedAboutLens(pitch: Double, pitchAtTargetDistance: Double, phoneHeight: Double, lensOffset: Double, targetDistance: Double): Double {
        // Point where the line is tangential to the circle
        val tX = lensOffset * sin(Math.toRadians(pitch - 90.0))
        val tY = (lensOffset * sin(Math.toRadians(pitch))) + phoneHeight
        // Point at target base
        val pX = targetDistance
        val pY = 0.0
        // Get the length from point t to p
        val length = CalcLinear.getDistanceBetweenPoints(tX, tY, pX, pY)
        // Return the height
        return CalcTrig.getSideGiven1Side2Angles(length, pitch, (180.0-pitch-pitchAtTargetDistance))
    }

    /**
     * Get velocity
     *
     * Formula: KE = 0.5 * m * v²
     * rearranged to..
     * v = √(2 * KE / m)
     *
     * @param mass (kg)
     * @param kineticEnergy (J)
     * @return velocity (m/s)
     */
    fun getVelocity(mass: Double, kineticEnergy: Double): Double {
        return sqrt(2.0 * kineticEnergy / mass)
    }

    /**
     * Get work required up and incline
     *
     * W = (m * g * Δd) * [sin(Θ) + (μ * cos(Θ))]
     * where μ is the coefficient of friction
     *
     * @param weight (g)
     * @param pitch (deg)
     * @param distanceTraveled (m)
     * @param coefficientOfFriction
     * @return work (N-m or J)
     */
    fun getWorkRequiredUpIncline(weight: Double, pitch: Double, distanceTraveled: Double, coefficientOfFriction: Double): Double {
        return ((weight * ACCELERATION_OF_GRAVITY * distanceTraveled) * (sin(Math.toRadians(pitch)) + cos(Math.toRadians(pitch)) * coefficientOfFriction))
    }

    /**
     * Get flight time
     *
     * t = x / v₀ₓ
     *
     * @param targetDistance
     * @param launchAngle
     * @param velocity
     * @return time (s)
     */
    fun getFlightTime(targetDistance: Double, launchAngle: Double, velocity: Double): Double {
        return if(velocity > 0){
            (targetDistance / getVelocityDirectionX(velocity, launchAngle))
        }
        else{
            0.0
        }
    }

    /**
     * Get impact distance for ideal parabolic trajectory
     *
     * x = v₀ * cos(α) * (v₀ * sin(α) + √((v₀ * sin(α))² + 2 * g * h)) / g
     *
     * @param velocity (m/s)
     * @param launchHeight (m)
     * @param launchAngle (deg)
     * @return impactDistance (m)
     */
    fun getImpactDistanceIdeal(velocity: Double, launchHeight: Double, launchAngle: Double): Double {
        return (((velocity * cos(Math.toRadians(launchAngle))) / ACCELERATION_OF_GRAVITY) *
                (velocity * sin(Math.toRadians(launchAngle)) +
                        sqrt(velocity.pow(2) * sin(Math.toRadians(launchAngle)).pow(2) +
                                2.0 * ACCELERATION_OF_GRAVITY * launchHeight)))
    }

    /**
     * Get impact height for ideal parabolic trajectory
     *
     * y = y₀ + v₀ᵧt - 0.5 * g * t²
     *
     * @param launchHeight (m)
     * @param launchAngle (deg)
     * @param velocity (m/s)
     * @param travelTime (s)
     * @return impactHeight (m)
     */
    fun getImpactHeightIdeal(launchHeight: Double, launchAngle: Double, velocity: Double, travelTime: Double): Double {
        val velocityY = getVelocityDirectionY(velocity, launchAngle)
        return (launchHeight + (velocityY * travelTime - (0.5 * ACCELERATION_OF_GRAVITY * travelTime.pow(2))))
    }

    /**
     * Get velocity in Y direction
     *
     * v₀ₓ = v * sin(α)
     *
     * @param velocity
     * @param launchAngle
     * @return velocity in Y direction
     */
    private fun getVelocityDirectionY(velocity: Double, launchAngle: Double): Double {
        return (velocity * sin(Math.toRadians(launchAngle)))
    }

    /**
     * Get velocity in X direction
     *
     * v₀ₓ = v * cos(α)
     *
     * @param velocity
     * @param launchAngle
     * @return velocity in Y direction
     */
    private fun getVelocityDirectionX(velocity: Double, launchAngle: Double): Double {
        return (velocity * cos(Math.toRadians(launchAngle)))
    }

    /**
     * (Blocking function)
     *
     * Run an ODE (ordinary differential equation) to pre-path the projectile with air drag
     * until the height is less or equal to zero (when the projectile hit the ground).
     */
    class ImpactData(
        var distance: Double = 0.0,
        var height: Double = 0.0)
    fun calculateProjectileWithQuadraticDrag(velocity : Double, launchAngle: Double, height: Double, targetDistance: Double, dragCoefficient : Double, deltaTimeSeconds : Double) : ImpactData {
        val impactData = ImpactData(0.0, 0.0)

        // Velocity in x/y component (initial)
        val vXinit = velocity * cos(Math.toRadians(launchAngle))
        val vYinit = velocity * sin(Math.toRadians(launchAngle))

        // Velocity
        var v: Double

        // Velocity in x/y component
        var vX = vXinit
        var vY = vYinit

        // Velocity in x/y component (previous)
        var vXprev: Double
        var vYprev: Double

        // Distance in x/y component
        var x = 0.0
        var y = height

        // Distance in x/y component (previous)
        var xPrev: Double
        var yPrev: Double

        // Height where x crosses the targetDistance
        var impactHeight = 0.0

        // Flag indicating the height at impact was found
        var gotImpactHeight = false

        // Run a quadratic drag routine until the height of the projectile reaches zero (hit the ground)
        do{
            v = sqrt(vX.pow(2) + vY.pow(2))

            vXprev = vX
            vYprev = vY

            vX -= (dragCoefficient * vXprev * v * deltaTimeSeconds)
            vY = (vYprev - (ACCELERATION_OF_GRAVITY * deltaTimeSeconds) - (dragCoefficient * vYprev * v * deltaTimeSeconds))

            xPrev = x
            yPrev = y

            // Get displacement from avg velocity
            x += (0.5 * (vX + vXprev) * deltaTimeSeconds)
            y += (0.5 * (vY + vYprev) * deltaTimeSeconds)

            // Get impact height when x crosses the target distance
            if(!gotImpactHeight && x >= targetDistance){
                gotImpactHeight = true
                impactHeight = max(0.0, CalcLinear.interpolate(targetDistance, x, xPrev, y, yPrev))
            }
        } while (y > 0.0)

        // Set the impact height
        impactData.height = impactHeight
        // Approximate the impact distance where y crosses zero
        impactData.distance = CalcLinear.interpolate(0.0, y, yPrev, x, xPrev)

        return impactData
    }
}
