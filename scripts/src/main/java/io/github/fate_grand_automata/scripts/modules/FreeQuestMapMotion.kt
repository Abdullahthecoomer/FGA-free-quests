package io.github.fate_grand_automata.scripts.modules

/** Compares small color samples of the terrain, excluding the fixed game HUD. */
object FreeQuestMapMotion {
    fun moved(before: IntArray, after: IntArray): Boolean {
        require(before.isNotEmpty() && before.size == after.size)
        val changed = before.indices.count { index ->
            val a = before[index]
            val b = after[index]
            listOf(0, 8, 16).any { shift ->
                kotlin.math.abs(((a shr shift) and 255) - ((b shr shift) and 255)) >= 12
            }
        }
        // Ignore a few blinking map markers and small capture/compression differences.
        return changed.toDouble() / before.size >= 0.15
    }
}
