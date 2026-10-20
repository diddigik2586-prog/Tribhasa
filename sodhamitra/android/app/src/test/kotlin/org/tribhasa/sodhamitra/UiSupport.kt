package org.tribhasa.sodhamitra

import android.content.Context
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.tribhasa.sodhamitra.data.Repository

typealias Rule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/** Small helpers shared by the Robolectric UI tests. They drive the real screens through the real MainActivity. */
class Ui(val rule: ComposeTestRule) {
    val app: TestApp get() = ApplicationProvider.getApplicationContext<Context>() as TestApp
    val repo: Repository get() = app.repository

    fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** waits (real time) until a node with exactly this text exists; data arrives from the database on another thread */
    fun await(text: String, timeoutMs: Long = 15_000) {
        try { rule.waitUntil(timeoutMs) { rule.waitForIdle(); exists(text) } }   // waitForIdle lets results from the database reach the main thread
        catch (e: androidx.compose.ui.test.ComposeTimeoutException) { throw AssertionError("text '$text' did not appear within $timeoutMs ms", e) }
    }

    fun awaitGone(text: String, timeoutMs: Long = 15_000) {
        rule.waitUntil(timeoutMs) { rule.waitForIdle(); !exists(text) }
    }

    fun node(text: String, index: Int = 0): SemanticsNodeInteraction = rule.onAllNodesWithText(text)[index]

    fun click(text: String, index: Int = 0) { await(text); node(text, index).performClick(); rule.waitForIdle() }
    fun longPress(text: String) { await(text); node(text).performTouchInput { longClick() }; rule.waitForIdle() }

    /** the text field whose label is [label] (labels of mandatory fields end with " *") */
    fun field(label: String): SemanticsNodeInteraction =
        rule.onNode(hasSetTextAction() and (hasText(label) or hasText("$label *")))
    fun type(label: String, value: String) { field(label).performTextInput(value); rule.waitForIdle() }

    /** replaces the whole content of a field (used when editing an existing record) */
    fun replace(label: String, value: String) { field(label).performTextReplacement(value); rule.waitForIdle() }

    fun top(text: String, index: Int = 0): Float = node(text, index).fetchSemanticsNode().positionInRoot.y

    /** asserts that the given texts are on screen from top to bottom in exactly this order */
    fun assertOrder(vararg texts: String) {
        val tops = texts.map { await(it); top(it) }
        assertEquals("order of ${texts.toList()}: $tops", tops.sorted(), tops)
        assertEquals("overlap in ${texts.toList()}", tops.distinct(), tops)
    }

    fun skipSplash() { await("PhD Research") }

    /** presses Save (new record) or Save Changes (existing record) and waits until the form has closed */
    fun save() { click("Save"); awaitGone("Save") }
    fun saveChanges() { click("Save Changes"); awaitGone("Save Changes") }

    /** waits until the database satisfies [condition] (writes finish on another thread) */
    fun awaitDb(timeoutMs: Long = 15_000, condition: () -> Boolean) {
        rule.waitUntil(timeoutMs) { rule.waitForIdle(); condition() }
    }

    fun count(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().size

    fun assertShown(vararg texts: String) = texts.forEach { await(it) }
}
