package path.utils.paths

import path.utils.beziers.contains
import path.utils.beziers.intersects
import path.utils.beziers.overlap
import path.utils.beziers.toBeziers
import path.utils.math.Vec2
import path.utils.math.near
import path.utils.paths.Command.HorizontalLineToRelative
import path.utils.paths.CommandType.*

enum class WindRule { NonNull, EvenOdd }

val Path.bounds get() = computeBounds(this)

private val rectType = listOf(MoveToType, HorizontalLineToRelativeType, VerticalLineToRelativeType, HorizontalLineToRelativeType, CloseType)
val Path.isRectangle: Boolean get() {
    var rect = minify().toRelative().validate()
    if (!rect.isClockwise) rect = rect.reversePath()

    if (rect.map { it.type } == rectType) {
        val top = rect[1] as HorizontalLineToRelative
        val bottom = rect[3] as HorizontalLineToRelative
        return top.dx near -bottom.dx
    }

    return false
}

val Path.isClockwise get() = toBeziers().isClockwise

fun Path.contains(x: Double, y: Double) = toBeziers().toCalmPath().contains(x, y)
fun Path.contains(p: Vec2) = contains(p.x, p.y)

fun Path.contains(other: Path) = toBeziers().contains(other.toBeziers())

fun Path.intersects(other: Path) = toBeziers().intersects(other.toBeziers())

fun Path.overlap(other: Path) = toBeziers().overlap(other.toBeziers())