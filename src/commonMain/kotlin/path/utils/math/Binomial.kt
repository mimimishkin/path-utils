package path.utils.math

private val lut = mutableListOf<List<Double>>(
             listOf(1.0),           // n=0
            listOf(1.0,1.0),          // n=1
           listOf(1.0,2.0,1.0),         // n=2
          listOf(1.0,3.0,3.0,1.0),        // n=3
         listOf(1.0,4.0,6.0,4.0,1.0),       // n=4
        listOf(1.0,5.0,10.0,10.0,5.0,1.0),    // n=5
       listOf(1.0,6.0,15.0,20.0,15.0,6.0,1.0)   // n=6
)
private val nul = listOf(0.0)

internal fun binomial(n: Int, k: Int): Double {
    while (n >= lut.size) {
        val row = mutableListOf<Double>()

        (nul + lut.last() + nul).zipWithNext().forEach { (cur, next) ->
            row += cur + next
        }

        lut.add(row)
    }

    return lut[n][k]
}