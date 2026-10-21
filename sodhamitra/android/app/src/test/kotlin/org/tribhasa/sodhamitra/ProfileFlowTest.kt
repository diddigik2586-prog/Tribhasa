package org.tribhasa.sodhamitra

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * V2 Profile: an editable form until a profile has been saved, then a read-only details view with an Edit button (pencil) at the
 * top right; Edit re-opens the form with the saved values; Save returns to the details; Cancel and a confirmed Back discard the
 * edits; the saved profile is shown again after the app is restarted.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h2400dp-xxhdpi")
class ProfileFlowTest {
    @get:Rule val rule = createEmptyComposeRule()

    private fun app() = ApplicationProvider.getApplicationContext<Context>() as TestApp
    private fun textFields() = rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
    private fun profileRows(): Int = app().repository.db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM profile").use { it.moveToFirst(); it.getInt(0) }

    private fun launch(): Pair<ActivityScenario<MainActivity>, Ui> {
        val sc = ActivityScenario.launch(MainActivity::class.java)
        val ui = Ui(rule); ui.skipSplash()
        return sc to ui
    }
    private fun ActivityScenario<MainActivity>.back(ui: Ui) { onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }

    @Test fun first_visit_shows_the_form_then_saving_shows_read_only_details_with_a_top_right_edit_button() {
        val (sc, ui) = launch()
        sc.use {
            ui.openSettings(); ui.click("Profile")
            ui.await("Save Changes")
            assertTrue("the editable form is shown when nothing is saved", textFields() >= 5)
            assertFalse(ui.exists("Edit"))
            ui.click("Dr."); ui.type("Name", "Diddigi"); ui.click("Male")
            ui.type("Designation", "Researcher"); ui.type("Institution", "ORI"); ui.type("Address", "Mysuru"); ui.type("Place", "Mysuru city")
            ui.click("Save Changes")
            ui.await("Profile saved."); ui.await("Edit")
            // read-only: every label and value, no text fields, no Save button
            ui.assertShown("Profile photograph", "Prefix", "Dr.", "Name", "Diddigi", "Sex", "Male", "Designation", "Researcher", "Institution", "ORI",
                "Address", "Mysuru", "Place", "Mysuru city")
            assertEquals(0, textFields()); assertFalse(ui.exists("Save Changes"))
            ui.assertOrder("Prefix", "Name", "Sex", "Designation", "Institution", "Address", "Place")
            // the Edit button (pencil) sits at the top right
            val edit = ui.node("Edit").fetchSemanticsNode(); val root = rule.onRoot().fetchSemanticsNode()
            assertTrue("Edit in the right half", edit.positionInRoot.x > root.size.width / 2f)
            assertTrue("Edit in the top bar", edit.positionInRoot.y < 140 * rule.density.density)
            assertEquals(1, profileRows())
        }
    }

    @Test fun empty_fields_of_a_saved_profile_are_still_listed_with_a_dash() {
        val (sc, ui) = launch()
        sc.use {
            ui.openSettings(); ui.click("Profile"); ui.await("Save Changes"); ui.type("Name", "Only name"); ui.click("Save Changes"); ui.await("Edit")
            ui.assertShown("Prefix", "Sex", "Designation", "Institution", "Address", "Place")
            assertEquals(6, ui.count("—"))                         // prefix, sex, designation, institution, address and place are empty
        }
    }

    @Test fun edit_prefills_save_returns_to_details_and_cancel_discards() {
        val (sc, ui) = launch()
        sc.use {
            ui.openSettings(); ui.click("Profile"); ui.await("Save Changes"); ui.type("Name", "First"); ui.type("Place", "Mysuru"); ui.click("Save Changes"); ui.await("Edit")
            // Edit -> the same form with the saved values
            ui.click("Edit")
            ui.await("Save Changes")
            assertTrue(rule.onAllNodes(hasSetTextAction() and hasText("First")).fetchSemanticsNodes().size == 1)
            assertTrue(rule.onAllNodes(hasSetTextAction() and hasText("Mysuru")).fetchSemanticsNodes().size == 1)
            // change something and Cancel: back to the unchanged details
            ui.replace("Name", "Changed"); ui.click("Cancel")
            ui.await("Edit"); ui.assertShown("First"); assertFalse(ui.exists("Changed"))
            assertEquals("First", runBlocking { app().repository.profileOnce() }.name)
            // change and Save: the details show the new value; still one profile row
            ui.click("Edit"); ui.await("Save Changes"); ui.replace("Name", "Second"); ui.replace("Place", "Bengaluru"); ui.click("Save Changes")
            ui.await("Second"); ui.assertShown("Bengaluru", "Edit"); assertFalse(ui.exists("First"))
            ui.awaitDb { runBlocking { app().repository.profileOnce() }.name == "Second" }
            assertEquals(1, profileRows())
            // editing again starts from the newly saved values, not from the discarded ones
            ui.click("Edit"); ui.await("Save Changes")
            assertTrue(rule.onAllNodes(hasSetTextAction() and hasText("Second")).fetchSemanticsNodes().size == 1)
        }
    }

    @Test fun back_with_unsaved_changes_asks_first_and_never_discards_silently() {
        val (sc, ui) = launch()
        sc.use {
            ui.openSettings(); ui.click("Profile"); ui.await("Save Changes"); ui.type("Name", "Keep me"); ui.click("Save Changes"); ui.await("Edit")
            ui.click("Edit"); ui.await("Save Changes")
            // no changes: Back simply returns to the details
            sc.back(ui); ui.await("Edit"); assertFalse(ui.exists("Discard changes?"))
            // with changes: Back asks
            ui.click("Edit"); ui.await("Save Changes"); ui.replace("Name", "Edited name")
            sc.back(ui); ui.await("Discard changes?")
            ui.assertShown("You have unsaved changes to your profile. Do you want to discard them?", "Keep editing", "Discard")
            ui.click("Keep editing"); ui.awaitGone("Discard changes?")
            assertTrue("the edits are still in the form", rule.onAllNodes(hasSetTextAction() and hasText("Edited name")).fetchSemanticsNodes().size == 1)
            // the arrow in the top bar behaves the same way
            ui.clickDesc("Back"); ui.await("Discard changes?")
            ui.click("Discard"); ui.await("Edit")
            ui.assertShown("Keep me"); assertFalse(ui.exists("Edited name"))
            assertEquals("Keep me", runBlocking { app().repository.profileOnce() }.name)
        }
    }

    @Test fun first_entry_form_asks_before_leaving_with_typed_text_and_leaves_freely_when_empty() {
        val (sc, ui) = launch()
        sc.use {
            ui.openSettings(); ui.click("Profile"); ui.await("Save Changes")
            sc.back(ui); ui.await("Profile")                       // nothing typed: straight back to Settings
            assertFalse(ui.exists("Discard changes?")); ui.assertShown("Feedback")
            ui.click("Profile"); ui.await("Save Changes"); ui.type("Name", "Typed")
            sc.back(ui); ui.await("Discard changes?")
            ui.click("Discard"); ui.await("Feedback")              // left without saving
            assertEquals(0, profileRows())
            ui.click("Profile"); ui.await("Save Changes")          // and the form is empty again
            assertEquals(0, rule.onAllNodes(hasSetTextAction() and hasText("Typed")).fetchSemanticsNodes().size)
        }
    }

    @Test fun the_saved_profile_is_shown_again_after_the_app_is_restarted() {
        val (sc, ui) = launch()
        sc.use {
            ui.openSettings(); ui.click("Profile"); ui.await("Save Changes"); ui.type("Name", "Persistent"); ui.type("Designation", "Scholar"); ui.click("Save Changes"); ui.await("Edit")
            ui.awaitDb { runBlocking { app().repository.profileOnce() }.name == "Persistent" }
        }
        // a completely new activity, view model and navigation stack (the database file is what remains)
        val (sc2, ui2) = launch()
        sc2.use {
            ui2.openSettings(); ui2.click("Profile")
            ui2.await("Edit"); ui2.assertShown("Persistent", "Scholar")
            assertEquals(0, textFields()); assertFalse(ui2.exists("Save Changes"))
            assertEquals(1, profileRows())
        }
    }
}

@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h2400dp-xxhdpi")
class ProfileCancelAfterKeepEditingTest {
    @get:Rule val rule = createEmptyComposeRule()
    @Test fun cancel_still_works_after_keep_editing() {
        val sc = ActivityScenario.launch(MainActivity::class.java)
        val ui = Ui(rule); ui.skipSplash()
        sc.use {
            ui.openSettings(); ui.click("Profile"); ui.await("Save Changes"); ui.type("Name", "Abc"); ui.click("Save Changes"); ui.await("Edit")
            ui.click("Edit"); ui.await("Cancel"); ui.type("Name", "def")
            sc.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
            ui.await("Discard changes?"); ui.click("Keep editing"); ui.awaitGone("Discard changes?")
            ui.click("Cancel")
            ui.await("Edit"); ui.assertShown("Abc")
        }
    }
}
