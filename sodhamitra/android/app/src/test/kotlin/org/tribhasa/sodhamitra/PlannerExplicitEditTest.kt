package org.tribhasa.sodhamitra

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerBook
import org.tribhasa.sodhamitra.data.PlannerList

/**
 * V2: in the Future Editing List and the Future Trans List a plain tap on a row never opens the edit form and there is no
 * "Tap to edit" helper text; a long-press shows the contextual options and Edit is chosen explicitly.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h2400dp-xxhdpi")
class PlannerExplicitEditTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var ui: Ui

    @Before fun setUp() {
        ui = Ui(rule); ui.skipSplash()
        runBlocking {
            ui.repo.saveManuscript(Manuscript(scope = MsScope.EDIT, title = "Row manuscript", stakeholder = "ORI", place = "Mysuru"))
            ui.repo.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = "Editing book", author = "A. Author"))
            ui.repo.savePlannerBook(PlannerBook(listType = PlannerList.TRANS, title = "Trans book", author = "T. Author"))
        }
    }

    private fun noTapHelperText() {
        assertEquals("no node mentions tapping", 0, rule.onAllNodes(hasText("Tap", substring = true)).fetchSemanticsNodes().size)
        assertFalse(ui.exists("Swipe sideways to see all columns. Tap a row to edit it; long-press for more."))
        ui.assertShown("Swipe sideways to see all columns. Long-press a row to edit or delete it.")   // the remaining hint says how to edit
    }

    private fun checkRow(title: String, formTitle: String, route: () -> Unit) {
        route()
        ui.await(title)
        noTapHelperText()
        // a normal tap on the row (title cell, another cell, even repeated) never opens the form
        ui.node(title).performClick(); rule.waitForIdle()
        ui.node(title).performClick(); rule.waitForIdle()
        assertFalse("tap must not open $formTitle", ui.exists(formTitle)); ui.assertShown(title)
        // a long-press offers the options, and Edit is chosen explicitly
        ui.longPress(title); ui.assertShown("Edit", "Delete")
        assertFalse(ui.exists(formTitle))
        ui.click("Edit"); ui.await(formTitle)
    }

    @Test fun editing_list_manuscript_rows() = checkRow("Row manuscript", "Edit Manuscript") {
        ui.click("Editing / Translation Planner"); ui.click("Future Editing List")
    }

    @Test fun editing_list_book_rows() = checkRow("Editing book", "Edit Book") {
        ui.click("Editing / Translation Planner"); ui.click("Future Editing List")
    }

    @Test fun translation_list_book_rows() = checkRow("Trans book", "Edit Book") {
        ui.click("Editing / Translation Planner"); ui.click("Future Trans List")
    }

    @Test fun delete_still_asks_for_confirmation_and_the_data_is_untouched_by_taps() {
        ui.click("Editing / Translation Planner"); ui.click("Future Trans List"); ui.await("Trans book")
        ui.node("Trans book").performClick(); rule.waitForIdle()
        ui.longPress("Trans book"); ui.click("Delete"); ui.await("Delete?"); ui.click("Cancel")
        ui.assertShown("Trans book")
        assertEquals(1, runBlocking { ui.repo.plannerBooks(PlannerList.TRANS).first() }.size)
        ui.longPress("Trans book"); ui.click("Delete"); ui.await("Delete?"); ui.click("Delete", ui.count("Delete") - 1)
        ui.awaitGone("Trans book")
        assertTrue(runBlocking { ui.repo.plannerBooks(PlannerList.TRANS).first() }.isEmpty())
    }

    @Test fun editing_through_the_menu_updates_the_record_in_place() {
        ui.click("Editing / Translation Planner"); ui.click("Future Editing List"); ui.await("Row manuscript")
        ui.longPress("Row manuscript"); ui.click("Edit"); ui.await("Edit Manuscript")
        ui.replace("Mss Title", "Row manuscript v2"); ui.saveChanges(); ui.await("Row manuscript v2")
        assertEquals(1, runBlocking { ui.repo.editingManuscripts().first() }.size)
    }
}
