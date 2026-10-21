package org.tribhasa.sodhamitra

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.tribhasa.sodhamitra.data.PlannerList
import org.tribhasa.sodhamitra.data.ProjectKind

/**
 * Runs AFTER the CI script has installed the V1 app (version 0.1.0), put a database of schema version 1 with a few records into it
 * and installed the newest build over it (same package, same signing key). Nothing is created or cleared here: the records made by
 * V1 must still be there and must be shown by the new screens.
 */
@RunWith(AndroidJUnit4::class)
class UpgradeKeepsDataTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun exists(t: String) = rule.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()
    private fun await(t: String) = rule.waitUntil(30_000) { rule.waitForIdle(); exists(t) }
    private fun click(t: String, i: Int = 0) { await(t); rule.onAllNodesWithText(t)[i].performClick(); rule.waitForIdle() }
    private fun back() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }

    @Test fun records_made_by_the_previous_version_survive_the_update_and_show_in_the_new_screens() {
        val repo = (rule.activity.application as SodhamitraApp).repository
        // the database itself
        assertEquals(listOf("Upgrade survivor — Nirukta thesis"), runBlocking { repo.projects(ProjectKind.PHD).first() }.map { it.title })
        assertEquals(listOf("Upgrade survivor — Book"), runBlocking { repo.projects(ProjectKind.BOOK).first() }.map { it.title })
        assertEquals("Upgrade survivor manuscript", runBlocking { repo.editingManuscripts().first() }.single().title)
        assertEquals("Upgrade survivor translation book", runBlocking { repo.plannerBooks(PlannerList.TRANS).first() }.single().title)
        assertEquals("Description text that must survive the update", runBlocking { repo.pointDescriptionOnce(1) })
        assertEquals("Upgrade Survivor", runBlocking { repo.profileOnce() }.name)

        // the new screens
        await("PhD Research Plan"); click("PhD Research Plan")
        await("Upgrade survivor — Nirukta thesis"); assertTrue(exists("+ Add Title of Thesis"))
        click("Upgrade survivor — Nirukta thesis"); click("Thesis Outline"); click("1. Upgrade chapter one"); click("Upgrade point")
        await("Description text that must survive the update")
        back(); back(); back(); back(); await("PhD Research Plan")
        click("Editing / Translation Planner"); click("Future Trans List"); await("Upgrade survivor translation book")
        back(); click("Future Editing List"); await("Upgrade survivor manuscript")
        back(); back(); await("PhD Research Plan")
        // a profile saved by V1 is shown in the new read-only view
        rule.onAllNodesWithContentDescription("Open menu")[0].performClick(); click("Profile")
        await("Edit"); await("Upgrade Survivor"); assertTrue(exists("Dr."))
    }
}
