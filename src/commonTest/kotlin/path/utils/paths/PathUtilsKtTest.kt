package path.utils.paths

import path.utils.math.Tolerance
import path.utils.math.Vec2
import kotlin.test.Test

class PathUtilsKtTest {
    @Test
    fun isRectangle() {
        assert(rect(0.0, 23.0, 45.0, 45.0).isRectangle)
        assert(!circle(0.0, 23.0, 45.0).isRectangle)
    }
    @Test
    fun isReversedRectangle() {
        assert(rect(0.0, 23.0, 45.0, 45.0).reversePath().isRectangle)
    }

    @Test
    fun isClockwise() {
        val circle = circle(10.0, 10.0, 5.0)
        assert(circle.isClockwise)
        assert(!circle.reversePath().isClockwise)
    }

    @Test
    fun contains() {
        val circle = circle(5.0, 5.0, 5.0)
        assert(circle.contains(Vec2(5.0, 5.0)))
        assert(circle.contains(Vec2(5.0, 0.0 + Tolerance)))
        assert(circle.contains(Vec2(10.0 - Tolerance, 5.0)))
        assert(circle.contains(Vec2(5.0, 10.0 - Tolerance)))
        assert(circle.contains(Vec2(0.0 + Tolerance, 5.0)))
    }

    @Test
    fun pathContains() {
        val inner = circle(20.0, 20.0, 5.0)
        val outer = circle(25.0, 15.0, 25.0)
        val outside = circle(125.0, 515.0, 25.0)
        val middle = circle(50.0, 50.0, 40.0)

        assert(outer.contains(inner))
        assert(!inner.contains(outer))

        assert(!outer.contains(outside))

        assert(!outer.contains(middle))

        assert(!outer.contains(inner union middle))
    }

    @Test
    fun intersects() {
        val inner = circle(20.0, 20.0, 5.0)
        val outer = circle(25.0, 15.0, 25.0)
        val outside = circle(125.0, 515.0, 25.0)
        val middle = circle(50.0, 50.0, 40.0)

        assert(outer.intersects(middle))
        assert(!outer.intersects(inner))
        assert(!outer.intersects(outside))
    }
}