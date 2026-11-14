package io.github.fate_grand_automata.scripts.modules

import io.github.lib_automata.Region
import io.github.fate_grand_automata.scripts.models.MapPanDirection

interface FreeQuestMapScreen {
    /** Only called outside the battle loop's cached grayscale screenshot. */
    fun badges(): List<Region>
    fun zoomOut()
    /** Returns whether the settled terrain moved; false indicates a map edge. */
    fun pan(direction: MapPanDirection): Boolean
}
