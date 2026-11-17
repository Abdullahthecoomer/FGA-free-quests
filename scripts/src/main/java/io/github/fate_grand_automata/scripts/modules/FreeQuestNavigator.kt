package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.Images
import io.github.fate_grand_automata.scripts.entrypoints.AutoBattle
import io.github.lib_automata.Location
import io.github.lib_automata.Size
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@ScriptScope
class FreeQuestNavigator @Inject constructor(
    api: IFgoAutomataApi,
    private val screen: FreeQuestMapScreen
) : IFgoAutomataApi by api {
    var completedNodes = 0
        private set
    private var returningToMap = true
    private var selectedNode: Location? = null
    private var battlesAtNode = 0
    private var nodeClickAttempts = 0
    private var emptyScans = 0
    private var sweep: FreeQuestMapSweep? = null
    private var mapConfirmed = false
    private var navigationStarted = TimeSource.Monotonic.markNow()

    /** Returns true only when the quest panel is ready for AutoBattle to select its first quest. */
    fun menu(): Boolean {
        if (navigationStarted.elapsedNow() > 90.seconds) stalled()
        if (images[Images.FreeQuestClosePanel] in locations.freeQuest.closePanelRegion) {
            if (returningToMap) {
                locations.freeQuest.closePanelClick.click()
                2.seconds.wait()
                return false
            }
            // Never start farming from a panel that was not opened by our map scan.
            if (selectedNode == null || battlesAtNode >= 6) stalled()
            nodeClickAttempts = 0
            return true
        }

        // Zooming or panning can put every blue quest marker outside the view.
        if (!mapConfirmed) {
            if (!isMap()) return false
            mapConfirmed = true
        }
        if (returningToMap) {
            screen.zoomOut()
            returningToMap = false
            selectedNode = null
            nodeClickAttempts = 0
            battlesAtNode = 0
            emptyScans = 0
            sweep = null
            2.seconds.wait()
            return false
        }

        val badges = screen.badges().filter { locations.freeQuest.isMapBadge(it) }
        if (badges.isEmpty()) {
            // Confirm this view is empty before moving to another part of the map.
            if (selectedNode != null) stalled()
            if (++emptyScans < 3) {
                2.seconds.wait()
                return false
            }
            val search = sweep ?: FreeQuestMapSweep().also { sweep = it }
            val moved = screen.pan(search.direction)
            when (search.afterPan(moved)) {
                FreeQuestMapSweep.Result.Complete ->
                    throw AutoBattle.BattleExitException(AutoBattle.ExitReason.FreeQuestComplete(completedNodes))
                FreeQuestMapSweep.Result.LimitReached -> stalled()
                FreeQuestMapSweep.Result.Continue -> {
                    if (moved) emptyScans = 0
                    navigationResumed()
                }
            }
            return false
        }
        emptyScans = 0
        sweep = null
        val previous = selectedNode
        val target = if (previous == null) badges.first() else {
            badges.minByOrNull { distanceSquared(it.center, previous) }!!.also {
                if (distanceSquared(it.center, previous) > 250 * 250) stalled()
            }
        }
        if (++nodeClickAttempts > 3) stalled()
        selectedNode = target.center
        val click = locations.freeQuest.questNodeClick(target, nodeClickAttempts) ?: stalled()
        click.click()
        2.seconds.wait()
        return false
    }

    private fun distanceSquared(a: Location, b: Location): Int =
        (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)

    private fun isMap(): Boolean = useSameSnapIn {
        val marker = images[Images.FreeQuestMapMarker]
        val hasMarker = (20..40).any { step ->
            val scale = step / 40.0
            marker.resize(Size((marker.width * scale).toInt(), (marker.height * scale).toInt())).use {
                locations.freeQuest.mapRegion.exists(it)
            }
        }
        hasMarker || locations.freeQuest.headerRegion.detectText().contains("Terminal", ignoreCase = true)
    }

    fun reward(): Boolean {
        if (selectedNode != null && !returningToMap) {
            completedNodes++
            returningToMap = true
            navigationStarted = TimeSource.Monotonic.markNow()
            return true
        }
        return false
    }

    fun battleStarted() {
        battlesAtNode++
        // Battle duration does not consume the navigation timeout.
        navigationResumed()
    }

    fun navigationResumed() {
        navigationStarted = TimeSource.Monotonic.markNow()
    }

    fun result() {
        navigationStarted = TimeSource.Monotonic.markNow()
    }

    fun closeRepeat() {
        if (navigationStarted.elapsedNow() > 90.seconds) stalled()
        val close = locations.freeQuest.repeatCloseRegion.find(images[Images.FreeQuestRepeatClose])
            ?: return
        close.region.click()
        2.seconds.wait()
    }

    private fun stalled(): Nothing =
        throw AutoBattle.BattleExitException(AutoBattle.ExitReason.FreeQuestNavigationFailed)
}
