package org.tribhasa.sodhamitra

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
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
import org.tribhasa.sodhamitra.data.SourceType

@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db, File(ctx.filesDir, "profile"))
    }
    @After fun tearDown() = db.close()

    @Test fun outline_chapters_points_and_descriptions_are_linked_but_independent() = runBlocking {
        val p = repo.saveProject(ProjectKind.PHD, null, "  Rāmāyaṇa studies  ")
        assertEquals("Rāmāyaṇa studies", repo.projects(ProjectKind.PHD).first().single().title)
        repo.saveChapter(p, null, "Introduction"); repo.saveChapter(p, null, "Methods")
        val ch = repo.chapters(p).first(); assertEquals(listOf("Introduction", "Methods"), ch.map { it.title })
        val a = repo.savePoint(ch[0].id, null, "Aim", "Line 1\nLine 2")
        val b = repo.savePoint(ch[0].id, null, "Scope", "")
        assertEquals("Line 1\nLine 2", repo.pointDescriptionOnce(a))
        assertEquals("", repo.pointDescriptionOnce(b))
        // editing updates the same record, it never duplicates
        repo.savePoint(ch[0].id, a, "Aim (revised)", "changed")
        assertEquals(listOf("Aim (revised)", "Scope"), repo.points(ch[0].id).first().map { it.title })
        assertEquals("changed", repo.pointDescriptionOnce(a)); assertEquals("", repo.pointDescriptionOnce(b))
        // deleting one point removes only that point and its own description
        repo.deletePoint(a)
        assertEquals(listOf("Scope"), repo.points(ch[0].id).first().map { it.title })
        // deleting a chapter removes its points; the other chapter is untouched
        repo.deleteChapter(ch[0].id)
        assertEquals(listOf("Methods"), repo.chapters(p).first().map { it.title })
        assertNull(repo.point(b))
    }

    @Test fun descriptions_of_any_length_are_stored_and_read_back_unchanged() = runBlocking {
        val p = repo.saveProject(ProjectKind.BOOK, null, "Big book")
        val n = repo.saveNote(p, null, "Long note", "x".repeat(1_500_000) + "\nಕನ್ನಡ ಸಂಸ್ಕೃತ")
        val back = repo.noteDescriptionOnce(n)
        assertEquals("x".repeat(1_500_000) + "\nಕನ್ನಡ ಸಂಸ್ಕೃತ", back)   // no limit is imposed by the app
    }

    @Test fun deleting_a_project_removes_everything_inside_it() = runBlocking {
        val p = repo.saveProject(ProjectKind.PHD, null, "T")
        repo.saveChapter(p, null, "C"); val c = repo.chapters(p).first().single()
        val pt = repo.savePoint(c.id, null, "P", "D")
        repo.saveBibBook(BibBook(projectId = p, title = "B")); repo.saveBibArticle(BibArticle(projectId = p, title = "A", issn = "1234-5678"))
        repo.saveManuscript(Manuscript(scope = MsScope.BIB, projectId = p, title = "M"))
        val other = repo.saveProject(ProjectKind.PHD, null, "Other")
        repo.saveBibBook(BibBook(projectId = other, title = "Keep"))
        repo.deleteProject(p)
        assertEquals(listOf("Other"), repo.projects(ProjectKind.PHD).first().map { it.title })
        assertTrue(repo.chapters(p).first().isEmpty()); assertNull(repo.point(pt)); assertEquals("", repo.pointDescriptionOnce(pt))
        assertTrue(repo.bibBooks(p).first().isEmpty()); assertTrue(repo.bibArticles(p).first().isEmpty())
        assertTrue(repo.manuscripts(MsScope.BIB, p).first().isEmpty())
        assertEquals(listOf("Keep"), repo.bibBooks(other).first().map { it.title })
    }

    @Test fun a_source_is_stored_once_however_often_it_is_cited() = runBlocking {
        val p = repo.saveProject(ProjectKind.BOOK, null, "Book")
        val c1 = repo.saveCitation(p, SourceType.BOOK, null, null, book = BibBook(projectId = p, title = "Mahābhārata", author = "Vyāsa"), pageFolio = "12", contextNote = "first")
        // the same book typed again (different case / spacing) reuses the stored source
        val c2 = repo.saveCitation(p, SourceType.BOOK, null, null, book = BibBook(projectId = p, title = "  mahābhārata ", author = "vyāsa"), pageFolio = "99", contextNote = "second")
        assertEquals(1, repo.bibBooks(p).first().size)
        val rows = repo.citationRows(p).first()
        assertEquals(2, rows.size); assertEquals(rows[0].book!!.id, rows[1].book!!.id)
        assertEquals(setOf("12", "99"), rows.map { it.citation.pageFolio }.toSet())
        assertEquals(2, repo.citationUseCount(SourceType.BOOK, rows[0].book!!.id))
        // deleting one citation keeps the shared source; deleting the last one removes it
        repo.deleteCitation(c1); assertEquals(1, repo.bibBooks(p).first().size)
        repo.deleteCitation(c2); assertEquals(0, repo.bibBooks(p).first().size)
    }

    @Test fun manuscript_and_article_citations_carry_page_or_folio_and_note() = runBlocking {
        val p = repo.saveProject(ProjectKind.BOOK, null, "Book")
        repo.saveCitation(p, SourceType.MSS, null, null, manuscript = Manuscript(scope = MsScope.CIT, projectId = p, title = "Ms A", accessionNo = "123"), pageFolio = "f. 12v", contextNote = "colophon")
        repo.saveCitation(p, SourceType.ARTICLE, null, null, article = BibArticle(projectId = p, title = "Art", issn = "1111-2222"), pageFolio = "pp. 3-9", contextNote = "")
        val rows = repo.citationRows(p).first()
        assertEquals("f. 12v", rows.first { it.manuscript != null }.citation.pageFolio)
        assertEquals("colophon", rows.first { it.manuscript != null }.citation.contextNote)
        assertEquals("1111-2222", rows.first { it.article != null }.article!!.issn)
        // editing a citation updates it in place
        val cid = rows.first { it.manuscript != null }.citation.id; val sid = rows.first { it.manuscript != null }.manuscript!!.id
        repo.saveCitation(p, SourceType.MSS, cid, sid, manuscript = Manuscript(scope = MsScope.CIT, projectId = p, title = "Ms A (corrected)", accessionNo = "123"), pageFolio = "f. 13r", contextNote = "x")
        val after = repo.citationRows(p).first()
        assertEquals(2, after.size); assertEquals("Ms A (corrected)", after.first { it.manuscript != null }.manuscript!!.title)
    }

    @Test fun planner_lists_are_separate_and_edits_never_duplicate() = runBlocking {
        val e = repo.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = "Book E"))
        repo.savePlannerBook(PlannerBook(listType = PlannerList.TRANS, title = "Book T"))
        assertEquals(listOf("Book E"), repo.plannerBooks(PlannerList.EDITING).first().map { it.title })
        assertEquals(listOf("Book T"), repo.plannerBooks(PlannerList.TRANS).first().map { it.title })
        repo.savePlannerBook(repo.plannerBook(e)!!.copy(title = "Book E2", stakeholder = "ORI", place = "Mysuru"))
        val list = repo.plannerBooks(PlannerList.EDITING).first(); assertEquals(1, list.size); assertEquals("Book E2", list[0].title)
        val m = repo.saveManuscript(Manuscript(scope = MsScope.EDIT, title = "Ms", published = true, toBeTranslated = true))
        val got = repo.editingManuscripts().first().single()
        assertTrue(got.published); assertTrue(got.toBeTranslated); assertTrue(!got.unpublished)
        repo.deleteManuscript(m); assertTrue(repo.editingManuscripts().first().isEmpty())
        repo.deletePlannerBook(e); assertTrue(repo.plannerBooks(PlannerList.EDITING).first().isEmpty())
    }

    @Test fun profile_is_optional_and_round_trips() = runBlocking {
        assertTrue(repo.profileOnce().isEmpty)
        repo.saveProfile(Profile(prefix = "Dr.", name = "Name", sex = "MALE", place = "Mysuru"))
        val p = repo.profileOnce(); assertEquals("Dr.", p.prefix); assertEquals("Mysuru", p.place); assertTrue(!p.isEmpty)
        assertNotNull(repo.profile().first())
    }

    @Test fun data_survives_closing_and_reopening_the_database_file() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.deleteDatabase(AppDatabase.FILE_NAME)
        val first = AppDatabase.open(ctx)
        val r1 = Repository(first, File(ctx.filesDir, "profile"))
        val p = r1.saveProject(ProjectKind.PHD, null, "Persistent")
        r1.savePlannerBook(PlannerBook(listType = PlannerList.TRANS, title = "Kept"))
        first.close()   // as if the app were closed and the phone restarted
        val second = AppDatabase.open(ctx)
        val r2 = Repository(second, File(ctx.filesDir, "profile"))
        assertEquals("Persistent", r2.projects(ProjectKind.PHD).first().single().title)
        assertEquals("Kept", r2.plannerBooks(PlannerList.TRANS).first().single().title)
        assertEquals(p, r2.projects(ProjectKind.PHD).first().single().id)
        second.close()
    }
}
