package com.android.greentech.plink.device


import android.content.Context
import androidx.preference.PreferenceManager
import com.android.greentech.plink.dataShared.DataShared
import com.android.greentech.plink.device.model.ModelData
import com.android.greentech.plink.utils.calculators.CalcBallistics
import com.android.greentech.plink.utils.calculators.CalcTrig
import com.android.greentech.plink.utils.converters.ConvertLength
import kotlin.math.*

class DeviceBallistics(context: Context, model: ModelData) {
    private val _prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val _model = model
    private var _pos = 0.0
    private var _efficiency = 0.0
    private var _flightTime = 0.0
    private var _impactDistance = 0.0
    private var _impactHeight = 0.0
    private val _efficiencyKey = "efficiency"

    inner class ImpactData(var distance: Double = 0.0, var height: Double = 0.0)

    var efficiency: Double
        set(value) {
            _efficiency = if(value in 0.0..1.0){
                value
            } else{
                1.0
            }
            // Store the value to preferences
            _prefs.edit().putFloat(_efficiencyKey, _efficiency.toFloat()).apply()
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
     * @param lensOffset (mm) Distance of the lens to the face of the long range sensor
     *
     * @return impactDistance (m)
     */
    fun getImpactData(position: Double, launchAngle: Double, launchHeight: Double, targetDistance: Double, lensOffset : Double = 0.0): ImpactData {
        // Cap the position to the max allowed position
        _pos = min(position, _model.getMaxCarriagePosition())

//        // Total distance traveled from current position to max
//        val distanceTraveled = ConvertLength.convert(ConvertLength.Unit.MM, ConvertLength.Unit.M, abs(_model.getMaxCarriagePosition() - _pos))
//
//        // Work (N/mm) required to move the (carriage + projectile) up an incline
//        val workRequiredUpIncline = CalcBallistics.getWorkRequiredUpIncline(_model.getTotalWeight(), launchAngle, distanceTraveled, 0.0)
//
//        // Net potential energy (N/mm) is the spring compression force over the carriage position distance minus the work required to move
//        // the (carriage + projectile) up an incline
//        val netPotentialEnergy = abs(_model.getPotentialEnergyAtPosition(_pos) - workRequiredUpIncline)

        // Get the approximate launch height which is the value of the (device height + max carriage pos + projectile center of gravity offset)
        // and then adjusted by the launch angle where the vertex starts at the device height.
        /*
                               /|
             Projectile --> ()/-|--- <---- Height offset
                             /  |
                        ____/)__|__-____________________
                             ^.__ Device Angle      ^.__ Device height
         */
        val heightOffset = ConvertLength.convert(ConvertLength.Unit.MM, ConvertLength.Unit.M, getProjectileHeightOffsetAtAngle(_model.getMaxCarriagePosition(), launchAngle))
        _adjustedLaunchHeight = (launchHeight + heightOffset)

        // Get the adjusted target distance which is the calculated target distance plus the distance behind the lens
        // at which the projectile will be launched.
        val offset = (lensOffset - _model.getProjectileCenterOfMassPosition(DataShared.device.model.getMaxCarriagePosition()))
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
        // Note: Passing weight and potential energy in terms of g and mJ instead of kg and J still gives velocity as m/s
//        _velocity = (CalcBallistics.getVelocity(_model.getTotalWeight(), netPotentialEnergy) * _efficiency)
        _velocity = (CalcBallistics.getVelocity(_model.getTotalWeight(), _model.getPotentialEnergyAtPosition(_pos)) * _efficiency)

        // If velocity <= zero then return zero for both impact distance and height
        if(_velocity <= 0.0){
            return ImpactData(0.0, 0.0)
        }

        // Calculate the impact distance
        _impactDistance = max(0.0, CalcBallistics.getImpactDistance(_velocity, _adjustedLaunchHeight, launchAngle))

        // Calculate the flight time of the projectile to find the impact height
        _flightTime = CalcBallistics.getFlightTime(_adjustedTargetDistance, launchAngle, _velocity)

        // Calculate the impact height
        _impactHeight = max(0.0, CalcBallistics.getImpactHeight(_adjustedLaunchHeight, launchAngle, _velocity, _flightTime))

        // Return the impact distance and height
        return ImpactData(_impactDistance, _impactHeight)
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
        private const val DEFAULT_EFFICIENCY_FACTOR = 0.70f
    }

    init {
        // Load the efficiency value on init
        _efficiency = _prefs.getFloat(_efficiencyKey, DEFAULT_EFFICIENCY_FACTOR).toDouble()
    }
}
