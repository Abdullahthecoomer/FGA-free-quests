package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.models.MapPanDirection

/** Reaches the top-left corner, then scans overlapping rows to the bottom edge. */
class FreeQuestMapSweep {
    private enum class Phase(val direction: MapPanDirection) {
        SeekLeft(MapPanDirection.Right),
        SeekTop(MapPanDirection.Down),
        AcrossRight(MapPanDirection.Left),
        DownAtRight(MapPanDirection.Up),
        AcrossLeft(MapPanDirection.Right),
        DownAtLeft(MapPanDirection.Up)
    }

    enum class Result { Continue, Complete, LimitReached }

    private var phase = Phase.SeekLeft
    private var panCount = 0
    val direction get() = phase.direction

    fun afterPan(moved: Boolean): Result {
        panCount++
        phase = when (phase) {
            Phase.SeekLeft -> if (moved) phase else Phase.SeekTop
            Phase.SeekTop -> if (moved) phase else Phase.AcrossRight
            Phase.AcrossRight -> if (moved) phase else Phase.DownAtRight
            Phase.DownAtRight -> if (moved) Phase.AcrossLeft else return Result.Complete
            Phase.AcrossLeft -> if (moved) phase else Phase.DownAtLeft
            Phase.DownAtLeft -> if (moved) Phase.AcrossRight else return Result.Complete
        }
        return if (panCount >= 80) Result.LimitReached else Result.Continue
    }
}
