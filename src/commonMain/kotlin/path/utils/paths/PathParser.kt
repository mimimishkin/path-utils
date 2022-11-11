package path.utils.paths

import kotlin.math.pow
import kotlin.properties.Delegates.notNull
import kotlin.Double.Companion.NEGATIVE_INFINITY as NegInfinity
import kotlin.Double.Companion.POSITIVE_INFINITY as Infinity

class PathParseException(cause: String) : RuntimeException(cause)

internal class PathParser(d: String, private val ignoreErrors: Boolean) {
    private var current by notNull<Int>()

    private val chars = d.iterator()
    private fun read() = if (chars.hasNext()) chars.nextChar().code else -1

    fun parse(): Path {
        current = read()
        val path = mutablePath()

        while (true) {
            when (current) {
                in commaSpaces -> {
                    current = read()
                    continue
                }

                'z'.code, 'Z'.code -> path.close().also { current = read() }
                'A'.code -> parseA { rx, ry, ax, laf, sf, x, y -> path.arcTo(rx, ry, ax, laf, sf, x, y) }
                'a'.code -> parseA { rx, ry, ax, laf, sf, x, y -> path.arcToRelative(rx, ry, ax, laf, sf, x, y) }
                in commands -> parseCommand(type(current)) { path.append(it) }

                end -> break
                else -> reportUnexpected(current)
            }
        }

        skipSpaces()
        if (current != end) {
            throw PathParseException("end of stream expected")
        }

        return path
    }

    private fun parseCommand(initialType: CommandType, doCommand: (Command) -> Unit) {
        var type = initialType
        current = read()
        skipSpaces()
        var expectNumber = true

        while (true) {
            if (current !in number) {
                if (expectNumber) reportUnexpected(current)
                return
            }

            doCommand(type.make(List(type.argsCount) { skipCommaSpaces(); parseFloat() }))
            expectNumber = skipCommaSpaces()
            type = type.next()
        }
    }

    private fun parseA(doCommand: (rx: Double, ry: Double, ax: Double, laf: Boolean, sf: Boolean, x: Double, y: Double) -> Unit) {
        current = read()
        skipSpaces()
        var expectNumber = true

        while (true) {
            if (current !in number) {
                if (expectNumber) reportUnexpected(current)
                return
            }

            val rx = parseFloat()
            skipCommaSpaces()
            val ry = parseFloat()
            skipCommaSpaces()
            val ax = parseFloat()
            skipCommaSpaces()

            val laf = when (current) {
                nul -> false
                one -> true
                else -> return reportUnexpected(current)
            }

            current = read()
            skipCommaSpaces()

            val sf = when (current) {
                nul -> false
                one -> true
                else -> return reportUnexpected(current)
            }

            current = read()
            skipCommaSpaces()
            val x = parseFloat()
            skipCommaSpaces()
            val y = parseFloat()

            doCommand(rx, ry, ax, laf, sf, x, y)

            expectNumber = skipCommaSpaces()
        }
    }

    private val pow10 = Array(128) { 10.0.pow(it) }
    private fun parseFloat(): Double {
        fun buildFloat(mant: Int, exp: Int): Double {
            if (exp < -125 || mant == 0) {
                return 0.0
            }

            if (exp >= 128) {
                return if (mant > 0) Infinity else NegInfinity
            }

            if (exp == 0) {
                return mant.toDouble()
            }

            var vmant = mant
            if (vmant >= (1 shl 26)) {
                vmant++  // round up trailing bits if they will be dropped.
            }

            return if (exp > 0) vmant * pow10[exp] else vmant / pow10[-exp]
        }

        var mant = 0
        var mantDig = 0
        var mantPos = true
        var mantRead = false
        var expV = 0
        var expDig = 0
        var expAdj = 0
        var expPos = true
        when (current) {
            minus -> {
                mantPos = false
                current = read()
            }

            plus -> current = read()
        }
        m1@ for (single in listOf(1)) when (current) {
            dot -> {}
            nul -> {
                mantRead = true
                l@ while (true) {
                    current = read()
                    when (current) {
                        one, two, three, four, five, six, seven, eight, nine -> break@l
                        dot, exp, EXP -> break@m1
                        nul -> {}
                        else -> return 0.0
                    }
                }
                mantRead = true
                l@ while (true) {
                    if (mantDig < 9) {
                        mantDig++
                        mant = mant * 10 + (current - nul)
                    } else {
                        expAdj++
                    }
                    current = read()
                    when (current) {
                        nul, one, two, three, four, five, six, seven, eight, nine -> {}
                        else -> break@l
                    }
                }
            }

            one, two, three, four, five, six, seven, eight, nine -> {
                mantRead = true
                l@ while (true) {
                    if (mantDig < 9) {
                        mantDig++
                        mant = mant * 10 + (current - nul)
                    } else {
                        expAdj++
                    }
                    current = read()
                    when (current) {
                        nul, one, two, three, four, five, six, seven, eight, nine -> {}
                        else -> break@l
                    }
                }
            }

            else -> {
                reportUnexpected(current)
                return 0.0
            }
        }
        if (current == dot) {
            current = read()
            m2@ for (single in listOf(1)) when (current) {
                exp, EXP -> if (!mantRead) {
                    reportUnexpected(current)
                    return 0.0
                }

                nul -> {
                    if (mantDig == 0) {
                        l@ while (true) {
                            current = read()
                            expAdj--
                            when (current) {
                                one, two, three, four, five, six, seven, eight, nine -> break@l
                                nul -> {}
                                else -> {
                                    if (!mantRead) {
                                        return 0.0
                                    }
                                    break@m2
                                }
                            }
                        }
                    }
                    l@ while (true) {
                        if (mantDig < 9) {
                            mantDig++
                            mant = mant * 10 + (current - nul)
                            expAdj--
                        }
                        current = read()
                        when (current) {
                            nul, one, two, three, four, five, six, seven, eight, nine -> {}
                            else -> break@l
                        }
                    }
                }

                one, two, three, four, five, six, seven, eight, nine -> l@ while (true) {
                    if (mantDig < 9) {
                        mantDig++
                        mant = mant * 10 + (current - nul)
                        expAdj--
                    }
                    current = read()
                    when (current) {
                        nul, one, two, three, four, five, six, seven, eight, nine -> {}
                        else -> break@l
                    }
                }

                else -> if (!mantRead) {
                    reportUnexpected(current)
                    return 0.0
                }
            }
        }
        when (current) {
            exp, EXP -> {
                current = read()
                when (current) {
                    minus -> {
                        expPos = false
                        current = read()
                        when (current) {
                            nul, one, two, three, four, five, six, seven, eight, nine -> {}
                            else -> {
                                reportUnexpected(current)
                                return 0.0
                            }
                        }
                    }

                    plus -> {
                        current = read()
                        when (current) {
                            nul, one, two, three, four, five, six, seven, eight, nine -> {}
                            else -> {
                                reportUnexpected(current)
                                return 0.0
                            }
                        }
                    }

                    nul, one, two, three, four, five, six, seven, eight, nine -> {}
                    else -> {
                        reportUnexpected(current)
                        return 0.0
                    }
                }
                en@ for (single in listOf(1)) when (current) {
                    nul -> {
                        l@ while (true) {
                            current = read()
                            when (current) {
                                one, two, three, four, five, six, seven, eight, nine -> break@l
                                nul -> {}
                                else -> break@en
                            }
                        }
                        l@ while (true) {
                            if (expDig < 3) {
                                expDig++
                                expV = expV * 10 + (current - nul)
                            }
                            current = read()
                            when (current) {
                                nul, one, two, three, four, five, six, seven, eight, nine -> {}
                                else -> break@l
                            }
                        }
                    }

                    one, two, three, four, five, six, seven, eight, nine -> l@ while (true) {
                        if (expDig < 3) {
                            expDig++
                            expV = expV * 10 + (current - nul)
                        }
                        current = read()
                        when (current) {
                            nul, one, two, three, four, five, six, seven, eight, nine -> {}
                            else -> break@l
                        }
                    }
                }
            }

            else -> {}
        }
        if (!expPos) {
            expV = -expV
        }
        expV += expAdj
        if (!mantPos) {
            mant = -mant
        }
        return buildFloat(mant, expV)
    }

    private fun skipSubPath() {
        while (current in skipUntil) {
            current = read()
        }
    }

    private fun skipSpaces() {
        while (current in spaces) {
            current = read()
        }
    }

    private fun skipCommaSpaces(): Boolean {
        while (current in commaSpaces) {
            current = read()
        }

        if (current != comma)
            return false // no comma.

        do {
            current = read()
        } while (current in spaces)

        return true  // had comma
    }

    private fun reportUnexpected(ch: Int) {
        if (ignoreErrors) {
            skipSubPath()
        } else {
            throw PathParseException("unexpected char with code $ch")
        }
    }

    private companion object {
        const val end = -1

        const val plus = '+'.code
        const val minus = '-'.code
        const val dot = '.'.code
        const val comma = ','.code
        const val nul = '0'.code
        const val one = '1'.code
        const val two = '2'.code
        const val three = '3'.code
        const val four = '4'.code
        const val five = '5'.code
        const val six = '6'.code
        const val seven = '7'.code
        const val eight = '8'.code
        const val nine = '9'.code

        const val exp = 'e'.code
        const val EXP = 'E'.code

        val spaces = listOf(0x20, 0x09, 0x0D, 0x0A)
        val commaSpaces = listOf(0x20, 0x9, 0xD, 0xA)
        val skipUntil = listOf(-1, 'm'.code, 'M'.code)
        val number = listOf(plus, minus, dot, nul, one, two, three, four, five, six, seven, eight, nine)
        val type = { i: Int -> CommandType.find(Char(i))!! }
        val commands = CommandType.symbols.map { it.code }
    }
}