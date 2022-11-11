package path.utils.math

import path.utils.beziers.Bezier
import path.utils.math.Polynomial.Companion.Zero
import path.utils.math.Polynomial.Companion.fromRoots
import kotlin.math.*
import kotlin.system.measureNanoTime

fun solve(coefficients: List<Double>): List<Double> {
    if (coefficients.firstOrNull() == 0.0) {
        return solve(coefficients.dropWhile { it == 0.0 })
    }

    if (coefficients.size <= 1) {
        return listOf()
    }

    if (coefficients.size == 2) {
        val a = coefficients[0]
        val b = coefficients[1]
        return if (a != 0.0) listOf(-b / a) else listOf()
    }

    if (coefficients.size == 3) {
        val a = coefficients[0]
        val b = coefficients[1] / a
        val c = coefficients[2] / a

        val discrim = b * b - 4 * c
        return when {
            discrim near 0.0 -> {
                val r = -b / 2
                listOf(r, r)
            }
            discrim > 0 -> {
                val e = sqrt(discrim)
                listOf((-b + e) / 2, (-b - e) / 2)
            }
            else -> listOf()
        }
    }

    /*if (coefficients.size == 4) {
        val a = coefficients[0]
        val b = coefficients[1] / a
        val c = coefficients[2] / a
        val d = coefficients[3] / a
        val sqrt3 = sqrt(3.0)

        val f = (3 * c - b * b) / 3
        val g = (2 * b * b * b - 9 * b * c + 27 * d) / 27
        val h = g * g / 4 + f * f * f / 27

        return when {
            f near 0.0 && g near 0.0 && h near 0.0 -> {
                // all roots are real and equal
                val root = cbrt(d)
                listOf(
                    root,
                    root,
                    root
                )
            }

            h <= 0 -> {
                // all 3 roots are real
                val i = sqrt(g * g / 4 - h)
                val j = cbrt(i)
                val k = acos(-(g / (2 * i)))
                val l = j * -1
                val m = cos(k / 3)
                val n = sqrt3 * sin(k/3)
                val p = -(b / 3)

                listOf(
                    2 * j * m - b / 3,
                    l * (m+n) + p,
                    l * (m-n) + p
                )
            }

            else -> {
                // one real and two imaginary roots
                val r = -g / 2 + sqrt(h)
                val s = cbrt(r)
                val t = -g / 2- sqrt(h)
                val u = cbrt(t)

                listOf((s + u) - (b / 3))
            }
        }
    }*/

    /*if (coefficients.size == 5) {
        val a = coefficients[0]
        val b = coefficients[1] / a
        val c = coefficients[2] / a
        val d = coefficients[3] / a
        val e = coefficients[4] / a

        val z0 = b / 4
        val b2 = b * b
        val p = -3 * b2 / 8 + c
        val q = b * b2 / 8 - 1.0 / 2 * b * c + d
        val r = -3.0 / 256 * b2 * b2 + c * b2 / 16 - b * d / 4 + e

        val y = solve(listOf(8.0, -4 * p, -8 * r, 4 * r * p - q * q)).last()

        val a0 = sqrt(-p + 2 * y)
        val b0 = if (a0 == 0.0) y * y - r else -q / 2 / a0

        val r1r2 = solve(listOf(1.0, a0, y + b0))
        val r3r4 = solve(listOf(1.0, -a0, y - b0))
        return (r1r2 + r3r4).map { it - z0 }
    }*/

    return JenkinsTraub.findRoots(coefficients)
}

internal fun implictize(a: Bezier, x: Polynomial, y: Polynomial): List<List<Polynomial>> {
    val n = a.order

    fun l(i: Int, j: Int): Polynomial {
        val det = x * (a[i].y - a[j].y) - y * (a[i].x - a[j].x) + (a[i].x * a[j].y - a[i].y * a[j].x)
        val bin = binomial(n, i) * binomial(n, j)
        return det * bin
    }

    val computedSmallL = mutableMapOf<Pair<Int, Int>, Polynomial>()
    fun getSmallL(i: Int, j: Int): Polynomial {
        val l = computedSmallL[i to j]
        if (l != null) return l

        computedSmallL[i to j] = l(i, j)
        return computedSmallL[i to j]!!
    }

    fun L(i: Int, j: Int): Polynomial {
        var sum = Zero
        for (m in 0..min(i, j)) {
            val k = i + j + 1 - m
            if (max(k, m) <= n) {
                sum += getSmallL(k, m)
            }
        }
        return sum
    }

    return List(n) { r -> List(n) { c -> L(n - r - 1, n - c - 1) } }
}

internal fun det(matrix: List<List<Polynomial>>): Polynomial {
    val n = matrix.size
    if (n == 1) {
        return matrix[0][0]
    }

    if (n == 2) {
        return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
    }

    if (n == 3) {
        val x = matrix[1][1] * matrix[2][2] - matrix[1][2] * matrix[2][1]
        val y = matrix[1][0] * matrix[2][2] - matrix[1][2] * matrix[2][0]
        val z = matrix[1][0] * matrix[2][1] - matrix[1][1] * matrix[2][0]
        return x * matrix[0][0] - y * matrix[0][1] + z * matrix[0][2]
    }

//    val sum1 = List(n) { i -> List(n) { j -> matrix[j        ][(i + j) % n] }.product() }.sum()
//    val sum2 = List(n) { i -> List(n) { j -> matrix[n - j - 1][(i + j) % n] }.product() }.sum()
//    val ret = sum1 - sum2

    var det = Zero
    val subN = n - 1
    val temp = List(subN) { MutableList(subN) { Zero } }
    for (col in 0 until n) {
        for (row in 1 until n) {
            for (subCol in 0 until col) {
                temp[row - 1][subCol] = matrix[row][subCol]
            }
            for (subCol in col + 1 until n) {
                temp[row - 1][subCol - 1] = matrix[row][subCol]
            }
        }

        det += matrix[0][col] * det(temp) * (-1.0).pow(col)
    }

    return det
}