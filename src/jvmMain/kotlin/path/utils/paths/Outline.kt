package path.utils.paths

import path.utils.math.Polynomial
import path.utils.math.solve
import java.awt.BasicStroke
import java.awt.BasicStroke.*
import java.awt.Stroke
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

fun BasicStroke(
    width: Double,
    cap: CapMode = CapMode.Square,
    join: JoinMode = JoinMode.Miter,
    miterLimit: Double = 10.0,
    dash: List<Double>? = null,
    dashPhase: Double = 0.0,
): Stroke = BasicStroke(
    width.toFloat(),
    when (cap) {
        CapMode.Butt -> CAP_BUTT
        CapMode.Round -> CAP_ROUND
        CapMode.Square -> CAP_SQUARE
    },
    when (join) {
        JoinMode.Miter -> JOIN_MITER
        JoinMode.Round -> JOIN_ROUND
        JoinMode.Bevel -> JOIN_BEVEL
    },
    miterLimit.toFloat(),
    dash?.let { FloatArray(dash.size) { dash[it].toFloat() } },
    dashPhase.toFloat()
)

fun Path.outline(stroke: Stroke) = stroke.createStrokedShape(asShape()).toPath()

actual fun Path.outline(
    width: Double,
    cap: CapMode,
    join: JoinMode,
    miterLimit: Double,
    dash: List<Double>?,
    dashPhase: Double,
): Path = outline(BasicStroke(width, cap, join, miterLimit, dash, dashPhase))