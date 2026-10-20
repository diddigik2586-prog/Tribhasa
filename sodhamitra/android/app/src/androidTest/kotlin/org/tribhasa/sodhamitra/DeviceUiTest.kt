package org.tribhasa.sodhamitra

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.tribhasa.sodhamitra.data.PlannerList

/** The real screens driven on a real device: launch, navigation, save, and the Stakeholder / Place rule. */
@RunWith(AndroidJUnit4::class)
class DeviceUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun exists(t: String) = rule.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()
    private fun await(t: String) = rule.waitUntil(30_000) { rule.waitForIdle(); exists(t) }
    private fun click(t: String, i: Int = 0) { await(t); rule.onAllNodesWithText(t)[i].performClick(); rule.waitForIdle() }
    private fun type(label: String, v: String) { rule.onNode(hasSetTextAction() and (hasText(label) or hasText("$label *"))).performTextInput(v); rule.waitForIdle() }

    @Test fun launch_navigate_and_apply_the_stakeholder_place_rule() {
        val repo = (rule.activity.application as SodhamitraApp).repository
        runBlocking { repo.db.clearAllTables() }
        await("PhD Research")
        listOf("PhD Research", "Individual Researcher / Author", "Editing / Translation Planner", "Settings").forEach { assertTrue(it, exists(it)) }
        click("Editing / Translation Planner"); click("Future Trans List"); click("+ Add", 0)
        type("Title of Book", "Device book"); type("Stakeholder", "A library"); click("Save")
        await("Place is required when Stakeholder is provided.")
        assertTrue(runBlocking { repo.plannerBooks(PlannerList.TRANS).first() }.isEmpty())
        type("Place", "Mysuru"); click("Save"); await("Device book")
        rule.waitUntil(10_000) { runBlocking { repo.plannerBooks(PlannerList.TRANS).first() }.size == 1 }
        assertEquals("Mysuru", runBlocking { repo.plannerBooks(PlannerList.TRANS).first() }.single().place)
    }
}
