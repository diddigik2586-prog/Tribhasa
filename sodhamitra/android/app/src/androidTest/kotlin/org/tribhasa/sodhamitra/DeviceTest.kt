package org.tribhasa.sodhamitra

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.tribhasa.sodhamitra.data.AppDatabase
import org.tribhasa.sodhamitra.data.BibArticle
import org.tribhasa.sodhamitra.data.BibBook
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerBook
import org.tribhasa.sodhamitra.data.PlannerList
import org.tribhasa.sodhamitra.data.Profile
import org.tribhasa.sodhamitra.data.ProjectKind
import org.tribhasa.sodhamitra.data.Repository
import org.tribhasa.sodhamitra.pdf.ExportSection
import org.tribhasa.sodhamitra.pdf.PdfExporter
import org.tribhasa.sodhamitra.ui.ExportTarget

/**
 * Runs on a real Android device / emulator against the real app: the real SQLite, the real PdfDocument, the real fonts.
 */
@RunWith(AndroidJUnit4::class)
class DeviceTest {
    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val outDir: File get() = File(ctx.getExternalFilesDir(null), "test-pdfs").apply { mkdirs() }

    private fun freshRepo(name: String): Repository {
        ctx.deleteDatabase(name)
        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, name).build()
        return Repository(db, File(ctx.filesDir, "profile-$name").apply { mkdirs() })
    }

    @Test fun descriptions_far_beyond_the_two_megabyte_row_limit_are_stored_and_read_back() {
        val repo = freshRepo("device-large.db")
        runBlocking {
            val p = repo.saveProject(ProjectKind.PHD, null, "Large")
            repo.saveChapter(p, null, "C"); val c = repo.dao.chaptersOnce(p).single()
            val ascii = "x".repeat(4_000_000)                          // 4 MB
            val kannada = "ಕನ್ನಡ ಸಂಸ್ಕೃತ\n".repeat(300_000)               // ~ 10 MB of UTF-8 text
            val a = repo.savePoint(c.id, null, "ascii", ascii); val k = repo.savePoint(c.id, null, "kannada", kannada)
            assertEquals(ascii, repo.pointDescriptionOnce(a))
            assertEquals(kannada, repo.pointDescriptionOnce(k))
            assertEquals(kannada, repo.pointDescription(k).first())
        }
        repo.db.close()
    }

    @Test fun real_pdfs_are_written_in_portrait_and_landscape() {
        val repo = freshRepo("device-pdf.db")
        val exporter = PdfExporter(ctx, repo)
        runBlocking {
            repo.saveProfile(Profile(prefix = "Dr.", name = "Test Scholar", designation = "Research Assistant", institution = "Oriental Research Institute", place = "Mysuru"))
            val p = repo.saveProject(ProjectKind.PHD, null, "Śaṅkara’s Vivekacūḍāmaṇi — ಸಂಶೋಧನೆ")
            for (c in 1..5) {
                repo.saveChapter(p, null, "Chapter $c ಅಧ್ಯಾಯ"); val ch = repo.dao.chaptersOnce(p).last()
                for (pt in 1..4) repo.savePoint(ch.id, null, "Point $c.$pt", (1..70).joinToString("\n") { "Line $it ${c}.$pt — Rāmāyaṇa ṭ ḍ ṇ ṣ ṃ ḥ ṛ, संस्कृतम्, ಕನ್ನಡ, తెలుగు." })
            }
            repo.saveBibBook(BibBook(projectId = p, title = "Book A", author = "Auth", publisher = "Pub", place = "Pune", year = "1999"))
            repo.saveBibArticle(BibArticle(projectId = p, title = "Art A", issn = "1234-5678", scopusIndexed = true, scopusDetails = "Q2"))
            for (i in 1..10) repo.saveManuscript(Manuscript(scope = MsScope.BIB, projectId = p, title = "Ms $i", stakeholder = "ORI", place = "Mysuru", remarks = "Remark. ".repeat(80)))
            val phd = exporter.build(ExportTarget.Phd(p), ExportTarget.Phd(p).let { setOf(ExportSection.THESIS_OUTLINE, ExportSection.BOOKS, ExportSection.MANUSCRIPTS, ExportSection.ARTICLES) })!!
            exporter.write(phd, "phd").copyTo(File(outDir, "phd-portrait.pdf"), overwrite = true)

            for (i in 1..40) repo.saveManuscript(Manuscript(scope = MsScope.EDIT, title = "Manuscript title $i, rather long", serialNo = "S$i", accessionNo = "ACC/$i", stakeholder = "ORI", place = "Mysuru",
                edited = "NOT_EDITED", author = "Author $i", language = "Sanskrit", script = "Kannada", material = "PALM_LEAF", folioCount = "$i", completeness = "Complete, good. ".repeat(3),
                catalogueRef = "Cat $i", remarks = "Remarks $i ".repeat(5), published = i % 2 == 0, unpublished = i % 2 == 1, toBeTranslated = i % 3 == 0))
            for (i in 1..25) repo.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = "Book $i", author = "Author", language = "Sanskrit", stakeholder = "Library", place = "Mysuru",
                eLibrary = "https://example.org/$i", translationAvailable = i % 2 == 0, translationLanguages = "Kannada"))
            val ed = exporter.build(ExportTarget.EditingList, setOf(ExportSection.EDIT_MANUSCRIPTS, ExportSection.EDIT_BOOKS))!!
            exporter.write(ed, "editing").copyTo(File(outDir, "editing-landscape.pdf"), overwrite = true)
        }
        for (n in listOf("phd-portrait.pdf", "editing-landscape.pdf")) {
            val f = File(outDir, n); assertTrue("$n missing", f.exists() && f.length() > 5_000)
            assertEquals("%PDF-", f.readBytes().take(5).toByteArray().decodeToString())
        }
        repo.db.close()
    }

    @Test fun the_real_app_data_survives_closing_and_reopening_the_database() {
        ctx.deleteDatabase("device-persist.db")
        var db = Room.databaseBuilder(ctx, AppDatabase::class.java, "device-persist.db").build()
        runBlocking { Repository(db, File(ctx.filesDir, "x")).saveProject(ProjectKind.BOOK, null, "ಉಳಿಯುತ್ತದೆ") }
        db.close()
        db = Room.databaseBuilder(ctx, AppDatabase::class.java, "device-persist.db").build()
        assertEquals("ಉಳಿಯುತ್ತದೆ", runBlocking { Repository(db, File(ctx.filesDir, "x")).projects(ProjectKind.BOOK).first() }.single().title)
        db.close()
    }
}
