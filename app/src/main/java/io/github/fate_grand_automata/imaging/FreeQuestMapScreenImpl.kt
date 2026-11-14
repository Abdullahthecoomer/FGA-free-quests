package io.github.fate_grand_automata.imaging

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.modules.FreeQuestBadgeDetector
import io.github.fate_grand_automata.scripts.modules.FreeQuestMapScreen
import io.github.lib_automata.Region
import io.github.lib_automata.GestureService
import io.github.lib_automata.Transformer
import javax.inject.Inject

class FreeQuestMapScreenImpl @Inject constructor(
    private val api: IFgoAutomataApi,
    private val transform: Transformer,
    private val gestures: GestureService
) : FreeQuestMapScreen {
    override fun zoomOut() {
        val locations = api.locations.freeQuest
        gestures.pinch(
            transform.toScreen(locations.zoomFirstStart), transform.toScreen(locations.zoomFirstEnd),
            transform.toScreen(locations.zoomSecondStart), transform.toScreen(locations.zoomSecondEnd)
        )
    }

    override fun badges(): List<Region> = with(api) {
        useColor {
            useSameSnapIn {
                locations.freeQuest.mapRegion.getPattern().use { pattern ->
                    (pattern as DroidCvPattern).asBitmap().use { bitmap ->
                        val pixels = IntArray(bitmap.width * bitmap.height)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        FreeQuestBadgeDetector.find(pixels, bitmap.width, bitmap.height).map {
                            transform.fromImage(it) + locations.freeQuest.mapRegion.location
                        }
                    }
                }
            }
        }
    }
}
