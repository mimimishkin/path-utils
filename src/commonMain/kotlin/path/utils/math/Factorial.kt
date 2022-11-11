package path.utils.math

private val precomputed = listOf(
    1.0,
    1.0,
    2.0,
    6.0,
    24.0,
    120.0,
    720.0,
    5040.0,
    40320.0,
    362880.0,
    3628800.0,
    39916800.0,
    479001600.0,
    6227020800.0,
    87178291200.0,
    1307674368000.0,
    20922789888000.0,
    355687428096000.0,
    6402373705728000.0,
    121645100408832000.0,
    2432902008176640000.0,
    51090942171709440000.0,
    1124000727777607680000.0,
    25852016738884976640000.0,
    620448401733239439360000.0,
    15511210043330985984000000.0,
    403291461126605635584000000.0,
    10888869450418352160768000000.0,
    304888344611713860501504000000.0,
    8841761993739701954543616000000.0,
)

private fun recfact(start: Double, n: Double): Double {
    var i: Double
    if (n <= 16) {
        var r = start
        i = start + 1.0
        while (i < start + n) {
            r *= i
            i++
        }
        return r
    }
    i = n / 2
    return recfact(start, i) * recfact(start + i, n - i)
}

internal fun factorial(n: Int): Double {
    return if (n in precomputed.indices) precomputed[n] else recfact(1.0, n.toDouble())
}