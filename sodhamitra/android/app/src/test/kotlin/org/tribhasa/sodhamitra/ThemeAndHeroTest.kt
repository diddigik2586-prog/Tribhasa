package org.tribhasa.sodhamitra

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.ui.screens.SplashScreen
import org.tribhasa.sodhamitra.ui.theme.SodhamitraTheme

private fun contrast(a: Color, b: Color): Float {
    val (hi, lo) = if (a.luminance() >= b.luminance()) a to b else b to a
    return (hi.luminance() + 0.05f) / (lo.luminance() + 0.05f)
}

/** V2: the approved palette, and the Hero (splash) page with its creator footer. */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class ThemeAndHeroTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun the_palette_is_maroon_primary_warm_ivory_accent_and_very_light_green_background_with_legible_text() {
        var s: ColorScheme? = null
        rule.setContent { SodhamitraTheme { s = MaterialTheme.colorScheme } }
        rule.waitForIdle()
        val c = s!!
        assertEquals(Color(0xFF800000), c.primary)
        assertEquals(Color(0xFFFFF6D8), c.surfaceVariant)          // cards: warm ivory
        assertEquals(Color(0xFFFFF6D8), c.onPrimary)              // text on maroon: warm ivory
        assertEquals(Color(0xFFF0F7E9), c.background)
        assertEquals(Color(0xFFF0F7E9), c.surface)
        // legibility (WCAG contrast ratios): text on every surface it is drawn on
        val pairs = mapOf(
            "onPrimary/primary" to (c.onPrimary to c.primary), "onBackground/background" to (c.onBackground to c.background),
            "onSurface/surface" to (c.onSurface to c.surface), "onSurface/ivory" to (c.onSurface to c.surfaceVariant),
            "onSurfaceVariant/ivory" to (c.onSurfaceVariant to c.surfaceVariant), "onSurfaceVariant/background" to (c.onSurfaceVariant to c.background),
            "onPrimaryContainer/primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
            "onSecondaryContainer/secondaryContainer" to (c.onSecondaryContainer to c.secondaryContainer),
            "primary/background" to (c.primary to c.background), "primary/ivory" to (c.primary to c.surfaceVariant),
            "error/background" to (c.error to c.background), "onSurface/dialog" to (c.onSurface to c.surfaceContainerHigh),
            "onSurface/menu" to (c.onSurface to c.surfaceContainer),
        )
        for ((name, p) in pairs) assertTrue("$name contrast ${contrast(p.first, p.second)} must be at least 4.5", contrast(p.first, p.second) >= 4.5f)
        // the old indigo scheme is gone
        for (old in listOf(Color(0xFF1D1673), Color(0xFFFFFBF5), Color(0xFFE5E2FF))) {
            assertTrue(old !in listOf(c.primary, c.background, c.surface, c.surfaceVariant, c.primaryContainer, c.secondaryContainer))
        }
    }

    @Test fun the_hero_page_has_the_brand_and_the_creator_footer_in_a_full_width_maroon_bar() {
        rule.setContent { SodhamitraTheme { androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) { SplashScreen {} } } }   // Root provides this Surface in the app
        rule.waitForIdle()
        rule.onNodeWithText("Śodhamitra").assertExists(); rule.onNodeWithText("Research Planner").assertExists()
        rule.onNodeWithText("Created by Dr. Diddigi Vamsi Krishna").assertExists()
        val footer = rule.onNodeWithTag("creator-footer").fetchSemanticsNode()
        val root = rule.onRoot().fetchSemanticsNode()
        assertEquals(root.size.width, footer.size.width)
        assertEquals(root.size.height.toFloat(), footer.positionInRoot.y + footer.size.height, 1f)
        val text = rule.onNodeWithText("Created by Dr. Diddigi Vamsi Krishna").fetchSemanticsNode()
        assertEquals(footer.positionInRoot.x + footer.size.width / 2f, text.positionInRoot.x + text.size.width / 2f, 1.5f)
        assertEquals(footer.positionInRoot.y + footer.size.height / 2f, text.positionInRoot.y + text.size.height / 2f, 1.5f)
        val bmp = Ui(rule).snapshot(rule.activity)
        fun px(x: Int, y: Int) = Color(bmp.getPixel(x, y)).copy(alpha = 1f)
        assertEquals(Color(0xFF800000), px(5, (footer.positionInRoot.y + footer.size.height / 2).toInt()))   // footer
        assertEquals(Color(0xFF800000), px(5, 5))                                                            // the bar at the top
        assertEquals(Color(0xFFF0F7E9), px(5, root.size.height / 2))                                         // the page
    }
}

/** The approved palette is not re-coloured when the phone itself is in dark mode. */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h891dp-night-xxhdpi")
class DarkModeKeepsPaletteTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun home_stays_light_green_and_maroon_in_system_dark_mode() {
        val ui = Ui(rule); ui.skipSplash()
        val bmp = ui.snapshot(rule.activity)
        val card = ui.node("PhD Research Plan").fetchSemanticsNode()
        val d = rule.density.density
        assertEquals(Color(0xFF800000), Color(bmp.getPixel((card.positionInRoot.x + 8 * d).toInt(), (card.positionInRoot.y + card.size.height / 2).toInt())).copy(alpha = 1f))
        assertEquals(Color(0xFFF0F7E9), Color(bmp.getPixel((card.positionInRoot.x - 14 * d).toInt().coerceAtLeast(1), (card.positionInRoot.y + card.size.height / 2).toInt())).copy(alpha = 1f))
    }
}
