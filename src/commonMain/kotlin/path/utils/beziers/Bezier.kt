package path.utils.beziers

import path.utils.math.*
import path.utils.math.det
import path.utils.math.factorial
import path.utils.math.implictize
import path.utils.paths.*
import kotlin.math.*

class NoArcLengthParametrizationException(msg: String) : Exception(msg)

open class Bezier(val points: List<Vec2>) : Cloneable {
    companion object {
        fun tryMerge(left: Bezier, right: Bezier): Bezier? {
            val (t, merged) = when {
                left.end near right.start -> left.antiSplit(right) ?: return null
                right.end near left.start -> right.antiSplit(left) ?: return null
                else -> return null
            }

            return merged.takeIf { t nearIn 0.0..1.0 }
        }

        internal fun eval(bezier: List<Double>, t: Double): Double {
            fun List<Double>.div() = zipWithNext { p1, p2 -> lerp(p1, p2, t) }
            val curves = generateSequence(bezier) { it.div().takeIf { it.isNotEmpty() } }
            return curves.last().single()
        }

        internal fun eval(bezier: List<Vec2>, t: Double): Vec2 {
            fun List<Vec2>.div() = zipWithNext { p1, p2 -> lerp(p1, p2, t) }
            val curves = generateSequence(bezier) { it.div().takeIf { it.isNotEmpty() } }
            return curves.last().single()
        }
    }

    constructor(vararg coords: Double) : this(coords.asIterable())

    constructor(coords: Iterable<Double>) : this(coords.chunked(2) { Vec2(it[0], it[1]) })

    constructor(vararg points: Vec2) : this(points.asList())

    inline val start get() = points.first()

    inline val end get() = points.last()

    inline val count get() = points.size

    inline val order get() = count - 1

    operator fun get(i: Int): Vec2 = points[i]

    operator fun component1() = this[0]
    operator fun component2() = this[1]
    operator fun component3() = this[2]
    operator fun component4() = this[3]

    open val reversed: Bezier by lazy { ReversedBezier(this) }

    fun x(t: Double) = when {
        t == 0.0 -> start.x
        t == 1.0 -> end.x
        xPolyComputed -> xPoly.eval(t)
        else -> eval(xCurve, t)
    }

    fun y(t: Double) = when {
        t == 0.0 -> start.y
        t == 1.0 -> end.y
        yPolyComputed -> yPoly.eval(t)
        else -> eval(yCurve, t)
    }

    fun point(t: Double) = Vec2(x(t), y(t))

    inline val xCurve get() = map { it.x }

    inline val yCurve get() = map { it.y }

    private fun coefficient(bezier: List<Double>, index: Int): Double {
        val order = bezier.size - 1

        if (index < 0)
            throw IndexOutOfBoundsException("index of coefficient ($index) must be positive")
        if (index > order)
            throw IndexOutOfBoundsException("$index'th coefficient was requested but bezier order is only $order")

        return when (order) {
            0 -> bezier.first()

            1 -> when (index) {
                0 -> bezier[1] - bezier[0]
                else -> bezier[0]
            }

            2 -> when (index) {
                0 -> bezier[0] - bezier[1] * 2 + bezier[2]
                1 -> -bezier[0] * 2 + bezier[1] * 2
                else -> bezier[0]
            }

            3 -> when (index) {
                0 -> -bezier[0] + bezier[1] * 3 - bezier[2] * 3 + bezier[3]
                1 -> bezier[0] * 3 - bezier[1] * 6 + bezier[2] * 3
                2 -> -bezier[0] * 3 + bezier[1] * 3
                else -> bezier[0]
            }

            else -> {
                fun List<Int>.product() = reduceOrNull { a, b -> a * b} ?: 1

                val j = order - index
                val multi = List(j) { m -> bezier.size - 1 - m }.product()
                val s = List(j + 1) { i ->
                    val sign = if ((i + j).isEven) 1 else -1
                    val denom = factorial(i) * factorial(j - i)
                    val p = bezier[i]

                    p * sign / denom
                }

                s.sum() * multi
            }
        }
    }

    private val _XPoly = lazy { Polynomial(List(count) { coefficient(xCurve, it) }) }
    val xPolyComputed get() = _XPoly.isInitialized()
    val xPoly by _XPoly
    val dxPoly get() = xPoly.derivative

    private val _YPoly = lazy { Polynomial(List(count) { coefficient(yCurve, it) }) }
    val yPolyComputed get() = _YPoly.isInitialized()
    val yPoly by _YPoly
    val dyPoly get() = yPoly.derivative

    val derivative by lazy {
        if (order < 1) {
            throw NoMoreDerivativeException("bezier function hasn't derivative")
        } else {
            Bezier(points.zipWithNext { cur, next -> (next - cur) * order })
        }
    }

    fun derivativeOrNull() = if (isEmpty) null else derivative

    private val _bounds = lazy { Bounds((dxPoly.roots + dyPoly.roots + 0.0 + 1.0).map { point(it) }) }
    val boundsComputed get() = _bounds.isInitialized()
    val bounds get() = _bounds.value.copy()

    private val _fastBounds = lazy {
        val bounds = Bounds(start)
        for (i in 1..points.lastIndex) {
            bounds.add(points[i])
        }

        return@lazy bounds
    }
    val fastBounds get() = if (order <= 5 || boundsComputed) bounds else _fastBounds.value.copy()

    /**
     *      *
     *        *
     *           [*]
     *                *  *
     *           [*]
     *        *
     *      *
     *           X
     */
    fun paramsAtX(x: Double): List<Double> =
        (xPoly - x).roots.filterBezier()

    /**
     *               *  *
     *            *        *
     *         [*]          [*]       Y
     *         *              *
     *        *                *
     */
    fun paramsAtY(y: Double): List<Double> =
        (yPoly - y).roots.filterBezier()

    /**
     *
     *            * *
     *           *   *
     *           [[*]]
     *          *  ↑  *
     *      *    point    *
     *
     * In most cases it returns one value or empty list,
     * so if you are sure that curve doesn't have a
     * self-intersection in [point], so you can
     * use `paramsAtPoint(p).singleOrNull()`, or
     * if it doesn't matter `paramsAtPoint(p).firstOrNull()`
     */
    fun paramsAtPoint(point: Vec2): List<Double> =
        allProjectionParams(point).filter { point(it) distToSq point near Tolerance }

    /**
     *     *                p
     *      [*]
     *          *  * [*]
     *                   *
     *                    [*]
     */
    fun allProjectionParams(point: Vec2): List<Double> {
        val xPoly = (xPoly - point.x) * dxPoly
        val yPoly = (yPoly - point.y) * dyPoly
        return (xPoly + yPoly).roots.coerceBezier()
    }

    /**
     *     *                p
     *       *
     *          *  * [*]
     *                   *
     *                     *
     */
    fun projectionParam(point: Vec2) = allProjectionParams(point).minBy { point distTo point(it) }

    fun project(point: Vec2) = point(projectionParam(point))

    fun split(t: Double): Pair<Bezier, Bezier> = when (t) {
        0.0 -> Bezier(List(count) { start }) to this
        1.0 -> this to Bezier(List(count) { end })
        else -> {
            val left = mutableListOf<Vec2>()
            val right = mutableListOf<Vec2>()
            var curve = points

            while (curve.isNotEmpty()) {
                left += curve.first()
                right += curve.last()
                curve = curve.zipWithNext { p1, p2 -> lerp(p1, p2, t) }
            }

            Bezier(left) to Bezier(right.asReversed())
        }
    }

    fun antiSplit(other: Bezier): Pair<Double, Bezier>? {
        /**
         *     S
         *       *               M = S + t(E - S)
         *         M             t = (M - S) / (E - S)
         *           *
         *             E
         * */
        fun splitParam(s: Vec2, m: Vec2, e: Vec2): Double? {
            val tx = (s.x - m.x) / (s.x - e.x)
            val ty = (s.y - m.y) / (s.y - e.y)
            return when {
                tx.isNaN() && ty.isNaN() -> return null
                tx.isNaN() -> ty
                ty.isNaN() -> tx
                !(tx near ty) -> return null
                else -> (tx + ty) / 2
            }
        }

        if (order == other.order) {
            var s = points[count - 2]; val e = other.points[1]
            val t = splitParam(s, end, e) ?: return null

            val newPoints = points.toMutableList()
            for (i in 0 until order) {
                for (j in order downTo i + 1) {
                    s = newPoints[j - 1]
                    val m = newPoints[j]
                    newPoints[j] = (m - s) / t + s
                }
            }

            val merged = Bezier(newPoints)
            val (left, right) = merged.split(t)
            if (left near this && right near other)
                return t to merged
        }

        return null
    }

    fun sub(t1: Double, t2: Double) = if (t1 == 0.0 && t2 == 1.0) this else split(t2).first.split(t1 / t2).second

    fun sub(interval: DoubleRange) = sub(interval.start, interval.endInclusive)

//    infix fun subOf(other: Bezier): Boolean {
//        if (order != other.order)
//            return false
//
//        val da = generateSequence({ this  }) { it.derivativeOrNull() }
//        val db = generateSequence({ other }) { it.derivativeOrNull() }
//        return da.zip(db).all { (a, b) -> a.start liesOn b && a.end liesOn b }
//    }

    fun tangent(t: Double) = derivative.point(t)

    fun normal(t: Double) = tangent(t).run { Vec2(-y / length, x / length) }

    val length by lazy { calculateLength(derivative) }

    fun intersectionsWith(other: Bezier): List<Pair<Double, Double>> = when {
        !this.fastBounds.overlap(other.fastBounds) -> emptyList()

        isLine && other.isLine -> {
            val asx = this.start.x;  val asy = this.start.y;  val aex = this.end.x;  val aey = this.end.y
            val bsy = other.start.y; val bsx = other.start.x; val bex = other.end.x; val bey = other.end.y
            val u = (bey - bsy) * (aex - asx) - (bex - bsx) * (aey - asy)

            if (u != 0.0) {
                val ua = ((bex - bsx) * (asy - bsy) - (bey - bsy) * (asx - bsx)) / u
                val ub = ((aex - asx) * (asy - bsy) - (aey - asy) * (asx - bsx)) / u
                val isOn = ua in 0.0..1.0 && ub in 0.0..1.0

                listOfNotNull((ua to ub).takeIf { isOn })
            } else emptyList()
        }

        isLine -> {
            val (l1, l2) = this
            val l = l2 - l1
            val length = l.length

            val aligned = run<Bezier> {
                val theta = -atan2(l.y, l.x)
                val s = sin(theta)
                val c = cos(theta)

                other.mapToBezier {
                    val (x, y) = it - l1
                    Vec2(
                        x = x * c - y * s,
                        y = x * s + y * c,
                    )
                }
            }

            val interval = 0.0..length
            val intersections = aligned.paramsAtY(0.0).map { aligned.x(it) / length to it }
            intersections.filter { it.first in interval }
        }

        other.isLine -> {
            val reversed = other.intersectionsWith(this)
            reversed.map { (b, a) -> a to b }
        }

        else -> {
            val matrix = implictize(this, other.xPoly, other.yPoly)
            val bParams = det(matrix).roots.filterBezier()
            bParams.flatMap { tb ->
                val point = other.point(tb)
                val aParams = this.paramsAtPoint(point)
                aParams.associateWith { tb }.toList()
            }
        }
    }

    fun intersectionsWith(rect: Bounds): List<Double> {
        val params = rect.run { paramsAtX(left) + paramsAtY(top) + paramsAtX(right) + paramsAtY(bottom) }
        return params.filter { rect.contains(this.point(it)) }
    }

    fun curvatureAt(t: Double): Double {
        if (order < 2) return 0.0
        val dp = derivative.point(t)
        val ddp = derivative.derivative.point(t)
        return (dp cross ddp) / dp.lengthSq.pow(3.0 / 2.0)
    }

    override fun toString() = "Bezier(" + points.joinToString { "${it.x}, ${it.y}" } + ")"

    override fun equals(other: Any?) = if (other is Bezier) points == other.points else false

    override fun hashCode() = points.hashCode()

    override fun clone() = Bezier(points)

    fun raiseOrder(): Bezier {
        val new = mutableListOf(start)
        for (i in 1 until count)
            new += points[i] * ((count - i) / count.toDouble()) + points[i - 1] * (i / count.toDouble())
        new += end

        return Bezier(new)
    }

    fun representAsLine(): Bezier {
        if (!isCurve) return this

        val line = Bezier(start, end)
        val controls = points.subList(1, count - 1)
        return if (controls.all { it liesOn line }) line else this

//        var bezier = this
//        while (true) {
//            val lowered = bezier.lowerOrder()
//            bezier = lowered ?: return bezier
//        }
    }

    private class Parametrization(val step: Double, p: (t: Double) -> Vec2) {
        val lengths = DoubleArray((1 / step).toInt() + 1)
        inline val flatLength get() = lengths.last()
        val paramForLength: ((Double) -> Double)
        val lengthForParam: ((Double) -> Double)

        init {
            lengths[0] = 0.0; var current = step; var i = 1; var last = p(0.0)
            while (current < 1.0) {
                lengths[i] = lengths[i - 1] + (last distTo p(current).also { last = it })
                current += step
                i += 1
            }
            lengths[lengths.lastIndex] = lengths[lengths.lastIndex - 1] + (last distTo p(1.0))

            paramForLength = { targetLength ->
                if (targetLength > flatLength)
                    throw IllegalArgumentException("Target length more then length of curve")
                if (targetLength < 0.0)
                    throw IllegalArgumentException("Target length less then zero")

                val upperIndex = lengths.indexOfFirst { it >= targetLength }
                val lowerIndex = upperIndex - 1

                val t = { index: Double -> index / lengths.lastIndex }
                val upperLength = lengths[upperIndex]
                if (upperLength == targetLength) {
                    t(upperIndex.toDouble())
                } else {
                    val lowerLength = lengths[lowerIndex]
                    val position = (targetLength - lowerLength) / (upperLength - lowerLength)

                    t(lowerIndex + position)
                }
            }

            lengthForParam = { targetParam ->
                if (targetParam !in 0.0..1.0)
                    throw IllegalArgumentException("Target param out of curve: $targetParam")

                val upperIndex = lengths.indices.first { it.toDouble() / lengths.lastIndex >= targetParam }
                val lowerIndex = upperIndex - 1

                val upperParam = upperIndex.toDouble() / lengths.lastIndex
                val upperLength = lengths[upperIndex]
                if (upperParam == targetParam) {
                    upperLength
                } else {
                    val lowerParam = lowerIndex.toDouble() / lengths.lastIndex
                    val position = (targetParam - lowerParam) / (upperParam - lowerParam)

                    lengths[lowerIndex] + (upperLength - lengths[lowerIndex]) * position
                }
            }
        }
    }

    private var parameterization = if (isLine) Parametrization(1.0, ::point) else null

    private fun needParam(): Nothing = throw NoArcLengthParametrizationException("Curve hasn't been parameterized with arc length yet")

    fun parameterizeWithArcLength(step: Double = 1.0 / 150) {
        if (isCurve && (parameterization == null || parameterization!!.step > step)) {
            parameterization = Parametrization(step, ::point)
        }
    }

    fun flatLength() = (parameterization ?: needParam()).flatLength

    fun paramAtLength(length: Double) = (parameterization ?: needParam()).paramForLength(length)

    fun paramAtLengthPercent(u: Double) = paramAtLength(flatLength() * u)

    fun pointAtLength(length: Double) = point(paramAtLength(length))

    fun pointAtLengthPercent(u: Double) = point(paramAtLengthPercent(u))

    fun lengthAt(t: Double) = (parameterization ?: needParam()).lengthForParam(t)
}

private class ReversedBezier(private val source: Bezier) : Bezier(source.points.asReversed()) {
    override val reversed get() = source
}

inline fun <T> Bezier.map(transform: (Vec2) -> T) = points.map(transform)

inline fun Bezier.mapToBezier(transform: (Vec2) -> Vec2) = Bezier(map(transform))

infix fun Vec2.distToSq(bezier: Bezier) = when {
    bezier.isEmpty -> Double.NaN
    bezier.isLine -> distToSegmentSq(bezier.start, bezier.end)
    else -> this distToSq bezier.project(this)
}

infix fun Vec2.distTo(bezier: Bezier) = sqrt(distToSq(bezier))

inline val Bezier.isEmpty get() = order <= 0

inline val Bezier.isLine get() = order == 1

inline val Bezier.isCurve get() = order > 1

internal val Bezier.isQuad get() = order == 2

internal val Bezier.isCubic get() = order == 3

internal val Bezier.isComplex get() = order > 3

infix fun Vec2.liesOn(bezier: Bezier) = when {
    bezier.isEmpty -> false
    bezier.isLine -> distToSegmentSq(bezier.start, bezier.end) near 0.0
    else -> bezier.project(this) near this
}

internal fun List<Double>.filterBezier() = filter { it in 0.0..1.0 }

internal fun List<Double>.coerceBezier() = map { it.coerceIn(0.0, 1.0) }

infix fun Bezier.near(other: Bezier) =
    order == other.order && points.zip(other.points).all { (a, b) -> a near b }

fun Bezier.approximateWithCubics(): Path = if (!isComplex) {
    BeziersPath(this).toPath()
} else {
    BeziersPath(this).toCalmPath().map { calmBezier ->
        curve(
            xy = { calmBezier.point(it) },
            dxy = { calmBezier.derivative.point(it) },
            interval = 0.0..1.0,
            next = { it + 1.0 / (order - 1) }
        )
    }.joinToPath().minify()
}
