package io.github.fate_grand_automata.scripts.modules

import io.github.lib_automata.Region

interface FreeQuestMapScreen {
    /** Only called outside the battle loop's cached grayscale screenshot. */
    fun badges(): List<Region>
    fun zoomOut()
}
