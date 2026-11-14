package io.github.fate_grand_automata.imaging

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.modules.FreeQuestBadgeDetector
import io.github.fate_grand_automata.scripts.modules.FreeQuestMapScreen
import io.github.fate_grand_automata.scripts.modules.FreeQuestMapMotion
import io.github.fate_grand_automata.scripts.models.MapPanDirection
import io.github.lib_automata.Region
import io.github.lib_automata.GestureService
import io.github.lib_automata.Transformer
import io.github.lib_automata.Size
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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

    override fun pan(direction: MapPanDirection): Boolean = with(api) {
        val before = motionSamples()
        val (start, end) = locations.freeQuest.panGesture(direction)
        gestures.swipe(transform.toScreen(start), transform.toScreen(end))
        1.5.seconds.wait()
        FreeQuestMapMotion.moved(before, motionSamples())
    }

    private fun motionSamples(): IntArray = with(api) {
        useColor {
            useSameSnapIn {
                locations.freeQuest.motionRegion.getPattern().use { pattern ->
                    pattern.resize(Size(48, 24)).use { sample ->
                        (sample as DroidCvPattern).asBitmap().use { bitmap ->
                            IntArray(bitmap.width * bitmap.height).also {
                                bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                            }
                        }
                    }
                }
            }
        }
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
