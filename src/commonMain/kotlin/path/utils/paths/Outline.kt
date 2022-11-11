package path.utils.paths

enum class CapMode {
    Butt, Round, Square
}

enum class JoinMode {
    Miter, Round, Bevel
}

expect fun Path.outline(
    width: Double,
    cap: CapMode = CapMode.Square,
    join: JoinMode = JoinMode.Miter,
    miterLimit: Double = 10.0,
    dash: List<Double>? = null,
    dashPhase: Double = 0.0,
): Path

