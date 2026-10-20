package org.tribhasa.sodhamitra.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // ---- projects
    @Query("SELECT * FROM projects WHERE kind = :kind ORDER BY id")
    fun projects(kind: String): Flow<List<Project>>
    @Query("SELECT * FROM projects WHERE id = :id")
    fun project(id: Long): Flow<Project?>
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun projectOnce(id: Long): Project?
    @Insert suspend fun insert(p: Project): Long
    @Update suspend fun update(p: Project)
    @Query("DELETE FROM projects WHERE id = :id") suspend fun deleteProject(id: Long)

    // ---- chapters, points, descriptions
    @Query("SELECT * FROM chapters WHERE projectId = :projectId ORDER BY id")
    fun chapters(projectId: Long): Flow<List<Chapter>>
    @Query("SELECT * FROM chapters WHERE projectId = :projectId ORDER BY id")
    suspend fun chaptersOnce(projectId: Long): List<Chapter>
    @Query("SELECT * FROM chapters WHERE id = :id")
    fun chapter(id: Long): Flow<Chapter?>
    @Insert suspend fun insert(c: Chapter): Long
    @Update suspend fun update(c: Chapter)
    @Query("DELETE FROM chapters WHERE id = :id") suspend fun deleteChapter(id: Long)

    @Query("SELECT * FROM points WHERE chapterId = :chapterId ORDER BY id")
    fun points(chapterId: Long): Flow<List<Point>>
    @Query("SELECT * FROM points WHERE chapterId = :chapterId ORDER BY id")
    suspend fun pointsOnce(chapterId: Long): List<Point>
    @Query("SELECT * FROM points WHERE id = :id")
    suspend fun pointOnce(id: Long): Point?
    @Insert suspend fun insert(p: Point): Long
    @Update suspend fun update(p: Point)
    @Query("DELETE FROM points WHERE id = :id") suspend fun deletePoint(id: Long)
    @Query("SELECT text FROM point_descriptions WHERE pointId = :pointId")
    fun pointDescription(pointId: Long): Flow<String?>
    @Query("SELECT text FROM point_descriptions WHERE pointId = :pointId")
    suspend fun pointDescriptionOnce(pointId: Long): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(d: PointDescription)

    // ---- notes
    @Query("SELECT * FROM notes WHERE projectId = :projectId ORDER BY id")
    fun notes(projectId: Long): Flow<List<Note>>
    @Query("SELECT * FROM notes WHERE projectId = :projectId ORDER BY id")
    suspend fun notesOnce(projectId: Long): List<Note>
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun noteOnce(id: Long): Note?
    @Insert suspend fun insert(n: Note): Long
    @Update suspend fun update(n: Note)
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: Long)
    @Query("SELECT text FROM note_descriptions WHERE noteId = :noteId")
    fun noteDescription(noteId: Long): Flow<String?>
    @Query("SELECT text FROM note_descriptions WHERE noteId = :noteId")
    suspend fun noteDescriptionOnce(noteId: Long): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(d: NoteDescription)

    // ---- bibliography books / articles
    @Query("SELECT * FROM bib_books WHERE projectId = :projectId ORDER BY id")
    fun bibBooks(projectId: Long): Flow<List<BibBook>>
    @Query("SELECT * FROM bib_books WHERE projectId = :projectId ORDER BY id")
    suspend fun bibBooksOnce(projectId: Long): List<BibBook>
    @Query("SELECT * FROM bib_books WHERE id = :id")
    suspend fun bibBookOnce(id: Long): BibBook?
    @Insert suspend fun insert(b: BibBook): Long
    @Update suspend fun update(b: BibBook)
    @Query("DELETE FROM bib_books WHERE id = :id") suspend fun deleteBibBook(id: Long)

    @Query("SELECT * FROM bib_articles WHERE projectId = :projectId ORDER BY id")
    fun bibArticles(projectId: Long): Flow<List<BibArticle>>
    @Query("SELECT * FROM bib_articles WHERE projectId = :projectId ORDER BY id")
    suspend fun bibArticlesOnce(projectId: Long): List<BibArticle>
    @Query("SELECT * FROM bib_articles WHERE id = :id")
    suspend fun bibArticleOnce(id: Long): BibArticle?
    @Insert suspend fun insert(a: BibArticle): Long
    @Update suspend fun update(a: BibArticle)
    @Query("DELETE FROM bib_articles WHERE id = :id") suspend fun deleteBibArticle(id: Long)

    // ---- manuscripts (shared record)
    @Query("SELECT * FROM manuscripts WHERE scope = :scope AND projectId = :projectId ORDER BY id")
    fun manuscripts(scope: String, projectId: Long): Flow<List<Manuscript>>
    @Query("SELECT * FROM manuscripts WHERE scope = :scope AND projectId = :projectId ORDER BY id")
    suspend fun manuscriptsOnce(scope: String, projectId: Long): List<Manuscript>
    @Query("SELECT * FROM manuscripts WHERE scope = 'EDIT' ORDER BY id")
    fun editingManuscripts(): Flow<List<Manuscript>>
    @Query("SELECT * FROM manuscripts WHERE scope = 'EDIT' ORDER BY id")
    suspend fun editingManuscriptsOnce(): List<Manuscript>
    @Query("SELECT * FROM manuscripts WHERE id = :id")
    suspend fun manuscriptOnce(id: Long): Manuscript?
    @Insert suspend fun insert(m: Manuscript): Long
    @Update suspend fun update(m: Manuscript)
    @Query("DELETE FROM manuscripts WHERE id = :id") suspend fun deleteManuscript(id: Long)

    // ---- citations
    @Query("SELECT * FROM citations WHERE projectId = :projectId ORDER BY id")
    fun citations(projectId: Long): Flow<List<Citation>>
    @Query("SELECT * FROM citations WHERE projectId = :projectId ORDER BY id")
    suspend fun citationsOnce(projectId: Long): List<Citation>
    @Query("SELECT * FROM citations WHERE id = :id")
    suspend fun citationOnce(id: Long): Citation?
    @Query("SELECT COUNT(*) FROM citations WHERE sourceType = :type AND sourceId = :sourceId")
    suspend fun citationCount(type: String, sourceId: Long): Int
    @Insert suspend fun insert(c: Citation): Long
    @Update suspend fun update(c: Citation)
    @Delete suspend fun delete(c: Citation)

    // ---- planner books
    @Query("SELECT * FROM planner_books WHERE listType = :list ORDER BY id")
    fun plannerBooks(list: String): Flow<List<PlannerBook>>
    @Query("SELECT * FROM planner_books WHERE listType = :list ORDER BY id")
    suspend fun plannerBooksOnce(list: String): List<PlannerBook>
    @Query("SELECT * FROM planner_books WHERE id = :id")
    suspend fun plannerBookOnce(id: Long): PlannerBook?
    @Insert suspend fun insert(b: PlannerBook): Long
    @Update suspend fun update(b: PlannerBook)
    @Query("DELETE FROM planner_books WHERE id = :id") suspend fun deletePlannerBook(id: Long)

    // ---- profile
    @Query("SELECT * FROM profile WHERE id = 1")
    fun profile(): Flow<Profile?>
    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun profileOnce(): Profile?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(p: Profile)

    @Transaction
    suspend fun savePoint(point: Point, text: String): Long {
        val id = if (point.id == 0L) insert(point) else { update(point); point.id }
        put(PointDescription(id, text))
        return id
    }

    @Transaction
    suspend fun saveNote(note: Note, text: String): Long {
        val id = if (note.id == 0L) insert(note) else { update(note); note.id }
        put(NoteDescription(id, text))
        return id
    }
}
