package path.utils.paths

import path.utils.paths.WindRule.EvenOdd
import java.awt.geom.Area

private class AreaOperand(val area: Area) : Operand {
    private fun Operand.toAreaOperand() =
        if (this is AreaOperand) this else AreaOperand(toPath().toArea())

    override fun toPath() = area.toPath()

    override fun union(other: Operand) = also { area.add(other.toAreaOperand().area) }

    override fun difference(other: Operand) = also { area.subtract(other.toAreaOperand().area) }

    override fun intersect(other: Operand) = also { area.intersect(other.toAreaOperand().area) }

    override fun xor(other: Operand) = also { area.exclusiveOr(other.toAreaOperand().area) }

    override fun geometryEquals(other: Operand) = area.equals(other.toAreaOperand().area)
}

fun Path.toArea(windRule: WindRule = EvenOdd) = Area(asShape(windRule))

actual fun Path.toOperand(rule: WindRule): Operand = AreaOperand(toArea(rule))


