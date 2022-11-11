package path.utils.math

import kotlin.math.acos
import kotlin.math.sqrt

data class Vec2(val x: Double = 0.0, val y: Double = 0.0) {
    operator fun plus(v: Vec2) = Vec2(x + v.x, y + v.y)

    operator fun minus(v: Vec2) = Vec2(x - v.x, y - v.y)

    operator fun times(s: Double) = Vec2(s * x, s * y)

    operator fun times(s: Int) = Vec2(s * x, s * y)

    operator fun div(s: Double) = Vec2(x / s, y / s)

    operator fun div(s: Int) = Vec2(x / s, y / s)

    operator fun unaryMinus() = Vec2(-x, -y)

    /** Dot product of this vector and another. */
    infix fun dot(v: Vec2) = x * v.x + y * v.y

    /** Magnitude of the cross product of this vector and another. */
    infix fun cross(v: Vec2) = x * v.y - y * v.x

    /** Returns the angle between two unit vectors. */
    infix fun angle(v: Vec2): Double {
        val sign = if (this cross v < 0) -1 else 1
        val dot = (this dot v).coerceIn(-1.0, 1.0)
        return sign * acos(dot)
    }

    override fun toString() = "($x, $y)"

    val lengthSq by lazy { x * x + y * y }

    val length by lazy { sqrt(lengthSq) }

    infix fun near(v: Vec2) = this distToSq v near 0.0

    fun normalize() = this / length
}

infix fun Vec2.distToSq(other: Vec2) = (other - this).lengthSq

infix fun Vec2.distTo(other: Vec2) = sqrt(distToSq(other))

fun Vec2.distToLineSq(a: Vec2, b: Vec2): Double {
    val e = b - a
    val p = this - a
    val dot = p dot e
    val projLenSq = dot * dot / e.lengthSq
    return (p.lengthSq - projLenSq).coerceAtLeast(0.0)
}

fun Vec2.distToLine(a: Vec2, b: Vec2) = sqrt(distToLineSq(a, b))

fun Vec2.distToSegmentSq(a: Vec2, b: Vec2): Double {
    val e = b - a
    var p = this - a

    var dot = p dot e
    val projectionLengthSq = if (dot <= 0.0) {
        0.0
    } else {
        p = e - p
        dot = p dot e
        if (dot <= 0.0) 0.0 else dot * dot / e.lengthSq
    }

    return (p.lengthSq - projectionLengthSq).coerceAtLeast(0.0)
}

fun Vec2.distToSegment(a: Vec2, b: Vec2) = sqrt(distToSegmentSq(a, b))

fun List<Vec2>.sum() = reduceOrNull { a, b -> a + b }.orNull()

fun List<Vec2>.average() = sum() / size

operator fun Double.times(s: Vec2) = Vec2(this * s.x, this * s.y)

operator fun Int.times(s: Vec2) = Vec2(this * s.x, this * s.y)

fun lerp(a: Vec2, b: Vec2, t: Double) = a + (b - a) * t

fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t

fun Vec2?.orNull() = this ?: Vec2()

fun Vec2.toCoordinates() = doubleArrayOf(x, y)

fun Collection<Vec2>.toCoordinates(): DoubleArray {
    val coordinates = DoubleArray(size * 2)
    var i = 0
    for (p in this) {
        coordinates[i    ] = p.x
        coordinates[i + 1] = p.y
        i += 2
    }
    return coordinates
}