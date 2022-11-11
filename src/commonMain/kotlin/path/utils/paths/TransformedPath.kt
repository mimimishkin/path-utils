package path.utils.paths

import path.utils.math.MatrixTransform
import path.utils.math.Transforms
import path.utils.math.Vec2
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

fun Path.transformWith(matrix: MatrixTransform) = transform { c: List<Double> -> matrix.transform(c) }

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

fun Path.scale(sx: Double = 1.0, sy: Double = sx): Path {
    fun Vec2.scale() = Vec2(x * sx, y * sy)
    return validate().map {
        when (it) {
            is MoveTo -> with(it) { MoveTo(p.scale()) }
            is LineTo -> with(it) { LineTo(p.scale()) }
            is VerticalLineTo -> with(it) { VerticalLineTo(y * sy) }
            is HorizontalLineTo -> with(it) { HorizontalLineTo(x * sx) }
            is QuadTo -> with(it) { QuadTo(p1.scale(), p.scale()) }
            is SmoothQuadTo -> with(it) { SmoothQuadTo(p.scale()) }
            is CubicTo -> with(it) { CubicTo(p1.scale(), p2.scale(), p.scale()) }
            is SmoothCubicTo -> with(it) { SmoothCubicTo(p2.scale(), p.scale()) }
            is ArcTo -> with(it) { ArcTo(rx * sx, ry * sy, xAxisRotation, largeArcFlag, sweepFlag, p.scale()) }

            is MoveToRelative -> with(it) { MoveToRelative(dp.scale()) }
            is LineToRelative -> with(it) { LineToRelative(dp.scale()) }
            is VerticalLineToRelative -> with(it) { VerticalLineToRelative(dy * sy) }
            is HorizontalLineToRelative -> with(it) { HorizontalLineToRelative(dx * sx) }
            is QuadToRelative -> with(it) { QuadToRelative(dp1.scale(), dp.scale()) }
            is SmoothQuadToRelative -> with(it) { SmoothQuadToRelative(dp.scale()) }
            is CubicToRelative -> with(it) { CubicToRelative(dp1.scale(), dp2.scale(), dp.scale()) }
            is SmoothCubicToRelative -> with(it) { SmoothCubicToRelative(dp2.scale(), dp.scale()) }
            is ArcToRelative -> with(it) { ArcToRelative(rx * sx, ry * sy, xAxisRotation, largeArcFlag, sweepFlag, dp.scale()) }

            is Close -> Close
        }
    }
}

fun Path.shear(shx: Double = 0.0, shy: Double = 0.0) = transformWith(Transforms.shear(shx, shy))

fun Path.rotate(theta: Double = 0.0, cx: Double = 0.0, cy: Double = 0.0) = transformWith(Transforms.rotate(theta, cx, cy))