package com.android.app.device.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.app.utils.calculators.CalcBallistics
import com.android.app.utils.calculators.CalcTrig
import com.android.app.utils.converters.ConvertLength
import kotlin.math.*

open class ModelBallistics(model: ModelData) {
    private var _model = model

    // Settable parameters
    private var _forceOffset = MutableLiveData(DEFAULT_FORCE_OFFSET)
    private var _efficiency = MutableLiveData(DEFAULT_EFFICIENCY_FACTOR)
    private var _frictionCoefficient = MutableLiveData(DEFAULT_FRICTION_COEFFICIENT)

    private var _impactData = CalcBallistics.ImpactData()
    val impactData : CalcBallistics.ImpactData
        get() = _impactData

    private var _adjustedLaunchHeight = 0.0 // Device height + offset based on projectile position and phone pitch
    val adjustedLaunchHeight: Double
        get() = _adjustedLaunchHeight

    private var _adjustedTargetDistance = 0.0 // Device height + offset based on projectile position and phone pitch
    val adjustedTargetDistance: Double
        get() = _adjustedTargetDistance

    val forceOffsetOnChange : LiveData<Double>
        get() = _forceOffset
    var forceOffset: Double
        set(value) {
            _forceOffset.value = if(value in 0.0..2.0) {
                value
            } else{
                DEFAULT_FORCE_OFFSET
            }
        }
        get() = _forceOffset.value!!

    fun resetForceOffset() {
        forceOffset = DEFAULT_FORCE_OFFSET
    }

    val frictionCoefficientOnChange : LiveData<Double>
        get() = _frictionCoefficient
    var frictionCoefficient : Double
        set(value) {
            _frictionCoefficient.value = if(value in 0.0..1.0) {
                value
            } else{
                DEFAULT_FRICTION_COEFFICIENT
            }
        }
        get() = _frictionCoefficient.value!!

    fun resetFrictionCoefficient() {
        frictionCoefficient = DEFAULT_FRICTION_COEFFICIENT
    }

    val efficiencyOnChange : LiveData<Double>
        get() = _efficiency
    var efficiency: Double
        set(value) {
            _efficiency.value = if(value in 0.5..1.0) {
                value
            } else{
                DEFAULT_EFFICIENCY_FACTOR
            }
        }
        get() = _efficiency.value!!

    fun resetEfficiency() {
        efficiency = DEFAULT_EFFICIENCY_FACTOR
    }

    class ProjectileBallistics(
        var netPotentialEnergy: Double = 0.0,
        var velocity: Double = 0.0)

    private var _projectileBallistics = ProjectileBallistics()

    val projectileBallistics : ProjectileBallistics
        get() = _projectileBallistics


    /**
     * Get estimated impact distance and height of a launched projectile.
     * This is a blocking function and should be called on a background task.
     *
     * @param position (mm) Distance between the face of the short range sensor and the back of the carriage
     * @param lensOffset (mm)
     * @param deviceHeight (m)
     * @param targetDistance (m)
     * @param launchAngle (deg) Pitch of the device
     */
    fun calcImpactData(
        position : Double,
        lensOffset : Double,
        deviceHeight : Double,
        launchAngle : Double,
        targetDistance : Double) : CalcBallistics.ImpactData
    {
        // Calculate the height of the projectile offset from the height captured by the sensor, angle of the phone affects it.
        val heightOffsetMM = getProjectileHeightOffsetAtAngle(_model.getMaxCarriagePosition(), launchAngle)
        val heightOffsetM = ConvertLength.convert(ConvertLength.Unit.MM, ConvertLength.Unit.M, heightOffsetMM)
        _adjustedLaunchHeight = (deviceHeight + heightOffsetM)

        // Get the adjusted target distance which is the calculated target distance plus the distance behind the lens
        // at which the projectile will be launched.
        val offset = (_model.caseBodyLength + lensOffset - _model.getProjectileCenterOfMassPosition(_model.getMaxCarriagePosition()))

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

        // Get projectile ballistics at carriage position and launch angle
        _model.ballistics.calcProjectileBallistics(position, launchAngle)

        // If velocity <= zero then return zero for both impact distance and height
        _impactData = if(_model.ballistics.projectileBallistics.velocity <= 0.0){
            CalcBallistics.ImpactData()
        }
        else{
            // Blocking routine
            CalcBallistics.calculateProjectileWithQuadraticDrag(
                _model.ballistics.projectileBallistics.velocity,
                launchAngle,
                _adjustedLaunchHeight,
                _adjustedTargetDistance,
                _model.projectile!!.drag,
                DEFAULT_DELTA_TIME_SECONDS
            )
        }

        return _impactData
    }

    /**
     * Calculate the hit confidence
     *
     * @param targetDistance (m)
     * @param targetHeight (m)
     * @param impactData default set to internal value
     */
    fun calcHitConfidence(
        targetDistance : Double,
        targetHeight : Double,
        impactData: CalcBallistics.ImpactData = _impactData) : Double {

        // Target distance + target height
        val totalLengthEst = if(impactData.distance >= targetDistance && targetHeight > 0.0){
            targetDistance + impactData.height
        } else{
            impactData.distance
        }

        // Get the totalLengthTarget
        val totalLengthTarget = (targetDistance + targetHeight)

        // Calculate the hit confidence percentage based on the total length
        val hitConfidence = (if(totalLengthEst == 0.0 || totalLengthTarget == 0.0){
            0.0 // Return 0 if no target or estimate length = 0
        } else{
            // Flip the percentage calculation if the estimate length is above or below the target length
            if (totalLengthEst > totalLengthTarget) {
                (totalLengthTarget / totalLengthEst) * 100.0
            } else {
                (totalLengthEst / totalLengthTarget) * 100.0
            }
        })

        return hitConfidence
    }

    /**
     * Get height offset of the projectile at given launch angle.
     *
     * @param position (mm)
     * @param launchAngle (deg)
     * @return heightOffset (mm)
     */
    private fun getProjectileHeightOffsetAtAngle(position: Double, launchAngle: Double) : Double {
        return CalcTrig.getSideAGivenSideCAngleA(_model.getProjectileCenterOfMassPosition(position), launchAngle)
    }

    /**
     * Calculate the projectile netPotential energy before launch and the exit velocity.
     *
     * @param position (mm) Distance between the face of the short range sensor and the back of the carrier
     * @param launchAngle (deg) Pitch of the device
     *
     * @return projectileBallistics
     */
    private fun calcProjectileBallistics(position: Double, launchAngle: Double): ProjectileBallistics {
        // Cap position to the max allowed
        val pos = min(position, _model.getMaxCarriagePosition())

        // Calculate energy loss due to gravity and friction
        val deltaPos = _model.getMaxCarriagePosition() - pos
        val kineticEnergyLoss = deltaPos * (forceOffset + (0.001 * _model.getTotalWeight() * CalcBallistics.ACCELERATION_OF_GRAVITY * sin(Math.toRadians(launchAngle)))
                + (0.001 * _model.getTotalWeight() * CalcBallistics.ACCELERATION_OF_GRAVITY * cos(Math.toRadians(launchAngle))) * frictionCoefficient)

        // Calculate net potential energy
        _projectileBallistics.netPotentialEnergy = max(0.0, (_model.getPotentialEnergyAtPosition(pos) - kineticEnergyLoss) * efficiency)

        // Calculate the velocity from the stored energy and apply an efficiency factor
        _projectileBallistics.velocity = CalcBallistics.getVelocity(_model.getTotalWeight(), _projectileBallistics.netPotentialEnergy)

        return _projectileBallistics
    }

    companion object {
        const val DEFAULT_FORCE_OFFSET = 0.5
        const val DEFAULT_EFFICIENCY_FACTOR = 0.68
        const val DEFAULT_FRICTION_COEFFICIENT = 0.0

        private const val DEFAULT_DELTA_TIME_SECONDS = 0.01
    }
}
