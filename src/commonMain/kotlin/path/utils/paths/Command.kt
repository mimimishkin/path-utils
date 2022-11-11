package path.utils.paths

import path.utils.beziers.toCommand
import path.utils.math.Vec2
import path.utils.math.arcToCurves
import path.utils.math.near
import path.utils.paths.Command.*
import path.utils.paths.CommandType.*

sealed class Command(val type: CommandType) {
    data class MoveTo(
        var x: Double,
        var y: Double
    ) : Command(MoveToType) {
        constructor(p: Vec2) : this(p.x, p.y)

        var p
            get() = Vec2(x, y)
            set(p) { x = p.x; y = p.y }
    }

    data class MoveToRelative(
        var dx: Double,
        var dy: Double
    ) : Command(MoveToRelativeType) {
        constructor(dp: Vec2) : this(dp.x, dp.y)

        var dp
            get() = Vec2(dx, dy)
            set(dp) { dx = dp.x; dy = dp.y }
    }

    data class LineTo(
        var x: Double,
        var y: Double
    ) : Command(LineToType) {
        constructor(p: Vec2) : this(p.x, p.y)

        var p
            get() = Vec2(x, y)
            set(p) { x = p.x; y = p.y }
    }

    data class LineToRelative(
        var dx: Double,
        var dy: Double
    ) : Command(LineToRelativeType) {
        constructor(dp: Vec2) : this(dp.x, dp.y)

        var dp
            get() = Vec2(dx, dy)
            set(dp) { dx = dp.x; dy = dp.y }
    }

    data class VerticalLineTo(
        var y: Double
    ) : Command(VerticalLineToType)

    data class VerticalLineToRelative(
        var dy: Double
    ) : Command(VerticalLineToRelativeType)

    data class HorizontalLineTo(
        var x: Double
    ) : Command(HorizontalLineToTo)

    data class HorizontalLineToRelative(
        var dx: Double
    ) : Command(HorizontalLineToRelativeType)

    data class QuadTo(
        var x1: Double,
        var y1: Double,
        var x: Double,
        var y: Double,
    ) : Command(QuadToType) {
        constructor(p1: Vec2, p: Vec2) : this(p1.x, p1.y, p.x, p.y)

        var p1
            get() = Vec2(x1, y1)
            set(p1) { x1 = p1.x; y1 = p1.y }

        var p
            get() = Vec2(x, y)
            set(p) { x = p.x; y = p.y }
    }

    data class QuadToRelative(
        var dx1: Double,
        var dy1: Double,
        var dx: Double,
        var dy: Double,
    ) : Command(QuadToRelativeType) {
        constructor(dp1: Vec2, dp: Vec2) : this(dp1.x, dp1.y, dp.x, dp.y)

        var dp1
            get() = Vec2(dx1, dy1)
            set(dp1) { dx1 = dp1.x; dy1 = dp1.y }

        var dp
            get() = Vec2(dx, dy)
            set(dp) { dx = dp.x; dy = dp.y }
    }

    data class CubicTo(
        var x1: Double,
        var y1: Double,
        var x2: Double,
        var y2: Double,
        var x: Double,
        var y: Double,
    ) : Command(CubicToType) {
        constructor(p1: Vec2, p2: Vec2, p: Vec2) : this(p1.x, p1.y, p2.x, p2.y, p.x, p.y)

        var p1
            get() = Vec2(x1, y1)
            set(p1) { x1 = p1.x; y1 = p1.y }

        var p2
            get() = Vec2(x2, y2)
            set(p2) { x2 = p2.x; y2 = p2.y }

        var p
            get() = Vec2(x, y)
            set(p) { x = p.x; y = p.y }
    }

    data class CubicToRelative(
        var dx1: Double,
        var dy1: Double,
        var dx2: Double,
        var dy2: Double,
        var dx: Double,
        var dy: Double,
    ) : Command(CubicToRelativeType) {
        constructor(dp1: Vec2, dp2: Vec2, dp: Vec2) : this(dp1.x, dp1.y, dp2.x, dp2.y, dp.x, dp.y)

        var dp1
            get() = Vec2(dx1, dy1)
            set(dp1) { dx1 = dp1.x; dy1 = dp1.y }

        var dp2
            get() = Vec2(dx2, dy2)
            set(dp2) { dx2 = dp2.x; dy2 = dp2.y }

        var dp
            get() = Vec2(dx, dy)
            set(dp) { dx = dp.x; dy = dp.y }
    }

    data class SmoothQuadTo(
        var x: Double,
        var y: Double,
    ) : Command(SmoothQuadToType) {
        constructor(p: Vec2) : this(p.x, p.y)

        var p
            get() = Vec2(x, y)
            set(p) { x = p.x; y = p.y }
    }

    data class SmoothQuadToRelative(
        var dx: Double,
        var dy: Double,
    ) : Command(SmoothQuadToRelativeType) {
        constructor(dp: Vec2) : this(dp.x, dp.y)

        var dp
            get() = Vec2(dx, dy)
            set(dp) { dx = dp.x; dy = dp.y }
    }

    data class SmoothCubicTo(
        var x2: Double,
        var y2: Double,
        var x: Double,
        var y: Double,
    ) : Command(SmoothCubicToType) {
        constructor(p2: Vec2, p: Vec2) : this(p2.x, p2.y, p.x, p.y)

        var p2
            get() = Vec2(x2, y2)
            set(p2) { x2 = p2.x; y2 = p2.y }

        var p
            get() = Vec2(x, y)
            set(p) { x = p.x; y = p.y }
    }

    data class SmoothCubicToRelative(
        var dx2: Double,
        var dy2: Double,
        var dx: Double,
        var dy: Double,
    ) : Command(SmoothCubicToRelativeType) {
        constructor(dp2: Vec2, dp: Vec2) : this(dp2.x, dp2.y, dp.x, dp.y)

        var dp2
            get() = Vec2(dx2, dy2)
            set(dp2) { dx2 = dp2.x; dy2 = dp2.y }

        var dp
            get() = Vec2(dx, dy)
            set(dp) { dx = dp.x; dy = dp.y }
    }

    data class ArcTo(
        var rx: Double,
        var ry: Double,
        var xAxisRotation: Double,
        var largeArcFlag: Boolean,
        var sweepFlag: Boolean,
        var x: Double,
        var y: Double,
    ) : Command(ArcToType) {
        constructor(
            rx: Double,
            ry: Double,
            xAxisRotation: Double,
            largeArcFlag: Boolean,
            sweepFlag: Boolean,
            p: Vec2,
        ) : this(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, p.x, p.y)

        var p
            get() = Vec2(x, y)
            set(p) { x = p.x; y = p.y }
    }

    data class ArcToRelative(
        var rx: Double,
        var ry: Double,
        var xAxisRotation: Double,
        var largeArcFlag: Boolean,
        var sweepFlag: Boolean,
        var dx: Double,
        var dy: Double,
    ) : Command(ArcToRelativeType) {
        constructor(
            rx: Double,
            ry: Double,
            xAxisRotation: Double,
            largeArcFlag: Boolean,
            sweepFlag: Boolean,
            dp: Vec2,
        ) : this(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, dp.x, dp.y)

        var dp
            get() = Vec2(dx, dy)
            set(dp) { dx = dp.x; dy = dp.y }
    }

    object Close : Command(CloseType)
}

enum class CommandType(val argsCount: Int, val symbol: Char) {
    LineToType(2, 'L'),

    LineToRelativeType(2, 'l'),

    MoveToType(2, 'M'),

    MoveToRelativeType(2, 'm'),

    VerticalLineToType(1, 'V'),

    VerticalLineToRelativeType(1, 'v'),

    HorizontalLineToTo(1, 'H'),

    HorizontalLineToRelativeType(1, 'h'),

    QuadToType(4, 'Q'),

    QuadToRelativeType(4, 'q'),

    CubicToType(6, 'C'),

    CubicToRelativeType(6, 'c'),

    SmoothQuadToType(2, 'T'),

    SmoothQuadToRelativeType(2, 't'),

    SmoothCubicToType(4, 'S'),

    SmoothCubicToRelativeType(4, 's'),

    ArcToType(7, 'A'),

    ArcToRelativeType(7, 'a'),

    CloseType(0, 'Z');

    companion object {
        val symbols = enumValues<CommandType>().map { it.symbol } + 'z'

        fun find(symbol: Char) = if (symbol == 'z') CloseType else enumValues<CommandType>().find { it.symbol == symbol }
    }

    fun make(args: List<Double>) = when (this) {
        LineToType -> LineTo(args[0], args[1])
        LineToRelativeType -> LineToRelative(args[0], args[1])
        MoveToType -> MoveTo(args[0], args[1])
        MoveToRelativeType -> MoveToRelative(args[0], args[1])
        VerticalLineToType -> VerticalLineTo(args[0])
        VerticalLineToRelativeType -> VerticalLineToRelative(args[0])
        HorizontalLineToTo -> HorizontalLineTo(args[0])
        HorizontalLineToRelativeType -> HorizontalLineToRelative(args[0])
        QuadToType -> QuadTo(args[0], args[1], args[2], args[3])
        QuadToRelativeType -> QuadToRelative(args[0], args[1], args[2], args[3])
        CubicToType -> CubicTo(args[0], args[1], args[2], args[3], args[4], args[5])
        CubicToRelativeType -> CubicToRelative(args[0], args[1], args[2], args[3], args[4], args[5])
        SmoothQuadToType -> SmoothQuadTo(args[0], args[1])
        SmoothQuadToRelativeType -> SmoothQuadToRelative(args[0], args[1])
        SmoothCubicToType -> SmoothCubicTo(args[0], args[1], args[2], args[3])
        SmoothCubicToRelativeType -> SmoothCubicToRelative(args[0], args[1], args[2], args[3])
        ArcToType -> ArcTo(args[0], args[1], args[2], args[3] == 1.0, args[4] == 1.0, args[5], args[6])
        ArcToRelativeType -> ArcToRelative(args[0], args[1], args[2], args[3] == 1.0, args[4] == 1.0, args[5], args[6])
        CloseType -> Close
    }

    fun next() = when (this) {
        MoveToType -> LineToType
        MoveToRelativeType -> LineToRelativeType
        else -> this
    }
}

val Command.arguments: List<Double> get() = when(this) {
    is ArcTo -> listOf(rx, ry, xAxisRotation, if (largeArcFlag) 1.0 else 0.0, if (sweepFlag) 1.0 else 0.0, x, y)
    is ArcToRelative -> listOf(rx, ry, xAxisRotation, if (largeArcFlag) 1.0 else 0.0, if (sweepFlag) 1.0 else 0.0, dx, dy)
    is Close -> listOf()
    is CubicTo -> listOf(x1, y1, x2, y2, x, y)
    is CubicToRelative -> listOf(dx1, dy1, dx2, dy2, dx, dy)
    is HorizontalLineTo -> listOf(x)
    is HorizontalLineToRelative -> listOf(dx)
    is LineTo -> listOf(x, y)
    is LineToRelative -> listOf(dx, dy)
    is MoveTo -> listOf(x, y)
    is MoveToRelative -> listOf(dx, dy)
    is QuadTo -> listOf(x1, y1, x, y)
    is QuadToRelative -> listOf(dx1, dy1, dx, dy)
    is SmoothCubicTo -> listOf(x2, y2, x, y)
    is SmoothCubicToRelative -> listOf(dx2, dy2, dx, dy)
    is SmoothQuadTo -> listOf(x, y)
    is SmoothQuadToRelative -> listOf(dx, dy)
    is VerticalLineTo -> listOf(y)
    is VerticalLineToRelative -> listOf(dy)
}

fun Command.toAbsolute(last: Vec2) = when(this) {
    is ArcToRelative -> ArcTo(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, last.x + dx, last.y + dy)
    is CubicToRelative -> CubicTo(last.x + dx1, last.y + dy1, last.x + dx2, last.y + dy2, last.x + dx, last.y + dy)
    is HorizontalLineToRelative -> HorizontalLineTo(last.x + dx)
    is LineToRelative -> LineTo(last.x + dx, last.y + dy)
    is MoveToRelative -> MoveTo(last.x + dx, last.y + dy)
    is QuadToRelative -> QuadTo(last.x + dx1, last.y + dy1, last.x + dx, last.y + dy)
    is SmoothCubicToRelative -> SmoothCubicTo(last.x + dx2, last.y + dy2, last.x + dx, last.y + dy)
    is SmoothQuadToRelative -> SmoothQuadTo(last.x + dx, last.y + dy)
    is VerticalLineToRelative -> VerticalLineTo(last.y + dy)
    else -> this
}

fun Command.toRelative(last: Vec2) = when(this) {
    is ArcTo -> ArcToRelative(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x - last.x, y - last.y)
    is CubicTo -> CubicToRelative(x1 - last.x, y1 - last.y, x2 - last.x, y2 - last.y, x - last.x, y - last.y)
    is HorizontalLineTo -> HorizontalLineToRelative(x - last.x)
    is LineTo -> LineToRelative(x - last.x, y - last.y)
    is MoveTo -> MoveToRelative(x - last.x, y - last.y)
    is QuadTo -> QuadToRelative(x1 - last.x, y1 - last.y, x - last.x, y - last.y)
    is SmoothCubicTo -> SmoothCubicToRelative(x2 - last.x, y2 - last.y, x - last.x, y - last.y)
    is SmoothQuadTo -> SmoothQuadToRelative(x - last.x, y - last.y)
    is VerticalLineTo -> VerticalLineToRelative(y - last.y)
    else -> this
}

fun Command.simplify(last: Vec2, anchor: Vec2? = null): List<Command> = when(val a = toAbsolute(last)) {
    is VerticalLineTo -> listOf(LineTo(last.x, a.y))
    is HorizontalLineTo -> listOf(LineTo(a.x, last.y))
    is SmoothQuadTo -> listOf(QuadTo(last * 2 - anchor!!, a.p))
    is SmoothCubicTo -> listOf(CubicTo(last * 2 - anchor!!, a.p2, a.p))
    is ArcTo -> a.curves(last)
    else -> listOf(a)
}

fun Command.minify(from: Vec2, anchor: Vec2?, move: Vec2): Command? {
    fun anchorsNear(a: Vec2?, b: Vec2) = a != null && from * 2 - a near b

    val abs = toAbsolute(from)
    val minified = when {
        abs.isLine -> {
            val to = abs.lastPoint(from, move)
            when {
                to near from -> return null
                to near move -> return Close
                to.x near from.x -> VerticalLineTo(to.y)
                to.y near from.y -> HorizontalLineTo(to.x)
                else -> abs
            }
        }

        abs is QuadTo -> with(abs) { when {
            from near p1 && p1 near p -> return null
            anchorsNear(anchor, p1) -> SmoothQuadTo(p)
            else -> abs
        } }

        abs is CubicTo -> with(abs) { when {
            from near p1 && p1 near p2 && p2 near p -> return null
            anchorsNear(anchor, p1) -> SmoothCubicTo(p2, p)
            else -> abs
        } }

        abs is MoveTo -> if (abs.p near from) return null else abs

        abs is ArcTo -> if (abs.p near from) return null else abs

        else -> abs
    }

    val absolute = minified
    val relative = absolute.toRelative(from)

    val relLen = relative.toSvg().length
    val absLen = absolute.toSvg().length
    return if (relLen < absLen) relative else absolute
}

fun Command.closeToLine(moveTo: Vec2) = if (isClose) LineTo(moveTo) else this

val Command?.isAbsolute get() =
    this is MoveTo ||
    this is LineTo ||
    this is VerticalLineTo ||
    this is HorizontalLineTo ||
    this is QuadTo ||
    this is CubicTo ||
    this is SmoothQuadTo ||
    this is SmoothCubicTo ||
    this is ArcTo ||
    this is Close

val Command?.isRelative get() =
    this is MoveToRelative ||
    this is LineToRelative ||
    this is VerticalLineToRelative ||
    this is HorizontalLineToRelative ||
    this is QuadToRelative ||
    this is CubicToRelative ||
    this is SmoothQuadToRelative ||
    this is SmoothCubicToRelative ||
    this is ArcToRelative ||
    this is Close

val Command?.isSimple get() =
    this is MoveTo || this is LineTo || this is QuadTo || this is CubicTo || this is Close

val Command?.isLine get() =
    this is LineTo || this is LineToRelative ||
    this is VerticalLineTo || this is VerticalLineToRelative ||
    this is HorizontalLineTo || this is HorizontalLineToRelative ||
    this is Close

val Command?.isCurve get() =
    this is QuadTo || this is QuadToRelative ||
    this is CubicTo || this is CubicToRelative ||
    this is SmoothQuadTo || this is SmoothQuadToRelative ||
    this is SmoothCubicTo || this is SmoothCubicToRelative

val Command?.isSmooth get() =
    this is SmoothQuadTo || this is SmoothQuadToRelative ||
    this is SmoothCubicTo || this is SmoothCubicToRelative

val Command?.isMove get() = this is MoveTo || this is MoveToRelative

val Command?.isArc get() = this is ArcTo || this is ArcToRelative

val Command?.isClose get() = this is Close

private fun bool(value: Boolean) = if (value) '1' else '0'

fun Command.toSvg() = when (this) {
    is ArcTo -> "A $rx $ry $xAxisRotation ${bool(largeArcFlag)} ${bool(sweepFlag)} $x $y"
    is ArcToRelative -> "a $rx $ry $xAxisRotation ${bool(largeArcFlag)} ${bool(sweepFlag)} $dx $dy"
    is Close -> "Z"
    is CubicTo -> "C $x1 $y1 $x2 $y2 $x $y"
    is CubicToRelative -> "c $dx1 $dy1 $dx2 $dy2 $dx $dy"
    is HorizontalLineTo -> "H $x"
    is HorizontalLineToRelative -> "h $dx"
    is LineTo -> "L $x $y"
    is LineToRelative -> "l $dx $dy"
    is MoveTo -> "M $x $y"
    is MoveToRelative -> "m $dx $dy"
    is QuadTo -> "Q $x1 $y1 $x $y"
    is QuadToRelative -> "q $dx1 $dy1 $dx $dy"
    is SmoothCubicTo -> "S $x2 $y2 $x $y"
    is SmoothCubicToRelative -> "s $dx2 $dy2 $dx $dy"
    is SmoothQuadTo -> "T $x $y"
    is SmoothQuadToRelative -> "t $dx $dy"
    is VerticalLineTo -> "V $y"
    is VerticalLineToRelative -> "v $dy"
}

fun Command.lastPoint(
    last: Vec2,
    moveTo: Vec2,
) = when (this) {
    is MoveTo -> Vec2(x, y)
    is MoveToRelative -> Vec2(last.x + dx, last.y + dy)
    is ArcTo -> Vec2(x, y)
    is ArcToRelative -> Vec2(last.x + dx, last.y + dy)
    is CubicTo -> Vec2(x, y)
    is CubicToRelative -> Vec2(last.x + dx, last.y + dy)
    is HorizontalLineTo -> Vec2(x, last.y)
    is HorizontalLineToRelative -> Vec2(last.x + dx, last.y)
    is LineTo -> Vec2(x, y)
    is LineToRelative -> Vec2(last.x + dx, last.y + dy)
    is QuadTo -> Vec2(x, y)
    is QuadToRelative -> Vec2(last.x + dx, last.y + dy)
    is SmoothCubicTo -> Vec2(x, y)
    is SmoothCubicToRelative -> Vec2(last.x + dx, last.y + dy)
    is SmoothQuadTo -> Vec2(x, y)
    is SmoothQuadToRelative -> Vec2(last.x + dx, last.y + dy)
    is VerticalLineTo -> Vec2(last.x, y)
    is VerticalLineToRelative -> Vec2(last.x, last.y + dy)
    is Close -> moveTo
}

fun Command.anchor(anchor: Vec2?, last: Vec2): Vec2? = when (this) {
    is QuadTo -> p1
    is CubicTo -> p2
    is SmoothCubicTo -> p2
    is QuadToRelative -> last + dp1
    is CubicToRelative -> last + dp2
    is SmoothCubicToRelative -> last + dp2
    is SmoothQuadTo, is SmoothQuadToRelative -> last * 2 - anchor!!
    else -> null
}

fun ArcTo.curves(from: Vec2): List<Command> {
    val beziers = arcToCurves(
        p1 = from,
        p2 = Vec2(x, y),
        r = Vec2(rx, ry),
        phi = xAxisRotation,
        largeArc = largeArcFlag,
        sweep = sweepFlag
    )

    return beziers.map { it.toCommand().second }
}