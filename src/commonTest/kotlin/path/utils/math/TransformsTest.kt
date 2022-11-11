package path.utils.math

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import path.utils.paths.*

class TransformsTest {
    private infix fun Command.near(other: Command) = type == other.type && arguments.zip(other.arguments).all { (a, b) -> a near b }

    private infix fun Path.near(other: Path) = size == other.size && this.zip(other).all { (a, b) -> a near b }

    private val path = path("M4 8 10 1 13 0 12 3 5 9C6 10 6 11 7 10 7 11 8 12 7 12A1.42 1.42 0 016 13 5 5 0 004 10Q3.5 9.9 3.5 10.5T2 11.8 1.2 11 2.5 9.5 3 9A5 5 90 000 7 1.42 1.42 0 011 6C1 5 2 6 3 6 2 7 3 7 4 8M10 1 10 3 12 3 10.2 2.8 10 1")

    @Test
    fun rectToRect() {
        val a = Bounds(10.0, 10.0, 15.0, 20.0)
        val b = Bounds(15.0, 15.0, 60.0, 120.0)
        val matrix = Transforms.rectToRect(a, b)

        val aPoly = rect(a).simplify()
        val bPoly = rect(b).simplify()
        val abPoly = aPoly.transformWith(matrix)

        assert(bPoly near abPoly)
    }

    @Test
    fun polyToPoly() {
        fun checkPolyToPoly(a: List<Vec2>, b: List<Vec2>): Boolean {
            val matrix = Transforms.polyToPoly(a, b)!!

            val aPoly = polygon(a)
            val bPoly = polygon(b)
            val abPoly = aPoly.transformWith(matrix)

            return bPoly near abPoly
        }

        val a4 = listOf(Vec2(0.0, 7.0), Vec2(-4.0, -4.0), Vec2(12.0, -6.0), Vec2(15.0, 3.0))
        val b4 = listOf(Vec2(12.0, 18.0), Vec2(30.0, 17.0), Vec2(17.0, 5.0), Vec2(7.0, 7.0))

        assert(checkPolyToPoly(a4, b4))

        val a3 = listOf(Vec2(2.0, 7.0), Vec2(7.0, 2.0), Vec2(11.0, 5.0))
        val b3 = listOf(Vec2(8.0, 12.0), Vec2(8.0, 9.0), Vec2(11.0, 9.0))

        assert(checkPolyToPoly(a3, b3))

        val a2 = listOf(Vec2(2.0, 7.0), Vec2(-1.0, -2.0))
        val b2 = listOf(Vec2(8.0, 12.0), Vec2(16.0, 8.0))

        assert(checkPolyToPoly(a2, b2))

        val a1 = listOf(Vec2(4.0, 5.0))
        val b1 = listOf(Vec2(17.0, 19.0))

        assert(checkPolyToPoly(a1, b1))

        val a0 = listOf<Vec2>()
        val b0 = listOf<Vec2>()

        assert(Transforms.polyToPoly(a0, b0) == Transforms.identical())
    }

    @Test
    fun inverted() {
        val matrix = Transforms.matrix(
            2.0, 3.0, 47.0,
            1.5, 2.0, 48.0,
            0.05, 0.12, 2.0,
        )

        val a = path.transformWith(matrix)
        val b = a.transformWith(matrix.invert()!!)

        assert(b near path.simplify())
    }
}