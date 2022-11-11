package path.utils.paths

import path.utils.beziers.*
import path.utils.math.Vec2
import path.utils.math.orNull
import path.utils.paths.Command.*

typealias Path = List<Command>

fun <T> Path.modified(
    pathMaker: (List<T>) -> Path,
    transform: (Command, last: Vec2, anchor: Vec2?, moveTo: Vec2) -> T
): Path {
    val modified = mutableListOf<T>()

    iteratePathFull { command, last, anchor, moveTo ->
        modified += transform(command, last.orNull(), anchor, moveTo.orNull())
    }

    return pathMaker(modified)
}

inline fun Path.iteratePath(
    action: (Command, last: Vec2, anchor: Vec2?, moveTo: Vec2) -> Unit
): Pair<Vec2, Vec2> {
    val (last, _, move) = iteratePathFull { command, last, anchor, moveTo ->
        action(command, last.orNull(), anchor, moveTo.orNull())
    }

    return Pair(last.orNull(), move.orNull())
}

inline fun Path.iteratePathFull(
    action: (Command, last: Vec2?, anchor: Vec2?, moveTo: Vec2?) -> Unit
): Triple<Vec2?, Vec2?, Vec2?> {
    var moveTo: Vec2? = null
    var last: Vec2? = null
    var anchor: Vec2? = null

    for (c in this) {
        action(c, last, anchor, moveTo)
        anchor = c.anchor(anchor, last.orNull())
        last = c.lastPoint(last.orNull(), moveTo.orNull())
        if (c.isMove) moveTo = last
    }

    return Triple(last, anchor, moveTo)
}

fun Path.toAbsolute() = modified({ it }) { c, last, _, _ -> c.toAbsolute(last) }

fun Path.toRelative() = modified({ it }) { c, last, _, _ -> c.toRelative(last) }

fun Path.simplify() = modified({ it.flatten() }) { c, last, anchor, _ -> c.simplify(last, anchor) }

fun Path.validate(): Path {
    val path = mutablePath()

    iteratePath { command, _, _, move ->
        val last = path.lastOrNull()
        if ((last == null || last.isClose) && !command.isMove)
            path.moveTo(move)

        if (last == null && command is MoveToRelative) { // first moveTo is absolute
            path.moveTo(command.dp)
        } else {
            path.append(command)
        }
    }

    return path
}

fun Path.closeToLine() = modified({ it }) { c, _, _, move -> c.closeToLine(move) }

fun Path.toFlatPath(flatness: Double, recursiveLimit: Int = 10) = toBeziers().toFlatPath(flatness, recursiveLimit).toPath()

fun Path.splitToSubPaths(): List<Path> {
    val paths = mutableListOf<MutablePath>()

    iteratePath { command, last, _, _ ->
        if (command.isMove) {
            paths += mutablePath().append(command.toAbsolute(last))
        } else {
            paths.last() += command
        }
    }

    return paths
}

fun List<Path>.joinToPath(): Path = flatMap { sub ->
    val first = sub.firstOrNull()
    val needMove = first != null && !first.isMove
    if (needMove) listOf(MoveTo(Vec2())) + sub else sub
}

fun Path.toSvg() = joinToString(" ") { it.toSvg() }