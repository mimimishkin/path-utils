package path.utils.paths

import path.utils.math.DoubleRange
import path.utils.math.Polynomial
import path.utils.math.Vec2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun ring(
    cx: Double,
    cy: Double,
    outRadius: Double,
    inRadius: Double = 0.8 * outRadius,
): Path = circle(cx, cy, outRadius) + circle(cx, cy, inRadius).reversePath()

fun quadRing(
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    offset: Double,
    rx: Double = 0.0,
    ry: Double = rx,
): Path = rect(x, y, w, h, rx + offset, ry + offset) + rect(x + offset, y + offset, w - 2 * offset, h - 2 * offset, rx, ry).reversePath()

fun star(
    cx: Double,
    cy: Double,
    outRadius: Double,
    inRadius: Double = 0.38 * outRadius,
    tips: Int = 5,
): Path {
    val points = DoubleArray(4 * tips)
    for (i in 0 until tips) {
        val theta1 = 2 * PI / tips * i - PI / 2
        val theta2 = 2 * PI / tips * (i + .5) - PI / 2
        points[4 * i + 0] = cx + outRadius * cos(theta1)
        points[4 * i + 1] = cy + outRadius * sin(theta1)
        points[4 * i + 2] = cx + inRadius * cos(theta2)
        points[4 * i + 3] = cy + inRadius * sin(theta2)
    }

    return polygon(points.asList())
}

fun polygon(
    cx: Double,
    cy: Double,
    r: Double,
    tips: Int = 5,
): Path {
    val points = DoubleArray(2 * tips)
    for (i in 0 until tips) {
        val theta = 2 * PI / tips * i - PI / 2
        points[2 * i + 0] = cx + r * cos(theta)
        points[2 * i + 1] = cy + r * sin(theta)
    }

    return polygon(points.asList())
}

fun spiral(
    start: Vec2,
    end: Vec2,
    coils: Int,
    gap: Double = (start - end).length,
    angle: Double = (coils % 1.0 * 2 * PI + atan2(end.y - start.y, end.x - start.x)),
    offset: Double = 0.0,
    clockwise: Boolean = false,
    outward: Boolean = true,
): Path = spiral(
    cx = start.x,
    cy = start.y,
    coils = coils,
    gap = gap,
    angle = angle,
    offset = offset,
    clockwise = clockwise,
    outward = outward
)

fun spiral(
    cx: Double,
    cy: Double,
    coils: Int,
    gap: Double,
    angle: Double,
    offset: Double = 0.0,
    clockwise: Boolean = false,
    outward: Boolean = true,
): Path {
    val m = if (clockwise) 1 else -1
    val o = if (outward) 1 else -1
    val xy = { t0: Double ->
        val t = if (outward) t0 else coils - t0
        val x = cx + gap * (t + offset) * cos(2 * PI * t * m + angle)
        val y = cy + gap * (t + offset) * sin(2 * PI * t * m + angle)
        Vec2(x, y)
    }
    val dxy = { t0: Double ->
        val t = if (outward) t0 else coils - t0
        val dx = gap * cos(2 * PI * t * m + angle) - (2 * PI * gap * (t + offset) * m * sin(2 * PI * t * m + angle))
        val dy = gap * sin(2 * PI * t * m + angle) + (2 * PI * gap * (t + offset) * m * cos(2 * PI * t * m + angle))
        Vec2(o * dx, o * dy)
    }

    return curve(
        xy = xy,
        dxy = dxy,
        interval = 0.0..coils.toDouble(),
        next = { it + 1.0 / 8.0 }
    )
}

fun curve(
    xy: (t: Double) -> Vec2,
    dxy: (t: Double) -> Vec2,
    interval: DoubleRange,
    next: (t: Double) -> Double
): Path {
    val path = mutablePath()

    var t = 0.0
    var lastT = 0.0
    var lastX = 0.0
    var lastY = 0.0
    var lastDX = 0.0
    var lastDY = 0.0

    fun doFirst() {
        val (x, y) = xy(0.0)
        val (dx, dy) = dxy(0.0)

        path.moveTo(x, y)

        lastX = x
        lastY = y
        lastDX = dx
        lastDY = dy
    }

    fun process(t: Double) {
        val (x, y) = xy(t)
        val (dx, dy) = dxy(t)

        val k = t - lastT
        val dx0 = lastDX * k
        val dy0 = lastDY * k
        val dx1 = dx * k
        val dy1 = dy * k

        path.cubicTo(
            (dx0 + 3 * lastX) / 3,
            (dy0 + 3 * lastY) / 3,
            (3 * x - dx1) / 3,
            (3 * y - dy1) / 3,
            x,
            y,
        )

        lastX = x
        lastY = y
        lastDX = dx
        lastDY = dy
        lastT = t
    }

    doFirst()
    val max = interval.endInclusive
    while (true) {
        t = next(t)
        if (t >= max) {
            process(max)
            break
        } else {
            process(t)
        }
    }

    return path
}

fun curve(
    x: Polynomial,
    y: Polynomial,
    interval: DoubleRange,
    next: (t: Double) -> Double
): Path = curve(
    xy = { Vec2(x.eval(it), y.eval(it)) },
    dxy = { Vec2(x.derivative.eval(it), y.derivative.eval(it)) },
    interval = interval,
    next = next
)