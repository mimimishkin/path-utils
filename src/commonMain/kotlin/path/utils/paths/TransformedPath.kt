package path.utils.paths

import path.utils.math.MatrixTransform
import path.utils.math.Transforms
import path.utils.math.Vec2
import path.utils.math.near
import path.utils.paths.Command.*

fun Path.transform(transform: (coords: List<Double>) -> List<Double>): Path {
    val transformed = mutablePath()
    var i = 0

    val simplified = simplify()
    val coordinates = transform(simplified.flatMap { it.arguments })
    simplified.forEach {
        val type = it.type
        val count = type.argsCount
        val args = coordinates.subList(i, i + count)

        transformed.append(type.make(args))
        i += count
    }

    return transformed
}

fun Path.transformWith(matrix: MatrixTransform): Path {
    if (matrix.normalize().isIdentical)
        return this

    if (matrix.isTranslate)
        return translate(matrix.m02, matrix.m12)

    if (matrix.isScale)
        return scale(matrix.m00, matrix.m11)

    return transform { c: List<Double> -> matrix.transform(c) }
}

fun Path.translate(tx: Double = 0.0, ty: Double = 0.0): Path {
    val tp = Vec2(tx, ty)
    return validate().map {
        when (it) {
            is MoveTo -> with(it) { MoveTo(p + tp) }
            is LineTo -> with(it) { LineTo(p + tp) }
            is VerticalLineTo -> with(it) { VerticalLineTo(y + ty) }
            is HorizontalLineTo -> with(it) { HorizontalLineTo(x + tx) }
            is QuadTo -> with(it) { QuadTo(p1 + tp, p + tp) }
            is SmoothQuadTo -> with(it) { SmoothQuadTo(p + tp) }
            is CubicTo -> with(it) { CubicTo(p1 + tp, p2 + tp, p + tp) }
            is SmoothCubicTo -> with(it) { SmoothCubicTo(p2 + tp, p + tp) }
            is ArcTo -> with(it) { ArcTo(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, p + tp) }
            else -> it // no need to change relative elements
        }
    }
}

fun Path.scale(sx: Double = 1.0, sy: Double = sx, anchor: Vec2 = Vec2()): Path {
    val scaleX: (Double) -> Double = if (anchor.x near 0.0) {{ it * sx }} else {{ (it - anchor.x) * sx + anchor.x }}
    val scaleY: (Double) -> Double = if (anchor.y near 0.0) {{ it * sy }} else {{ (it - anchor.y) * sy + anchor.y }}
    fun Vec2.scaleAbs() = Vec2(scaleX(x), scaleY(y))
    fun Vec2.scaleRel() = Vec2(x * sx, y * sy)
    return validate().map {
        when (it) {
            is MoveTo -> with(it) { MoveTo(p.scaleAbs()) }
            is LineTo -> with(it) { LineTo(p.scaleAbs()) }
            is VerticalLineTo -> with(it) { VerticalLineTo(scaleY(y)) }
            is HorizontalLineTo -> with(it) { HorizontalLineTo(scaleX(x)) }
            is QuadTo -> with(it) { QuadTo(p1.scaleAbs(), p.scaleAbs()) }
            is SmoothQuadTo -> with(it) { SmoothQuadTo(p.scaleAbs()) }
            is CubicTo -> with(it) { CubicTo(p1.scaleAbs(), p2.scaleAbs(), p.scaleAbs()) }
            is SmoothCubicTo -> with(it) { SmoothCubicTo(p2.scaleAbs(), p.scaleAbs()) }
            is ArcTo -> with(it) { ArcTo(rx * sx, ry * sy, xAxisRotation, largeArcFlag, sweepFlag, p.scaleAbs()) }

            is MoveToRelative -> with(it) { MoveToRelative(dp.scaleRel()) }
            is LineToRelative -> with(it) { LineToRelative(dp.scaleRel()) }
            is VerticalLineToRelative -> with(it) { VerticalLineToRelative(dy * sy) }
            is HorizontalLineToRelative -> with(it) { HorizontalLineToRelative(dx * sx) }
            is QuadToRelative -> with(it) { QuadToRelative(dp1.scaleRel(), dp.scaleRel()) }
            is SmoothQuadToRelative -> with(it) { SmoothQuadToRelative(dp.scaleRel()) }
            is CubicToRelative -> with(it) { CubicToRelative(dp1.scaleRel(), dp2.scaleRel(), dp.scaleRel()) }
            is SmoothCubicToRelative -> with(it) { SmoothCubicToRelative(dp2.scaleRel(), dp.scaleRel()) }
            is ArcToRelative -> with(it) { ArcToRelative(rx * sx, ry * sy, xAxisRotation, largeArcFlag, sweepFlag, dp.scaleRel()) }

            is Close -> Close
        }
    }
}

fun Path.shear(shx: Double = 0.0, shy: Double = 0.0) = transformWith(Transforms.shear(shx, shy))

fun Path.rotate(theta: Double = 0.0, anchor: Vec2 = Vec2()) = transformWith(Transforms.rotate(theta, anchor.x, anchor.y))