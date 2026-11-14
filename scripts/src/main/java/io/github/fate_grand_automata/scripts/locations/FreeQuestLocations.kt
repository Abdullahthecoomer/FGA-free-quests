package io.github.fate_grand_automata.scripts.locations

import io.github.lib_automata.Location
import io.github.lib_automata.Region
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

    fun isMapBadge(badge: Region) =
        listOf(statusRegion, menuRegion, headerRegion).none { hud ->
            badge.center.x in hud.x until hud.right && badge.center.y in hud.y until hud.bottom
        }
}
