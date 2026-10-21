package org.tribhasa.sodhamitra

import android.content.ClipboardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextClearance
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerList
import org.tribhasa.sodhamitra.data.ProjectKind
import org.tribhasa.sodhamitra.util.Feedback

@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h2400dp-xxhdpi")
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var ui: Ui

    @Before fun setUp() { ui = Ui(rule); ui.skipSplash() }

    // ---- Home, Settings, branding ---------------------------------------------------------------------------

    @Test fun home_has_logo_brand_in_order_and_exactly_three_sections() {
        rule.onNodeWithContentDescription("Tribhāṣā").assertExists()
        ui.assertOrder("Śodhamitra", "Research Planner", "©Dr. Diddigi Vamsi Krishna", "PhD Research Plan",
            "Individual Researcher / Author", "Editing / Translation Planner", "Created by Dr. Diddigi Vamsi Krishna")
        assertFalse(ui.exists("Settings"))                      // Settings now lives in the menu (the hamburger icon), not on the page
        assertTrue(ui.existsDesc("Open menu"))
        // the logo is above the name
        val logoTop = rule.onNodeWithContentDescription("Tribhāṣā").fetchSemanticsNode().boundsInRoot.top
        assertTrue(logoTop < ui.top("Śodhamitra"))
        assertEquals(1, ui.count("©Dr. Diddigi Vamsi Krishna"))   // the copyright line under the brand appears once
        assertEquals(1, ui.count("Created by Dr. Diddigi Vamsi Krishna"))   // and so does the footer
    }

    @Test fun settings_lists_exactly_the_six_options_in_order_with_the_copyright_footer() {
        ui.openSettings()
        ui.assertOrder("Profile", "Language", "About App", "About Dr. DVK", "Support Śodhamitra", "Feedback", "©Dr. Diddigi Vamsi Krishna")
        assertEquals(1, ui.count("©Dr. Diddigi Vamsi Krishna"))
        // the footer is at the very bottom of the page
        assertTrue(ui.top("©Dr. Diddigi Vamsi Krishna") > ui.top("Feedback"))
    }

    @Test fun feedback_opens_a_prefilled_draft_and_never_sends() {
        ui.openSettings(); ui.click("Feedback")
        val intent = shadowOf(ui.app).nextStartedActivity
        assertNotNull(intent)
        assertEquals(Intent.ACTION_SENDTO, intent.action)   // a draft the user composes and sends; there is no send action
        assertTrue(intent.dataString!!.startsWith("mailto:dvkrishna92@gmail.com"))
        assertEquals("Śodhamitra App Feedback", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("dvkrishna92@gmail.com", intent.getStringArrayExtra(Intent.EXTRA_EMAIL)!!.single())
        assertTrue(intent.dataString!!.endsWith("subject=" + android.net.Uri.encode("Śodhamitra App Feedback")))
    }

    @Test fun feedback_without_an_email_app_shows_a_message_and_a_copyable_address() {
        val old = Feedback.starter
        Feedback.starter = { _, _ -> throw ActivityNotFoundException() }
        try {
            ui.openSettings(); ui.click("Feedback")
            ui.await("No email app found"); ui.assertShown("dvkrishna92@gmail.com", "Copy email address")
            ui.click("Copy email address")
            val cm = ui.app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals("dvkrishna92@gmail.com", cm.primaryClip!!.getItemAt(0).text.toString())
        } finally { Feedback.starter = old }
    }

    @Test fun support_page_shows_the_supplied_payment_details_and_copies_the_upi_id() {
        ui.openSettings(); ui.click("Support Śodhamitra")
        ui.assertShown("8970500450@kotakbank", "Copy UPI ID", "Bank transfer details")
        rule.onNodeWithContentDescription("UPI QR").assertExists()
        assertFalse(ui.exists("1348857820"))            // bank section is collapsed until opened
        ui.click("Bank transfer details")
        ui.assertShown("1348857820", "KKBK0000425", "MYSORE", "DIDDIGI VAMSI KRISHNA")
        ui.click("Copy UPI ID")
        val cm = ui.app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("8970500450@kotakbank", cm.primaryClip!!.getItemAt(0).text.toString())
        ui.assertShown("Contributions are entirely optional. Every research feature remains available without contributing.")
        assertEquals(0, listOf("UPI PIN", "OTP").count { ui.exists(it) })   // nothing asks for a PIN or OTP
    }

    @Test fun about_pages_have_the_copyright_once() {
        ui.openSettings(); ui.click("About App")
        ui.assertShown("Fonts and licences"); assertEquals(1, ui.count("©Dr. Diddigi Vamsi Krishna"))
        ui.click("Show licence texts"); ui.await("UFL-Ubuntu-Font-Licence-1.0")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        ui.click("About Dr. DVK")
        ui.assertShown("Dr. Diddigi Vamsi Krishna", "Sanskrit Scholar | Manuscriptologist | Researcher")
        assertEquals(1, ui.count("©Dr. Diddigi Vamsi Krishna"))
        assertFalse(ui.exists("Veda Peetha Prashasti (2024)"))
    }

    // ---- Language ---------------------------------------------------------------------------------------------

    @Test fun choosing_a_language_in_settings_changes_the_interface_but_never_the_users_content() {
        ui.click("PhD Research Plan"); ui.click("+ Add Title of Thesis"); ui.await("Add Title of Thesis")
        ui.type("Research Title", "ರಾಮಾಯಣ — Rāmāyaṇa"); ui.save(); ui.await("ರಾಮಾಯಣ — Rāmāyaṇa")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; ui.await("PhD Research Plan")
        ui.openSettings(); ui.click("Language")
        ui.assertShown("English", "ಕನ್ನಡ", "संस्कृतम्", "తెలుగు", "हिन्दी", "தமிழ்", "বাংলা", "ગુજરાતી", "മലയാളം", "ଓଡ଼ିଆ")
        ui.assertOrder("English", "ಕನ್ನಡ", "संस्कृतम्", "తెలుగు", "हिन्दी", "தமிழ்", "বাংলা", "ગુજરાતી", "മലയാളം", "ଓଡ଼ିଆ")
        ui.click("ಕನ್ನಡ")                                   // the activity is recreated in Kannada
        ui.await("ಭಾಷೆ")                                    // the Language page now has its Kannada title
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        ui.await("ಪಿಎಚ್.ಡಿ. ಸಂಶೋಧನಾ ಯೋಜನೆ"); assertFalse(ui.exists("PhD Research Plan"))
        ui.click("ಪಿಎಚ್.ಡಿ. ಸಂಶೋಧನಾ ಯೋಜನೆ")
        ui.await("ರಾಮಾಯಣ — Rāmāyaṇa")                       // the project title is exactly as typed
        assertEquals("ರಾಮಾಯಣ — Rāmāyaṇa", runBlocking { ui.repo.projects(ProjectKind.PHD).first() }.single().title)
        // and back to English
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; ui.awaitDesc("ಮೆನು ತೆರೆಯಿರಿ")
        ui.openSettings("ಮೆನು ತೆರೆಯಿರಿ", "ಸೆಟ್ಟಿಂಗ್‌ಗಳು"); ui.click("ಭಾಷೆ"); ui.click("English")
        ui.await("Language")
    }

    // ---- Profile ----------------------------------------------------------------------------------------------

    @Test fun profile_has_no_mandatory_fields() {
        ui.openSettings(); ui.click("Profile")
        ui.click("Save Changes"); ui.await("Profile saved.")        // an entirely empty profile can be saved ...
        assertTrue(runBlocking { ui.repo.profileOnce() }.isEmpty)
        ui.await("Edit")                                            // ... and is then shown as read-only details
        ui.click("Edit"); ui.click("Dr."); ui.type("Name", "Tester"); ui.click("Female"); ui.click("Save Changes")
        ui.awaitDb { runBlocking { ui.repo.profileOnce() }.name == "Tester" }
        val p = runBlocking { ui.repo.profileOnce() }
        assertEquals("Dr.", p.prefix); assertEquals("Tester", p.name); assertEquals("FEMALE", p.sex)
    }

    // ---- PhD Research ---------------------------------------------------------------------------------------

    private fun openPhdProject(title: String) {
        ui.click("PhD Research Plan"); ui.click("+ Add Title of Thesis")
        ui.await("Add Title of Thesis"); ui.type("Research Title", title); ui.save(); ui.await(title)
        ui.click(title)
    }

    @Test fun phd_project_outline_chapter_point_description_expand_collapse() {
        openPhdProject("My thesis")
        ui.assertOrder("Thesis Outline", "Bibliography")
        ui.click("Thesis Outline"); ui.click("+ Add Chapter")
        ui.await("Add Chapter"); ui.type("Chapter Title", "Introduction"); ui.save(); ui.await("1. Introduction")
        ui.click("1. Introduction")
        ui.assertShown("+ Add Point"); ui.await("Introduction"); ui.awaitDb { ui.count("Introduction") == 2 }   // app bar title + the chapter's own title
        ui.click("+ Add Point")
        ui.type("Point Title", "Aim"); ui.type("Description", "The first description\nsecond line"); ui.save(); ui.await("Aim")
        ui.click("+ Add Point")
        ui.type("Point Title", "Scope"); ui.type("Description", "Other text"); ui.save(); ui.await("Scope")
        assertFalse(ui.exists("The first description")); assertFalse(ui.exists("Other text"))   // collapsed: titles only
        ui.click("Aim"); ui.await("The first description")
        assertFalse(ui.exists("Other text"))                                                   // expanding shows only its own description
        ui.click("Scope"); ui.await("Other text"); ui.assertShown("The first description", "second line")
        ui.click("Aim"); ui.awaitGone("The first description"); ui.assertShown("Other text")
    }

    @Test fun edit_point_updates_in_place_and_cancel_discards() {
        openPhdProject("T"); ui.click("Thesis Outline"); ui.click("+ Add Chapter")
        ui.type("Chapter Title", "C1"); ui.save(); ui.click("1. C1"); ui.click("+ Add Point")
        ui.type("Point Title", "P1"); ui.type("Description", "D1"); ui.save(); ui.await("P1")
        ui.longPress("P1"); ui.assertShown("Edit", "Delete"); ui.click("Edit")
        ui.await("Edit Point"); ui.type("Point Title", " changed"); ui.click("Cancel"); ui.await("P1")
        assertFalse(ui.exists("P1 changed"))
        ui.longPress("P1"); ui.click("Edit"); ui.await("Edit Point"); ui.replace("Point Title", "P1 v2"); ui.saveChanges()
        ui.await("P1 v2")
        assertEquals(1, runBlocking { ui.repo.dao.pointsOnce(ui.repo.dao.chaptersOnce(1)[0].id) }.size)   // updated, not duplicated
    }

    @Test fun long_press_project_offers_edit_delete_export_and_delete_asks_first() {
        ui.click("PhD Research Plan"); ui.click("+ Add Title of Thesis"); ui.type("Research Title", "Doomed"); ui.save(); ui.await("Doomed")
        ui.longPress("Doomed"); ui.assertShown("Edit", "Delete", "Export")
        ui.click("Delete"); ui.await("Delete?"); ui.assertShown("Delete this thesis title and everything inside it? This cannot be undone.")
        ui.click("Cancel"); ui.assertShown("Doomed")                                   // cancelled: still there
        ui.longPress("Doomed"); ui.click("Delete"); ui.await("Delete?")
        ui.click("Delete", index = ui.count("Delete") - 1)                              // the dialog's confirm button
        ui.awaitGone("Doomed")
        assertTrue(runBlocking { ui.repo.projects(ProjectKind.PHD).first() }.isEmpty())
    }

    @Test fun edit_project_title_keeps_one_record() {
        ui.click("PhD Research Plan"); ui.click("+ Add Title of Thesis"); ui.type("Research Title", "Old"); ui.save(); ui.await("Old")
        ui.longPress("Old"); ui.click("Edit"); ui.await("Edit Title of Thesis"); ui.replace("Research Title", "Old new"); ui.saveChanges()
        ui.await("Old new")
        assertEquals(1, runBlocking { ui.repo.projects(ProjectKind.PHD).first() }.size)
    }

    @Test fun bibliography_has_three_categories_each_with_its_own_add() {
        openPhdProject("B"); ui.click("Bibliography")
        ui.assertOrder("Books", "MSS / Manuscripts", "Articles"); assertEquals(3, ui.count("+ Add"))
        ui.click("+ Add", 0); ui.type("Title", "A Book"); ui.save(); ui.await("A Book")
        ui.click("+ Add", 2)                                       // Articles
        ui.type("Title of Article", "An Article"); ui.click("Save")
        ui.await("ISSN is required.")                              // ISSN is mandatory
        ui.type("ISSN", "1234-5678")
        assertFalse(ui.exists("Scopus details"))                   // shown only once Scopus indexing is ticked
        ui.click("Indexed in Scopus"); ui.assertShown("Scopus details"); ui.assertShown("Impact Factor")
        ui.save(); ui.await("An Article")
        assertEquals("1234-5678", runBlocking { ui.repo.dao.bibArticlesOnce(1) }.single().issn)
    }

    // ---- shared manuscript form ----------------------------------------------------------------------------

    private val manuscriptLabels = listOf("Mss Serial No.", "Mss Accession No.", "Mss Title *", "Stakeholder", "Place", "Edited / Not Edited",
        "Author / Attributed Author", "Language", "Script", "Material (Palm Leaf / Paper / Other)", "Folio Count",
        "Completeness and Condition", "Catalogue Reference", "Remarks")

    @Test fun manuscript_form_in_all_three_places_has_the_same_fourteen_fields_in_order() {
        // 1. PhD Research -> Bibliography -> MSS / Manuscripts
        openPhdProject("M"); ui.click("Bibliography"); ui.click("+ Add", 1)
        ui.assertOrder(*manuscriptLabels.toTypedArray()); ui.assertShown("Add Manuscript")
        assertFalse(ui.exists("Published")); assertFalse(ui.exists("Stakeholding Institution"))   // planning boxes only in the Editing List
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        // 2. Individual Researcher / Author -> Research Notes -> References / Citations -> MSS / Manuscripts
        repeat(3) { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
        ui.await("PhD Research Plan"); ui.click("Individual Researcher / Author"); ui.click("+ Add Title of Book")
        ui.type("Book Title", "Bk"); ui.save(); ui.click("Bk"); ui.click("Research Notes")
        ui.await("References / Citations"); ui.click("+ Add", 1)   // the MSS / Manuscripts heading of the citations
        ui.assertOrder(*manuscriptLabels.toTypedArray(), "Page / Folio", "Contextual note")
        repeat(4) { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
        // 3. Editing / Translation Planner -> Future Editing List -> Manuscript
        ui.click("Editing / Translation Planner"); ui.click("Future Editing List"); ui.click("+ Add", 0)
        ui.assertOrder(*manuscriptLabels.toTypedArray(), "Published", "Unpublished", "To be translated")
    }

    @Test fun manuscript_stakeholder_place_rule_on_create_and_edit() {
        ui.click("Editing / Translation Planner"); ui.click("Future Editing List"); ui.click("+ Add", 0)
        // only the title: saves, Stakeholder empty
        ui.type("Mss Title", "Title only"); ui.save(); ui.await("Title only")
        assertEquals(1, runBlocking { ui.repo.editingManuscripts().first() }.size)
        // stakeholder without place: refused with the localised message
        ui.click("+ Add", 0); ui.type("Mss Title", "With holder"); ui.type("Stakeholder", "ORI Mysuru")
        ui.assertShown("Place *")                                   // the marker appears as soon as Stakeholder has text
        ui.click("Save"); ui.await("Place is required when Stakeholder is provided.")
        assertEquals(1, runBlocking { ui.repo.editingManuscripts().first() }.size)   // nothing was saved
        // clearing the stakeholder makes Place optional again
        ui.field("Stakeholder").performTextClearance(); rule.waitForIdle()
        ui.awaitGone("Place is required when Stakeholder is provided.")
        // stakeholder + place saves
        ui.type("Stakeholder", "ORI Mysuru"); ui.type("Place", "Mysuru"); ui.save(); ui.await("With holder")
        val saved = runBlocking { ui.repo.editingManuscripts().first() }
        assertEquals(2, saved.size); assertEquals("Mysuru", saved[1].place)
        // editing: removing the place of a record that has a stakeholder is refused as well, and the record is updated in place
        ui.longPress("With holder"); ui.click("Edit"); ui.await("Edit Manuscript")
        ui.field("Place").performTextClearance(); ui.click("Save Changes")
        ui.await("Place is required when Stakeholder is provided.")
        ui.replace("Place", "Mysore"); ui.saveChanges(); ui.await("Mysore")
        val after = runBlocking { ui.repo.editingManuscripts().first() }
        assertEquals(2, after.size); assertEquals("Mysore", after[1].place)
    }

    @Test fun editing_list_planning_checkboxes_are_independent() {
        ui.click("Editing / Translation Planner"); ui.click("Future Editing List"); ui.click("+ Add", 0)
        ui.type("Mss Title", "Plan"); ui.click("Published"); ui.click("To be translated"); ui.save(); ui.await("Plan")
        val m = runBlocking { ui.repo.editingManuscripts().first() }.single()
        assertTrue(m.published); assertTrue(m.toBeTranslated); assertFalse(m.unpublished)
        // both "Published" and "Unpublished" may be ticked together: they are not one status
        ui.longPress("Plan"); ui.click("Edit"); ui.click("Unpublished"); ui.saveChanges(); ui.await("Plan")
        val m2 = runBlocking { ui.repo.editingManuscripts().first() }.single()
        assertTrue(m2.published && m2.unpublished && m2.toBeTranslated)
    }

    // ---- Book forms of the Future Editing List and the Future Trans List ------------------------------------

    private val bookLabels = listOf("Title of Book *", "Author", "Editor", "Edition", "Language", "Publisher", "Year", "To be translated",
        "Stakeholder", "Place", "Accession No.", "E-Library Name / Paste Link of the Book", "Translation Available")

    private fun bookFormChecks(list: String, addIndex: Int) {
        ui.assertOrder(*bookLabels.toTypedArray())
        assertFalse(ui.exists("Language(s) in which translation is available"))
        ui.click("Translation Available")                          // reveals the optional languages box (14th field)
        ui.assertOrder(*bookLabels.toTypedArray(), "Language(s) in which translation is available")
        ui.click("Translation Available"); assertFalse(ui.exists("Language(s) in which translation is available"))
        // stakeholder without place is refused; the record is not saved
        ui.type("Title of Book", "Rule book"); ui.type("Stakeholder", "A library"); ui.click("Save")
        ui.await("Place is required when Stakeholder is provided.")
        assertTrue(runBlocking { ui.repo.plannerBooks(list).first() }.isEmpty())
        ui.type("Place", "Mysuru"); ui.save(); ui.await("Rule book")
        assertEquals(1, runBlocking { ui.repo.plannerBooks(list).first() }.size)
        // title only (stakeholder empty) saves too
        ui.click("+ Add", addIndex); ui.type("Title of Book", "Only a title"); ui.save(); ui.await("Only a title")
        assertEquals(listOf("Rule book", "Only a title"), runBlocking { ui.repo.plannerBooks(list).first() }.map { it.title })
    }

    @Test fun future_editing_list_book_form() {
        ui.click("Editing / Translation Planner"); ui.click("Future Editing List")
        ui.assertOrder("Manuscript", "Book"); ui.click("+ Add", 1)
        bookFormChecks(PlannerList.EDITING, addIndex = 1)   // the Book heading is the second "+ Add"
    }

    @Test fun future_trans_list_book_form_is_identical() {
        ui.click("Editing / Translation Planner"); ui.click("Future Trans List")
        assertFalse(ui.exists("Manuscript")); ui.assertShown("Book"); ui.click("+ Add", 0)
        bookFormChecks(PlannerList.TRANS, addIndex = 0)
    }

    @Test fun planner_offers_exactly_two_options() {
        ui.click("Editing / Translation Planner")
        ui.assertOrder("Future Editing List", "Future Trans List")
        assertFalse(ui.exists("Settings"))
    }

    @Test fun planner_table_shows_records_and_delete_asks_first() {
        ui.click("Editing / Translation Planner"); ui.click("Future Trans List"); ui.click("+ Add", 0)
        ui.type("Title of Book", "Tabled"); ui.type("Author", "Someone"); ui.save(); ui.await("Tabled")
        ui.assertShown("Someone", "Title of Book", "Accession No.")   // header row + record row of the table
        ui.longPress("Tabled"); ui.click("Delete"); ui.await("Delete?"); ui.click("Cancel"); ui.assertShown("Tabled")
        ui.longPress("Tabled"); ui.click("Delete"); ui.await("Delete?"); ui.click("Delete", ui.count("Delete") - 1)
        ui.awaitGone("Tabled")
    }

    // ---- Individual Researcher / Author ----------------------------------------------------------------------------

    @Test fun individual_researcher_notes_and_citations_without_duplicate_sources() {
        ui.click("Individual Researcher / Author"); ui.click("+ Add Title of Book"); ui.type("Book Title", "My book"); ui.save(); ui.click("My book")
        ui.assertOrder("Book Outline", "Research Notes")
        ui.click("Research Notes")
        ui.click("+ Add Note"); ui.type("Note Title", "Idea"); ui.type("Description", "Long thought"); ui.save(); ui.await("Idea")
        assertFalse(ui.exists("Long thought")); ui.click("Idea"); ui.await("Long thought")
        ui.assertOrder("Research Notes", "References / Citations", "Books", "MSS / Manuscripts", "Articles")
        ui.click("+ Add", 0)                                              // citations -> Books
        ui.await("Add Citation"); ui.type("Title", "Mahābhārata"); ui.type("Page / Folio", "12"); ui.type("Contextual note", "see here"); ui.save(); ui.await("Page / Folio: 12")
        ui.click("+ Add", 0)
        ui.await("Add Citation"); ui.type("Title", "mahābhārata"); ui.type("Page / Folio", "40"); ui.save(); ui.await("Page / Folio: 40")
        assertEquals(1, runBlocking { ui.repo.dao.bibBooksOnce(1) }.size)          // one stored source, two citations
        assertEquals(2, runBlocking { ui.repo.dao.citationsOnce(1) }.size)
    }

    // ---- export ----------------------------------------------------------------------------------------------------

    @Test fun export_with_nothing_to_export_creates_no_pdf() {
        ui.click("PhD Research Plan"); ui.click("+ Add Title of Thesis"); ui.type("Research Title", "Empty"); ui.save(); ui.await("Empty")
        ui.longPress("Empty"); ui.click("Export")
        ui.assertOrder("Thesis Outline", "Books", "Manuscripts", "Articles"); ui.assertShown("Page: A4 Portrait")
        ui.click("Create PDF"); ui.await("Nothing to export: every selected section is empty.")
        assertTrue(java.io.File(ui.app.cacheDir, "exports").listFiles().orEmpty().isEmpty())
        // unticking everything gives a different message
        for (t in listOf("Thesis Outline", "Books", "Manuscripts", "Articles")) ui.click(t)
        ui.click("Create PDF"); ui.await("Select at least one section.")
    }

    @Test fun planner_export_is_a4_landscape() {
        ui.click("Editing / Translation Planner"); ui.click("Future Editing List"); ui.click("Export")
        ui.assertOrder("Manuscript", "Book"); ui.assertShown("Page: A4 Landscape")
    }
}
