package com.android.app.utils.calculators

import kotlin.math.*

class Point(val x : Double, val y : Double)
class TangentPoints(val t1 : Point, val t2 : Point)

object CalcGeometry {
    /**
     * Get the tangent points where a line from the given point is tangent
     * to a circle with a given center point and radius.
     *
     * @param circleCenter
     * @param radius
     * @param point
     * @return tangentPoints
     */
    fun getTangentPointsOfLine(circleCenter : Point, radius : Double, point : Point) : TangentPoints? {
        val c = circleCenter
        val r = radius
        val p = point

        val dx = p.x - c.x
        val dy = p.y - c.y

        val dxr = -dy
        val dyr = dx

        val d = sqrt(dx.pow(2) + dy.pow(2))
        if (d >= r) {
            val rho = r / d
            val ad = rho.pow(2)
            val bd = rho * sqrt(1 - rho.pow(2))

            return TangentPoints(
                Point(c.x + ad * dx + bd * dxr, c.y + ad * dy + bd * dyr),
                Point(c.x + ad * dx - bd * dxr, c.y + ad * dy - bd * dyr)
            )
        }

        return null
    }
}