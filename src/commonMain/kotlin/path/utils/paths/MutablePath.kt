package path.utils.paths

import path.utils.math.Vec2
import path.utils.paths.Command.*

typealias MutablePath = MutableList<Command>

fun mutablePath(): MutablePath = mutableListOf()

fun MutablePath.done(): Path = this

fun MutablePath.moveTo(x: Double, y: Double) =
    also { it += MoveTo(x, y) }

fun MutablePath.moveToRelative(dx: Double, dy: Double) =
    also { it += MoveToRelative(dx, dy) }

fun MutablePath.lineTo(x: Double, y: Double) =
    also { it += LineTo(x, y) }

fun MutablePath.lineOrMoveTo(x: Double, y: Double) =
    if (isEmpty()) moveTo(x, y) else lineTo(x, y)

fun MutablePath.lineToRelative(dx: Double, dy: Double) =
    also { it += LineToRelative(dx, dy) }

fun MutablePath.verticalLineTo(y: Double) =
    also { it += VerticalLineTo(y) }

fun MutablePath.verticalLineToRelative(dy: Double) =
    also { it += VerticalLineToRelative(dy) }

fun MutablePath.horizontalLineTo(x: Double) =
    also { it += HorizontalLineTo(x) }

fun MutablePath.horizontalLineToRelative(dx: Double) =
    also { it += HorizontalLineToRelative(dx) }

fun MutablePath.quadTo(x1: Double, y1: Double, x: Double, y: Double) =
    also { it += QuadTo(x1, y1, x, y) }

fun MutablePath.quadToRelative(dx1: Double, dy1: Double, dx: Double, dy: Double) =
    also { it += QuadToRelative(dx1, dy1, dx, dy) }

fun MutablePath.cubicTo(x1: Double, y1: Double, x2: Double, y2: Double, x: Double, y: Double) =
    also { it += CubicTo(x1, y1, x2, y2, x, y) }

fun MutablePath.cubicToRelative(dx1: Double, dy1: Double, dx2: Double, dy2: Double, dx: Double, dy: Double) =
    also { it += CubicToRelative(dx1, dy1, dx2, dy2, dx, dy) }

fun MutablePath.smoothQuadTo(x: Double, y: Double) =
    also { it += SmoothQuadTo(x, y) }

fun MutablePath.smoothQuadToRelative(dx: Double, dy: Double) =
    also { it += SmoothQuadToRelative(dx, dy) }

fun MutablePath.smoothCubicTo(x2: Double, y2: Double, x: Double, y: Double) =
    also { it += SmoothCubicTo(x2, y2, x, y) }

fun MutablePath.smoothCubicToRelative(dx2: Double, dy2: Double, dx: Double, dy: Double) =
    also { it += SmoothCubicToRelative(dx2, dy2, dx, dy) }

fun MutablePath.arcTo(rx: Double, ry: Double, xAxisRotation: Double, largeArcFlag: Boolean, sweepFlag: Boolean, x: Double, y: Double,) =
    also { it += ArcTo(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y) }

fun MutablePath.arcToRelative(rx: Double, ry: Double, xAxisRotation: Double, largeArcFlag: Boolean, sweepFlag: Boolean, dx: Double, dy: Double,) =
    also { it += ArcToRelative(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, dx, dy) }

fun MutablePath.moveTo(p: Vec2) = moveTo(p.x, p.y)
fun MutablePath.lineTo(p: Vec2) = lineTo(p.x, p.y)
fun MutablePath.lineOrMoveTo(p: Vec2) = lineOrMoveTo(p.x, p.y)
fun MutablePath.quadTo(p1: Vec2, p: Vec2) = quadTo(p1.x, p1.y, p.x, p.y)
fun MutablePath.cubicTo(p1: Vec2, p2: Vec2, p: Vec2) = cubicTo(p1.x, p1.y, p2.x, p2.y, p.x, p.y)
fun MutablePath.smoothQuadTo(p: Vec2) = smoothQuadTo(p.x, p.y)
fun MutablePath.smoothCubicTo(p1: Vec2, p: Vec2) = smoothCubicTo(p1.x, p1.y, p.x, p.y)
fun MutablePath.arcTo(rx: Double, ry: Double, xAxisRotation: Double, largeArcFlag: Boolean, sweepFlag: Boolean, p: Vec2) =
    arcTo(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, p.x, p.y)

fun MutablePath.close() =
    also { it += Close }

fun MutablePath.append(command: Command) =
    also { it += command }

fun MutablePath.append(other: Path) =
    also { it += other }