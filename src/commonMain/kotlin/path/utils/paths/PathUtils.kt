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

private val rectType1 = listOf(MoveToRelativeType, HorizontalLineToRelativeType, VerticalLineToRelativeType, HorizontalLineToRelativeType, CloseType)
private val rectType2 = listOf(MoveToRelativeType, VerticalLineToRelativeType, HorizontalLineToRelativeType, VerticalLineToRelativeType, CloseType)
val Path.isRectangle: Boolean get() {
    val rect = minify().validate().toRelative()

    val types = rect.map { it.type }
    if (types == rectType1) {
        val top = rect[1] as HorizontalLineToRelative
        val bottom = rect[3] as HorizontalLineToRelative
        return top.dx near -bottom.dx
    } else if (types == rectType2) {
        val top = rect[1] as Command.VerticalLineToRelative
        val bottom = rect[3] as Command.VerticalLineToRelative
        return top.dy near -bottom.dy
    }

    return false
}

val Path.isClockwise get() = toBeziers().isClockwise

fun Path.contains(x: Double, y: Double) = toBeziers().toCalmPath().contains(x, y)
fun Path.contains(p: Vec2) = contains(p.x, p.y)

fun Path.contains(other: Path) = toBeziers().contains(other.toBeziers())

fun Path.intersects(other: Path) = toBeziers().intersects(other.toBeziers())

fun Path.overlap(other: Path) = toBeziers().overlap(other.toBeziers())