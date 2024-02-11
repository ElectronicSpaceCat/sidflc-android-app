package com.android.app.device.model

import com.android.app.device.projectile.ProjectileData
import com.android.app.device.springs.Spring
import com.android.app.device.springs.SpringData
import com.android.app.utils.calculators.CalcMisc
import com.android.app.utils.calculators.CalcTrig
import kotlin.math.*

/**
 * @param name Model name
 * @param defaultSpringName Default spring that comes with the model
 * @param caseBodyLength Length of the case (mm)
 * @param studCenterToStudCenter Distance from left spring stud center to right spring stud center (mm)
 * @param sensorToStudCenter Distance from face of sensor to horizontal center of the spring stud (mm)
 * @param sensorToCarriageBackFace Distance from face of sensor to carriage back face (mm)
 * @param carriageSpringGripAngle Angle of the spring grip (deg)
 * @param carriageBackFaceToSpringPoint Distance from the back face of the carriage to the point where the springs meet (mm)
 * @param carriageBackFaceToCarriageSlotPoint Distance from the back face of the carriage to back point of the carriage pocket (mm)
 * @param carriageWeight Weight of the carriage (g)
 * @param springStudRadius Radius of the stud that the spring sits on (mm)
 * @param springSupportRadius Radius of the spring support that holds against the lower spring leg (mm)
 * @param springSupportAngleFromHorizontal Angle of the imaginary line between the spring stud center to the spring support center (deg)
 * @param studCenterToSpringSupportCenter Distance of the imaginary line between the spring stud center to the spring support center(mm)
 */
 class ModelData(
    name: String,
    defaultSpringName: Spring.Name,
    caseBodyLength: Double,
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
    private val _caseBodyLength = caseBodyLength
    private val _studCenterToStudCenter = studCenterToStudCenter
    private val _sensorToStudCenter = sensorToStudCenter
    private val _sensorToCarriageBackFace = sensorToCarriageBackFace
    private val _carriageSpringGripAngle = carriageSpringGripAngle
    private val _carriageBackFaceToSpringPoint = carriageBackFaceToSpringPoint
    private val _carriageBackFaceToCarriageSlotPoint = carriageBackFaceToCarriageSlotPoint
    private val _carriageWeight = carriageWeight
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

    private var _unloadedSpringAngle : Double = 0.0
    private var _carriageBackFaceToSpringPointAdj : Double = 0.0

    // Center point of the spring
    private var _cX = 0.0
    private var _cY = 0.0
    // Vertex point of the unloaded spring
    private var _pXunloaded = 0.0
    private var _pYunloaded = 0.0
    // Max x/y of the unloaded spring as a grid
    private var _xMax = 0.0
    private var _yMax = 0.0
    // Offset from the spring stud center to the spring center
    private var _xOffset = 0.0
    private var _yOffset = 0.0

    /**
     * Look up table with position and potential energy
     */
    private class TableData(var position: Double = 0.0, var potentialEnergy: Double = 0.0)
    private var _lookUpTable: ArrayList<TableData> = arrayListOf()

    val caseBodyLength : Double
        get() = _caseBodyLength

    val unloadedSpringAngle : Double
        get() = _unloadedSpringAngle

    val defaultSpringName : Spring.Name
        get() = _defaultSpringName

    inner class Point(val x : Double = 0.0, val y : Double = 0.0)

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
            _unloadedSpringAngle = 0.0
            _carriageBackFaceToSpringPointAdj = 0.0

            _lookUpTable.clear()

            return
        }

        // Get adjusted distance from carriage back face to the spring leg center when springs are loaded in the carriage
        _carriageBackFaceToSpringPointAdj = _carriageBackFaceToSpringPoint - CalcTrig.getSideGiven1Side2Angles(_spring!!.wireDiameter/2.0, 90.0 , _carriageSpringGripAngle/2.0)

        // Case dimension reference data
        val r1 = _springStudRadius
        val r2 = _spring!!.meanDiameter / 2.0
        val d1 = _spring!!.wireDiameter / 2.0
        val d2 = r1 + d1
        val d3 = r2 - d2

        // Hourglass 1
        val h1angleA = CalcTrig.getAngleAGivenSideASideC((_springStudRadius + _springSupportRadius + spring!!.wireDiameter), _studCenterToSpringSupportCenter)
        val h1angleB = 90.0 - h1angleA
        val h1angleC = CalcTrig.getAngleGiven2Angles(_springSupportAngleFromHorizontal, h1angleB)

        _unloadedSpringAngle = h1angleC

        // Hourglass 2
        val h2a1a2 = r2
        val h2b1 = d3
        val h2angleC = 90.0
        val h2angleB = h1angleC
        val h2angleA = CalcTrig.getAngleGiven2Angles(h2angleC, h2angleB)
        val h2a1 = CalcTrig.getSideGiven1Side2Angles(h2b1, h2angleA, h2angleB)
        val h2a2 = h2a1a2 - h2a1
        val h2b2 = CalcTrig.getSideGiven1Side2Angles(h2a2, h2angleB, h2angleC)
        val h2c1 = CalcTrig.getSideGiven1Side2Angles(h2a1, h2angleC, h2angleA)
        val h2c2 = CalcTrig.getSideGiven1Side2Angles(h2a2, h2angleA, h2angleC)
        val h2c1c2 = h2c1 + h2c2

        // Offsets from spring stud center to spring coil center
        _yOffset = CalcTrig.getSideGiven1Side2Angles(d3, h1angleC, 90.0)
        _xOffset = CalcTrig.getSideGiven1Side2Angles(d3, h2angleA, 90.0)

        // Point of spring vertex to vertical center when spring is unloaded
        _xMax = _studCenterToVerticalCenterLine + h2b2

        // Hourglass 3
        val h3c1 = CalcTrig.getSideGiven1Side2Angles(d3, h2angleA, h1angleC)
        val h3c2 = r2 - h3c1
//        val h3c1c2 = h3c1 + h3c2
//        val h3b1 = CalcTrig.getSideGiven1Side2Angles(h2b1, 90.0, h1angleC)
        val h3b2 = CalcTrig.getSideGiven1Side2Angles(h3c2, h2angleA, 90.0)
//        val h3a1 = d3
        val h3a2 = CalcTrig.getSideGiven1Side2Angles(h3b2, h1angleC, h2angleA)

        // Unloaded spring point
        _cX = h3a2 + _xOffset
        _cY = 0.0
        _pXunloaded = 0.0
        _pYunloaded = h2c1c2 - _yOffset
        val slopeRadius = (_pYunloaded-_cY)/(_pXunloaded-_cX)
        val slopeTangentLine = -1.0/slopeRadius

        // Height of spring vertex point to point where spring leg crosses vertical line when unloaded
        _yMax = (slopeTangentLine * (_xMax-_pXunloaded)) + _pYunloaded

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
     *
     * @return mass (g)
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
        if(_lookUpTable.size <= 0) {
            return 0.0
        }

        if (position !in 0.0.._lookUpTable[0].position) {
            return 0.0
        }

        // Search list for match
        for (i in 1.._lookUpTable.size) {
            // Position found?
            if (position >= _lookUpTable[i].position) {
                // Yes - Now interpolate between the two points
                return CalcMisc.interpolate(
                    position,
                    _lookUpTable[i].position,
                    _lookUpTable[i - 1].position,
                    _lookUpTable[i].potentialEnergy,
                    _lookUpTable[i - 1].potentialEnergy)
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

        val springMeanRadius = _spring!!.meanDiameter/2.0

        val pointUnloadedToLoaded = (_sensorToStudCenter + _yMax + _yOffset) - getSensorToSpringPoint(position)
        val pX = _xMax
        val pY = _yMax - pointUnloadedToLoaded
        val tangentPoint = getTangentPoint(springMeanRadius, _cX, _cY, pX, pY)
        val springMomentArmLength = sqrt((pX - tangentPoint.x).pow(2) + (pY - tangentPoint.y).pow(2))
        val springOppositeArmLength = sqrt((tangentPoint.x - pX).pow(2) + (tangentPoint.y - tangentPoint.y).pow(2))
        val springAdjacentArmLength = sqrt(springMomentArmLength.pow(2) - springOppositeArmLength.pow(2))

        val ref = sqrt((_pXunloaded - tangentPoint.x).pow(2) + (_pYunloaded - tangentPoint.y).pow(2))

        val springAngle = CalcTrig.getAngleGiven3Sides(ref, springMeanRadius, springMeanRadius)

        // Get resultant force acting on carriage from single spring
        val netForce = spring!!.getForceAtDegree(springAngle, springMomentArmLength)

        val angleRefToHorizontal = CalcTrig.getAngleGiven3Sides(springAdjacentArmLength, springMomentArmLength, springOppositeArmLength)

        // Return the vertical directional force
        return getForceInVerticalDirection(netForce, angleRefToHorizontal)
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
        if(_spring == null) return 0.0

        val springMeanRadius = _spring!!.meanDiameter/2.0

        val pointUnloadedToLoaded = (_sensorToStudCenter + _yMax + _yOffset) - getSensorToSpringPoint(position)
        val pX = _xMax
        val pY = _yMax - pointUnloadedToLoaded
        val tangentPoint = getTangentPoint(springMeanRadius, _cX, _cY, pX, pY)

        val ref = sqrt((_pXunloaded - tangentPoint.x).pow(2) + (_pYunloaded - tangentPoint.y).pow(2))

        return CalcTrig.getAngleGiven3Sides(ref, springMeanRadius, springMeanRadius)
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
     * Get tangent point where the active spring point to the spring
     */
    private fun getTangentPoint(radius : Double, cX : Double, cY : Double, pX : Double, pY : Double) : Point {
        val dx = pX - cX
        val dy = pY - cY
        val dxr = -1.0 * dy
        val dyr = dx
        val d = sqrt(dx.pow(2) + dy.pow(2))
        val rho = radius/d
        val ad = rho.pow(2)
        val bd = rho * sqrt(1 - ad)

        val tX = cX + ad * dx + bd * dxr
        val tY = cY + ad * dy + bd * dyr

        return Point(tX, tY)
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
