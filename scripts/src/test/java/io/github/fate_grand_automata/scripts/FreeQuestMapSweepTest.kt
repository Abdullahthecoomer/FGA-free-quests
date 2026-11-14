package io.github.fate_grand_automata.scripts

import io.github.fate_grand_automata.scripts.models.MapPanDirection
import io.github.fate_grand_automata.scripts.modules.FreeQuestMapMotion
import io.github.fate_grand_automata.scripts.modules.FreeQuestMapSweep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreeQuestMapSweepTest {
    @Test
    fun visitsEveryRowAndColumnBeforeDeclaringTheMapEmpty() {
        val sweep = FreeQuestMapSweep()
        var x = 1
        var y = 1
        val visited = mutableSetOf(x to y)
        var result = FreeQuestMapSweep.Result.Continue
        repeat(80) {
            if (result != FreeQuestMapSweep.Result.Continue) return@repeat
            val before = x to y
            when (sweep.direction) {
                MapPanDirection.Right -> x = (x - 1).coerceAtLeast(0)
                MapPanDirection.Left -> x = (x + 1).coerceAtMost(3)
                MapPanDirection.Down -> y = (y - 1).coerceAtLeast(0)
                MapPanDirection.Up -> y = (y + 1).coerceAtMost(2)
            }
            visited += x to y
            result = sweep.afterPan(before != (x to y))
        }
        assertEquals(FreeQuestMapSweep.Result.Complete, result)
        assertEquals((0..3).flatMap { column -> (0..2).map { row -> column to row } }.toSet(), visited)
    }

    @Test
    fun anUnboundedOrUnsettledMapIsAnErrorRatherThanACompleteMap() {
        val sweep = FreeQuestMapSweep()
        repeat(79) { assertEquals(FreeQuestMapSweep.Result.Continue, sweep.afterPan(moved = true)) }
        assertEquals(FreeQuestMapSweep.Result.LimitReached, sweep.afterPan(moved = true))
    }

    @Test
    fun identicalTerrainAndBlinkingMarkersDoNotCountAsPanning() {
        val before = IntArray(100) { 0xFF406050.toInt() }
        assertFalse(FreeQuestMapMotion.moved(before, before.copyOf()))
        val after = before.copyOf()
        repeat(10) { after[it] = 0xFFFFFFFF.toInt() }
        assertFalse(FreeQuestMapMotion.moved(before, after))
    }

    @Test
    fun broadTerrainChangesCountAsPanningButSmallCaptureNoiseDoesNot() {
        val before = IntArray(100) { 0xFF406050.toInt() }
        val noise = IntArray(100) { 0xFF426253.toInt() }
        assertFalse(FreeQuestMapMotion.moved(before, noise))
        val after = before.copyOf()
        repeat(50) { after[it] = 0xFF709020.toInt() }
        assertTrue(FreeQuestMapMotion.moved(before, after))
    }
}
