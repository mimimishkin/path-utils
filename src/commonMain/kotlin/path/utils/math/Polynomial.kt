package path.utils.math

import path.utils.math.Polynomial.Companion.One
import path.utils.math.Polynomial.Companion.Zero
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sign

class NoMoreDerivativeException(msg: String) : Exception(msg)

class Polynomial(val coefficients: List<Double>) {
    companion object {
        fun fromRoots(vararg roots: Double) =
            roots.map { Polynomial(1.0, -it) }.fold(One) { v, cur -> v * cur }

        val Zero = Polynomial(0.0)

        val One = Polynomial(1.0)

        val X = Polynomial(1.0, 0.0)

        val OneMinusX = Polynomial(-1.0, 1.0)

        fun Num(n: Double) = Polynomial(n)
    }

    constructor(vararg coefficients: Double) : this(coefficients.asList())

    inline val n get() = coefficients.size - 1

    inline val isEmpty get() = n == -1

    inline val isNumber get() = n == 0

    inline val isLine get() = n == 1

    inline val isSimple get() = coefficients.firstOrNull()?.near(0.0) == false

    fun eval(x: Double) = coefficients.fold(0.0) { r, c -> r * x + c }

    fun eval(x: Polynomial) = coefficients.fold(Zero) { r, c -> r * x + c }

    val derivative by lazy {
        if (n < 0) {
            throw NoMoreDerivativeException("polynomial has no derivative")
        } else {
            Polynomial(List(n) { coefficients[it] * (n - it) })
        }
    }

    fun derivativeOrNull() = if (n < 1) null else derivative

    fun simplify(): Polynomial {
        val a = coefficients.find { !(it near 0.0) } ?: return Zero
        val simplified = Polynomial(coefficients.dropWhile { it near 0.0 }.map { it / a })
        return if (simplified.isEmpty) Zero else simplified
    }

    val roots by lazy { solve(coefficients) }

    override fun toString(): String {
        var result = String()
        var first = true

        for ((i, c) in coefficients.withIndex()) {
            val sign = c.sign
            if (sign != 0.0) {
                val number = c.absoluteValue.let {
                    when {
                        i == coefficients.lastIndex -> it.toString()
                        it != 1.0 -> it.toString()
                        else -> ""
                    }
                }
                val x = when (val v = n - i) {
                        0 -> ""
                        1 -> "x"
                        else -> "x" + v.toString().toPow()
                    }
                val minus = sign == -1.0

                result += when {
                    first && minus ->  "-"
                    first -> ""
                    minus -> " - "
                    else -> " + "
                } + number + x

                first = false
            }
        }

        return result.takeIf { it.isNotEmpty() } ?: "0"
    }

    fun toFullString(): String {
        var result = String()

        for ((i, c) in coefficients.withIndex()) {
            result += when {
                i == 0 && c < 0 -> "-"
                i == 0 -> ""
                c < 0 -> " - "
                else -> " + "
            } + c.absoluteValue.toString() + ("x" + (n - i).toString().toPow())
        }

        return result
    }

    private fun String.toPow(): String {
        return this
            .replace('0', '⁰')
            .replace('1', '¹')
            .replace('2', '²')
            .replace('3', '³')
            .replace('4', '⁴')
            .replace('5', '⁵')
            .replace('6', '⁶')
            .replace('7', '⁷')
            .replace('8', '⁸')
            .replace('9', '⁹')
    }

    private operator fun get(i: Int) = coefficients[i]

    operator fun unaryMinus() = Polynomial(coefficients.map { -it })

    operator fun unaryPlus() = this

    operator fun plus(a: Double) = Polynomial(coefficients.take(n) + (this[n] + a))

    operator fun minus(a: Double) = plus(-a)

    operator fun times(a: Double) = Polynomial(coefficients.map { it * a })

    operator fun div(a: Double) = Polynomial(coefficients.map { it / a })

    operator fun plus(that: Polynomial): Polynomial {
        val n = max(this.n, that.n) + 1
        val coefficients = MutableList(n) { 0.0 }

        for (i in this.coefficients.indices) {
            coefficients[n - this.n - 1 + i] = this[i]
        }

        for (i in that.coefficients.indices) {
            coefficients[n - that.n - 1 + i] += that[i]
        }

        return Polynomial(coefficients)
    }

    operator fun minus(that: Polynomial): Polynomial {
        val n = max(this.n, that.n) + 1
        val coefficients = MutableList(n) { 0.0 }

        for (i in this.coefficients.indices) {
            coefficients[n - this.n - 1 + i] = this[i]
        }

        for (i in that.coefficients.indices) {
            coefficients[n - that.n - 1 + i] -= that[i]
        }

        return Polynomial(coefficients)
    }

    operator fun times(that: Polynomial): Polynomial {
        val n = this.n + that.n + 1
        val coefficients = MutableList(n) { 0.0 }

        for (i in 0..this.n)
            for (j in 0..that.n)
                coefficients[i + j] += this[i] * that[j]

        return Polynomial(coefficients)
    }

//    data class LongDivisionResult(val quotient: Polynomial, val remainder: Polynomial)
//
//    operator fun div(that: Polynomial): LongDivisionResult {
//        var q = Polynomial(0.0)
//        var r = Polynomial(this.coefficients)
//        val zeroP = Polynomial(0.0)
//
//        while (zeroP != r && r.n >= that.n) {
//            val rc = r.coefficients.find { it != 0.0 } ?: 0.0
//            val dc = that.coefficients.find { it != 0.0 } ?: 0.0
//
//            val tc = rc / dc
//            val te = r.n - that.n
//            val t = Polynomial(tc, *DoubleArray(te))
//
//            q += t
//            r -= t * that
//        }
//
//        return LongDivisionResult(q, r)
//    }

    fun pow(e: Int) = when {
        e == 0 -> One
        n == -1 -> Zero
        e == 1 -> this
        else -> {
            var poly = this
            repeat(e - 1) { poly *= this }
            poly
        }
    }

    operator fun component1() = coefficients.component1()
    operator fun component2() = coefficients.component2()
    operator fun component3() = coefficients.component3()
    operator fun component4() = coefficients.component4()
    operator fun component5() = coefficients.component5()

    override fun equals(other: Any?) =
        if (other !is Polynomial) false else simplify().coefficients == other.simplify().coefficients

    override fun hashCode() = simplify().coefficients.hashCode()
}

fun List<Polynomial>.sum() = reduceOrNull { a, b -> a + b } ?: Zero

fun List<Polynomial>.product() = reduceOrNull { a, b -> a * b } ?: One