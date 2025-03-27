package path.utils.math

import path.utils.math.Transforms.AspectRatio.*
import path.utils.paths.Bounds
import kotlin.math.*

private typealias Matrix3 = DoubleArray

private operator fun Matrix3.get(row: Int, column: Int) = this[row * 3 + column]

private operator fun Matrix3.set(row: Int, column: Int, value: Double) { this[row * 3 + column] = value }

private operator fun Matrix3.times(that: Matrix3): Matrix3 = doubleArrayOf(
    this[0, 0] * that[0, 0] + this[0, 1] * that[1, 0] + this[0, 2] * that[2, 0],
    this[0, 0] * that[0, 1] + this[0, 1] * that[1, 1] + this[0, 2] * that[2, 1],
    this[0, 0] * that[0, 2] + this[0, 1] * that[1, 2] + this[0, 2] * that[2, 2],
    this[1, 0] * that[0, 0] + this[1, 1] * that[1, 0] + this[1, 2] * that[2, 0],
    this[1, 0] * that[0, 1] + this[1, 1] * that[1, 1] + this[1, 2] * that[2, 1],
    this[1, 0] * that[0, 2] + this[1, 1] * that[1, 2] + this[1, 2] * that[2, 2],
    this[2, 0] * that[0, 0] + this[2, 1] * that[1, 0] + this[2, 2] * that[2, 0],
    this[2, 0] * that[0, 1] + this[2, 1] * that[1, 1] + this[2, 2] * that[2, 1],
    this[2, 0] * that[0, 2] + this[2, 1] * that[1, 2] + this[2, 2] * that[2, 2],
)

object Transforms {
    fun identical() = matrix(
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0,
    )

    fun translate(tx: Double = 0.0, ty: Double = 0.0) = matrix(
        1.0, 0.0, tx,
        0.0, 1.0, ty,
        0.0, 0.0, 1.0,
    )

    fun scale(sx: Double = 1.0, sy: Double = sx, cx: Double = 0.0, cy: Double = 0.0): MatrixTransform {
        val scale = matrix(
            sx, 0.0, 0.0,
            0.0, sy, 0.0,
            0.0, 0.0, 1.0,
        )

        if (cx != 0.0 || cy != 0.0) {
            scale.preTranslate(-cx, -cy)
            scale.translate(cx, cy)
        }

        return scale
    }

    fun shear(shx: Double, shy: Double) = matrix(
        1.0, shx, 0.0,
        shy, 1.0, 0.0,
        0.0, 0.0, 1.0,
    )

    fun reflectAbout(l1: Vec2, l2: Vec2): MatrixTransform {
        val lx = l2.x - l1.x
        val ly = l2.y - l1.y
        val lx2 = lx * lx
        val ly2 = ly * ly
        val lSq = lx2 + ly2
        val reflect = matrix(
            (lx2 - ly2) / lSq,   (2 * lx * ly) / lSq, 0.0,
            (2 * lx * ly) / lSq, (ly2 - lx2) / lSq,   0.0,
            0.0,                 0.0,                 1.0,
        )

        if (l1 != Vec2()) {
            reflect.preTranslate(-l1.x, -l1.y)
            reflect.translate(l1.x, l1.y)
        }

        return reflect
    }

    fun rotate(theta: Double, cx: Double = 0.0, cy: Double = 0.0): MatrixTransform {
        val cos = cos(theta)
        val sin = sin(theta)
        val rotate = matrix(
            cos, -sin, 0.0,
            sin, cos, 0.0,
            0.0, 0.0, 1.0,
        )

        if (cx != 0.0 || cy != 0.0) {
            rotate.preTranslate(-cx, -cy)
            rotate.translate(cx, cy)
        }

        return rotate
    }

    fun rotateX(theta: Double, cx: Double = 0.0, cy: Double = 0.0): MatrixTransform {
        val cos = cos(theta)
        val sin = sin(theta)
        val rotate = matrix(
            1.0, 0.0, 0.0,
            0.0, cos, -sin,
            0.0, sin, cos,
        )

        if (cx != 0.0 || cy != 0.0) {
            rotate.preTranslate(-cx, -cy)
            rotate.translate(cx, cy)
        }

        return rotate
    }

    fun rotateY(theta: Double, cx: Double = 0.0, cy: Double = 0.0): MatrixTransform {
        val cos = cos(theta)
        val sin = sin(theta)
        val rotate = matrix(
            cos, 0.0, sin,
            0.0, 1.0, 0.0,
            -sin, 0.0, cos,
        )

        if (cx != 0.0 || cy != 0.0) {
            rotate.preTranslate(-cx, -cy)
            rotate.translate(cx, cy)
        }

        return rotate
    }

    enum class AspectRatio(
        internal val x: Boolean?,
        internal val y: Boolean?, // false - min, null - mid, true - max
        internal val slice: Boolean,
    ) {
        XMinYMinMeet(false, false, false),
        XMidYMinMeet(null, false, false),
        XMaxYMinMeet(true, false, false),

        XMinYMidMeet(false, null, false),
        XMidYMidMeet(null, null, false),
        XMaxYMidMeet(true, null, false),

        XMinYMaxMeet(false, true, false),
        XMidYMaxMeet(null, true, false),
        XMaxYMaxMeet(true, true, false),

        XMinYMinSlice(false, false, true),
        XMidYMinSlice(null, false, true),
        XMaxYMinSlice(true, false, true),

        XMinYMidSlice(false, null, true),
        XMidYMidSlice(null, null, true),
        XMaxYMidSlice(true, null, true),

        XMinYMaxSlice(false, true, true),
        XMidYMaxSlice(null, true, true),
        XMaxYMaxSlice(true, true, true),

        None(false, false, false)
    }

    fun rectToRect(a: Bounds, b: Bounds, aspectRatio: AspectRatio = None): MatrixTransform {
        if (aspectRatio == None) {
            val sx = b.w / a.w
            val sy = b.h / a.h
            val tx = b.x - a.x * sx
            val ty = b.y - a.y * sy

            return matrix(
                sx, 0.0, tx,
                0.0, sy, ty,
                0.0, 0.0, 1.0,
            )
        }

        val h = if (!aspectRatio.slice) min(b.w, b.h) else max(b.w, b.h)
        val w = (a.w / a.h) * h
        val x = when (aspectRatio.x) {
            false -> b.x
            null -> b.x + (b.w - w) / 2
            true -> b.right - w
        }
        val y = when (aspectRatio.y) {
            false -> b.y
            null -> b.y + (b.h - h) / 2
            true -> b.bottom - h
        }
        return rectToRect(a, Bounds(x, y, w, h))
    }

    fun polyToPoly(a: List<Vec2>, b: List<Vec2>): MatrixTransform? {
        fun transFor2(p: List<Vec2>, scale: Vec2) = matrix(
            (p[1].y - p[0].y) / scale.y,  (p[1].x - p[0].x) / scale.y,  p[0].x,
            (p[0].x - p[1].x) / scale.y,  (p[1].y - p[0].y) / scale.y,  p[0].y,
            0.0,                          0.0,                          1.0,
        )

        fun transFor3(p: List<Vec2>, scale: Vec2) = matrix(
            (p[2].x - p[0].x) / scale.x,  (p[1].x - p[0].x) / scale.y,  p[0].x,
            (p[2].y - p[0].y) / scale.x,  (p[1].y - p[0].y) / scale.y,  p[0].y,
            0.0,                          0.0,                          1.0,
        )

        fun transFor4(p: List<Vec2>, scale: Vec2): MatrixTransform? {
            val x0 = p[2].x - p[0].x
            val y0 = p[2].y - p[0].y
            val x1 = p[2].x - p[1].x
            val y1 = p[2].y - p[1].y
            val x2 = p[2].x - p[3].x
            val y2 = p[2].y - p[3].y

            val a1 = if (abs(x2) > abs(y2)) {
                val denom = (x1 * y2) / x2 - y1
                if (denom near 0.0)
                    return null

                (((x0 - x1) * y2 / x2) - y0 + y1) / denom
            } else {
                val denom = x1 - (y1 * x2) / y2
                if (denom near 0.0)
                    return null

                (x0 - x1 - ((y0 - y1) * x2) / y2) / denom
            }

            val a2 = if (abs(x1) > abs(y1)) {
                val denom = y2 - (x2 * y1) / x1
                if (denom near 0.0)
                    return null

                (y0 - y2 - ((x0 - x2) * y1) / x1) / denom
            } else {
                val denom = (y2 * x1) / y1 - x2
                if (denom near 0.0)
                    return null

                (((y0 - y2) * x1) / y1 - x0 + x2) / denom
            }

            return matrix(
                (a2 * p[3].x + p[3].x - p[0].x) / scale.x,  (a1 * p[1].x + p[1].x - p[0].x) / scale.y,  p[0].x,
                (a2 * p[3].y + p[3].y - p[0].y) / scale.x,  (a1 * p[1].y + p[1].y - p[0].y) / scale.y,  p[0].y,
                a2 / scale.x,                               a1 / scale.y,                               1.0,
            )
        }

        val notEnough = { throw IllegalArgumentException("Not enough points to make transform: a=$a, b=$b") }
        val count = if (a.size == b.size) a.size else notEnough()
        val transform = when (count) {
            0 -> return identical()
            1 -> return translate(b[0].x - a[0].x, b[0].y - a[0].y)
            2 -> ::transFor2
            3 -> ::transFor3
            4 -> ::transFor4
            else -> return null
        }

        val scale = run {
            val pt1 = a[1] - a[0]
            val y = pt1.length
            if (y near 0.0)
                return null

            val x = when (count) {
                2 -> { 1.0 }

                3 -> {
                    val pt2 = Vec2(
                        a[0].y - a[2].y,
                        a[2].x - a[0].x
                    )

                    pt1 dot pt2 / y
                }

                else -> {
                    val pt2 = Vec2(
                        a[0].y - a[3].y,
                        a[3].x - a[0].x
                    )

                    pt1 dot pt2 / y
                }
            }

            Vec2(x, y)
        }
        if (scale near Vec2())
            return null

        var temp1 = transform(a, scale) ?: return null
        val temp2 = temp1.invert() ?: return null
        temp1  = transform(b, scale) ?: return null
        return temp1.pre(temp2)
    }

    fun matrix(
        m00: Double = 1.0, m01: Double = 0.0, m02: Double = 0.0,
        m10: Double = 0.0, m11: Double = 1.0, m12: Double = 0.0,
        m20: Double = 0.0, m21: Double = 0.0, m22: Double = 1.0,
    ) = MatrixTransform(
        m00, m01, m02,
        m10, m11, m12,
        m20, m21, m22,
    )

    fun inverted(
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double
    ): MatrixTransform? {
        val det = m00 * (m11 * m22 - m12 * m21) - m01 * (m10 * m22 - m12 * m20) + m02 * (m10 * m21 - m11 * m20)
        if (det near 0.0)
            return null

        return matrix(
            +(m11 * m22 - m21 * m12) / det, -(m01 * m22 - m21 * m02) / det, +(m01 * m12 - m11 * m02) / det,
            -(m10 * m22 - m20 * m12) / det, +(m00 * m22 - m20 * m02) / det, -(m00 * m12 - m10 * m02) / det,
            +(m10 * m21 - m20 * m11) / det, -(m00 * m21 - m20 * m01) / det, +(m00 * m11 - m10 * m01) / det,
        )
    }
}

class MatrixTransform(vararg matrix: Double) {
    var matrix: Matrix3
        private set

    inline var m00: Double
        get() = matrix[0]
        set(v) { matrix[0] = v }
    inline var m01: Double
        get() = matrix[1]
        set(v) { matrix[1] = v }
    inline var m02: Double
        get() = matrix[2]
        set(v) { matrix[2] = v }
    inline var m10: Double
        get() = matrix[3]
        set(v) { matrix[3] = v }
    inline var m11: Double
        get() = matrix[4]
        set(v) { matrix[4] = v }
    inline var m12: Double
        get() = matrix[5]
        set(v) { matrix[5] = v }
    inline var m20: Double
        get() = matrix[6]
        set(v) { matrix[6] = v }
    inline var m21: Double
        get() = matrix[7]
        set(v) { matrix[7] = v }
    inline var m22: Double
        get() = matrix[8]
        set(v) { matrix[8] = v }

    val isIdentical get() = matrix.contentEquals(doubleArrayOf(
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0,))

    val isAffine: Boolean get() = m20 == 0.0 && m21 == 0.0

    val isTranslate get() = isAffine && m00 == m22 && m11 == m22 && m01 == 0.0 && m10 == 0.0

    val isScale get() = isAffine && m01 == 0.0 && m12 == 0.0 && m10 == 0.0 && m12 == 0.0

    val isRotate: Boolean get() {
        if (!isAffine || m02 != 0.0 || m12 != 0.0 || m01 != m11 || m01 != -m10)
            return false

        return sin(acos(m00)) near m10
    }

    val isRotateX: Boolean get() {
        if (m00 != 1.0 || m01 != 0.0 || m02 != 0.0 || m10 != 0.0 || m20 != 0.0 || m11 != m22 || m12 != -m20)
            return false

        return sin(acos(m11)) near m21
    }

    val isRotateY: Boolean get() {
        if (m01 != 0.0 || m01 != 0.0 || m11 != 1.0 || m12 != 0.0 || m21 != 0.0 || m00 != m22 || m02 != -m20)
            return false

        return sin(acos(m00)) near m02
    }

    // TODO: tests

    init {
        if (matrix.size != 9)
            throw IllegalArgumentException("Matrix 3x3 was expected")

        this.matrix = matrix
    }

    fun normalize(): MatrixTransform {
        val z = matrix[2, 2]
        if (z != 1.0) {
            matrix[0, 0] /= z
            matrix[0, 1] /= z
            matrix[0, 2] /= z
            matrix[1, 0] /= z
            matrix[1, 1] /= z
            matrix[1, 2] /= z
            matrix[2, 0] /= z
            matrix[2, 1] /= z
            matrix[2, 2] /= z
        }

        return this
    }

    fun pre(other: MatrixTransform): MatrixTransform {
        matrix *= other.matrix
        return this
    }

    fun preTranslate(tx: Double = 0.0, ty: Double = 0.0) = pre(Transforms.translate(tx, ty))

    fun preScale(sx: Double, sy: Double = sx) = pre(Transforms.scale(sx, sy))

    fun preShear(shx: Double, shy: Double) = pre(Transforms.shear(shx, shy))

    fun preRotate(theta: Double, cx: Double = 0.0, cy: Double = 0.0) = pre(Transforms.rotate(theta, cx, cy))

    fun preRotateX(theta: Double, cx: Double = 0.0, cy: Double = 0.0) = pre(Transforms.rotateX(theta, cx, cy))

    fun preRotateY(theta: Double, cx: Double = 0.0, cy: Double = 0.0) = pre(Transforms.rotateY(theta, cx, cy))

    fun preReflectAbout(l1: Vec2, l2: Vec2) = pre(Transforms.reflectAbout(l1, l2))

    fun post(other: MatrixTransform): MatrixTransform {
        matrix = other.matrix * matrix
        return this
    }

    fun translate(tx: Double = 0.0, ty: Double = 0.0) = post(Transforms.translate(tx, ty))

    fun scale(sx: Double, sy: Double = sx) = post(Transforms.scale(sx, sy))

    fun shear(shx: Double, shy: Double) = post(Transforms.shear(shx, shy))

    fun rotate(theta: Double, cx: Double = 0.0, cy: Double = 0.0) = post(Transforms.rotate(theta, cx, cy))

    fun rotateX(theta: Double, cx: Double = 0.0, cy: Double = 0.0) = post(Transforms.rotateX(theta, cx, cy))

    fun rotateY(theta: Double, cx: Double = 0.0, cy: Double = 0.0) = post(Transforms.rotateY(theta, cx, cy))

    fun reflectAbout(l1: Vec2, l2: Vec2) = post(Transforms.reflectAbout(l1, l2))

    @JvmName("transformCoords")
    fun transform(coords: Iterable<Double>): List<Double> {
        // / m00  m01  m02 \   / x \   / x * m00  +  y * m01  +  m02 \
        // | m10  m11  m12 | * | y | = | x * m10  +  y * m11  +  m12 |
        // \ m20  m21  m22 /   \ 1 /   \ x * m20  +  y * m21  +  m22 /

        var i = 0
        val new = coords.toMutableList()
        while (i + 1 < new.size) {
            val x = new[i]
            val y = new[i + 1]

            val v0 = m00 * x + m01 * y + m02
            val v1 = m10 * x + m11 * y + m12
            val v2 = m20 * x + m21 * y + m22

            new[i] = v0 / v2
            new[i + 1] = v1 / v2

            i += 2
        }

        return new
    }

    fun transform(points: Iterable<Vec2>) = points.map { transform(it) }

    fun transform(point: Vec2): Vec2 {
        val x = m00 * point.x + m01 * point.y + m02
        val y = m10 * point.x + m11 * point.y + m12
        val z = m20 * point.x + m21 * point.y + m22
        return Vec2(x / z, y / z)
    }

    fun invert() = Transforms.inverted(m00, m01, m02, m10, m11, m12, m20, m21, m22)

    override fun toString() = "Matrix[$m00, $m01, $m02][$m10, $m11, $m12][$m20, $m21, $m22]"

    override fun equals(other: Any?): Boolean {
        if (other !is MatrixTransform) return false
        return matrix.contentEquals(other.matrix)
    }

    override fun hashCode() = matrix.contentHashCode()
}