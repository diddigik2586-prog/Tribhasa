package org.tribhasa.sodhamitra

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.data.ProjectKind
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.Route
import org.tribhasa.sodhamitra.ui.screens.HomeCardCorner

/**
 * V2 Homepage: three large rectangular feature cards (not pills) in the approved palette, the hamburger menu that opens a
 * right-side drawer, the right-edge swipe that works on the Homepage only, and the creator footer.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class HomeV2Test {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var ui: Ui

    private val maroon = Color(0xFF800000)
    private val ivory = Color(0xFFFFF6D8)
    private val lightGreen = Color(0xFFF0F7E9)

    @Before fun setUp() { ui = Ui(rule); ui.skipSplash() }

    private val vm: AppViewModel get() = ViewModelProvider(rule.activity)[AppViewModel::class.java]
    private fun node(text: String, index: Int = 0): SemanticsNode = ui.node(text, index).fetchSemanticsNode()
    private fun tagged(tag: String): SemanticsNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
    private fun back() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }

    private operator fun android.graphics.Bitmap.get(x: Int, y: Int): Color = Color(getPixel(x, y))

    private val cards = listOf("PhD Research Plan", "Individual Researcher / Author", "Editing / Translation Planner")

    private fun menuOpen() = rule.onAllNodesWithTag("menu-panel").fetchSemanticsNodes().isNotEmpty()
    private fun awaitMenu(open: Boolean) = rule.waitUntil(10_000) { rule.waitForIdle(); menuOpen() == open }
    private fun inMenu(text: String) = rule.onAllNodes(hasText(text) and hasAnyAncestor(hasTestTag("menu-panel"))).fetchSemanticsNodes().isNotEmpty()

    private fun edgeSwipe(fromX: Float, toX: Float, y: Float? = null, durationMs: Long = 250) {
        rule.onRoot().performTouchInput { swipe(start = Offset(fromX, y ?: (height / 2f)), end = Offset(toX, y ?: (height / 2f)), durationMillis = durationMs) }
        rule.waitForIdle()
    }

    // ---- cards -----------------------------------------------------------------------------------------------------------

    @Test fun three_large_rectangular_cards_with_the_exact_labels_in_order() {
        ui.assertOrder(*cards.toTypedArray())
        val d = rule.density
        val rootW = rule.onRoot().fetchSemanticsNode().size.width
        val nodes = cards.map { node(it) }
        for (n in nodes) {
            val h = n.size.height / d.density
            assertTrue("card height ${h}dp must be generous", h >= 96f)
            assertTrue("card spans the page width: ${n.size.width} of $rootW", n.size.width >= rootW * 0.85)
            // moderate rounding: a pill would have a corner radius of half the card height
            assertTrue("corner ${HomeCardCorner.value}dp must be well below half the height ${h / 2}dp", HomeCardCorner.value <= h / 4)
        }
        // consistent sizes and comfortable gaps
        assertTrue(nodes.map { it.size.height }.let { (it.max() - it.min()) <= 2 * d.density * 16 })   // equal unless a label needs another line
        assertTrue(nodes.zipWithNext().all { (a, b) -> (b.positionInRoot.y - (a.positionInRoot.y + a.size.height)) >= 8 * d.density })
        assertEquals(0, nodes.count { it.size.width + it.positionInRoot.x > rootW + 1 })           // nothing overflows sideways
    }

    @Test fun cards_are_maroon_on_a_very_light_green_page_with_ivory_text_and_arrow() {
        val image = ui.snapshot(rule.activity)
        val n = node("PhD Research Plan")
        val d = rule.density.density
        // a point inside the card, left of the text: the card colour
        val cardPx = image[(n.positionInRoot.x + 8 * d).toInt(), (n.positionInRoot.y + n.size.height / 2).toInt()]
        assertEquals(maroon, cardPx.copy(alpha = 1f))
        // a point left of the card (page margin): the page background
        val pagePx = image[(n.positionInRoot.x - 14 * d).toInt().coerceAtLeast(1), (n.positionInRoot.y + n.size.height / 2).toInt()]
        assertEquals(lightGreen, pagePx.copy(alpha = 1f))
        // the arrow chip at the right end of the card is ivory
        val chip = image[(n.positionInRoot.x + n.size.width - 40 * d).toInt(), (n.positionInRoot.y + n.size.height / 2f + 13 * d).toInt()]
        assertTrue("arrow chip should be ivory-like, was $chip", chip.red > 0.9f && chip.green > 0.9f && chip.blue > 0.7f)
    }

    @Test fun each_card_opens_its_existing_destination_and_the_internal_names_are_unchanged() {
        ui.click("PhD Research Plan")
        ui.await("+ Add Title of Thesis")
        assertEquals(Route.Projects(ProjectKind.PHD), vm.top.route)              // same route as before
        assertTrue(ui.exists("PhD Research"))                                    // the internal section keeps its name
        assertFalse(ui.exists("PhD Research Plan"))                              // the Homepage label is a Homepage label only
        back(); ui.await("PhD Research Plan")

        ui.click("Individual Researcher / Author")
        ui.await("+ Add Title of Book")
        assertEquals(Route.Projects(ProjectKind.BOOK), vm.top.route)
        back(); ui.await("PhD Research Plan")

        ui.click("Editing / Translation Planner")
        ui.assertOrder("Future Editing List", "Future Trans List")
        assertEquals(Route.Planner, vm.top.route)
    }

    @Test fun creator_footer_is_a_full_width_maroon_bar_with_centred_ivory_text() {
        val footer = tagged("creator-footer")
        val root = rule.onRoot().fetchSemanticsNode()
        assertEquals(root.size.width, footer.size.width)                          // full width
        assertEquals(root.size.height.toFloat(), footer.positionInRoot.y + footer.size.height, 1f)   // at the very bottom
        val text = node("Created by Dr. Diddigi Vamsi Krishna")
        assertEquals(footer.positionInRoot.x + footer.size.width / 2f, text.positionInRoot.x + text.size.width / 2f, 1.5f)    // horizontally centred
        assertEquals(footer.positionInRoot.y + footer.size.height / 2f, text.positionInRoot.y + text.size.height / 2f, 1.5f)  // vertically centred
        val image = ui.snapshot(rule.activity)
        val bg = image[(footer.positionInRoot.x + 4).toInt(), (footer.positionInRoot.y + footer.size.height / 2).toInt()]
        assertEquals(maroon, bg.copy(alpha = 1f))
        // some pixel of the text is ivory
        var ivoryPixels = 0
        for (x in text.positionInRoot.x.toInt() until (text.positionInRoot.x + text.size.width).toInt())
            for (y in text.positionInRoot.y.toInt() until (text.positionInRoot.y + text.size.height).toInt())
                if (image[x, y].let { kotlin.math.abs(it.red - ivory.red) < 0.12f && kotlin.math.abs(it.green - ivory.green) < 0.12f && kotlin.math.abs(it.blue - ivory.blue) < 0.15f }) ivoryPixels++
        assertTrue("footer text must be drawn in warm ivory ($ivoryPixels ivory pixels)", ivoryPixels > 40)
    }

    // ---- hamburger menu and right-side drawer ---------------------------------------------------------------------------

    @Test fun settings_button_is_replaced_by_a_hamburger_menu_at_the_top_right() {
        assertFalse(ui.exists("Settings"))
        val icon = rule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Open menu")).fetchSemanticsNodes().single()
        val root = rule.onRoot().fetchSemanticsNode()
        assertTrue("menu icon in the right half", icon.positionInRoot.x > root.size.width / 2)
        assertTrue("menu icon at the top", icon.positionInRoot.y < 140 * rule.density.density)
    }

    @Test fun menu_opens_from_the_right_lists_all_destinations_and_closes_three_ways() {
        ui.clickDesc("Open menu"); awaitMenu(true)
        val panel = tagged("menu-panel"); val root = rule.onRoot().fetchSemanticsNode()
        assertEquals("the panel touches the right edge", root.size.width.toFloat(), panel.positionInRoot.x + panel.size.width, 2f)
        assertTrue("and does not cover the whole page", panel.positionInRoot.x > root.size.width * 0.12f)
        for (item in listOf("Menu", "PhD Research Plan", "Individual Researcher / Author", "Editing / Translation Planner", "Profile", "Language", "Settings",
            "About App", "About Dr. DVK", "Support Śodhamitra", "Feedback")) assertTrue("menu item $item", inMenu(item))
        // 1. its close control
        rule.onNodeWithTag("menu-close").performClick(); awaitMenu(false)
        assertEquals(Route.Home, vm.top.route)
        // 2. a tap outside it (on the dimmed page)
        ui.clickDesc("Open menu"); awaitMenu(true)
        rule.onNodeWithTag("menu-scrim").performTouchInput { click(Offset(12f, 12f)) }; rule.waitForIdle(); awaitMenu(false)
        assertEquals(Route.Home, vm.top.route)
        // 3. the Android Back button: closes the menu and stays on the Homepage
        ui.clickDesc("Open menu"); awaitMenu(true)
        back(); awaitMenu(false)
        assertEquals(Route.Home, vm.top.route); ui.assertShown("PhD Research Plan")
    }

    @Test fun a_tap_inside_the_menu_panel_does_not_close_it_and_menu_items_navigate() {
        ui.clickDesc("Open menu"); awaitMenu(true)
        rule.onNodeWithTag("menu-panel").performTouchInput { click(Offset(width / 2f, height - 8f)) }; rule.waitForIdle()
        assertTrue(menuOpen())
        rule.onNode(hasText("Profile") and hasAnyAncestor(hasTestTag("menu-panel"))).performClick(); rule.waitForIdle()
        assertEquals(Route.Profile, vm.top.route)
        back(); ui.await("PhD Research Plan"); assertFalse("the menu is closed when coming back", menuOpen())
        // and Settings still lists exactly its six options
        ui.openSettings()
        ui.assertOrder("Profile", "Language", "About App", "About Dr. DVK", "Support Śodhamitra", "Feedback")
    }

    // ---- right-edge swipe: Homepage only ----------------------------------------------------------------------------------

    @Test fun swiping_from_the_right_edge_towards_the_left_opens_the_menu_on_the_homepage() {
        val w = rule.onRoot().fetchSemanticsNode().size.width.toFloat()
        assertFalse(menuOpen())
        edgeSwipe(fromX = w - 6f, toX = w * 0.4f)
        awaitMenu(true)
        assertEquals(Route.Home, vm.top.route)
    }

    @Test fun other_swipes_on_the_homepage_do_not_open_the_menu() {
        val w = rule.onRoot().fetchSemanticsNode().size.width.toFloat()
        val h = rule.onRoot().fetchSemanticsNode().size.height.toFloat()
        edgeSwipe(fromX = w * 0.6f, toX = w * 0.2f)                  // leftwards, but not from the edge
        edgeSwipe(fromX = 6f, toX = w * 0.6f)                        // from the LEFT edge, towards the right
        edgeSwipe(fromX = w - 6f, toX = w - 6f, y = h * 0.7f)       // a tap-length touch on the right edge
        rule.onRoot().performTouchInput { swipe(start = Offset(w - 6f, h * 0.7f), end = Offset(w - 6f, h * 0.3f), durationMillis = 250) }   // vertical, along the edge
        rule.waitForIdle()
        assertFalse(menuOpen())
        // a tiny leftwards move from the edge is not enough
        edgeSwipe(fromX = w - 6f, toX = w - 30f)
        assertFalse(menuOpen())
    }

    @Test fun the_edge_swipe_does_nothing_on_other_screens() {
        val w = rule.onRoot().fetchSemanticsNode().size.width.toFloat()
        for (open in listOf<() -> Unit>(
            { ui.openSettings() },
            { ui.openSettings(); ui.click("Profile") },
            { ui.openSettings(); ui.click("Language") },
            { ui.openSettings(); ui.click("About App") },
            { ui.openSettings(); ui.click("About Dr. DVK") },
            { ui.openSettings(); ui.click("Support Śodhamitra") },
            { ui.click("PhD Research Plan") },
            { ui.click("Editing / Translation Planner") },
        )) {
            open()
            val before = vm.top.route
            edgeSwipe(fromX = w - 6f, toX = w * 0.3f)
            assertFalse("no menu on $before", menuOpen())
            assertEquals(before, vm.top.route)
            rule.runOnUiThread { vm.toHome() }; rule.waitForIdle(); ui.await("PhD Research Plan")
        }
    }

    @Test fun the_menu_survives_nothing_it_is_closed_after_choosing_an_item_and_returning() {
        ui.clickDesc("Open menu"); awaitMenu(true)
        rule.onNode(hasText("Language") and hasAnyAncestor(hasTestTag("menu-panel"))).performClick(); rule.waitForIdle()
        assertEquals(Route.Language, vm.top.route)
        back(); ui.await("PhD Research Plan")
        assertEquals(0, rule.onAllNodesWithTag("menu-scrim").fetchSemanticsNodes().size)
    }

    @Test fun no_old_add_project_wording_on_the_way_through_both_sections() {
        ui.click("PhD Research Plan")
        ui.assertShown("+ Add Title of Thesis", "No thesis titles yet. Tap “+ Add Title of Thesis” to begin.")
        assertFalse(ui.exists("+ Add Project")); assertFalse(ui.exists("No projects yet. Tap “+ Add Project” to begin."))
        ui.click("+ Add Title of Thesis"); ui.await("Add Title of Thesis"); assertFalse(ui.exists("New Research Project"))
        back(); back(); ui.await("PhD Research Plan")
        ui.click("Individual Researcher / Author")
        ui.assertShown("+ Add Title of Book", "No book titles yet. Tap “+ Add Title of Book” to begin.")
        assertFalse(ui.exists("+ Add Project"))
        ui.click("+ Add Title of Book"); ui.await("Add Title of Book"); assertFalse(ui.exists("New Book Project"))
    }
}
