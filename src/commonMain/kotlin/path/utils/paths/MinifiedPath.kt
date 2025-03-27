package path.utils.paths

import path.utils.beziers.Bezier
import path.utils.beziers.toBezier
import path.utils.beziers.toCommand
import path.utils.math.Vec2
import path.utils.math.orZero
import path.utils.paths.Command.*

fun Path.minify(): Path {
    val path = mutablePath()

    var current: Bezier? = null
    var last: Vec2? = null
    var anchor: Vec2? = null

    fun placeMove(from: Vec2, move: Vec2) {
        val moveTo = MoveTo(from).minify(last.orZero(), null, move)
        if (moveTo != null) path.append(moveTo)
    }

    fun doAppend(move: Vec2) {
        val (from, command) = current!!.representAsLine().toCommand()
        val previous = path.lastOrNull()
        val lastCurve = previous.isCurve() && !previous.isArc()
        val minified = command.minify(from, anchor.takeIf { lastCurve }, move)

        if (minified != null) {
            placeMove(from, move)
            path.append(minified)
            last = minified.lastPoint(from, move)
            anchor = minified.anchor(anchor, from)
        }
    }

    val (_, _, move) = iteratePathFull { c, from, anc, move ->
        fun doAppend(new: Bezier?) {
            if (current != null) doAppend(move.orZero())
            current = new
        }

        when {
            c.isMove() -> doAppend(null)

            c.isArc() -> {
                val minified = c.minify(from.orZero(), null, move.orZero())
                if (minified != null) {
                    doAppend(null)
                    placeMove(from.orZero(), move.orZero())
                    path.append(minified)

                    last = if (minified is ArcToRelative) {
                        minified.dp + last.orZero()
                    } else {
                        (minified as ArcTo).p
                    }
                }
            }

            else -> {
                val simple = c.simplify(from.orZero(), anc).single()
                val bezier = simple.closeToLine(move.orZero()).toBezier(from.orZero())

                val merged = current?.let { Bezier.tryMerge(it, bezier) }
                if (merged != null) current = merged
                else doAppend(bezier)
            }
        }
    }

    if (current != null) // if path wasn't empty we have Bézier curve that haven't been appended to path yet
        doAppend(move.orZero())

    return path
}