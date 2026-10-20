package org.tribhasa.sodhamitra

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerBook
import org.tribhasa.sodhamitra.data.PlannerList
import org.tribhasa.sodhamitra.data.Profile
import org.tribhasa.sodhamitra.data.ProjectKind
import org.tribhasa.sodhamitra.data.Repository
import org.tribhasa.sodhamitra.pdf.BitmapSink
import org.tribhasa.sodhamitra.pdf.CountingSink
import org.tribhasa.sodhamitra.pdf.ExportSection
import org.tribhasa.sodhamitra.pdf.PdfDoc
import org.tribhasa.sodhamitra.pdf.PdfExporter
import org.tribhasa.sodhamitra.pdf.PdfFonts
import org.tribhasa.sodhamitra.pdf.PdfRenderer
import org.tribhasa.sodhamitra.ui.ExportTarget

/** Lays real data out with the real PDF engine and checks pages, orientation, flow and the reserved footer band. */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, sdk = [35])
class PdfLayoutTest {
    private lateinit var ctx: Context
    private lateinit var repo: Repository
    private lateinit var exporter: PdfExporter
    private val outDir = File("build/pdf-test").apply { mkdirs() }

    @Before fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        repo = (ctx as SodhamitraApp).repository
        exporter = PdfExporter(ctx, repo)
    }

    private fun go(body: suspend () -> Unit) { runBlocking { body() } }

    private fun render(doc: PdfDoc, name: String): List<Bitmap> {
        val fonts = PdfFonts(ctx)
        val count = CountingSink(); PdfRenderer(fonts, doc, count, 1).render()
        val sink = BitmapSink(1f); PdfRenderer(fonts, doc, sink, count.pages).render()
        assertEquals(count.pages, sink.pages.size)
        // hi-resolution copies (216 dpi) of the first pages, to look at
        val hi = BitmapSink(3f); PdfRenderer(fonts, doc, hi, count.pages).render()
        hi.pages.take(3).forEachIndexed { i, b -> File(outDir, "$name-p${i + 1}.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        return sink.pages
    }

    /** the band between the content area and the footer rule must be empty on every page */
    private fun assertNoOverflowIntoFooter(pages: List<Bitmap>, landscape: Boolean) {
        val h = if (landscape) 595 else 842
        val bandTop = h - 78 + 1; val bandBottom = h - 78 + 3   // just below the content limit (h-78); the footer rule is at h-72
        for ((i, p) in pages.withIndex()) for (y in bandTop..bandBottom) for (x in 0 until p.width)
            assertTrue("page ${i + 1}: content crosses into the footer band at ($x,$y)", Color.red(p.getPixel(x, y)) > 240 && Color.green(p.getPixel(x, y)) > 240)
    }

    private fun darkRowsIn(p: Bitmap, yFrom: Int, yTo: Int) = (yFrom until yTo).count { y -> (0 until p.width).any { x -> Color.red(p.getPixel(x, y)) < 200 } }

    @Test fun portrait_outline_flows_over_many_pages_with_footer_on_every_page() = go {
        val p = repo.saveProject(ProjectKind.PHD, null, "Śaṅkara’s Vivekacūḍāmaṇi — ಸಂಶೋಧನೆ")
        repo.saveProfile(Profile(prefix = "Dr.", name = "Test Scholar", designation = "Research Assistant", institution = "Oriental Research Institute", place = "Mysuru", sex = "MALE"))
        for (c in 1..6) {
            repo.saveChapter(p, null, "Chapter title $c ಅಧ್ಯಾಯ")
            val ch = repo.dao.chaptersOnce(p).last()
            for (pt in 1..4) repo.savePoint(ch.id, null, "Point $c.$pt", (1..60).joinToString("\n") { "Line $it of the description ${c}.$pt — Rāmāyaṇa ṭ ḍ ṇ ṣ ṃ ḥ ṛ, संस्कृतम्, ಕನ್ನಡ, తెలుగు." })
        }
        val doc = exporter.build(ExportTarget.Phd(p), setOf(ExportSection.THESIS_OUTLINE))!!
        assertTrue(!doc.landscape)
        assertEquals("Śodhamitra — Research Planner", doc.footerLine1); assertEquals("Created by © Dr. Diddigi Vamsi Krishna", doc.footerLine2)
        val pages = render(doc, "outline")
        assertTrue("expected many pages, got ${pages.size}", pages.size >= 8)
        pages.forEach { assertEquals(595, it.width); assertEquals(842, it.height) }
        assertNoOverflowIntoFooter(pages, false)
        // the footer (rule + 3 lines) is drawn on every page
        for ((i, pg) in pages.withIndex()) assertTrue("footer text missing on page ${i + 1}", darkRowsIn(pg, 842 - 70, 842 - 8) >= 15)
        // the profile header is on the first page only
        assertTrue(darkRowsIn(pages[0], 40, 100) > 10)
    }

    @Test fun nothing_selected_has_content_means_no_document() = go {
        val p = repo.saveProject(ProjectKind.PHD, null, "Empty")
        assertNull(exporter.build(ExportTarget.Phd(p), setOf(ExportSection.THESIS_OUTLINE, ExportSection.BOOKS, ExportSection.MANUSCRIPTS, ExportSection.ARTICLES)))
        repo.saveChapter(p, null, "C")
        assertNotNull(exporter.build(ExportTarget.Phd(p), setOf(ExportSection.THESIS_OUTLINE)))
        assertNull(exporter.build(ExportTarget.Phd(p), setOf(ExportSection.BOOKS)))   // an empty section is skipped
    }

    @Test fun landscape_planner_tables_flow_across_pages_without_touching_the_footer() = go {
        for (i in 1..45) repo.saveManuscript(Manuscript(scope = MsScope.EDIT, title = "Manuscript title number $i with a rather long descriptive title", serialNo = "S$i", accessionNo = "ACC/$i",
            stakeholder = "Oriental Research Institute", place = "Mysuru", edited = "NOT_EDITED", author = "Attributed author $i", language = "Sanskrit", script = "Kannada", material = "PALM_LEAF",
            folioCount = "${100 + i}", completeness = "Complete; condition good, some worm damage on the last folios. ".repeat(2), catalogueRef = "Cat. vol. $i p. 12", remarks = "Remarks for item $i ".repeat(4),
            published = i % 2 == 0, unpublished = i % 2 == 1, toBeTranslated = i % 3 == 0))
        for (i in 1..30) repo.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = "Book $i", author = "Author $i", editor = "Editor", edition = "2", language = "Sanskrit",
            publisher = "Pub", year = "19${40 + i}", toBeTranslated = i % 2 == 0, stakeholder = "Library", place = "Mysuru", accessionNo = "A$i", eLibrary = "https://example.org/book/$i",
            translationAvailable = i % 3 == 0, translationLanguages = if (i % 3 == 0) "Kannada, English" else ""))
        val doc = exporter.build(ExportTarget.EditingList, setOf(ExportSection.EDIT_MANUSCRIPTS, ExportSection.EDIT_BOOKS))!!
        assertTrue(doc.landscape)
        val pages = render(doc, "editing-landscape")
        assertTrue("expected a multi-page table, got ${pages.size} page(s)", pages.size >= 4)
        pages.forEach { assertEquals(842, it.width); assertEquals(595, it.height) }
        assertNoOverflowIntoFooter(pages, true)
        for ((i, pg) in pages.withIndex()) assertTrue("footer text missing on page ${i + 1}", darkRowsIn(pg, 595 - 70, 595 - 8) >= 15)
    }

    @Test fun trans_list_exports_only_books_in_landscape() = go {
        repo.savePlannerBook(PlannerBook(listType = PlannerList.TRANS, title = "T1", stakeholder = "S", place = "P"))
        repo.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = "E1"))
        val doc = exporter.build(ExportTarget.TransList, setOf(ExportSection.TRANS_BOOKS))!!
        assertTrue(doc.landscape)
        val grid = doc.blocks.filterIsInstance<org.tribhasa.sodhamitra.pdf.Grid>().single()
        assertEquals(1, grid.rows.size); assertEquals("T1", grid.rows[0][1]); assertEquals(15, grid.headers.size)
        render(doc, "trans-landscape")
    }

    @Test fun portrait_bibliography_manuscripts_are_record_tables_and_books_articles_are_grids() = go {
        val p = repo.saveProject(ProjectKind.PHD, null, "Bibliography project")
        repo.saveBibBook(org.tribhasa.sodhamitra.data.BibBook(projectId = p, title = "Book A", author = "Auth", publisher = "Pub", place = "Pune", year = "1999"))
        repo.saveBibArticle(org.tribhasa.sodhamitra.data.BibArticle(projectId = p, title = "Art A", issn = "1234-5678", scopusIndexed = true, scopusDetails = "Q2", impactFactor = "0.8"))
        for (i in 1..12) repo.saveManuscript(Manuscript(scope = MsScope.BIB, projectId = p, title = "Ms $i", serialNo = "$i", stakeholder = "ORI", place = "Mysuru", script = "Grantha",
            remarks = "Very long remark. ".repeat(60)))
        val doc = exporter.build(ExportTarget.Phd(p), setOf(ExportSection.BOOKS, ExportSection.MANUSCRIPTS, ExportSection.ARTICLES))!!
        assertTrue(doc.blocks.any { it is org.tribhasa.sodhamitra.pdf.RecordTables })
        assertEquals(2, doc.blocks.count { it is org.tribhasa.sodhamitra.pdf.Grid })
        val pages = render(doc, "bibliography")
        assertTrue(pages.size >= 3); assertNoOverflowIntoFooter(pages, false)
    }

    @Test fun a_single_table_row_taller_than_a_page_is_split_across_pages() = go {
        repo.saveManuscript(Manuscript(scope = MsScope.EDIT, title = "Giant", remarks = (1..400).joinToString("\n") { "remark line $it" }))
        val doc = exporter.build(ExportTarget.EditingList, setOf(ExportSection.EDIT_MANUSCRIPTS))!!
        val pages = render(doc, "giant-row")
        assertTrue("a 400-line cell must continue over several pages, got ${pages.size}", pages.size >= 3)
        assertNoOverflowIntoFooter(pages, true)
    }

    @Test fun profile_photograph_appears_in_the_first_page_header_and_an_empty_profile_still_works() = go {
        val p = repo.saveProject(ProjectKind.PHD, null, "With photo")
        repo.saveChapter(p, null, "C1")
        // no profile at all: the PDF is still produced, with no header block
        val without = exporter.build(ExportTarget.Phd(p), setOf(ExportSection.THESIS_OUTLINE))!!
        assertNull(without.profile)
        render(without, "no-profile")
        // a profile with a photograph
        val photo = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).also { b -> android.graphics.Canvas(b).drawColor(Color.rgb(200, 30, 30)) }
        repo.photoDir.mkdirs()
        File(repo.photoDir, "profile_photo_test.jpg").outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        repo.saveProfile(Profile(prefix = "Prof.", name = "Photo Person", designation = "Professor", photoFile = "profile_photo_test.jpg"))
        val with = exporter.build(ExportTarget.Phd(p), setOf(ExportSection.THESIS_OUTLINE))!!
        assertNotNull(with.profile!!.photo)
        val pages = render(with, "with-photo")
        // the red photograph is drawn at the top-left of page 1 (62 x 62 pt at the page margin)
        val px = pages[0].getPixel(40 + 30, 40 + 30)
        assertTrue("photo pixel should be red, was ${Integer.toHexString(px)}", Color.red(px) > 150 && Color.green(px) < 90)
    }
}
