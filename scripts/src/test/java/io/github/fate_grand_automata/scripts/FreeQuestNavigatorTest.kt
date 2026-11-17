package io.github.fate_grand_automata.scripts

import io.github.fate_grand_automata.scripts.entrypoints.AutoBattle
import io.github.fate_grand_automata.scripts.enums.GameServer
import io.github.fate_grand_automata.scripts.locations.FreeQuestLocations
import io.github.fate_grand_automata.scripts.locations.IScriptAreaTransforms
import io.github.fate_grand_automata.scripts.locations.Locations
import io.github.fate_grand_automata.scripts.modules.FreeQuestMapScreen
import io.github.fate_grand_automata.scripts.modules.FreeQuestNavigator
import io.github.fate_grand_automata.scripts.models.MapPanDirection
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
        override val gameServer = GameServer.En.Original
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
        every { api.useSameSnapIn<Boolean>(any()) } answers { firstArg<() -> Boolean>()() }
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
    fun emptyMapRequiresOneZoomAndAnEntireSweepBeforeStopping() {
        val navigator = FreeQuestNavigator(api, screen)
        badges = emptyList()
        assertFalse(navigator.menu()) // normalize zoom
        verify(exactly = 1) { screen.zoomOut() }
        repeat(2) { assertFalse(navigator.menu()) }
        verify(exactly = 0) { screen.pan(any()) }
        repeat(3) { assertFalse(navigator.menu()) }
        val exit = assertFailsWith<AutoBattle.BattleExitException> { navigator.menu() }
        assertIs<AutoBattle.ExitReason.FreeQuestComplete>(exit.reason)
        verify(exactly = 1) { screen.pan(MapPanDirection.Right) }
        verify(exactly = 1) { screen.pan(MapPanDirection.Down) }
        verify(exactly = 1) { screen.pan(MapPanDirection.Left) }
        verify(exactly = 1) { screen.pan(MapPanDirection.Up) }
    }

    @Test
    fun sweepFindsAnOffscreenQuestEvenWhenIntermediateViewsHaveNoBlueMarkers() {
        val navigator = FreeQuestNavigator(api, screen)
        var x = 1
        var y = 1
        val target = Region(1500, 500, 60, 60)
        every { screen.badges() } answers { if (x == 2 && y == 2) listOf(target) else emptyList() }
        every { screen.pan(any()) } answers {
            val previous = x to y
            when (firstArg<MapPanDirection>()) {
                MapPanDirection.Right -> x = (x - 1).coerceAtLeast(0)
                MapPanDirection.Left -> x = (x + 1).coerceAtMost(2)
                MapPanDirection.Down -> y = (y - 1).coerceAtLeast(0)
                MapPanDirection.Up -> y = (y + 1).coerceAtMost(2)
            }
            mapVisible = false
            previous != (x to y)
        }
        every { with(api) { target.center.click(any()) } } answers { panelOpen = true }
        repeat(100) { if (!panelOpen) navigator.menu() }
        assertTrue(panelOpen)
        assertTrue(navigator.menu())
        verify(exactly = 1) { with(api) { target.center.click(any()) } }
    }

    @Test
    fun everyMovedViewMustBeConfirmedEmptyBeforeAnotherSwipe() {
        val navigator = FreeQuestNavigator(api, screen)
        badges = emptyList()
        every { screen.pan(any()) } returns true
        navigator.menu()
        repeat(3) { navigator.menu() }
        verify(exactly = 1) { screen.pan(any()) }
        repeat(2) { navigator.menu() }
        verify(exactly = 1) { screen.pan(any()) }
        navigator.menu()
        verify(exactly = 2) { screen.pan(any()) }
    }

    @Test
    fun terminalButtonConfirmsAMapWithAllQuestMarkersOutsideTheInitialView() {
        val navigator = FreeQuestNavigator(api, screen)
        mapVisible = false
        badges = emptyList()
        every { with(api) { freeLocations.headerRegion.detectText(any()) } } returns "TERMINAL"
        navigator.menu()
        repeat(3) { navigator.menu() }
        verify(exactly = 1) { screen.zoomOut() }
        verify(exactly = 1) { screen.pan(MapPanDirection.Right) }
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
    fun failedBadgeTapRetriesTheIconUsingTheLatestBadgePositionAndSize() {
        val navigator = FreeQuestNavigator(api, screen)
        val original = badges.single()
        val moved = Region(1510, 510, 80, 80)
        val clicks = mutableListOf<Location>()
        every { with(api) { any<Location>().click(any()) } } answers {
            clicks += firstArg<Location>()
            if (clicks.size == 1) badges = listOf(moved)
            if (clicks.size == 2) panelOpen = true
        }
        navigator.menu() // zoom
        assertFalse(navigator.menu()) // badge
        assertFalse(navigator.menu()) // icon
        assertTrue(navigator.menu()) // panel is open; no more map taps
        assertEquals(listOf(original.center, Location(1470, 630)), clicks)
    }

    @Test
    fun aSecondFallbackMovesFartherIntoTheIconWhenThePanelStillDoesNotOpen() {
        val navigator = FreeQuestNavigator(api, screen)
        val clicks = mutableListOf<Location>()
        every { with(api) { any<Location>().click(any()) } } answers {
            clicks += firstArg<Location>()
            if (clicks.size == 3) panelOpen = true
        }
        navigator.menu()
        repeat(3) { assertFalse(navigator.menu()) }
        assertTrue(navigator.menu())
        assertEquals(listOf(Location(1530, 530), Location(1470, 590), Location(1440, 590)), clicks)
    }

    @Test
    fun aQuestThatNeverOpensStopsAfterTheBadgeTapAndTwoIconFallbacks() {
        val navigator = FreeQuestNavigator(api, screen)
        val clicks = mutableListOf<Location>()
        every { with(api) { any<Location>().click(any()) } } answers {
            clicks += firstArg<Location>()
        }
        navigator.menu()
        repeat(3) { assertFalse(navigator.menu()) }
        val exit = assertFailsWith<AutoBattle.BattleExitException> { navigator.menu() }
        assertIs<AutoBattle.ExitReason.FreeQuestNavigationFailed>(exit.reason)
        assertEquals(3, clicks.size)
    }

    @Test
    fun iconFallbackDoesNotTapOutsideTheMapOrOnTheGamesHud() {
        for (badge in listOf(
            Region(10, 400, 60, 60),
            Region(800, 1040, 60, 60),
            Region(570, 40, 60, 60),
            Region(1600, 1380, 60, 60)
        )) {
            assertEquals(badge.center, freeLocations.questNodeClick(badge, attempt = 1))
            assertEquals(null, freeLocations.questNodeClick(badge, attempt = 2))
        }
    }

    @Test
    fun apWaitAndQuestSelectionRetriesDoNotCountAsCompletedBattles() {
        val navigator = FreeQuestNavigator(api, screen)
        navigator.menu()
        navigator.menu()
        panelOpen = true
        repeat(8) {
            assertTrue(navigator.menu())
            navigator.navigationResumed()
        }
        repeat(6) {
            assertTrue(navigator.menu())
            navigator.battleStarted()
            navigator.result()
        }
        val exit = assertFailsWith<AutoBattle.BattleExitException> { navigator.menu() }
        assertIs<AutoBattle.ExitReason.FreeQuestNavigationFailed>(exit.reason)
    }

    @Test
    fun giftAndMissionCountersAreExcludedButBottomMapNodesRemainEligible() {
        assertFalse(freeLocations.isMapBadge(Region(800, 1250, 60, 60)))
        assertFalse(freeLocations.isMapBadge(Region(800, 1080, 60, 60)))
        assertTrue(freeLocations.isMapBadge(Region(1700, 1300, 60, 60)))
    }
}
