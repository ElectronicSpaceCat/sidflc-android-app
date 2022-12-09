package com.android.greentech.plink.device.model

import com.android.greentech.plink.device.projectile.ProjectileData
import com.android.greentech.plink.device.springs.Spring
import com.android.greentech.plink.device.springs.SpringData
import com.android.greentech.plink.utils.calculators.CalcTrig
import kotlin.math.*

/**
 * @param name Model name
 * @param defaultSpringName Default spring that comes with the model
 * @param studCenterToStudCenter Distance from left spring stud center to right spring stud center
 * @param sensorToStudCenter Distance from face of sensor to horizontal center of the spring stud
 * @param sensorToCarriageBackFace Distance from face of sensor to carriage back face
 * @param carriageSpringGripAngle Angle of the spring grip
 * @param carriageBackFaceToSpringPoint Distance from the back face of the carriage to the point where the springs meet
 * @param carriageBackFaceToCarriageSlotPoint Distance from the back face of the carriage to back point of the carriage pocket
 * @param carriageWeight Weight of the carriage in grams
 * @param springStudRadius
 * @param springSupportRadius
 * @param springSupportAngleFromHorizontal
 * @param studCenterToSpringSupportCenter
 */
 class ModelData(
    name: String,
    defaultSpringName: Spring.Name,
    studCenterToStudCenter: Double,
    sensorToStudCenter: Double,
    sensorToCarriageBackFace: Double,
    carriageSpringGripAngle : Double,
    carriageBackFaceToSpringPoint : Double,
    carriageBackFaceToCarriageSlotPoint : Double,
    carriageWeight: Double,
    springStudRadius : Double,
    springSupportRadius : Double,
    springSupportAngleFromHorizontal : Double,
    studCenterToSpringSupportCenter : Double)
{
    /**
     * Device model constants
     */
    private val _name = name
    private val _defaultSpringName = defaultSpringName
    private val _studCenterToStudCenter = studCenterToStudCenter // mm
    private val _sensorToStudCenter = sensorToStudCenter // mm
    private val _sensorToCarriageBackFace = sensorToCarriageBackFace // mm
    private val _carriageSpringGripAngle = carriageSpringGripAngle // deg
    private val _carriageBackFaceToSpringPoint = carriageBackFaceToSpringPoint // mm
    private val _carriageBackFaceToCarriageSlotPoint = carriageBackFaceToCarriageSlotPoint // mm
    private val _carriageWeight = carriageWeight // g
    private val _springStudRadius = springStudRadius
    private val _springSupportRadius = springSupportRadius
    private val _springSupportAngleFromHorizontal = springSupportAngleFromHorizontal
    private val _studCenterToSpringSupportCenter = studCenterToSpringSupportCenter

    /**
     * Device model calculated dependencies that are constant
     */
    private val _studCenterToVerticalCenterLine: Double = (_studCenterToStudCenter / 2.0)
    private val _resolutionSize: Double = 1.0 // Resolution in mm to generate the look up table

    val name: String
        get() = _name

    /**
     * Device model calculated dependencies that are dependent on spring and projectile used
     */
    private var _spring : SpringData ?= null
    private var _projectile : ProjectileData ?= null

    private var _projectileOffset : Double = 0.0
    private var _totalWeight : Double = 0.0 // Total weight of (carriage + projectile)

    private var _unloadedRefAngle : Double = 0.0 // Used as base reference
    private var _unloadedLeverArmLength : Double = 0.0 // Used as base reference
    private var _unloadedSpringAngle : Double = 0.0
    private var _sensorToSpringPointUnloaded : Double = 0.0
    private var _carriageBackFaceToSpringPointAdj : Double = 0.0

    /**
     * Look up table with position and potential energy
     */
    private class TableData(var position: Double = 0.0, var potentialEnergy: Double = 0.0)
    private var _lookUpTable: ArrayList<TableData> = arrayListOf()

    val unloadedSpringAngle : Double
        get() = _unloadedSpringAngle

    val defaultSpringName : Spring.Name
        get() = _defaultSpringName

    /**
     * Set the spring and updates spring related parameters
     *
     * @param spring
     */
    fun setSpring(spring: SpringData?){
        // Set spring data
        _spring = spring

        // Reset data if spring is null or both unload ref values are not valid
        if(_spring == null) {
            _unloadedRefAngle = 0.0
            _unloadedLeverArmLength = 0.0
            _unloadedSpringAngle = 0.0
            _sensorToSpringPointUnloaded = 0.0
            _carriageBackFaceToSpringPointAdj = 0.0

            _lookUpTable.clear()

            return
        }

        // Get adjusted distance from carriage back face to the spring leg center when springs are loaded in the carriage
        _carriageBackFaceToSpringPointAdj = _carriageBackFaceToSpringPoint - CalcTrig.getSideGiven1Side2Angles(_spring!!.wireDiameter/2.0, 90.0 , _carriageSpringGripAngle/2.0)

        // Set the distance from sensor face to the horizontal line where the spring pivot point sits
        val sensorToStudCenterPlusSpringMeanRadius = (_sensorToStudCenter + ((_spring!!.outerDiameter + _spring!!.wireDiameter) / 2.0))

        // Get the unloaded spring angle based on case dimensions and how the spring sits, also depends on wire diameter
        val angleLoc = 90.0 - CalcTrig.getAngleAGivenSideASideC((_springStudRadius + _springSupportRadius + _spring!!.wireDiameter), _studCenterToSpringSupportCenter)
        _unloadedSpringAngle = (180.0 - _springSupportAngleFromHorizontal - angleLoc)

        // Calculate references needed to calculate the look up table
        val angleA = 90.0
        val angleC = _unloadedSpringAngle
        val angleB = CalcTrig.getAngleGiven2Angles(angleA, angleC)
        val sideB = _studCenterToVerticalCenterLine
        val sideC = CalcTrig.getSideGiven1Side2Angles(sideB, angleC, angleB)
        val sideA = CalcTrig.getSideGiven1Side2Angles(sideB, angleA, angleB)
        _unloadedRefAngle = angleB
        _unloadedLeverArmLength = sideA
        _sensorToSpringPointUnloaded = (sideC + sensorToStudCenterPlusSpringMeanRadius)

        // Regenerate potential energy table
        generatePotentialEnergyTable()
    }

    /**
     * Get spring data
     */
    val spring: SpringData?
        get() = _spring

    /**
     * Set projectile and projectile related parameters
     *
     * @param projectile
     */
    fun setProjectile(projectile: ProjectileData?){
        // Set the projectile data
        _projectile = projectile

        if(_projectile == null){
            _totalWeight = _carriageWeight
            _projectileOffset = 0.0
            return
        }

        // Set the total weight
        _totalWeight = (projectile!!.weight + _carriageWeight)
        // Set the total height offset
        _projectileOffset = calcProjectileOffset(projectile.diameter)
    }

    /**
     * Get projectile data
     */
    val projectile: ProjectileData?
        get() = _projectile

    /**
     * Get max carriage position
     * @return maxPosition (mm)
     */
    fun getMaxCarriagePosition() : Double{
        return _sensorToCarriageBackFace
    }

    /**
     * Get the position of the projectile's center of mass.
     * (Distance from sensor face to projectile center)
     *
     * @param position
     * @return projectilePosition (mm)
     */
    fun getProjectileCenterOfMassPosition(position: Double): Double{
        return (position + _projectileOffset)
    }

    /**
     * Get total weight which combines the carriage weight with the projectile weight
     */
    fun getTotalWeight(): Double{
        return _totalWeight
    }

    /**
     * Get the stored potential energy at carriage position
     *
     * @param position
     * @return potential energy (N-mm)
     */
    fun getPotentialEnergyAtPosition(position: Double): Double{
        if(_lookUpTable.size > 0) {
            // Get potential energy at position if within range
            if (position in 0.0.._lookUpTable[0].position) {
                // Search list for match
                for (i in 1.._lookUpTable.size) {
                    // Position found?
                    if (position >= _lookUpTable[i].position) {
                        // Yes - Now interpolate between the two points
                        return _lookUpTable[i - 1].potentialEnergy +
                                ((position - _lookUpTable[i - 1].position) * (_lookUpTable[i].potentialEnergy - _lookUpTable[i - 1].potentialEnergy)) /
                                (_lookUpTable[i].position - _lookUpTable[i - 1].position)

                    }
                }
            }
        }
        return 0.0
    }

    /**
     * Get total forward force which is the forward force of one
     * spring multiplied by 2 since there are two springs in the device
     */
    private fun getTotalForwardForce(position: Double) : Double{
        return (2.0 * getForwardForce(position))
    }

    /**
     * Get total the forward force acting on the carriage from the springs
     * which depends on the position of the carriage
     */
    private fun getForwardForce(position: Double) : Double{
        if(_spring == null) return 0.0

        // Get reference length to complete triangle
        val springPointLoadedToSpringPointUnloaded = (_sensorToSpringPointUnloaded - getSensorToSpringPoint(position))
        // Get spring leg length
        val springLeverArmLength = CalcTrig.getSideGiven2Sides1Angle(_unloadedLeverArmLength, springPointLoadedToSpringPointUnloaded, _unloadedRefAngle)
        // Get spring angle
        val springAngle = CalcTrig.getAngleGiven3Sides(springPointLoadedToSpringPointUnloaded, _unloadedLeverArmLength, springLeverArmLength)
        // Get resultant force acting on carriage from single spring
        val netForce = spring!!.getForceAtDegree(springAngle, springLeverArmLength)
        // Return the vertical directional force
        return getForceInVerticalDirection(netForce, abs(_unloadedSpringAngle - springAngle))
    }

    /**
     * Get the active spring angle which
     * is calculated based on the carriage
     * position.
     *
     * @param position (mm)
     * @return activeAngle (deg)
     */
    fun getSpringAngleAtPosition(position: Double): Double {
        val pos = min(position, _sensorToCarriageBackFace)
        // Get reference length to complete triangle
        val springPointLoadedToSpringPointUnloaded = (_sensorToSpringPointUnloaded - getSensorToSpringPoint(pos))
        // Get spring leg length
        val springLeverArmLength = CalcTrig.getSideGiven2Sides1Angle(_unloadedLeverArmLength, springPointLoadedToSpringPointUnloaded, _unloadedRefAngle)
        // Get spring angle
        return CalcTrig.getAngleGiven3Sides(springPointLoadedToSpringPointUnloaded, _unloadedLeverArmLength, springLeverArmLength)
    }

    /**
     * Get the distance from the back face of the carriage
     * to the center point of the projectile.
     */
    private fun calcProjectileOffset(projectileDiameter: Double): Double{
        // Get height offset from projectile
        val positionOffsetFromProjectile = CalcTrig.getSideGiven1Side2Angles((projectileDiameter / 2.0),90.0, 45.0)
        // Set the total height offset
        return (positionOffsetFromProjectile + _carriageBackFaceToCarriageSlotPoint)
    }

    /**
     * Get distance from sensor face to approximated center point of the spring leg when the
     * springs are loaded in the carriage
     */
    private fun getSensorToSpringPoint(position: Double): Double{
        return (position + _carriageBackFaceToSpringPointAdj)
    }

    /**
     * Get the force in the vertical direction
     */
    private fun getForceInVerticalDirection(netForce: Double, springAngle: Double): Double {
        return (netForce * cos(Math.toRadians(springAngle)))
    }

    /**
     * Generates a look-up-table for PotentialEnergy/position
     * over a range of resolution size to sensorToCarriageBackFace
     */
    private fun generatePotentialEnergyTable(){
        var lastPotentialEnergyValue = 0.0
        var lastPositionValue = _sensorToCarriageBackFace
        // Clear tables
        _lookUpTable.clear()
        // Set initial condition
        _lookUpTable.add(TableData(lastPositionValue, lastPotentialEnergyValue))
        // Populate the force table

        while(lastPositionValue > 0.0){
            // Decrement the position by the resolution size
            lastPositionValue -= _resolutionSize
            // Integrate the force value over position
            lastPotentialEnergyValue += getTotalForwardForce(lastPositionValue)
            // Add data to table
            _lookUpTable.add(TableData(lastPositionValue, lastPotentialEnergyValue))
        }
    }
}
