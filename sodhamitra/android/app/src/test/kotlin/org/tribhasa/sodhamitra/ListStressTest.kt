package org.tribhasa.sodhamitra

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerBook
import org.tribhasa.sodhamitra.data.PlannerList

/** Opens the Future Editing List again and again with stored rows in both tables; every table must show its rows. */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class ListStressTest {
    @get:Rule val rule = createEmptyComposeRule()

    @Test fun both_tables_always_show_their_rows() {
        val app = ApplicationProvider.getApplicationContext<Context>() as TestApp
        runBlocking {
            app.repository.saveManuscript(Manuscript(scope = MsScope.EDIT, title = "Stored manuscript"))
            app.repository.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = "Stored book"))
        }
        val ui = Ui(rule)
        val failures = ArrayList<String>()
        repeat(12) { i ->
            ActivityScenario.launch(MainActivity::class.java).use {
                ui.skipSplash()
                ui.click("Editing / Translation Planner"); ui.click("Future Editing List")
                val ok = try { ui.await("Stored book", 8_000); ui.await("Stored manuscript", 8_000); true } catch (e: AssertionError) { false }
                if (!ok) failures.add("launch $i: book shown=${ui.exists("Stored book")} manuscript shown=${ui.exists("Stored manuscript")} emptyHints=${ui.count("No records yet.")}")
            }
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }
}
