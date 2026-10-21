package org.tribhasa.sodhamitra

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerBook
import org.tribhasa.sodhamitra.data.PlannerList

/** Writes screenshots of the V2 screens (build/screenshots/v2-*.png) so that they can be looked at; nothing is asserted here. */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class V2ScreenshotTour {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var ui: Ui
    private val dir = File("build/screenshots").apply { mkdirs() }

    @Before fun setUp() { ui = Ui(rule); ui.skipSplash() }

    private fun shot(name: String) {
        val bmp = ui.snapshot(rule.activity)
        File(dir, "v2-$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    /** the Save / Cancel buttons are at the end of the scrolling Profile form: scroll them into view first */
    private fun scrollClick(text: String) { ui.await(text); rule.onNode(androidx.compose.ui.test.hasText(text)).performScrollTo().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); rule.waitForIdle() }
    private fun back() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }

    @Test fun tour() {
        runBlocking {
            ui.repo.saveManuscript(Manuscript(scope = MsScope.EDIT, title = "Rāmāyaṇa palm-leaf bundle", serialNo = "KN-1", stakeholder = "ORI", place = "Mysuru", language = "Sanskrit", script = "Kannada"))
            ui.repo.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = "Śaṅkarabhāṣya", author = "Śaṅkara", year = "1900"))
        }
        // projects lists
        ui.click("PhD Research Plan"); ui.await("+ Add Title of Thesis"); shot("10-phd-empty")
        ui.click("+ Add Title of Thesis"); ui.await("Add Title of Thesis"); ui.type("Research Title", "Nirukta tradition"); shot("11-add-thesis")
        ui.save(); ui.await("Nirukta tradition"); shot("12-phd-list"); back()
        ui.click("Individual Researcher / Author"); ui.await("+ Add Title of Book"); shot("13-book-empty"); back()
        // profile
        ui.openMenu(); shot("20-menu"); ui.click("Profile"); ui.await("Save Changes"); shot("21-profile-form")
        ui.click("Dr."); ui.type("Name", "Diddigi Vamsi Krishna"); ui.type("Designation", "Research Assistant"); ui.type("Place", "Mysuru")
        scrollClick("Save Changes"); ui.await("Edit"); shot("22-profile-details"); ui.awaitGone("Profile saved.")   // the message would cover the bottom buttons
        ui.click("Edit"); ui.await("Cancel"); ui.type("Name", " (edited)"); shot("23-profile-edit")
        back(); ui.await("Discard changes?"); shot("24-profile-discard-dialog"); ui.click("Keep editing"); ui.awaitGone("Discard changes?")
        scrollClick("Cancel"); ui.await("Edit")
        back(); ui.await("PhD Research Plan")
        // information pages
        ui.openMenu(); ui.click("About App"); ui.await("Fonts and licences"); shot("30-about-app"); back()
        ui.openMenu(); ui.click("Support Śodhamitra"); ui.await("Copy UPI ID"); ui.click("Bank transfer details"); ui.await("1348857820"); shot("31-support-bank"); back()
        ui.await("PhD Research Plan")
        // planner tables: no helper text about tapping; long-press menu
        ui.click("Editing / Translation Planner"); shot("40-planner"); ui.click("Future Editing List"); ui.await("Rāmāyaṇa palm-leaf bundle"); shot("41-editing-list")
        ui.longPress("Rāmāyaṇa palm-leaf bundle"); shot("42-editing-list-menu")
    }
}
