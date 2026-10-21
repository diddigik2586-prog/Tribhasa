package org.tribhasa.sodhamitra

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V2 on a real Android system: the real window insets (status bar, navigation bar, cut-outs) are read from the device and the
 * Hero/Home bars, the menu, the forms and the informational pages are checked against them; screenshots are saved for review.
 * The CI script runs this once with gesture navigation and once with the three-button navigation bar.
 */
@RunWith(AndroidJUnit4::class)
class DeviceInsetsTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val prefix = InstrumentationRegistry.getArguments().getString("shotPrefix", "device")

    @Before fun clean() {
        val repo = (rule.activity.application as SodhamitraApp).repository
        runBlocking { repo.db.clearAllTables() }
    }

    private fun exists(t: String) = rule.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()
    private fun await(t: String) = rule.waitUntil(30_000) { rule.waitForIdle(); exists(t) }
    private fun click(t: String, i: Int = 0) { await(t); rule.onAllNodesWithText(t)[i].performClick(); rule.waitForIdle() }
    private fun openSettings() { rule.onNodeWithContentDescription("Open menu").performClick(); click("Settings") }

    private val decor get() = rule.activity.window.decorView
    private val windowH get() = decor.height
    private val windowW get() = decor.width

    private class Bars(val top: Int, val bottom: Int, val left: Int, val right: Int)
    private fun bars(): Bars {
        val wi = ViewCompat.getRootWindowInsets(decor)!!
        val i = wi.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        return Bars(i.top, i.bottom, i.left, i.right)
    }

    private fun SemanticsNode.top() = boundsInWindow.top
    private fun SemanticsNode.bottom() = boundsInWindow.bottom
    private fun SemanticsNode.left() = boundsInWindow.left
    private fun SemanticsNode.right() = boundsInWindow.right
    private fun textNode(t: String) = rule.onAllNodesWithText(t)[0].fetchSemanticsNode()

    private fun shot(name: String) {
        rule.waitForIdle(); Thread.sleep(500)
        val bmp: Bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dir = File(rule.activity.getExternalFilesDir(null), "test-shots").apply { mkdirs() }
        File(dir, "$prefix-$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** a real system-level input command (it goes through Android's input dispatcher and the system gesture detection) */
    private fun shell(cmd: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { String(it.readBytes()) }
    }

    /** records the touch events that the activity really receives, to see in the CI log what a real swipe looked like */
    private val touches = mutableListOf<String>()
    private fun spyOnTouches() = rule.runOnUiThread {
        val w = rule.activity.window
        val orig = w.callback
        w.callback = object : android.view.Window.Callback by orig {
            override fun dispatchTouchEvent(e: android.view.MotionEvent): Boolean {
                synchronized(touches) { touches.add("${android.view.MotionEvent.actionToString(e.actionMasked)}@${e.x.toInt()},${e.y.toInt()} source=${e.source}") }
                return orig.dispatchTouchEvent(e)
            }
        }
    }
    private fun logTouches() = synchronized(touches) {
        log("touch events received by the activity: ${touches.size}; first=${touches.take(3)} last=${touches.takeLast(3)}")
    }
    private fun menuIsOpen() = rule.onAllNodes(hasContentDescription("Close menu")).fetchSemanticsNodes().isNotEmpty()

    /** goes to logcat and to a text file that the CI script pulls together with the screenshots */
    private fun log(msg: String) {
        Log.i("InsetsTest", msg)
        val dir = File(rule.activity.getExternalFilesDir(null), "test-shots").apply { mkdirs() }
        File(dir, "$prefix-insets.log").appendText("$msg\n")
    }

    @Test fun home_bars_menu_and_cards_respect_the_system_bars() {
        await("PhD Research Plan")
        val b = bars()
        log("window ${windowW}x$windowH  insets top=${b.top} bottom=${b.bottom} left=${b.left} right=${b.right}")
        assertTrue("the status bar has a height", b.top > 0)
        // hamburger icon: below the status bar and inside the right edge
        val menu = rule.onNodeWithContentDescription("Open menu").fetchSemanticsNode()
        assertTrue("menu icon ${menu.top()} below status bar ${b.top}", menu.top() >= b.top - 1)
        assertTrue("menu icon inside the right edge", menu.right() <= windowW - b.right + 1)
        // the creator footer reaches the very bottom of the window but its text stays above the navigation bar
        val footer = rule.onNodeWithTag("creator-footer").fetchSemanticsNode()
        assertEquals("footer bar reaches the bottom of the window", windowH.toFloat(), footer.bottom(), 1.5f)
        assertEquals("footer is full width", windowW.toFloat(), footer.right() - footer.left(), 1.5f)
        val text = textNode("Created by Dr. Diddigi Vamsi Krishna")
        assertTrue("footer text bottom ${text.bottom()} above navigation bar top ${windowH - b.bottom}", text.bottom() <= windowH - b.bottom + 1)
        assertEquals("text centred horizontally", windowW / 2f, (text.left() + text.right()) / 2f, 2f)
        // cards fit between the bars and never overflow sideways
        for (c in listOf("PhD Research Plan", "Individual Researcher / Author", "Editing / Translation Planner")) {
            val n = textNode(c)
            assertTrue("$c inside left", n.left() >= b.left); assertTrue("$c inside right", n.right() <= windowW - b.right)
        }
        shot("01-home")
        rule.onNodeWithContentDescription("Open menu").performClick(); rule.waitForIdle()
        rule.waitUntil(10_000) { rule.onAllNodes(hasContentDescription("Close menu")).fetchSemanticsNodes().isNotEmpty() }
        val panel = rule.onNodeWithTag("menu-panel").fetchSemanticsNode()
        assertEquals("the menu panel reaches the right edge", windowW.toFloat(), panel.right(), 2f)
        val title = textNode("Menu")
        assertTrue("menu title below the status bar", title.top() >= b.top - 1)
        shot("02-menu")
        val last = textNode("Feedback")
        assertTrue("last menu item above the navigation bar (${last.bottom()} <= ${windowH - b.bottom})", last.bottom() <= windowH - b.bottom + 1)
    }

    @Test fun forms_and_informational_pages_clear_the_navigation_bar() {
        await("PhD Research Plan")
        val b = bars()
        // a form with the Save / Cancel bar
        click("Editing / Translation Planner"); click("Future Trans List"); click("+ Add", 0)
        await("Save")
        val save = rule.onAllNodesWithText("Save")[0].fetchSemanticsNode()
        assertTrue("Save button bottom ${save.bottom()} above nav bar ${windowH - b.bottom}", save.bottom() <= windowH - b.bottom + 1)
        shot("03-form")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        await("PhD Research Plan")
        // Profile: first-entry form, then the read-only details, then Edit with Cancel / Save
        openSettings(); click("Profile"); await("Save Changes")
        rule.onNode(hasSetTextAction() and (hasText("Name"))).performTextInput("Device tester"); rule.waitForIdle()
        shot("04-profile-form")
        rule.onAllNodesWithText("Save Changes")[0].performScrollTo().performClick(); rule.waitForIdle()
        await("Edit"); shot("05-profile-details")
        click("Edit"); await("Cancel")
        val cancel = rule.onAllNodesWithText("Cancel")[0].performScrollTo().fetchSemanticsNode()
        assertTrue("Cancel above nav bar", cancel.bottom() <= windowH - b.bottom + 1)
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()   // no edits: back to the details
        await("Edit")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()   // details -> Settings
        await("Feedback")
        // informational pages scroll to their last line, which stays above the bars
        click("About Dr. DVK"); await("©Dr. Diddigi Vamsi Krishna")
        shot("06-about-dvk")
        val dvkLast = rule.onNode(hasText("©Dr. Diddigi Vamsi Krishna")).performScrollTo().fetchSemanticsNode()
        assertTrue("About Dr. DVK last line above the nav bar", dvkLast.bottom() <= windowH - b.bottom + 1)
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        click("Support Śodhamitra"); await("Copy UPI ID"); shot("07-support")
        val safety = rule.onNode(hasText("Śodhamitra never asks", substring = true)).performScrollTo().fetchSemanticsNode()
        assertTrue("Support last line above the nav bar", safety.bottom() <= windowH - b.bottom + 1)
        val qr = rule.onNodeWithContentDescription("UPI QR").fetchSemanticsNode()
        assertEquals("QR centred", windowW / 2f, (qr.left() + qr.right()) / 2f, 3f)
        assertEquals("QR square", qr.right() - qr.left(), qr.bottom() - qr.top(), 2f)
    }

    @Test fun a_real_swipe_from_the_right_edge_opens_the_menu_on_the_homepage_only() {
        await("PhD Research Plan")
        val w = windowW; val h = windowH
        // a real swipe from the very edge of the screen, towards the left, at mid height
        spyOnTouches()
        val out = shell("input swipe ${w - 2} ${h / 2} ${w / 2} ${h / 2} 300")
        log("input swipe output: '${out.trim()}' (window ${w}x$h)")
        try { rule.waitUntil(10_000) { rule.waitForIdle(); menuIsOpen() } } catch (e: Throwable) { logTouches(); shot("08-swipe-did-not-open"); throw e }
        logTouches()
        log("the swipe opened the menu on the Homepage")
        shot("08-menu-after-real-swipe")
        // close it again (Back), go to Settings and swipe there: no menu may appear
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        rule.waitUntil(10_000) { rule.waitForIdle(); !menuIsOpen() }
        openSettings(); await("Feedback")
        shell("input swipe ${w - 2} ${h / 2} ${w / 2} ${h / 2} 300")
        Thread.sleep(1500); rule.waitForIdle()
        assertTrue("no menu on the Settings page", !menuIsOpen() && rule.onAllNodesWithTag("menu-panel").fetchSemanticsNodes().isEmpty())
        log("the swipe did not open a menu on Settings")
    }
}
