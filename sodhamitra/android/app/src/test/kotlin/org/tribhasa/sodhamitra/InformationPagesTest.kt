package org.tribhasa.sodhamitra

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private fun SemanticsNode.shownText(): String = config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()
private fun SemanticsNode.textAlign(): TextAlign? {
    val results = mutableListOf<TextLayoutResult>()
    config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
    return results.firstOrNull()?.layoutInput?.style?.textAlign
}

/** V2 informational pages: the approved biography verbatim, existing About App text kept, headings and descriptions centred. */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class InformationPagesTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var ui: Ui

    @Before fun setUp() { ui = Ui(rule); ui.skipSplash() }

    private fun nodeOf(text: String): SemanticsNode = ui.node(text).fetchSemanticsNode()

    /** the approved biography, character for character as supplied in the V2 specification */
    private val biography = listOf(
        "Dr. Diddigi Vamsi Krishna",
        "Sanskrit Scholar | Manuscriptologist | Researcher",
        "Dr. Diddigi Vamsi Krishna is a Sanskrit scholar and manuscriptologist specialising in Sanskrit textual studies, manuscript preservation, critical editing, cataloguing and Indic script studies. He holds a Ph.D. in Sanskrit from the University of Mysore, with research focused on the Nirukta tradition.",
        "He serves as a Research Assistant Grade II at the Oriental Research Institute (ORI), University of Mysore, Mysuru, contributing to manuscript research, scholarly editing, archival documentation and publication. As Coordinator of the ORI Digital Books Project, he is involved in digitisation, digital preservation and the documentation of scholarly collections.",
        "His scholarly interests include Sanskrit textual scholarship, multilingual manuscripts, transliteration, traditional knowledge systems and digital heritage preservation. He has also contributed to editorial activities, academic publications and manuscriptology training.",
        "Dr. Krishna is the Founding Director of Shimshumara Shodha Pratishthanam, Mysuru, and has served as Honorary Chief Editor at Adishila Publication, Bengaluru. He has also served as a reviewer for the Jahnavi Sanskrit e-Journal.",
        "He received the Grantha Puraskara (2020) from Karnataka Samskrit University for his Sanskrit work Niruktacintanam and the Saṃskṛta-Suptanidhi-Ācārya recognition in 2014.",
        "©Dr. Diddigi Vamsi Krishna",
    )

    @Test fun about_dr_dvk_shows_the_approved_biography_verbatim_in_order_and_centred() {
        ui.openMenu(); ui.click("About Dr. DVK")
        ui.await(biography[0])
        val nodes = biography.map { nodeOf(it) }                          // each paragraph exists as ONE text, exactly as supplied
        for ((text, n) in biography.zip(nodes)) {
            assertEquals(text, n.shownText())
            assertEquals("'$text' is centred", TextAlign.Center, n.textAlign())
        }
        val tops = nodes.map { it.positionInRoot.y }
        assertEquals("paragraph order", tops.sorted(), tops)
        assertEquals("no paragraph overlaps another", tops.distinct(), tops)
        // every paragraph is horizontally centred on the page
        val rootW = rule.onRoot().fetchSemanticsNode().size.width
        for (n in nodes) assertEquals(rootW / 2f, n.positionInRoot.x + n.size.width / 2f, 2f)
        // the designation is exactly "Founding Director"; nothing from the superseded draft or unapproved awards
        assertTrue(nodes[5].shownText().contains("Founding Director"))
        for (gone in listOf("Recognition", "Veda Peetha", "2024", "2025", "M.A. and Ph.D."))
            assertEquals("'$gone' must not appear", 0, rule.onAllNodes(androidx.compose.ui.test.hasText(gone, substring = true)).fetchSemanticsNodes().size)
        // the whole page is reachable by ordinary scrolling
        rule.onNode(androidx.compose.ui.test.hasText("©Dr. Diddigi Vamsi Krishna")).performScrollTo().assertIsDisplayed()
        assertEquals(1, ui.count("©Dr. Diddigi Vamsi Krishna"))
    }

    @Test fun the_biography_is_not_translated_when_the_interface_language_changes() {
        ui.openMenu(); ui.click("Language"); ui.click("ಕನ್ನಡ"); ui.await("ಭಾಷೆ")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; ui.awaitDesc("ಮೆನು ತೆರೆಯಿರಿ")
        ui.openMenu("ಮೆನು ತೆರೆಯಿರಿ"); ui.click("ಡಾ. ಡಿವಿಕೆ ಬಗ್ಗೆ")
        ui.await(biography[0])
        for (text in biography) assertEquals(text, nodeOf(text).shownText())
    }

    @Test fun about_app_keeps_every_approved_paragraph_in_order_and_centres_them() {
        ui.openMenu(); ui.click("About App")
        val ctx = rule.activity
        val ids = listOf(R.string.about_purpose, R.string.about_sections, R.string.about_projects, R.string.about_notes, R.string.about_bibliography,
            R.string.about_planner, R.string.about_languages, R.string.about_storage, R.string.about_pdf)
        val texts = ids.map { ctx.getString(it) }
        ui.await(texts[0])
        val nodes = texts.map { nodeOf(it) }
        val tops = nodes.map { it.positionInRoot.y }
        assertEquals(tops.sorted(), tops)
        for ((t, n) in texts.zip(nodes)) { assertEquals(t, n.shownText()); assertEquals(TextAlign.Center, n.textAlign()) }
        for (heading in listOf("Fonts and licences", ctx.getString(R.string.about_fonts_note))) assertEquals(TextAlign.Center, nodeOf(heading).textAlign())
        // the logo block (logo, name, tagline, copyright) is still above the description, version line included
        ui.assertOrder("Śodhamitra", "Research Planner", "©Dr. Diddigi Vamsi Krishna", texts[0])
        ui.assertShown("Version ${BuildConfig.VERSION_NAME}")
        assertEquals(TextAlign.Center, nodeOf("Version ${BuildConfig.VERSION_NAME}").textAlign())
        // the licence texts themselves keep their conventional left alignment
        rule.onNode(androidx.compose.ui.test.hasText("Show licence texts")).performScrollTo().performClick(); rule.waitForIdle(); ui.await("UFL-Ubuntu-Font-Licence-1.0")
        assertEquals(TextAlign.Center, nodeOf("UFL-Ubuntu-Font-Licence-1.0").textAlign())
    }

    @Test fun support_page_is_centred_and_keeps_its_qr_code_and_payment_actions() {
        ui.openMenu(); ui.click("Support Śodhamitra")
        ui.await("Copy UPI ID")
        val rootW = rule.onRoot().fetchSemanticsNode().size.width
        for (t in listOf(
            "Śodhamitra is free for everyone. If it helps your work, you may make a voluntary contribution towards its continued development.",
            "Contributions are entirely optional. Every research feature remains available without contributing.",
            "Scan with any UPI app")) assertEquals(t, TextAlign.Center, nodeOf(t).textAlign())
        val qr = rule.onNodeWithContentDescription("UPI QR").fetchSemanticsNode()
        assertEquals("QR centred", rootW / 2f, qr.positionInRoot.x + qr.size.width / 2f, 2f)
        assertEquals("QR keeps its square shape", qr.size.width.toFloat(), qr.size.height.toFloat(), 1.5f)
        assertTrue(qr.positionInRoot.x >= 0 && qr.positionInRoot.x + qr.size.width <= rootW)
        // existing payment details and actions
        ui.assertShown("8970500450@kotakbank", "Copy UPI ID", "Bank transfer details")
        ui.click("Bank transfer details")
        ui.assertShown("1348857820", "KKBK0000425", "MYSORE", "DIDDIGI VAMSI KRISHNA", "Kotak Mahindra Bank")
        assertEquals(TextAlign.Center, nodeOf("1348857820").textAlign())
        assertFalse(ui.exists("UPI PIN"))
    }

    @Test fun page_headings_are_centred_in_the_top_bar_of_the_three_informational_pages() {
        val rootW = rule.onRoot().fetchSemanticsNode().size.width
        for ((entry, title) in listOf("About App" to "About App", "About Dr. DVK" to "About Dr. DVK", "Support Śodhamitra" to "Support Śodhamitra")) {
            ui.openMenu(); ui.click(entry); ui.await("Version ${BuildConfig.VERSION_NAME}".takeIf { entry == "About App" } ?: title)
            // the title text in the bar (the first match is the bar, the settings row is gone)
            val n = ui.node(title).fetchSemanticsNode()
            assertEquals("$title centred in the bar", rootW / 2f, n.positionInRoot.x + n.size.width / 2f, 6f)
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle(); ui.await("PhD Research Plan")
        }
    }
}

/** V2 on a narrow phone: the Support page must not clip the QR code. */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w320dp-h568dp-xhdpi")
class NarrowScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun support_qr_and_home_cards_fit_a_320dp_screen() {
        val ui = Ui(rule); ui.skipSplash()
        val rootW = rule.onRoot().fetchSemanticsNode().size.width
        for (c in listOf("PhD Research Plan", "Individual Researcher / Author", "Editing / Translation Planner")) {
            val n = ui.node(c).fetchSemanticsNode()
            assertTrue("$c fits", n.positionInRoot.x >= 0 && n.positionInRoot.x + n.size.width <= rootW)
        }
        ui.openMenu(); ui.click("Support Śodhamitra"); ui.await("Copy UPI ID")
        val qr = rule.onNodeWithContentDescription("UPI QR").fetchSemanticsNode()
        assertTrue("QR inside the screen: x=${qr.positionInRoot.x} w=${qr.size.width} of $rootW", qr.positionInRoot.x >= 0 && qr.positionInRoot.x + qr.size.width <= rootW)
        assertEquals(qr.size.width.toFloat(), qr.size.height.toFloat(), 1.5f)
        assertEquals(rootW / 2f, qr.positionInRoot.x + qr.size.width / 2f, 2f)
    }
}
