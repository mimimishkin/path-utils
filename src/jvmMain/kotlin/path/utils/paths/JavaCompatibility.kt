package path.utils.paths

import path.utils.math.MatrixTransform
import path.utils.paths.WindRule.EvenOdd
import path.utils.paths.WindRule.NonNull
import java.awt.Shape
import java.awt.geom.*
import java.awt.geom.PathIterator.*

fun Path.asShape(rule: WindRule = EvenOdd) = object : Shape {
    private val iter get() = getPathIterator(null)

    override fun getBounds2D() = this@asShape.bounds.run { Rectangle2D.Double(x, y, w, h) }

    override fun getBounds() = bounds2D.bounds

    override fun contains(x: Double, y: Double) = Path2D.contains(iter, x, y)

    override fun contains(p: Point2D) = contains(p.x, p.y)

    override fun contains(x: Double, y: Double, w: Double, h: Double) = Path2D.contains(iter, x, y, w, h)

    override fun contains(r: Rectangle2D) = contains(r.x, r.y, r.width, r.height)

    override fun intersects(x: Double, y: Double, w: Double, h: Double) = Path2D.intersects(iter, x, y, w, h)

    override fun intersects(r: Rectangle2D) = intersects(r.x, r.y, r.width, r.height)

    override fun getPathIterator(at: AffineTransform?) = TransformedPathIterator(Iter(this@asShape, rule), at)

    override fun getPathIterator(at: AffineTransform?, flatness: Double) = FlatteningPathIterator(getPathIterator(at), flatness)
}

fun Shape.toPath(): Path = getPathIterator(null).toPath()

fun PathIterator.toPath(): Path {
    val path = mutablePath()

    val args = DoubleArray(6) { 0.0 }
    while (!isDone) {
        when(currentSegment(args)) {
            SEG_MOVETO -> path.moveTo(args[0], args[1])
            SEG_LINETO -> path.lineTo(args[0], args[1])
            SEG_QUADTO -> path.quadTo(args[0], args[1], args[2], args[3])
            SEG_CUBICTO -> path.cubicTo(args[0], args[1], args[2], args[3], args[4], args[5])
            SEG_CLOSE -> path.close()
        }
        next()
    }

    return path.done()
}

private class TransformedPathIterator(
    private val iterator: PathIterator,
    private val transform: AffineTransform?
) : PathIterator {
    override fun getWindingRule() = iterator.windingRule

    override fun isDone() = iterator.isDone

    override fun next() = iterator.next()

    override fun currentSegment(coords: FloatArray) = iterator.currentSegment(coords)
        .also { transform?.transform(coords, 0, coords, 0, coords.size / 2) }

    override fun currentSegment(coords: DoubleArray) = iterator.currentSegment(coords)
        .also { transform?.transform(coords, 0, coords, 0, coords.size / 2) }
}

private class Iter(path: Path, val rule: WindRule) : PathIterator {
    val iterator = path.simplify().iterator()
    var current: Command? = null

    init {
        next()
    }

    override fun getWindingRule() = when (rule) {
        NonNull -> WIND_NON_ZERO
        EvenOdd -> WIND_EVEN_ODD
    }

    override fun isDone() = current == null

    override fun next() {
        current = try {
            iterator.next()
        } catch (_: NoSuchElementException) {
            null
        }
    }

    override fun currentSegment(coords: FloatArray): Int {
        current!!.arguments.forEachIndexed { i, arg -> coords[i] = arg.toFloat() }
        return when (current!!.type) {
            CommandType.LineToType -> SEG_LINETO
            CommandType.MoveToType -> SEG_MOVETO
            CommandType.QuadToType -> SEG_QUADTO
            CommandType.CubicToType -> SEG_CUBICTO
            CommandType.CloseType -> SEG_CLOSE
            else -> throw RuntimeException()
        }
    }

    override fun currentSegment(coords: DoubleArray): Int {
        current!!.arguments.forEachIndexed { i, arg -> coords[i] = arg }
        return when (current!!.type) {
            CommandType.LineToType -> SEG_LINETO
            CommandType.MoveToType -> SEG_MOVETO
            CommandType.QuadToType -> SEG_QUADTO
            CommandType.CubicToType -> SEG_CUBICTO
            CommandType.CloseType -> SEG_CLOSE
            else -> throw RuntimeException()
        }
    }
}

fun MatrixTransform.toAffineTransform(): AffineTransform {
    normalize()
    if (m20 != 0.0 || m21 != 0.0 || m22 != 1.0)
        throw RuntimeException("Transform is not affine")

    return AffineTransform().apply { setTransform(m00, m10, m01, m11, m02, m12) }
}

fun AffineTransform.toMatrixTransform() = MatrixTransform(
    scaleX, shearX, translateX,
    shearY, scaleY, translateY,
    0.0,    0.0,    1.0
)