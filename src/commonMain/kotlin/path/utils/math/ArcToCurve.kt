package path.utils.math

import path.utils.beziers.Bezier
import path.utils.beziers.mapToBezier
import kotlin.math.*

internal fun arcToCurves(
    p1: Vec2,
    p2: Vec2,
    r: Vec2,
    phi: Double,
    largeArc: Boolean,
    sweep: Boolean
): List<Bezier> {
    if (p1 == p2) {
        // Start and end points are the same so arc is invisible.
        return emptyList()
    } else if (r == Vec2()) {
        // Either radius is zero, treat arc as line.
        return listOf(Bezier(p1, p2))
    }

    val cos = cos(phi)
    val sin = sin(phi)

    // Step 1: Move ellipse so origin is middle point between start and end points.
    // Also rotate it to line up ellipse axes with the XY axes.
    val x1p = cos * (p1.x - p2.x) / 2 + sin * (p1.y - p2.y) / 2
    val y1p = -sin * (p1.x - p2.x) / 2 + cos * (p1.y - p2.y) / 2

    var rx = abs(r.x)
    var ry = abs(r.y)
    val rxSq = rx * rx
    val rySq = ry * ry
    val x1pSq = x1p * x1p
    val y1pSq = y1p * y1p

    // Compensate out-of-range radii
    val lambda = sqrt(x1pSq / rxSq + y1pSq / rySq)
    if (lambda > 1) {
        rx *= lambda
        ry *= lambda
    }

    // Get arc center coordinates and angles. Step 1 is was done previously.
    // More info at: https://www.w3.org/TR/SVG11/implnote.html#ArcImplementationNotes

    // Step 2: Compute coordinates of the center in this new coordinate system.
    val t = rxSq * y1pSq + rySq * x1pSq
    var radicant = max(0.0, (rxSq * rySq - t) / t)
    radicant = sqrt(radicant) * if (largeArc == sweep) -1 else 1

    val cxp = radicant * rx / ry * y1p
    val cyp = radicant * -ry / rx * x1p

    // Step 3: Transform back to get coordinates of center in original coordinate system.
    val cx = cos * cxp - sin * cyp + (p1.x + p2.x) / 2
    val cy = sin * cxp + cos * cyp + (p1.y + p2.y) / 2

    // Step 4: compute start angle and extent angle.
    val v1 = Vec2((x1p - cxp) / rx, (y1p - cyp) / ry)
    val v2 = Vec2(-(x1p + cxp) / rx, -(y1p + cyp) / ry)

    val startAngle = Vec2(1.0, 0.0) angle v1

    var extent = v1 angle v2
    if (!sweep && extent > 0) extent -= TAU
    if (sweep && extent < 0) extent += TAU

    // Split arc into multiple segments, each less than 90 degrees.
    val count = ceil(abs(extent) / (TAU / 4)).toInt().coerceAtLeast(1)
    val segmentLength = extent / count

    return List(count) {
        /**
         * Approximate an arc of the unit circle with a cubic bezier curve.
         * See: [http://math.stackexchange.com/questions/873224].
         */
        val startAngle1 = startAngle + it * segmentLength
        val alpha = 4.0 / 3.0 * tan(segmentLength / 4)
        val start = Vec2(cos(startAngle1), sin(startAngle1))
        val end = Vec2(cos(startAngle1 + segmentLength), sin(startAngle1 + segmentLength))
        val c1 = Vec2(start.x - start.y * alpha, start.y + start.x * alpha)
        val c2 = Vec2(end.x + end.y * alpha, end.y - end.x * alpha)

        val curve = Bezier(start, c1, c2, end)

        curve.mapToBezier { p ->
            val x = p.x * rx
            val y = p.y * ry
            val xp = cos * x - sin * y
            val yp = sin * x + cos * y
            Vec2(xp + cx, yp + cy)
        }
    }
}