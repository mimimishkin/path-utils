package path.utils.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

typealias DoubleRange = ClosedFloatingPointRange<Double>

const val TAU = PI * 2

fun cbrt(n: Double) = n.pow(1 / 3.0)

inline val Int.isOdd get() = this and 1 == 1

inline val Int.isEven get() = this and 1 == 0

var Tolerance = 1e-10

infix fun Double.near(that: Double) =
    abs(this - that) <= Tolerance
//    abs(this - that) <= max(abs(this), abs(that)) * ComputeTolerance

infix fun Double.nearOrLess(that: Double) = this < that || this near that

infix fun Double.nearOrMore(that: Double) = this > that || this near that
infix fun Double.nearIn(range: DoubleRange) =
    this nearOrMore range.start && this nearOrLess range.endInclusive

infix fun DoubleRange.near(that: DoubleRange) =
    this.start near that.start && this.endInclusive near that.endInclusive

fun Iterable<Double>.veryDistinct(): List<Double> {
    val values = toMutableList()
    val distinct = mutableListOf<Double>()
    while (true) {
        val d = values.firstOrNull() ?: return distinct
        values.removeAll { it near d }
        distinct += d
    }
}