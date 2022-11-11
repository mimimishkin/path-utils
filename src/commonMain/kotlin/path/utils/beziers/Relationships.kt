package path.utils.beziers

import path.utils.beziers.Relationship.*

private enum class Relationship {
    None, Complex, Intersects, Contains, Contained,
}

fun BeziersPath.contains(other: BeziersPath) = computeRelationship(this, other) == Contains

fun BeziersPath.intersects(other: BeziersPath) = computeRelationship(this, other) == Intersects

fun BeziersPath.overlap(other: BeziersPath) = computeRelationship(this, other) != None

private fun computeRelationship(a: BeziersPath, b: BeziersPath): Relationship {
    if (!a.isCalm || !b.isCalm)
        return computeRelationship(a.toCalmPath(), b.toCalmPath())

    if (!a.fastBounds.overlap(b.fastBounds)) {
        return None
    }

    var relationship: Relationship? = null
    a.iteratePath { ac, aMove ->
        b.iteratePath { bc, _ ->
            val inter = ac.intersectionsWith(bc)
            if (inter.isNotEmpty())
                return Intersects
        }

        if (aMove) {
            if (b.contains(ac.start)) {
                if (relationship == null)
                    relationship = Contained
            } else if (relationship != null) {
                return Complex
            }
        }
    }

    if (relationship == null) {
        b.iteratePath { bc, bMove ->
            if (bMove) {
                if (a.contains(bc.start)) {
                    if (relationship == null)
                        relationship = Contains
                } else if (relationship != null) {
                    return Complex
                }
            }
        }
    }

    return relationship ?: None
}