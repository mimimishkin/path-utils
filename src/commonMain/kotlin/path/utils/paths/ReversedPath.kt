package path.utils.paths

import path.utils.paths.Command.*

fun Path.reversePath(): Path = validate().toRelative().splitToSubPaths().asReversed().flatMap { sub ->
    val hasClose = sub.lastOrNull()?.isClose == true
    val toIndex = sub.size - if (hasClose) 1 else 0
    val (lastPoint, _) = sub.subList(0, toIndex).iteratePath { _, _, _, _ -> }

    val original = sub.subList(1, toIndex).asReversed()
    val reversed = mutablePath().moveTo(lastPoint)

    var index = 0
    while (index < original.size) {
        when (val command = original[index]) {
            is LineToRelative -> with(command) {
                reversed += LineToRelative(dx = -dx, dy = -dy)
            }

            is HorizontalLineToRelative -> with(command) {
                reversed += HorizontalLineToRelative(dx = -dx)
            }

            is VerticalLineToRelative -> with(command) {
                reversed += VerticalLineToRelative(dy = -dy)
            }

            is QuadToRelative -> with(command) {
                reversed += QuadToRelative(
                    dx1 = dx1 - dx,
                    dy1 = dy1 - dy,
                    dx = -dx,
                    dy = -dy
                )
            }

            is CubicToRelative -> with(command) {
                reversed += CubicToRelative(
                    dx1 = dx2 - dx,
                    dy1 = dy2 - dy,
                    dx2 = dx1 - dx,
                    dy2 = dy1 - dy,
                    dx = -dx,
                    dy = -dy
                )
            }

            is SmoothQuadToRelative -> {
                val smooths = original
                    .subList(index, original.size)
                    .takeWhile { it is SmoothQuadToRelative }
                    .asReversed() as List<SmoothQuadToRelative>
                val quad = original[index + smooths.size] as QuadToRelative

                val last = smooths.last()
                var dx1 = quad.dx - quad.dx1
                var dy1 = quad.dy - quad.dy1
                val newSmooths = mutableListOf<SmoothQuadToRelative>()

                smooths.subList(0, smooths.size - 1).forEach {
                    dx1 = it.dx - dx1
                    dy1 = it.dy - dy1

                    newSmooths += SmoothQuadToRelative(
                        dx = -it.dx,
                        dy = -it.dy
                    )
                }

                reversed += QuadToRelative(
                    dx1 = dx1 - last.dx,
                    dy1 = dy1 - last.dy,
                    dx = -last.dx,
                    dy = -last.dy
                )

                reversed += newSmooths.asReversed()

                reversed += SmoothQuadToRelative(
                    dx = -quad.dx,
                    dy = -quad.dy
                )

                index += smooths.size
            }

            is SmoothCubicToRelative -> {
                val smooths = original
                    .subList(index, original.size)
                    .takeWhile { it is SmoothCubicToRelative } as List<SmoothCubicToRelative>
                val cubic = original[index + smooths.size] as CubicToRelative

                val all = listOf(cubic) + smooths.map { CubicToRelative(0.0, 0.0, it.dx2, it.dy2, it.dx, it.dy) }.asReversed()
                all.zipWithNext().forEach { (cur, next) ->
                    next.dx1 = cur.dx - cur.dx2
                    next.dy1 = cur.dy - cur.dy2
                }

                val last = all.last()
                reversed += CubicToRelative(
                    dx1 = last.dx2 - last.dx,
                    dy1 = last.dy2 - last.dy,
                    dx2 = last.dx1 - last.dx,
                    dy2 = last.dy1 - last.dy,
                    dx = -last.dx,
                    dy = -last.dy
                )

                all.subList(0, all.size - 1).asReversed().forEach { curve ->
                    reversed += SmoothCubicToRelative(
                        dx2 = curve.dx1 - curve.dx,
                        dy2 = curve.dy1 - curve.dy,
                        dx = -curve.dx,
                        dy = -curve.dy
                    )
                }

                index += smooths.size
            }

            is ArcToRelative -> with(command) {
                reversed += ArcToRelative(
                    rx = rx,
                    ry = ry,
                    xAxisRotation = xAxisRotation,
                    largeArcFlag = largeArcFlag,
                    sweepFlag = !sweepFlag,
                    dx = -dx,
                    dy = -dy
                )
            }

            else -> throw AssertionError()
        }

        index++
    }

    if (hasClose)
        reversed.close()

    reversed
}