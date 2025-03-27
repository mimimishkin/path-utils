package path.utils.paths

import path.utils.paths.WindRule.EvenOdd
import path.utils.paths.WindRule.NonNull

interface Operand {
    fun toPath(): Path

    infix fun union(other: Operand): Operand

    infix fun difference(other: Operand): Operand

    infix fun intersect(other: Operand): Operand

    infix fun xor(other: Operand): Operand

    infix fun geometryEquals(other: Operand): Boolean
}

expect fun Path.toOperand(rule: WindRule = EvenOdd): Operand

fun Path.nonZero() = toOperand(NonNull).toPath()

fun Path.evenOdd() = toOperand(EvenOdd).toPath()

infix fun Path.union(other: Path) = (toOperand() union other.toOperand()).toPath()

inline infix fun Path.or(other: Path) = this union other

infix fun Path.difference(other: Path) = (toOperand() difference other.toOperand()).toPath()

inline infix fun Path.not(other: Path) = this difference other

infix fun Path.intersect(other: Path) = (toOperand() intersect other.toOperand()).toPath()

inline infix fun Path.and(other: Path) = this intersect other

infix fun Path.xor(other: Path) = (toOperand() xor other.toOperand()).toPath()

infix fun Path.geometryEquals(other: Path) = toOperand() geometryEquals other.toOperand()