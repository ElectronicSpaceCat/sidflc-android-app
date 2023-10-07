package com.android.greentech.plink.device

import com.android.greentech.plink.device.model.ModelData
import com.android.greentech.plink.utils.calculators.CalcBallistics
import com.android.greentech.plink.utils.calculators.CalcMisc
import com.android.greentech.plink.utils.calculators.CalcTrig
import com.android.greentech.plink.utils.converters.ConvertLength
import kotlin.math.*

class DeviceBallistics(model: ModelData) {
    private val _model = model
    private var _pos = 0.0

    private var _impactDistance = 0.0
    private var _impactHeight = 0.0

    private var _forceOffset = 0.0
    private var _frictionCoefficient = 0.0 // Currently set to 0.0 since it's almost negligible
    private var _efficiency = 0.0

    inner class ImpactData(var distance: Double = 0.0, var height: Double = 0.0)

    var forceOffset: Double
        set(value) {
            _forceOffset = if(value in 0.0..2.0){
                value
            } else{
                0.0
            }
        }
        get() = _forceOffset

    var frictionCoefficient : Double
        set(value) {
            _frictionCoefficient = if(value in 0.0..1.0){
                value
            } else{
                0.0
            }
        }
        get() = _frictionCoefficient

    var efficiency: Double
        set(value) {
            _efficiency = if(value in 0.5..1.0){
                value
            } else{
                1.0
            }
        }
        get() = _efficiency

    private var _velocity = 0.0
    val getVelocity: Double
        get() = _velocity

    private var _adjustedLaunchHeight = 0.0 // Device height + offset based on projectile position and phone pitch
    val getAdjustedLaunchHeight: Double
        get() = _adjustedLaunchHeight

    private var _adjustedTargetDistance = 0.0 // Device height + offset based on projectile position and phone pitch
    val adjustedTargetDistance: Double
        get() = _adjustedTargetDistance

    /**
     * Get estimated impact distance and height of a launched projectile.
     *
     * @param position (mm) Distance between the face of the short range sensor and the back of the carrier
     * @param launchAngle (deg) Pitch of the device
     * @param launchHeight (m) Launch height of the device
     * @param targetDistance (m) Distance of the target to hit
     * @param lensOffset (mm) Distance from the front of case to the center of the mobile's main camera lens
     *
     * @return impactDistance (m)
     */
    fun getImpactData(position: Double, launchAngle: Double, launchHeight: Double, targetDistance: Double, lensOffset : Double = 0.0): ImpactData {
        // Cap the position to the max allowed position
        _pos = min(position, _model.getMaxCarriagePosition())

        val deltaPos = _model.getMaxCarriagePosition() - _pos

        // Distance times the energy loss due to gravity and friction at an angle
        val kineticEnergyLoss = deltaPos * (_forceOffset + (0.001 * _model.getTotalWeight() * CalcBallistics.ACCELERATION_OF_GRAVITY * sin(Math.toRadians(launchAngle)))
        + (0.001 * _model.getTotalWeight() * CalcBallistics.ACCELERATION_OF_GRAVITY * cos(Math.toRadians(launchAngle))) * _frictionCoefficient)

        // Total potential energy multiplied by an efficiency factor
        val netPotentialEnergy = max(0.0, (_model.getPotentialEnergyAtPosition(_pos) - kineticEnergyLoss) * _efficiency)

        // Get the approximate launch height which is the value of the (device height + max carriage pos + projectile center of gravity offset)
        // and then adjusted by the launch angle where the vertex starts at the device height.
        /*
                               /|
             Projectile -->  ()-|--- <---- Height offset
                             /  |
                        ____/)__|__-____________________
                             ^.__ Device Angle      ^.__ Device height
         */
        val heightOffsetMM = getProjectileHeightOffsetAtAngle(_model.getMaxCarriagePosition(), launchAngle)
        val heightOffsetM = ConvertLength.convert(ConvertLength.Unit.MM, ConvertLength.Unit.M, heightOffsetMM)
        _adjustedLaunchHeight = (launchHeight + heightOffsetM)

        // Get the adjusted target distance which is the calculated target distance plus the distance behind the lens
        // at which the projectile will be launched.
        val offset = ((_model.caseBodyLength + lensOffset) - _model.getProjectileCenterOfMassPosition(_model.getMaxCarriagePosition()))
        // Only adjust targetDistance if the offset is greater than the offset of the projectiles center of mass location
        _adjustedTargetDistance = if(offset > 0.0){
            // Convert the offset from mm to m
            val offsetMeters = ConvertLength.convert(ConvertLength.Unit.MM, ConvertLength.Unit.M, offset)
            // Return adjusted target distance
            (targetDistance + CalcTrig.getSideBGivenSideCAngleA(offsetMeters, launchAngle))
        } else{
            // Return target distance
            targetDistance
        }

        // Calculate the velocity from the stored energy and apply an efficiency factor
        _velocity = CalcBallistics.getVelocity(_model.getTotalWeight(), netPotentialEnergy)

        // If velocity <= zero then return zero for both impact distance and height
        if(_velocity <= 0.0){
            return ImpactData(0.0, 0.0)
        }

        // Blocking routine
        calculateProjectileWithQuadraticDrag(_velocity, _adjustedLaunchHeight, launchAngle, DEFAULT_DELTA_TIME_SECONDS)

        // Return the impact distance and height
        return ImpactData(_impactDistance, max(0.0,_impactHeight))
    }

    /**
     * (Blocking function)
     *
     * Run an ODE (ordinary differential equation) to pre-path the projectile with air drag
     * until the height is less or equal to zero (when the projectile hit the ground).
     */
    private fun calculateProjectileWithQuadraticDrag(velocity : Double, height: Double, launchAngle: Double, deltaTimeSeconds : Double) {
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

        // Drag coefficient
        val dragCoefficient = _model.projectile!!.drag

        // Run a quadratic drag routine until the height of the projectile reaches zero (hit the ground)
        do{
            v = sqrt(vX.pow(2) + vY.pow(2))

            vXprev = vX
            vYprev = vY

            vX -= (dragCoefficient * vX * v * deltaTimeSeconds)
            vY -= (CalcBallistics.ACCELERATION_OF_GRAVITY * deltaTimeSeconds) - (dragCoefficient * vY * v * deltaTimeSeconds)

            xPrev = x
            yPrev = y

			// Get displacement from avg velocity
            x += 0.5 * (vX + vXprev) * deltaTimeSeconds
            y += 0.5 * (vY + vYprev) * deltaTimeSeconds

            // Get impact height when x crosses the target distance
            if(!gotImpactHeight && x >= _adjustedTargetDistance){
                gotImpactHeight = true
                _impactHeight = CalcMisc.interpolate(_adjustedTargetDistance, x, xPrev, y, yPrev)
            }
        }while (y > 0.0)

        // Set height to zero if it was not found
        if(!gotImpactHeight){
            _impactHeight = 0.0
        }

        // Get the distance in the x-direction
        _impactDistance = CalcMisc.interpolate(0.0, y, yPrev, x, xPrev)
    }

    /**
     * Get height offset of the projectile
     * at the current launch angle and carrier position.
     *
     * @param position (mm)
     * @param launchAngle (deg)
     * @return heightOffset (mm)
     */
    private fun getProjectileHeightOffsetAtAngle(position: Double, launchAngle: Double): Double {
        return CalcTrig.getSideAGivenSideCAngleA(_model.getProjectileCenterOfMassPosition(position), launchAngle)
    }

    companion object {
        private const val DEFAULT_DELTA_TIME_SECONDS = 0.01
        private const val DEFAULT_EFFICIENCY_FACTOR = 0.65f
    }
}
