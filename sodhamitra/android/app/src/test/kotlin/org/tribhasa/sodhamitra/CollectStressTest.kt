package org.tribhasa.sodhamitra

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerBook
import org.tribhasa.sodhamitra.data.PlannerList

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class)
class CollectStressTest {
    @Test fun two_collected_flows_both_arrive() {
        val app = ApplicationProvider.getApplicationContext<Context>() as TestApp
        val repo = app.repository
        runBlocking {
            repo.saveManuscript(Manuscript(scope = MsScope.EDIT, title = "M"))
            repo.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = "B"))
        }
        val bad = ArrayList<String>()
        repeat(25) { i ->
            runAndroidComposeUiTest<ComponentActivity> {
                setContent {
                    val m by remember { repo.editingManuscripts() }.collectAsState(initial = emptyList())
                    val b by remember { repo.plannerBooks(PlannerList.EDITING) }.collectAsState(initial = emptyList())
                    Text("m=${m.size} b=${b.size}")
                }
                try { waitUntil(timeoutMillis = 6_000L) { waitForIdle(); onAllNodesWithTextExists("m=1 b=1") } } catch (e: Throwable) { bad.add("round $i") }
            }
        }
        assertEquals(bad.toString(), 0, bad.size)
    }
}
@OptIn(ExperimentalTestApi::class)
private fun androidx.compose.ui.test.AndroidComposeUiTest<*>.onAllNodesWithTextExists(t: String) =
    onAllNodes(androidx.compose.ui.test.hasText(t)).fetchSemanticsNodes().isNotEmpty()
