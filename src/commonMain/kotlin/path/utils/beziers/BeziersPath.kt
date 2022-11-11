package path.utils.beziers

import path.utils.math.MatrixTransform
import path.utils.math.Vec2
import path.utils.math.distToSegmentSq
import path.utils.math.isOdd
import path.utils.paths.*
import path.utils.paths.Command.*

class BeziersPath(val beziers: List<Bezier>, val isCalm: Boolean = false) {
    constructor(bezier: Bezier) : this(listOf(bezier))

    constructor() : this(emptyList())

    inline fun iteratePath(action: (Bezier, isMove: Boolean) -> Unit) {
        var last: Vec2? = null
        for (b in beziers) {
            if (b.isEmpty) continue
            action(b, last == null || !last.near(b.start))
            last = b.end
        }
    }

    inline fun <T> map(transform: (Bezier) -> T): List<T> = beziers.map(transform)

    inline fun mapToBeziers(transform: (Bezier) -> Bezier) = BeziersPath(map(transform))

    val isClockwise by lazy {
        val points = beziers.flatMap { it.points }
        var area = 0.0
        for ((cur, next) in (points + points.first()).zipWithNext())
            area += (next.x - cur.x) * (next.y + cur.y)

        area >= 0.0
    }

    fun splitToSubPaths(): List<BeziersPath> {
        val subs = mutableListOf<MutableList<Bezier>>()
        iteratePath { bezier, isMove ->
            if (isMove) {
                subs += mutableListOf<Bezier>()
            }

            subs.last() += bezier
        }

        return subs.map { BeziersPath(it) }
    }

    fun toFlatPath(flatness: Double, recursiveLimit: Int = 10): BeziersPath {
        val sq = flatness * flatness
        val lines = beziers.flatMap {
            when {
                it.isEmpty -> listOf()
                it.isLine -> listOf(it)
                else -> flatten(it, sq, recursiveLimit)
            }
        }

        return BeziersPath(lines)
    }

    val isFlat by lazy { beziers.all { !it.isCurve } }

    fun toCalmPath(): BeziersPath {
        if (isCalm) return this

        val calmBeziers = beziers.flatMap { bezier ->
            // t values where curve change direction
            val roots = bezier.dyPoly.roots + listOf(0.0, 1.0)
            // We split curves to observe them as lines
            roots.filterBezier().distinct().sorted().zipWithNext {
                    t1, t2 -> bezier.sub(t1, t2)
            }
        }

        return BeziersPath(calmBeziers, true)
    }

    private val _bounds = lazy { map { it.bounds }.reduce { a, b -> a union b } }
    val bounds get() = _bounds.value.copy()

    private val _fastBounds = lazy { map { it.fastBounds }.reduce { a, b -> a union b } }
    val fastBounds get() = _fastBounds.value.copy()

    fun contains(x: Double, y: Double): Boolean {
        if (!isCalm) return toCalmPath().contains(x, y)

        if (!fastBounds.contains(x, y)) {
            return false
        }

        var crossings = 0
        for (bezier in beziers) {
            //                   *
            //         p      *         check if p is on left side
            //              *           of curve
            //             *

            val interval = listOf(bezier.start.y, bezier.end.y).run { min()..max() }
            val (minX, maxX) = bezier.map { it.x }.run { min() to max() }
            fun x4y(y: Double) = bezier.x(bezier.paramsAtY(y).single())
            if (y in interval && (x < minX || x < maxX && x < x4y(y)))
                crossings += 1
        }

        return crossings.isOdd
    }

    fun contains(p: Vec2) = contains(p.x, p.y)

    fun isEmpty() = beziers.isEmpty()
}

fun Command.toBezier(from: Vec2) = when(this) {
    is LineTo -> Bezier(from, p)
    is QuadTo -> Bezier(from, p1, p)
    is CubicTo -> Bezier(from, p1, p2, p)
    else -> throw IllegalArgumentException("Line, Quad or Cubic are expected")
}

fun Path.toBeziers(): BeziersPath {
    val beziers = mutableListOf<Bezier>()
    var hasClose = true
    val (last, move) = validate().simplify().iteratePath { command, last, _, move ->
        when (command) {
            is MoveTo -> {
                if (!hasClose) beziers += Bezier(last, move)
                hasClose = false
            }

            is Close -> {
                beziers += Bezier(last, move)
                hasClose = true
            }

            is LineTo, is QuadTo, is CubicTo -> {
                beziers += command.toBezier(last)
            }

            else -> throw IllegalArgumentException("simple path was expected")
        }
    }

    if (!(last near move))
        beziers += Bezier(last, move)
    return BeziersPath(beziers)
}

internal val Bezier.isQuad get() = order == 2

internal val Bezier.isCubic get() = order == 3

internal val Bezier.isComplex get() = order > 3

fun Bezier.toCommand(): Pair<Vec2, Command> = when {
    isEmpty -> throw IllegalArgumentException("no points to form curve")
    isLine  -> start to LineTo(this[1])
    isQuad  -> start to QuadTo(this[1], this[2])
    isCubic -> start to CubicTo(this[1], this[2], this[3])
    else -> throw IllegalArgumentException("too big curve order: use toFlatPath() instead")
}

fun BeziersPath.toPath(): Path {
    val path = mutablePath()

    iteratePath { bezier, isMove ->
        if (isMove) {
            path.moveTo(bezier.start)
        }

        path += when {
            bezier.isEmpty -> emptyList()
            bezier.isComplex -> bezier.approximateWithCubic().run { subList(1, size) } // remove first moveTo
            else -> listOf(bezier.toCommand().second)
        }
    }

    return path
}

internal fun flatten(
    bezier: Bezier,
    flatnessSq: Double,
    limit: Int,
    iteration: Int = 0,
    result: MutableList<Bezier> = mutableListOf()
): List<Bezier> {
    fun calculateFlatnessSq(b: Bezier): Double {
        val controls = b.points.let { it.subList(1, it.size - 1) }
        return controls.maxOf { it.distToSegmentSq(b.start, b.end) }
    }

    if (iteration == limit || calculateFlatnessSq(bezier) < flatnessSq) {
        result += bezier.let { Bezier(it.start, it.end) }
    } else {
        val (left, right) = bezier.split(0.5)
        flatten(left, flatnessSq, iteration + 1, limit, result)
        flatten(right, flatnessSq, iteration + 1, limit, result)
    }

    return result
}

fun BeziersPath.transformWith(matrix: MatrixTransform): BeziersPath {
    return mapToBeziers { it.mapToBezier { p -> matrix.transform(p) } }
}