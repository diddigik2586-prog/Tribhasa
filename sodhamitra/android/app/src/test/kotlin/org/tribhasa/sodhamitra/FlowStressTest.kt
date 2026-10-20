package org.tribhasa.sodhamitra

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.data.AppDatabase
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerBook
import org.tribhasa.sodhamitra.data.PlannerList
import org.tribhasa.sodhamitra.data.Repository

/** Several Room flows started together must each deliver the stored rows as their first value, every time. */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class)
class FlowStressTest {
    @Test fun concurrently_started_flows_always_deliver_the_stored_rows() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        repeat(40) { round ->
            val db = Room.databaseBuilder(ctx, AppDatabase::class.java, "stress-$round-${System.nanoTime()}.db").build()
            val repo = Repository(db, File(ctx.filesDir, "p"))
            runBlocking {
                repo.saveManuscript(Manuscript(scope = MsScope.EDIT, title = "M"))
                repo.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = "B"))
                val results = withTimeout(20_000) {
                    listOf(
                        async(Dispatchers.Default) { repo.editingManuscripts().first().size },
                        async(Dispatchers.Default) { repo.plannerBooks(PlannerList.EDITING).first().size },
                        async(Dispatchers.Default) { repo.plannerBooks(PlannerList.TRANS).first().size },
                    ).awaitAll()
                }
                assertEquals("round $round: manuscripts, editing books, trans books", listOf(1, 1, 0), results)
            }
            db.close()
        }
    }
}
