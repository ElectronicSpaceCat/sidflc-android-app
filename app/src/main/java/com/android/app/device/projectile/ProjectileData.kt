package com.android.app.device.projectile

/**
 * Projectile data
 *
 * @constructor
 * @param name
 * @param weight (mm)
 * @param diameter (mm)
 * @param drag
 */
class ProjectileData(
    name: String = "Projectile",
    weight: Double = 0.0,
    diameter: Double = 0.0,
    drag : Double = 0.0) {
    private var _name = name
    private var _weight = weight
    private var _diameter = diameter
    private var _drag = drag

    val name: String
        get() = _name
    fun setName(name: String) {
        _name = name
    }
    val weight: Double
        get() = _weight
    fun setWeight(weight: Double) {
        _weight = weight
    }
    val diameter: Double
        get() = _diameter
    fun setDiameter(diameter: Double) {
        _diameter = diameter
    }
    val drag: Double
        get() = _drag
    fun setDrag(drag: Double) {
        _drag = drag
    }
}
