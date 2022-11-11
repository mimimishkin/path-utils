package path.utils.paths

import path.utils.math.Vec2
import path.utils.paths.*

fun rect(x: Double, y: Double, width: Double, height: Double, rx: Double = 0.0, ry: Double = rx) = mutablePath()
    .moveTo(x + rx, y)
    .horizontalLineToRelative(width - rx * 2)
    .arcToRelative(rx, ry, 0.0, false, true, rx, ry)
    .verticalLineToRelative(height - ry * 2)
    .arcToRelative(rx, ry, 0.0, false, true, -rx, ry)
    .horizontalLineToRelative(-(width - rx * 2))
    .arcToRelative(rx, ry, 0.0, false, true, -rx, -ry)
    .verticalLineToRelative(-(height - ry * 2))
    .arcToRelative(rx, ry, 0.0, false, true, rx, -ry)
    .done()

fun rect(x: Double, y: Double, width: Double, height: Double) = mutablePath()
    .moveTo(x, y)
    .horizontalLineToRelative(width)
    .verticalLineToRelative(height)
    .horizontalLineToRelative(-width)
    .close()
    .done()

fun rect(bounds: Bounds) = bounds.run { rect(x, y, w, h) }

fun circle(cx: Double, cy: Double, r: Double) = ellipse(cx, cy, r, r)

fun ellipse(cx: Double, cy: Double, rx: Double, ry: Double) = mutablePath()
    .moveTo(cx - rx, cy)
    .arcToRelative(rx, ry, 0.0, false, false, rx * 2, 0.0)
    .arcToRelative(rx, ry, 0.0, false, false, -rx * 2, 0.0)
    .done()

fun line(x1: Double, y1: Double, x2: Double, y2: Double) = mutablePath()
    .moveTo(x1, y1)
    .lineTo(x2, y2)
    .done()

private fun polyShape(points: Iterable<Vec2>) = mutablePath().apply { points.forEach { lineOrMoveTo(it) } }

@JvmName("polyline0")
fun polyline(points: Iterable<Vec2>) = polyShape(points).done()

fun polyline(coords: Iterable<Double>) = polyline(coords.chunked(2) { Vec2(it[0], it[1]) })

fun polyline(vararg coords: Double) = polyline(coords.asList())

@JvmName("polygon0")
fun polygon(points: Iterable<Vec2>) = polyShape(points).close().done()

fun polygon(coords: Iterable<Double>) = polygon(coords.chunked(2) { Vec2(it[0], it[1]) })

fun polygon(vararg coords: Double) = polygon(coords.asList())

fun path(d: String, ignoreErrors: Boolean = false) = PathParser(d, ignoreErrors).parse()