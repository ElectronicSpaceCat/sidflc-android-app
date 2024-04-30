package com.android.app.utils.calculators

import kotlin.math.*

object CalcBallistics {
    const val ACCELERATION_OF_GRAVITY = 9.80665
    /**
     * Get target distance from device height and pitch
     *
     * @param height (m)
     * @param pitch (deg)
     * @return targetDistance (m)
     */
    fun getTargetDistance(height: Double, pitch: Double): Double {
        return CalcTrig.getSideGiven1Side1Angle(height, pitch)
    }

    /**
     * Get target height from launch height, target angle @ target distance, pitch
     *
     * @param height (m)
     * @param trgtDistAngle (deg)
     * @param pitch (deg)
     * @return targetHeight (m)
     */
    fun getTargetHeight(height: Double, trgtDistAngle: Double, pitch: Double): Double {
        val angleC1 = (90.0) // Right triangle
        val angleB2 = (pitch - trgtDistAngle)
        val angleA1 = CalcTrig.getAngleGiven2Angles(angleC1, trgtDistAngle)
        val angleA2 = (angleC1 - angleA1)
        val angleC2 = CalcTrig.getAngleGiven2Angles(angleA2, angleB2)
        val sideC = CalcTrig.getSideGiven1Side2Angles(height, angleC1, angleA1)
        return CalcTrig.getSideGiven1Side2Angles(sideC, angleB2, angleC2)
    }

    /**
     * Get velocity
     *
     * Formula: KE = 0.5 * m * v²
     *
     * rearrange to..
     *
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
        return ((weight * ACCELERATION_OF_GRAVITY * distanceTraveled) * (sin(pitch) + cos(pitch) * coefficientOfFriction))
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
    fun getImpactDistance(velocity: Double, launchHeight: Double, launchAngle: Double): Double {
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
    fun getImpactHeight(launchHeight: Double, launchAngle: Double, velocity: Double, travelTime: Double): Double {
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

        // Flag indicating the height at impact was found
        var gotImpactHeight = false

        // Run a quadratic drag routine until the height of the projectile reaches zero (hit the ground)
        do{
            v = sqrt(vX.pow(2) + vY.pow(2))

            vXprev = vX
            vYprev = vY

            vX -= (dragCoefficient * vX * v * deltaTimeSeconds)
            vY -= (ACCELERATION_OF_GRAVITY * deltaTimeSeconds) - (dragCoefficient * vY * v * deltaTimeSeconds)

            xPrev = x
            yPrev = y

            // Get displacement from avg velocity
            x += 0.5 * (vX + vXprev) * deltaTimeSeconds
            y += 0.5 * (vY + vYprev) * deltaTimeSeconds

            // Get impact height when x crosses the target distance
            if(!gotImpactHeight && x >= targetDistance){
                gotImpactHeight = true

                impactData.height = CalcMisc.interpolate(targetDistance, x, xPrev, y, yPrev)
            }
        }while (y > 0.0)

        // Set height to zero if it was not found
        if(!gotImpactHeight){
            impactData.height = 0.0
        }

        // Get the distance in the x-direction
        impactData.distance = CalcMisc.interpolate(0.0, y, yPrev, x, xPrev)

        return impactData
    }
}
