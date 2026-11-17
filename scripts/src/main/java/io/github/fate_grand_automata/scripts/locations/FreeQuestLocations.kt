package io.github.fate_grand_automata.scripts.locations

import io.github.lib_automata.Location
import io.github.lib_automata.Region
import io.github.fate_grand_automata.scripts.models.MapPanDirection
import javax.inject.Inject

class FreeQuestLocations @Inject constructor(
    transforms: IScriptAreaTransforms
) : IScriptAreaTransforms by transforms {
    val mapRegion = scriptArea
    val statusRegion = Region(0, 1100, 1350, 340)
    val menuRegion = Region(-600, -250, 600, 250).xFromRight().yFromBottom()
    val headerRegion = Region(0, 0, 600, 180)
    val closePanelRegion = Region(0, 0, 600, 180)
    val closePanelClick = Location(190, 85)
    val repeatCloseRegion = Region(-800, 960, 800, 330).xFromCenter()
    val repeatCloseClick = Location(-400, 1120).xFromCenter()
    val zoomFirstStart = Location(-700, 700).xFromCenter()
    val zoomFirstEnd = Location(-100, 700).xFromCenter()
    val zoomSecondStart = Location(700, 700).xFromCenter()
    val zoomSecondEnd = Location(100, 700).xFromCenter()

    val motionRegion = Region(-900, 250, 1800, 750).xFromCenter()
    private val horizontalPanDistance = (scriptArea.width * if (canLongSwipe) 0.55 else 0.35).toInt()
    private val panLeft = Location(-horizontalPanDistance / 2, 700).xFromCenter()
    private val panRight = Location(horizontalPanDistance / 2, 700).xFromCenter()
    private val panTop = Location(0, if (canLongSwipe) 350 else 550).xFromCenter()
    private val panBottom = Location(0, 1000).xFromCenter()

    fun panGesture(direction: MapPanDirection): Pair<Location, Location> = when (direction) {
        MapPanDirection.Left -> panRight to panLeft
        MapPanDirection.Right -> panLeft to panRight
        MapPanDirection.Up -> panBottom to panTop
        MapPanDirection.Down -> panTop to panBottom
    }

    fun questNodeClick(badge: Region, attempt: Int): Location? {
        val offset = when (attempt) {
            1 -> Location()
            // The notification sits above and to the right of the node artwork.
            2 -> Location(-badge.width, badge.height)
            3 -> Location(-badge.width * 3 / 2, badge.height)
            else -> return null
        }
        return (badge.center + offset).takeIf { isMapPoint(it) }
    }

    fun isMapBadge(badge: Region) = isMapPoint(badge.center)

    private fun isMapPoint(point: Location) =
        point.x in mapRegion.x until mapRegion.right &&
        point.y in mapRegion.y until mapRegion.bottom &&
        listOf(statusRegion, menuRegion, headerRegion).none { hud ->
            point.x in hud.x until hud.right && point.y in hud.y until hud.bottom
        }
}
