package path.utils.math

import kotlin.math.*

internal object JenkinsTraub {
    fun findRoots(coeffs: List<Double>): List<Double> {
        val complexRoots = GlobalEnvironment.rpoly(coeffs.toDoubleArray()).filterNotNull()
        val roots = complexRoots.filter { abs(it.im) < Tolerance }
        return roots.map { it.re }
    }

    private fun quadsd(nn: Int, u: Double, v: Double, p: DoubleArray, q: DoubleArray, rem: DoubleArray) {
        var b = p[1]
        q[1] = b
        var a = p[2] - u * b
        q[2] = a
        for (i in 3..nn) {
            val c = p[i] - u * a - v * b
            q[i] = c
            b = a
            a = c
        }
        rem[0] = a
        rem[1] = b
    }

    private fun quad(a: Double, b: Double, c: Double): Array<Complex> {
        if (a == 0.0 && b == 0.0) {
            return arrayOf(Complex(0.0), Complex(0.0))
        }
        if (a == 0.0) {
            return arrayOf(Complex(-c / b), Complex(0.0))
        } // only one zero
        if (c == 0.0) {
            return arrayOf(Complex(0.0), Complex(-b / a))
        }
        // Compute discriminant, avoiding overflow.
        val b2 = b / 2
        val e: Double
        val d: Double
        if (abs(b2) < abs(c)) {
            val e1 = if (c >= 0) a else -a
            e = b2 * (b2 / abs(c)) - e1
            d = sqrt(abs(e)) * sqrt(abs(c))
        } else {
            e = 1 - a / b2 * (c / b2)
            d = sqrt(abs(e)) * abs(b2)
        }

        return if (e >= 0) {
            val d2 = if (b2 >= 0) -d else d
            val lr = (-b2 + d2) / a // larger real zero
            val sr: Double = if (lr != 0.0) c / lr / a else 0.0
            arrayOf(
                Complex(sr),
                Complex(lr)
            )
        } else {                                                 // complex conjugate zeros
            val z1 = Complex(-b2 / a, abs(d / a))
            arrayOf(z1, z1.conj)
        }
    }

    data class Complex(val re: Double = 0.0, val im: Double = 0.0) {
        val conj get() = Complex(re, -im)

        companion object {
            val Zero = Complex(0.0)
        }
    }

    object GlobalEnvironment {
        // Global variables:
        private var n = 0
        private var nn = 0
        private lateinit var p: DoubleArray
        private var qp: DoubleArray? = null
        private lateinit var k: DoubleArray
        private var qk: DoubleArray? = null
        private var u = 0.0
        private var v = 0.0
        private var a = 0.0
        private var b = 0.0
        private var c = 0.0
        private var d = 0.0
        private var a1 = 0.0
//        private val a2 = 0.0
        private var a3 = 0.0
//        private val a6 = 0.0
        private var a7 = 0.0
        private var e = 0.0
        private var f = 0.0
        private var g = 0.0
        private var h = 0.0
        private var sz : Complex? = null
        private var lz : Complex? = null

        fun rpoly(coeffs: DoubleArray): Array<Complex?> {
            val degree = coeffs.size - 1
            val zeros = arrayOfNulls<Complex>(degree)
            val lo = smalno / eta
            var xx = sqrt(0.5)
            var yy = -xx
            n = degree
            require(coeffs[0] != 0.0) { "The leading coefficient must not be zero." }
            while (n > 0 && coeffs[n] == 0.0) {
                zeros[degree - n] = Complex.Zero
                n--
            }
            nn = n + 1
            p = DoubleArray(nn + 1)
            for (i in 1..nn) {
                p[i] = coeffs[i - 1]
            }
            while (true) {
                if (n < 1)
                    return zeros

                if (n == 1) {
                    zeros[degree - 1] = Complex(-p[2] / p[1])
                    return zeros
                }

                if (n == 2) {
                    val temp1 = quad(p[1], p[2], p[3])
                    zeros[degree - 2] = temp1[0]
                    zeros[degree - 1] = temp1[1]
                    return zeros
                }
                var max = 0.0
                var min = infin
                for (i in 1..nn) {
                    val x = abs(p[i])
                    if (x > max) {
                        max = x
                    }
                    if (x != 0.0 && x < min) {
                        min = x
                    }
                }
                // Scale if there are large or very small coefficients.
                // Computes a scale factor to multiply the coefficients of the polynomial.
                // The scaling is done to avoid overflow and to avoid undetected underflow
                // interfering with the convergence criterion.
                // The factor is a power of the base.
                var sc = lo / min
                if (sc == 0.0) {
                    sc = smalno
                }
                if (sc > 1 && infin / sc >= max || sc <= 1 && max > 10) {
                    val l = ln(sc) / ln(base) + 0.5
                    val factor = base.pow(l)
                    if (factor != 1.0) {
                        for (i in 1..nn) {
                            p[i] = factor * p[i]
                        }
                    }
                }
                // Compute lower bound on moduli of zeros.
                val pt = DoubleArray(nn + 1)
                for (i in 1..nn) {
                    pt[i] = abs(p[i])
                }
                pt[nn] = -pt[nn]
                // Compute upper estimate of bound.
                var x = exp((ln(-pt[nn]) - ln(pt[1])) / n)
                if (pt[n] != 0.0) {
                    // If Newton step at the origin is better, use it.
                    val xm = -pt[nn] / pt[n]
                    if (xm < x) {
                        x = xm
                    }
                }
                // Chop the interval (0,x) until ff <= 0.
                while (true) {
                    val xm = x * 0.1
                    var ff = pt[1]
                    for (i in 2..nn) {
                        ff = ff * xm + pt[i]
                    }
                    if (ff <= 0) {
                        break
                    }
                    x = xm
                }
                var dx = x
                // Do Newton iteration until x converges to two decimal places.
                while (abs(dx / x) > 0.005) {
                    var ff = pt[1]
                    var df = ff
                    for (i in 2..n) {
                        ff = ff * x + pt[i]
                        df = df * x + ff
                    }
                    ff = ff * x + pt[nn]
                    dx = ff / df
                    x = x - dx
                }
                val bnd = x
                // Compute the derivative as the initial k polynomial and do 5 steps with no shift.
                val nm1 = n - 1
                k = DoubleArray(n + 1)
                for (i in 2..n) {
                    k[i] = (nn - i) * p[i] / n
                }
                k[1] = p[1]
                val aa = p[nn]
                val bb = p[n]
                var zerok = k[n] == 0.0
                for (jj in 1..5) {
                    val cc = k[n]
                    if (zerok) {
                        // Use unscaled form of recurrence.
                        for (i in 1..nm1) {
                            val j = nn - i
                            k[j] = k[j - 1]
                        }
                        k[1] = 0.0
                        zerok = k[n] == 0.0
                    } else {
                        // Use scaled form of recurrence if value of k at 0 is nonzero.
                        val t = -aa / cc
                        for (i in 1..nm1) {
                            val j = nn - i
                            k[j] = t * k[j - 1] + p[j]
                        }
                        k[1] = p[1]
                        zerok = abs(k[n]) <= abs(bb) * eta * 10
                    }
                }
                // Save k for restarts with new shifts.
                val temp = DoubleArray(n + 1)
                for (i in 1..n) {
                    temp[i] = k[i]
                }
                // Loop to select the quadratic corresponding to each new shift.
                var cnt = 1
                while (true) {
                    // Quadratic corresponds to a double shift to a non-real point and its complex conjugate.
                    // The point has modulus bnd and amplitude rotated by 94 degrees from the previous shift.
                    val xxx = cosr * xx - sinr * yy
                    yy = sinr * xx + cosr * yy
                    xx = xxx
                    val sr = bnd * xx
                    // si = bnd * yy;                                 // (si is never used)
                    u = -2 * sr
                    v = bnd
                    // Second stage calculation, fixed quadratic.
                    qp = DoubleArray(nn + 1)
                    qk = DoubleArray(n + 1)
                    val nz = fxshfr(20 * cnt, sr)
                    if (nz > 0) {                                     // one or two zeros have been found
                        // The second stage jumps directly to one of the third stage iterations and returns here if successful.
                        // Deflate the polynomial, store the zero or zeros and return to the main algorithm.
                        zeros[degree - n] = sz
                        if (nz > 1) {
                            zeros[degree - n + 1] = lz
                        }
                        nn = nn - nz
                        n = nn - 1
                        for (i in 1..nn) {
                            p[i] = qp!![i]
                        }
                        qp = null // just to be sure that qp is no longer used from here on
                        qk = null // just to make sure that qk is no longer used from here on
                        break
                    } // continue with main loop, after zeros hav been found
                    // If the iteration is unsuccessful another quadratic is chosen after restoring k.
                    for (i in 1..n) {
                        k[i] = temp[i]
                    }
                    // Failure if no convergence with 20 shifts.
                    if (cnt++ > 20) {
                        throw RuntimeException("No convergence.")
                    }
                }
            }
        }

        // Computes up to l2 fixed shift k-polynomials, testing for convergence in the linear
        // or quadratic case. Initiates one of the variable shift iterations and returns with
        // the number of zeros found.
        //
        // @param l2
        //    limit of fixed shift steps.
        // @return
        //    the number of zeros found.
        private fun fxshfr(l2: Int, sr: Double): Int {
            var ots = 0.0
            var otv = 0.0
            var betav = 0.25
            var betas = 0.25
            var oss = sr
            var ovv = v
            // Evaluate polynomial by synthetic division.
            val temp1 = DoubleArray(2)
            quadsd(nn, u, v, p, qp!!, temp1)
            a = temp1[0]
            b = temp1[1]
            var type = calcsc()
            for (j in 1..l2) {
                // Calculate next k polynomial and estimate v.
                nextk(type)
                type = calcsc()
                val temp2 = newest(type)
                var ui = temp2[0]
                var vi = temp2[1]
                val vv = vi
                // Estimate s
                var ss = 0.0
                if (k[n] != 0.0) {
                    ss = -p[nn] / k[n]
                }
                var tv = 1.0
                var ts = 1.0
                if (j != 1 && type != 3) {
                    // Compute relative measures of convergence of s and v sequences.
                    if (vv != 0.0) {
                        tv = abs((vv - ovv) / vv)
                    }
                    if (ss != 0.0) {
                        ts = abs((ss - oss) / ss)
                    }
                    // If decreasing, multiply two most recent convergence measures.
                    val tvv: Double = if (tv < otv) tv * otv else 1.0
                    val tss: Double = if (ts < ots) ts * ots else 1.0
                    // Compare with convergence criteria.
                    val vpass = tvv < betav
                    val spass = tss < betas
                    if (spass || vpass) {
                        // At least one sequence has passed the convergence test. Store variables before iterating.
                        val svu = u
                        val svv = v
                        val svk = DoubleArray(n + 1)
                        for (i in 1..n) {
                            svk[i] = k[i]
                        }
                        var s = ss
                        // Choose iteration according to the fastest converging sequence.
                        var vtry = false
                        var stry = false
                        var state = if (spass && (!vpass || tss < tvv)) 40 else 20
                        while (state != 70) {
                            // State machine to implement the Fortran spaghetti code.
                            // The state numbers correspond to the labels within the Fortran source code.
                            when (state) {
                                20 -> {
                                    val nz = quadit(ui, vi)
                                    if (nz > 0) {
                                        return nz
                                    }
                                    // Quadratic iteration has failed. Flag that it has been tried and decrease
                                    // the convergence criterion.
                                    vtry = true
                                    betav *= 0.25
                                    // Try linear iteration if it has not been tried and the s sequence is converging.
                                    if (stry || !spass) {
//                                        state = 50
                                        break
                                    }
                                    var i = 1
                                    while (i <= n) {
                                        k[i] = svk[i]
                                        i++
                                    }
                                    state = 40
                                }

                                40 -> {
                                    val realitOut = realit(s)
                                    if (realitOut.nz > 0) {
                                        return realitOut.nz
                                    }
                                    s = realitOut.sss
                                    // Linear iteration has failed. Flag that it has been tried and decrease the
                                    // convergence criterion.
                                    stry = true
                                    betas *= 0.25
                                    if (realitOut.iflag) {
//                                        state = 50
                                        break
                                    }
                                    // If linear iteration signals an almost double real zero attempt quadratic interation.
                                    ui = -(s + s)
                                    vi = s * s
                                    state = 20
                                }

                                50 -> {

                                    // Restore variables.
                                    u = svu
                                    v = svv
                                    var i = 1
                                    while (i <= n) {
                                        k[i] = svk[i]
                                        i++
                                    }
                                    // Try quadratic iteration if it has not been tried and the v sequence is converging.
                                    if (vpass && !vtry) {
//                                        state = 20
                                        break
                                    }
                                    // Recompute qp and scalar values to continue the second stage.
                                    val temp3 = DoubleArray(2)
                                    quadsd(nn, u, v, p, qp!!, temp3)
                                    a = temp3[0]
                                    b = temp3[1]
                                    type = calcsc()
                                    state = 70
                                }

                                else -> throw AssertionError()
                            }
                        }
                    }
                }
                ovv = vv
                oss = ss
                otv = tv
                ots = ts
            }
            return 0
        }

        // Variable-shift k-polynomial iteration for a quadratic factor converges only if the zeros are
        // equimodular or nearly so.
        // uu,vv - coefficients of starting quadratic
        // Returns the number of zeros found.
        private fun quadit(uu: Double, vv: Double): Int {
            var tried = false
            var omp = 0.0
            var relstp = 0.0
            u = uu
            v = vv
            var j = 0
            // main loop
            while (true) {
                val zeros = quad(1.0, u, v)
                sz = zeros[0]
                lz = zeros[1]
                // Return if roots of the quadratic are real and not close to multiple or
                // nearly equal and of opposite sign.
                // 2013-07-29 chdh: There is no test whether the roots are real?
                if (abs(abs(sz!!.re) - abs(lz!!.re)) > 0.01 * abs(
                        lz!!.re
                    )
                ) {
                    return 0
                }
                // Evaluate polynomial by quadratic synthetic division.
                val temp1 = DoubleArray(2)
                quadsd(nn, u, v, p, qp!!, temp1)
                a = temp1[0]
                b = temp1[1]
                val mp = abs(a - sz!!.re * b) + abs(sz!!.im * b)
                // Compute a rigorous bound on the rounding error in evaluting p.
                val zm = sqrt(abs(v))
                var ee = 2 * abs(qp!![1])
                val t = -sz!!.re * b
                for (i in 2..n) {
                    ee = ee * zm + abs(qp!![i])
                }
                ee = ee * zm + abs(a + t)
                ee = (5 * mre + 4 * are) * ee -
                        (5 * mre + 2 * are) * (abs(a + t) + abs(b) * zm) +
                        2 * are * abs(t)
                // Iteration has converged sufficiently if the polynomial value is less than 20 times this bound.
                if (mp <= 20 * ee) {
                    return 2
                }
                j++
                // Stop iteration after 20 steps.
                if (j > 20) {
                    return 0
                }
                if (j >= 2 && relstp <= 0.01 && mp >= omp && !tried) {
                    // A cluster appears to be stalling the convergence.
                    // Five fixed shift steps are taken with a u,v close to the cluster.
                    if (relstp < eta) {
                        relstp = eta
                    }
                    relstp = sqrt(relstp)
                    u -= u * relstp
                    v += v * relstp
                    val temp2 = DoubleArray(2)
                    quadsd(nn, u, v, p, qp!!, temp2)
                    a = temp2[0]
                    b = temp2[1]
                    for (i in 1..5) {
                        val type = calcsc()
                        nextk(type)
                    }
                    tried = true
                    j = 0
                } // reset loop counter
                omp = mp
                // Calculate next k polynomial and new u and v.
                val type1 = calcsc()
                nextk(type1)
                val type2 = calcsc()
                val temp2 = newest(type2)
                val ui = temp2[0]
                val vi = temp2[1]
                // If vi is zero the iteration is not converging.
                if (vi == 0.0) {
                    return 0
                }
                relstp = abs((vi - v) / vi)
                u = ui
                v = vi
            }
        }

        private class RealitOut(var sss: Double, var nz: Int, var iflag: Boolean)

        private fun realit(sss: Double): RealitOut {
            var omp = 0.0
            var t = 0.0
            var s = sss
            var j = 0
            while (true) {
                var pv = p[1]
                qp!![1] = pv
                for (i in 2..nn) {
                    pv = pv * s + p[i]
                    qp!![i] = pv
                }
                val mp = abs(pv)
                val ms = abs(s)
                var ee = mre / (are + mre) * abs(
                    qp!![1]
                )
                for (i in 2..nn) {
                    ee = ee * ms + abs(qp!![i])
                }
                if (mp <= 20 * ((are + mre) * ee - mre * mp)) {
                    sz = Complex(s)
                    return RealitOut(sss, 1, false)
                }
                j++
                if (j > 10) {
                    return RealitOut(sss, 0, false)
                }
                if (j >= 2 && abs(t) <= 0.001 * abs(s - t) && mp > omp) {
                    return RealitOut(s, 0, true)
                }
                omp = mp
                var kv = k[1]
                qk!![1] = kv
                for (i in 2..n) {
                    kv = kv * s + k[i]
                    qk!![i] = kv
                }
                if (abs(kv) <= abs(k[n]) * 10 * eta) {
                    k[1] = 0.0
                    for (i in 2..n) {
                        k[i] = qk!![i - 1]
                    }
                } else {
                    t = -pv / kv
                    k[1] = qp!![1]
                    for (i in 2..n) {
                        k[i] = t * qk!![i - 1] + qp!![i]
                    }
                }
                kv = k[1]
                for (i in 2..n) {
                    kv = kv * s + k[i]
                }
                t = 0.0
                if (abs(kv) > abs(k[n]) * 10 * eta) {
                    t = -pv / kv
                }
                s += t
            }
        }

        // this routine calculates scalar quantities used to compute the next k
        // polynomial and new estimates of the quadratic coefficients.
        // Returns an integer (type) indicating how the calculations are
        // normalized to avoid overflow.
        private fun calcsc(): Int {
            val temp = DoubleArray(2)
            quadsd(n, u, v, k, qk!!, temp) // synthetic division of k by the quadratic 1,u,v
            c = temp[0]
            d = temp[1]
            if (abs(c) <= abs(k[n]) * 100 * eta || abs(d) <= abs(
                    k[n - 1]
                ) * 100 * eta
            ) {
                return 3
            } // type=3 indicates the quadratic is almost a factor c of k
            return if (abs(d) < abs(c)) {
                e = a / c
                f = d / c
                g = u * e
                h = v * b
                a3 = a * e + (h / c + g) * b
                a1 = b - a * (d / c)
                a7 = a + g * d + h * f
                1
            } // type=1 indicates that all formulas are divided by c
            else {
                e = a / d
                f = c / d
                g = u * b
                h = v * b
                a3 = (a + g) * e + h * (b / d)
                a1 = b * f - a
                a7 = (f + u) * a + h
                2
            }
        } // type=2 indicates that all formulas are divided by d

        // Computes the next k polynomials using scalars computed in calcsc.
        private fun nextk(type: Int) {
            if (type == 3) {                                        // use unscaled form of the recurrence
                k[1] = 0.0
                k[2] = 0.0
                for (i in 3..n) {
                    k[i] = qk!![i - 2]
                }
                return
            }
            val temp = if (type == 1) b else a
            if (abs(a1) > abs(temp) * eta * 10) {
                // Use scaled form of the recurrence.
                a7 = a7 / a1
                a3 = a3 / a1
                k[1] = qp!![1]
                k[2] = qp!![2] - a7 * qp!![1]
                for (i in 3..n) {
                    k[i] = a3 * qk!![i - 2] - a7 * qp!![i - 1] + qp!![i]
                }
            } else {
                // If a1 is nearly zero then use a special form of the recurrence.
                k[1] = 0.0
                k[2] = -a7 * qp!![1]
                for (i in 3..n) {
                    k[i] = a3 * qk!![i - 2] - a7 * qp!![i - 1]
                }
            }
        }

        // Compute new estimates of the quadratic coefficients using the scalars computed in calcsc.
        private fun newest(type: Int): DoubleArray {
            // Use formulas appropriate to setting of type.
            if (type == 3) {                                        // if type=3 the quadratic is zeroed
                return doubleArrayOf(0.0, 0.0)
            }
            val a4: Double
            val a5: Double
            if (type == 2) {
                a4 = (a + g) * f + h
                a5 = (f + u) * c + v * d
            } else {
                a4 = a + u * b + h * f
                a5 = c + (u + v * f) * d
            }
            // Evaluate new quadratic coefficients.
            val b1 = -k[n] / p[nn]
            val b2 = -(k[n - 1] + b1 * p[n]) / p[nn]
            val c1 = v * b2 * a1
            val c2 = b1 * a7
            val c3 = b1 * b1 * a3
            val c4 = c1 - c2 - c3
            val temp = a5 + b1 * a4 - c4
            if (temp == 0.0) {
                return doubleArrayOf(0.0, 0.0)
            }
            val uu = u - (u * (c3 + c2) + v * (b1 * a1 + b2 * a7)) / temp
            val vv = v * (1 + c4 / temp)
            return doubleArrayOf(uu, vv)
        }

        private const val eta = 2.22E-16
        private const val base = 2.0 // the base of the floating-point number system used
        private const val infin = Double.MAX_VALUE
        private const val smalno = 2.2250738585072014E-308
        private const val are = eta
        private const val mre = eta
        private const val rotationAngleDeg = 94.0
        private const val rotationAngle = rotationAngleDeg / 360 * 2 * PI
        private val cosr = cos(rotationAngle)
        private val sinr = sin(rotationAngle)
    }
}