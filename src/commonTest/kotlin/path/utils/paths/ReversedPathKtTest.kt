package path.utils.paths

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class ReversedPathKtTest {
    private val path = path("M4 8 10 1 13 0 12 3 5 9C6 10 6 11 7 10 7 11 8 12 7 12A1.42 1.42 0 016 13 5 5 0 004 10Q3.5 9.9 3.5 10.5T2 11.8 1.2 11 2.5 9.5 3 9A5 5 90 000 7 1.42 1.42 0 011 6C1 5 2 6 3 6 2 7 3 7 4 8M10 1 10 3 12 3 10.2 2.8 10 1")

    @Test
    fun reversePath() {
        val abs1 = path.toAbsolute()
        val abs2 = path.reversePath().reversePath().toAbsolute()

        assertEquals(abs1, abs2)
        assert(abs1 geometryEquals abs2)
    }
}