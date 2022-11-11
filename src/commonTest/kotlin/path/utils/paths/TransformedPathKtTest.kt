package path.utils.paths

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import path.utils.math.near
import java.lang.Math.toRadians
import kotlin.random.Random

class TransformedPathKtTest {
    private val path = path("M4 8 10 1 13 0 12 3 5 9C6 10 6 11 7 10 7 11 8 12 7 12A1.42 1.42 0 016 13 5 5 0 004 10Q3.5 9.9 3.5 10.5T2 11.8 1.2 11 2.5 9.5 3 9A5 5 90 000 7 1.42 1.42 0 011 6C1 5 2 6 3 6 2 7 3 7 4 8M10 1 10 3 12 3 10.2 2.8 10 1")

    @Test
    fun translate() {
        val (x, y, w, h) = path.bounds
        val tx = Random.nextDouble()
        val ty = Random.nextDouble()
        val (nx, ny, nw, nh) = path.translate(tx, ty).bounds

        assert(x + tx near nx)
        assert(y + ty near ny)
        assert(w near nw)
        assert(h near nh)
    }

    @Test
    fun scale() {
        val (x, y, w, h) = path.bounds
        val sx = Random.nextDouble()
        val sy = Random.nextDouble()
        val (nx, ny, nw, nh) = path.scale(sx, sy).bounds

        assert(x * sx near nx)
        assert(y * sy near ny)
        assert(w * sx near nw)
        assert(h * sy near nh)
    }
}