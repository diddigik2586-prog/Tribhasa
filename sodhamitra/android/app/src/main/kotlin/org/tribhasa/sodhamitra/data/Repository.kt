package org.tribhasa.sodhamitra.data

import androidx.room.withTransaction
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A citation together with the (shared) source it points to. Exactly one of book / manuscript / article is set. */
data class CitationRow(
    val citation: Citation,
    val book: BibBook? = null,
    val manuscript: Manuscript? = null,
    val article: BibArticle? = null,
)

/** All reading and writing of research data goes through here. Everything is local; nothing leaves the device. */
class Repository(val db: AppDatabase, val photoDir: File) {
    val dao: AppDao = db.dao()

    private fun now() = System.currentTimeMillis()

    // ---- projects ------------------------------------------------------------------------------------------
    fun projects(kind: String): Flow<List<Project>> = dao.projects(kind)
    fun project(id: Long): Flow<Project?> = dao.project(id)

    suspend fun saveProject(kind: String, id: Long?, title: String): Long {
        val t = title.trim()
        return if (id == null) dao.insert(Project(kind = kind, title = t, createdAt = now(), updatedAt = now()))
        else {
            val old = dao.projectOnce(id) ?: return id
            dao.update(old.copy(title = t, updatedAt = now())); id
        }
    }

    suspend fun deleteProject(id: Long) = db.withTransaction {
        // the foreign keys delete chapters, points, descriptions, notes, bibliography and citations with the project
        dao.deleteProject(id)
    }

    // ---- outline: chapters, points, descriptions --------------------------------------------------------------
    fun chapters(projectId: Long) = dao.chapters(projectId)
    fun chapter(id: Long) = dao.chapter(id)
    suspend fun saveChapter(projectId: Long, id: Long?, title: String) {
        if (id == null) dao.insert(Chapter(projectId = projectId, title = title.trim()))
        else dao.update(Chapter(id = id, projectId = projectId, title = title.trim()))
    }
    suspend fun deleteChapter(id: Long) = dao.deleteChapter(id)

    fun points(chapterId: Long) = dao.points(chapterId)
    fun pointDescription(pointId: Long) = dao.pointDescription(pointId)
    suspend fun point(id: Long) = dao.pointOnce(id)
    suspend fun pointDescriptionOnce(id: Long) = dao.pointDescriptionOnce(id).orEmpty()
    suspend fun savePoint(chapterId: Long, id: Long?, title: String, description: String): Long =
        dao.savePoint(Point(id = id ?: 0, chapterId = chapterId, title = title.trim()), description)
    suspend fun deletePoint(id: Long) = dao.deletePoint(id)

    // ---- research notes ------------------------------------------------------------------------------------
    fun notes(projectId: Long) = dao.notes(projectId)
    fun noteDescription(noteId: Long) = dao.noteDescription(noteId)
    suspend fun note(id: Long) = dao.noteOnce(id)
    suspend fun noteDescriptionOnce(id: Long) = dao.noteDescriptionOnce(id).orEmpty()
    suspend fun saveNote(projectId: Long, id: Long?, title: String, description: String): Long =
        dao.saveNote(Note(id = id ?: 0, projectId = projectId, title = title.trim()), description)
    suspend fun deleteNote(id: Long) = dao.deleteNote(id)

    // ---- PhD bibliography ----------------------------------------------------------------------------------
    fun bibBooks(projectId: Long) = dao.bibBooks(projectId)
    fun bibArticles(projectId: Long) = dao.bibArticles(projectId)
    fun manuscripts(scope: String, projectId: Long) = dao.manuscripts(scope, projectId)
    suspend fun bibBook(id: Long) = dao.bibBookOnce(id)
    suspend fun bibArticle(id: Long) = dao.bibArticleOnce(id)
    suspend fun manuscript(id: Long) = dao.manuscriptOnce(id)

    suspend fun saveBibBook(b: BibBook): Long = if (b.id == 0L) dao.insert(b) else { dao.update(b); b.id }
    suspend fun saveBibArticle(a: BibArticle): Long = if (a.id == 0L) dao.insert(a) else { dao.update(a); a.id }
    suspend fun saveManuscript(m: Manuscript): Long = if (m.id == 0L) dao.insert(m) else { dao.update(m); m.id }
    suspend fun deleteBibBook(id: Long) = dao.deleteBibBook(id)
    suspend fun deleteBibArticle(id: Long) = dao.deleteBibArticle(id)
    suspend fun deleteManuscript(id: Long) = dao.deleteManuscript(id)

    // ---- citations (sources are stored once and shared) ------------------------------------------------------
    fun citationRows(projectId: Long): Flow<List<CitationRow>> =
        combine(
            dao.citations(projectId), dao.bibBooks(projectId),
            dao.manuscripts(MsScope.CIT, projectId), dao.bibArticles(projectId),
        ) { cits, books, mss, arts ->
            val b = books.associateBy { it.id }; val m = mss.associateBy { it.id }; val a = arts.associateBy { it.id }
            cits.map { c ->
                when (c.sourceType) {
                    SourceType.BOOK -> CitationRow(c, book = b[c.sourceId])
                    SourceType.MSS -> CitationRow(c, manuscript = m[c.sourceId])
                    else -> CitationRow(c, article = a[c.sourceId])
                }
            }
        }

    suspend fun citation(id: Long) = dao.citationOnce(id)
    suspend fun citationUseCount(type: String, sourceId: Long) = dao.citationCount(type, sourceId)

    private fun norm(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")

    /**
     * Saves a citation. A source that is already stored (chosen from the list, or an identical one) is reused, so the
     * same book, manuscript or article is never stored twice. [selectedSourceId] is the source being edited / chosen.
     */
    suspend fun saveCitation(
        projectId: Long, type: String, citationId: Long?, selectedSourceId: Long?,
        book: BibBook? = null, manuscript: Manuscript? = null, article: BibArticle? = null,
        pageFolio: String, contextNote: String,
    ): Long = db.withTransaction {
        val sourceId: Long = when (type) {
            SourceType.BOOK -> {
                val nb = book!!
                if (selectedSourceId != null) { dao.update(nb.copy(id = selectedSourceId)); selectedSourceId }
                else dao.bibBooksOnce(projectId).firstOrNull {
                    norm(it.title) == norm(nb.title) && norm(it.author) == norm(nb.author) &&
                        norm(it.edition) == norm(nb.edition) && norm(it.year) == norm(nb.year)
                }?.id ?: dao.insert(nb)
            }
            SourceType.MSS -> {
                val nm = manuscript!!
                if (selectedSourceId != null) { dao.update(nm.copy(id = selectedSourceId)); selectedSourceId }
                else dao.manuscriptsOnce(MsScope.CIT, projectId).firstOrNull {
                    norm(it.title) == norm(nm.title) && norm(it.accessionNo) == norm(nm.accessionNo) &&
                        norm(it.serialNo) == norm(nm.serialNo) && norm(it.stakeholder) == norm(nm.stakeholder)
                }?.id ?: dao.insert(nm)
            }
            else -> {
                val na = article!!
                if (selectedSourceId != null) { dao.update(na.copy(id = selectedSourceId)); selectedSourceId }
                else dao.bibArticlesOnce(projectId).firstOrNull {
                    norm(it.title) == norm(na.title) && norm(it.author) == norm(na.author) && norm(it.year) == norm(na.year)
                }?.id ?: dao.insert(na)
            }
        }
        val id: Long
        if (citationId == null) {
            id = dao.insert(Citation(projectId = projectId, sourceType = type, sourceId = sourceId,
                pageFolio = pageFolio.trim(), contextNote = contextNote.trim()))
        } else {
            val old = dao.citationOnce(citationId)
            dao.update(Citation(id = citationId, projectId = projectId, sourceType = type, sourceId = sourceId,
                pageFolio = pageFolio.trim(), contextNote = contextNote.trim()))
            id = citationId
            if (old != null && old.sourceId != sourceId) deleteSourceIfUnused(type, old.sourceId)
        }
        id
    }

    private suspend fun deleteSourceIfUnused(type: String, sourceId: Long) {
        if (dao.citationCount(type, sourceId) == 0) when (type) {
            SourceType.BOOK -> dao.deleteBibBook(sourceId)
            SourceType.MSS -> dao.deleteManuscript(sourceId)
            else -> dao.deleteBibArticle(sourceId)
        }
    }

    suspend fun deleteCitation(id: Long) = db.withTransaction {
        val c = dao.citationOnce(id) ?: return@withTransaction
        dao.delete(c)
        deleteSourceIfUnused(c.sourceType, c.sourceId)
    }

    // ---- planner -------------------------------------------------------------------------------------------
    fun editingManuscripts() = dao.editingManuscripts()
    fun plannerBooks(list: String) = dao.plannerBooks(list)
    suspend fun plannerBook(id: Long) = dao.plannerBookOnce(id)
    suspend fun savePlannerBook(b: PlannerBook): Long = if (b.id == 0L) dao.insert(b) else { dao.update(b); b.id }
    suspend fun deletePlannerBook(id: Long) = dao.deletePlannerBook(id)

    // ---- profile ------------------------------------------------------------------------------------------
    fun profile(): Flow<Profile?> = dao.profile()
    suspend fun profileOnce(): Profile = dao.profileOnce() ?: Profile()
    suspend fun saveProfile(p: Profile) = dao.put(p.copy(id = 1))
}
