// from https://github.com/mickleness/pumpernickel/blob/master/src/main/java/com/pump/geom/ShapeBounds.java
package path.utils.paths

import path.utils.math.Vec2
import path.utils.paths.Command.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class Bounds(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var w: Double = 0.0,
    var h: Double = 0.0,
) {
    constructor(dot: Vec2) : this(dot.x, dot.y)

    constructor(start: Vec2, end: Vec2) : this(start) { add(end) }

    constructor(points: List<Vec2>) : this(points.first()) { points.forEach { add(it) } }

    inline val cx get() = x + w / 2

    inline val cy get() = y + h / 2

    inline val center get() = Vec2(cx, cy)

    inline var top
        get() = y
        set(v) { h += y - v; y = v }

    inline var left
        get() = x
        set(v) { w += x - v; x = v }

    inline var bottom
        get() = y + h
        set(v) { h = v - y }

    inline var right
        get() = x + w
        set(v) { w = v - x }

    inline var axis
        get() = left..right
        set(v) { left = v.start; right = v.endInclusive }

    inline var ordinate
        get() = top..bottom
        set(v) { top = v.start; bottom = v.endInclusive }

    inline val area get() = w * h

    fun contains(x: Double, y: Double) = x in axis && y in ordinate

    fun contains(p: Vec2) = contains(p.x, p.y)

    fun contains(x: Double, y: Double, w: Double, h: Double) = x > left && y > top && x + w < right && y + h < bottom

    fun contains(b: Bounds) = contains(b.x, b.y, b.w, b.h)

    fun add(x: Double, y: Double) {
        top = min(top, y)
        bottom = max(bottom, y)
        left = min(left, x)
        right = max(right, x)
    }

    fun add(p: Vec2) = add(p.x, p.y)

    fun add(other: Bounds) {
        left = min(left, other.top)
        right = max(right, other.right)
        top = min(top, other.top)
        bottom = max(bottom, other.bottom)
    }

    infix fun union(other: Bounds) = copy().also { it.add(other) }

    fun set(x: Double, y: Double, w: Double, h: Double) {
        this.x = x
        this.y = y
        this.w = w
        this.h = h
    }

    infix fun overlap(other: Bounds) =
        right > other.left && other.right > left && bottom > other.top && other.bottom > top
}

class EmptyPathException : RuntimeException()

internal fun computeBounds(path: Path): Bounds {
    var egles: DoubleArray? = null
    val topMaxX = 0
    val topMaxY = 1
    val rightMaxX = 2
    val rightMaxY = 3
    val bottomMaxX = 4
    val bottomMaxY = 5
    val leftMaxX = 6
    val leftMaxY = 7

    fun refreshSizes(x: Double, y: Double) {
        if (x < egles!![leftMaxX]) {
            egles!![leftMaxX] = x
            egles!![leftMaxY] = y
        }
        if (y < egles!![topMaxY]) {
            egles!![topMaxX] = x
            egles!![topMaxY] = y
        }
        if (x > egles!![rightMaxX]) {
            egles!![rightMaxX] = x
            egles!![rightMaxY] = y
        }
        if (y > egles!![bottomMaxY]) {
            egles!![bottomMaxX] = x
            egles!![bottomMaxY] = y
        }
    }

    // A, B, C, and D in the equation x = a*t^3+b*t^2+c*t+d
    // or A, B, and C in the equation x = a*t^2+b*t+c
    val xCoeff = DoubleArray(4)
    val yCoeff = DoubleArray(4)

    path.simplify().iteratePath { command, last, _, _ ->
        if (command !is MoveTo && command !is Close) {
            if (egles == null) {
                egles = doubleArrayOf(
                    last.x, last.y, last.x, last.y,
                    last.x, last.y, last.x, last.y
                )
            } else {
                refreshSizes(last.x, last.y)
            }

            when {
                command is LineTo -> refreshSizes(command.x, command.y)

                command is QuadTo -> {
                    // check the end point
                    refreshSizes(command.x, command.y)

                    // find the extrema
                    xCoeff[0] = last.x - 2 * command.x1 + command.x
                    xCoeff[1] = -2 * last.x + 2 * command.x1
                    xCoeff[2] = last.x
                    yCoeff[0] = last.y - 2 * command.y1 + command.y
                    yCoeff[1] = -2 * last.y + 2 * command.y1
                    yCoeff[2] = last.y

                    // x = a*t^2+b*t+c
                    // dx/dt = 0 = 2*a*t+b
                    // t = -b/(2a)
                    var t = -xCoeff[1] / (2 * xCoeff[0])
                    if (t > 0 && t < 1) {
                        val x = xCoeff[0] * t * t + xCoeff[1] * t + xCoeff[2]
                        if (x < egles!![leftMaxX]) {
                            egles!![leftMaxX] = x
                            egles!![leftMaxY] = yCoeff[0] * t * t + yCoeff[1] * t + yCoeff[2]
                        }
                        if (x > egles!![rightMaxX]) {
                            egles!![rightMaxX] = x
                            egles!![rightMaxY] = yCoeff[0] * t * t + yCoeff[1] * t + yCoeff[2]
                        }
                    }
                    t = -yCoeff[1] / (2 * yCoeff[0])
                    if (t > 0 && t < 1) {
                        val y = yCoeff[0] * t * t + yCoeff[1] * t + yCoeff[2]
                        if (y < egles!![topMaxY]) {
                            egles!![topMaxX] = xCoeff[0] * t * t + xCoeff[1] * t + xCoeff[2]
                            egles!![topMaxY] = y
                        }
                        if (y > egles!![bottomMaxY]) {
                            egles!![bottomMaxX] = xCoeff[0] * t * t + xCoeff[1] * t + xCoeff[2]
                            egles!![bottomMaxY] = y
                        }
                    }
                }

                command is CubicTo -> {
                    refreshSizes(command.x, command.y)

                    xCoeff[0] = -last.x + 3 * command.x1 - 3 * command.x2 + command.x
                    xCoeff[1] = 3 * last.x - 6 * command.x1 + 3 * command.x2
                    xCoeff[2] = -3 * last.x + 3 * command.x1
                    xCoeff[3] = last.x
                    yCoeff[0] = -last.y + 3 * command.y1 - 3 * command.y2 + command.y
                    yCoeff[1] = 3 * last.y - 6 * command.y1 + 3 * command.y2
                    yCoeff[2] = -3 * last.y + 3 * command.y1
                    yCoeff[3] = last.y

                    // x = a*t*t*t+b*t*t+c*t+d
                    // dx/dt = 3*a*t*t+2*b*t+c
                    // t = [-B+-sqrt(B^2-4*A*C)]/(2A)
                    // A = 3*a
                    // B = 2*b
                    // C = c
                    // t = (-2*b+-sqrt(4*b*b-12*a*c)]/(6*a)
                    var det = (4 * xCoeff[1] * xCoeff[1] - 12 * xCoeff[0] * xCoeff[2])
                    if (det < 0) {
                        // there are no solutions! nothing to do here
                    } else if (det == 0.0) {
                        // there is 1 solution
                        val t = -2 * xCoeff[1] / (6 * xCoeff[0])
                        if (t > 0 && t < 1) {
                            val x = xCoeff[0] * t * t * t + xCoeff[1] * t * t + xCoeff[2] * t + xCoeff[3]
                            if (x < egles!![leftMaxX]) {
                                egles!![leftMaxX] = x
                                egles!![leftMaxY] = yCoeff[0] * t * t * t + yCoeff[1] * t * t + yCoeff[2] * t + yCoeff[3]
                            }
                            if (x > egles!![rightMaxX]) {
                                egles!![rightMaxX] = x
                                egles!![rightMaxY] = (yCoeff[0] * t * t * t) + yCoeff[1] * t * t + yCoeff[2] * t + yCoeff[3]
                            }
                        }
                    } else {
                        // there are 2 solutions:
                        det = sqrt(det)
                        var t = (-2 * xCoeff[1] + det) / (6 * xCoeff[0])
                        if (t > 0 && t < 1) {
                            val x = xCoeff[0] * t * t * t + xCoeff[1] * t * t + xCoeff[2] * t + xCoeff[3]
                            if (x < egles!![leftMaxX]) {
                                egles!![leftMaxX] = x
                                egles!![leftMaxY] = yCoeff[0] * t * t * t + yCoeff[1] * t * t + yCoeff[2] * t + yCoeff[3]
                            }
                            if (x > egles!![rightMaxX]) {
                                egles!![rightMaxX] = x
                                egles!![rightMaxY] = (yCoeff[0] * t * t * t) + yCoeff[1] * t * t + yCoeff[2] * t + yCoeff[3]
                            }
                        }
                        t = (-2 * xCoeff[1] - det) / (6 * xCoeff[0])
                        if (t > 0 && t < 1) {
                            val x = xCoeff[0] * t * t * t + xCoeff[1] * t * t + xCoeff[2] * t + xCoeff[3]
                            if (x < egles!![leftMaxX]) {
                                egles!![leftMaxX] = x
                                egles!![leftMaxY] = yCoeff[0] * t * t * t + yCoeff[1] * t * t + yCoeff[2] * t + yCoeff[3]
                            }
                            if (x > egles!![rightMaxX]) {
                                egles!![rightMaxX] = x
                                egles!![rightMaxY] = (yCoeff[0] * t * t * t) + yCoeff[1] * t * t + yCoeff[2] * t + yCoeff[3]
                            }
                        }
                    }
                    det = (4 * yCoeff[1] * yCoeff[1] - 12 * yCoeff[0] * yCoeff[2])
                    if (det < 0) {
                        // there are no solutions! nothing to do here
                    } else if (det == 0.0) {
                        // there is 1 solution
                        val t = -2 * yCoeff[1] / (6 * yCoeff[0])
                        if (t > 0 && t < 1) {
                            val y = yCoeff[0] * t * t * t + yCoeff[1] * t * t + yCoeff[2] * t + yCoeff[3]
                            if (y < egles!![topMaxY]) {
                                egles!![topMaxX] = xCoeff[0] * t * t * t + xCoeff[1] * t * t + xCoeff[2] * t + xCoeff[3]
                                egles!![topMaxY] = y
                            }
                            if (y > egles!![bottomMaxY]) {
                                egles!![bottomMaxX] = (xCoeff[0] * t * t * t) + xCoeff[1] * t * t + xCoeff[2] * t + xCoeff[3]
                                egles!![bottomMaxY] = y
                            }
                        }
                    } else {
                        // there are 2 solutions:
                        det = sqrt(det)
                        var t = (-2 * yCoeff[1] + det) / (6 * yCoeff[0])
                        if (t > 0 && t < 1) {
                            val y = yCoeff[0] * t * t * t + yCoeff[1] * t * t + yCoeff[2] * t + yCoeff[3]
                            if (y < egles!![topMaxY]) {
                                egles!![topMaxX] = xCoeff[0] * t * t * t + xCoeff[1] * t * t + xCoeff[2] * t + xCoeff[3]
                                egles!![topMaxY] = y
                            }
                            if (y > egles!![bottomMaxY]) {
                                egles!![bottomMaxX] = (xCoeff[0] * t * t * t) + xCoeff[1] * t * t + xCoeff[2] * t + xCoeff[3]
                                egles!![bottomMaxY] = y
                            }
                        }
                        t = (-2 * yCoeff[1] - det) / (6 * yCoeff[0])
                        if (t > 0 && t < 1) {
                            val y = yCoeff[0] * t * t * t + yCoeff[1] * t * t + yCoeff[2] * t + yCoeff[3]
                            if (y < egles!![topMaxY]) {
                                egles!![topMaxX] = xCoeff[0] * t * t * t + xCoeff[1] * t * t + xCoeff[2] * t + xCoeff[3]
                                egles!![topMaxY] = y
                            }
                            if (y > egles!![bottomMaxY]) {
                                egles!![bottomMaxX] = (xCoeff[0] * t * t * t) + xCoeff[1] * t * t + xCoeff[2] * t + xCoeff[3]
                                egles!![bottomMaxY] = y
                            }
                        }
                    }
                }
            }
        }
    }

    val points = egles ?: throw EmptyPathException()
    return Bounds(
        x = points[leftMaxX],
        y = points[topMaxY],
        w = points[rightMaxX] - points[leftMaxX],
        h = points[bottomMaxY] - points[topMaxY]
    )
}