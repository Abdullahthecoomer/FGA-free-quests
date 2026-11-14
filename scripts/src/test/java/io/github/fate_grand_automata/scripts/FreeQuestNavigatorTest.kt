package io.github.fate_grand_automata.scripts

import io.github.fate_grand_automata.scripts.entrypoints.AutoBattle
import io.github.fate_grand_automata.scripts.enums.GameServer
import io.github.fate_grand_automata.scripts.locations.FreeQuestLocations
import io.github.fate_grand_automata.scripts.locations.IScriptAreaTransforms
import io.github.fate_grand_automata.scripts.locations.Locations
import io.github.fate_grand_automata.scripts.modules.FreeQuestMapScreen
import io.github.fate_grand_automata.scripts.modules.FreeQuestNavigator
import io.github.lib_automata.Location
import io.github.lib_automata.Pattern
import io.github.lib_automata.Region
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FreeQuestNavigatorTest {
    private val transforms = object : IScriptAreaTransforms {
        override val scriptArea = Region(0, 0, 2880, 1440)
        override val isWide = true
        override val gameServer = GameServer.En()
        override val canLongSwipe = true
        override fun Location.xFromCenter() = this + Location(1440, 0)
        override fun Region.xFromCenter() = this + Location(1440, 0)
        override fun Location.xFromRight() = this + Location(2880, 0)
        override fun Region.xFromRight() = this + Location(2880, 0)
        override fun Location.yFromBottom() = this + Location(0, 1440)
        override fun Region.yFromBottom() = this + Location(0, 1440)
    }
    private val freeLocations = FreeQuestLocations(transforms)
    private val locations = Locations(transforms, mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), freeLocations)
    private val api = mockk<IFgoAutomataApi>(relaxed = true)
    private val screen = mockk<FreeQuestMapScreen>(relaxed = true)
    private val marker = mockk<Pattern>(relaxed = true)
    private val close = mockk<Pattern>()
    private var panelOpen = false
    private var mapVisible = true
    private var badges = listOf(Region(1500, 500, 60, 60))

    init {
        every { api.locations } returns locations
        every { api.images[Images.FreeQuestClosePanel] } returns close
        every { api.images[Images.FreeQuestMapMarker] } returns marker
        every { marker.width } returns 50
        every { marker.height } returns 24
        every { marker.resize(any()) } returns marker
        every { with(api) { freeLocations.closePanelRegion.contains(close) } } answers { panelOpen }
        every { with(api) { freeLocations.mapRegion.exists(marker, any(), any()) } } answers { mapVisible }
        every { screen.badges() } answers { badges }
    }

    @Test
    fun emptyMapNeedsThreeConfirmedScansAfterZoomingOut() {
        val navigator = FreeQuestNavigator(api, screen)
        badges = emptyList()
        assertFalse(navigator.menu()) // normalize zoom
        verify(exactly = 3) { screen.zoomOut() }
        repeat(2) { assertFalse(navigator.menu()) }
        val exit = assertFailsWith<AutoBattle.BattleExitException> { navigator.menu() }
        assertIs<AutoBattle.ExitReason.FreeQuestComplete>(exit.reason)
    }

    @Test
    fun unrecognizedScreenNeverCountsAsEmptyMap() {
        val navigator = FreeQuestNavigator(api, screen)
        mapVisible = false
        badges = emptyList()
        repeat(5) { assertFalse(navigator.menu()) }
        verify(exactly = 0) { screen.badges() }
    }

    @Test
    fun questPanelCanOnlyRunAfterSelectingBadgeAndRewardCountsOnce() {
        val navigator = FreeQuestNavigator(api, screen)
        navigator.menu() // zoom
        navigator.menu() // select badge
        panelOpen = true
        assertTrue(navigator.menu())
        assertTrue(navigator.reward())
        assertFalse(navigator.reward())
        assertEquals(1, navigator.completedNodes)
        assertFalse(navigator.menu()) // close panel instead of replaying cleared quest
        verify(exactly = 1) { with(api) { freeLocations.closePanelClick.click(any()) } }
    }

    @Test
    fun giftAndMissionCountersAreExcludedButBottomMapNodesRemainEligible() {
        assertFalse(freeLocations.isMapBadge(Region(800, 1250, 60, 60)))
        assertTrue(freeLocations.isMapBadge(Region(1700, 1300, 60, 60)))
    }
}
