package org.tribhasa.sodhamitra.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import androidx.annotation.StringRes
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.BibArticle
import org.tribhasa.sodhamitra.data.BibBook
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerList
import org.tribhasa.sodhamitra.data.ProjectKind
import org.tribhasa.sodhamitra.data.Repository
import org.tribhasa.sodhamitra.domain.Display
import org.tribhasa.sodhamitra.ui.ExportTarget
import org.tribhasa.sodhamitra.util.PdfFiles
import org.tribhasa.sodhamitra.util.ProfilePhotos

/** The parts of a project that can be ticked for export. */
enum class ExportSection(@StringRes val label: Int) {
    THESIS_OUTLINE(R.string.thesis_outline), BOOKS(R.string.books), MANUSCRIPTS(R.string.manuscripts_export), ARTICLES(R.string.articles),
    BOOK_OUTLINE(R.string.book_outline), RESEARCH_NOTES(R.string.research_notes), CITATIONS(R.string.references_citations),
    EDIT_MANUSCRIPTS(R.string.heading_manuscript), EDIT_BOOKS(R.string.heading_book), TRANS_BOOKS(R.string.heading_book),
}

fun ExportTarget.sections(): List<ExportSection> = when (this) {
    is ExportTarget.Phd -> listOf(ExportSection.THESIS_OUTLINE, ExportSection.BOOKS, ExportSection.MANUSCRIPTS, ExportSection.ARTICLES)
    is ExportTarget.Book -> listOf(ExportSection.BOOK_OUTLINE, ExportSection.RESEARCH_NOTES, ExportSection.CITATIONS)
    ExportTarget.EditingList -> listOf(ExportSection.EDIT_MANUSCRIPTS, ExportSection.EDIT_BOOKS)
    ExportTarget.TransList -> listOf(ExportSection.TRANS_BOOKS)
}

/** PhD Research and Individual Researcher / Author: A4 portrait. Editing / Translation Planner: A4 landscape. */
fun ExportTarget.landscape(): Boolean = this == ExportTarget.EditingList || this == ExportTarget.TransList

/** Builds the content of a PDF from the stored data and writes it. Everything happens on the device; no network is used. */
class PdfExporter(private val context: Context, private val repo: Repository) {
    private val d = Display(context)
    private fun s(id: Int) = d.s(id)

    /** null when every selected section is empty: no empty PDF is ever produced */
    suspend fun build(target: ExportTarget, selected: Set<ExportSection>): PdfDoc? {
        val blocks = ArrayList<Block>()
        val title: String
        val subtitle: String
        when (target) {
            is ExportTarget.Phd -> {
                val p = repo.dao.projectOnce(target.projectId) ?: return null
                title = p.title; subtitle = s(R.string.home_phd)
                if (ExportSection.THESIS_OUTLINE in selected) outline(blocks, p.id, s(R.string.thesis_outline))
                bibliography(blocks, p.id, selected)
            }
            is ExportTarget.Book -> {
                val p = repo.dao.projectOnce(target.projectId) ?: return null
                title = p.title; subtitle = s(R.string.home_individual)
                if (ExportSection.BOOK_OUTLINE in selected) outline(blocks, p.id, s(R.string.book_outline))
                if (ExportSection.RESEARCH_NOTES in selected) notes(blocks, p.id)
                if (ExportSection.CITATIONS in selected) citations(blocks, p.id)
            }
            ExportTarget.EditingList -> {
                title = s(R.string.future_editing_list); subtitle = s(R.string.home_planner)
                val ms = if (ExportSection.EDIT_MANUSCRIPTS in selected) repo.dao.editingManuscriptsOnce() else emptyList()
                val bk = if (ExportSection.EDIT_BOOKS in selected) repo.dao.plannerBooksOnce(PlannerList.EDITING) else emptyList()
                if (ms.isNotEmpty() || bk.isNotEmpty()) blocks.add(H1(s(R.string.future_editing_list)))
                if (ms.isNotEmpty()) { blocks.add(H2(s(R.string.heading_manuscript))); blocks.add(editingManuscriptGrid(ms)) }
                if (bk.isNotEmpty()) { blocks.add(H2(s(R.string.heading_book))); blocks.add(plannerBookGrid(bk.map { it })) }
            }
            ExportTarget.TransList -> {
                title = s(R.string.future_trans_list); subtitle = s(R.string.home_planner)
                val bk = if (ExportSection.TRANS_BOOKS in selected) repo.dao.plannerBooksOnce(PlannerList.TRANS) else emptyList()
                if (bk.isNotEmpty()) { blocks.add(H1(s(R.string.future_trans_list))); blocks.add(H2(s(R.string.heading_book))); blocks.add(plannerBookGrid(bk)) }
            }
        }
        if (blocks.isEmpty()) return null
        return PdfDoc(
            title = title, subtitle = subtitle, landscape = target.landscape(), blocks = blocks, profile = profileHeader(),
            pageOfFormat = s(R.string.pdf_page_of), footerLine1 = s(R.string.pdf_footer_line1), footerLine2 = s(R.string.pdf_footer_line2),
        )
    }

    // ---- content ------------------------------------------------------------------------------------------------
    private suspend fun outline(out: MutableList<Block>, projectId: Long, heading: String) {
        val chapters = repo.dao.chaptersOnce(projectId)
        if (chapters.isEmpty()) return
        out.add(H1(heading))
        chapters.forEachIndexed { i, c ->
            out.add(H2(String.format(s(R.string.pdf_chapter), i + 1) + ": " + c.title))
            repo.dao.pointsOnce(c.id).forEachIndexed { j, p ->
                out.add(H3("${i + 1}.${j + 1}  ${p.title}"))
                repo.dao.pointDescriptionOnce(p.id)?.takeIf { it.isNotBlank() }?.let { out.add(Para(it)) }
            }
        }
    }

    private suspend fun notes(out: MutableList<Block>, projectId: Long) {
        val notes = repo.dao.notesOnce(projectId)
        if (notes.isEmpty()) return
        out.add(H1(s(R.string.research_notes)))
        notes.forEachIndexed { i, n ->
            out.add(H3("${i + 1}.  ${n.title}"))
            repo.dao.noteDescriptionOnce(n.id)?.takeIf { it.isNotBlank() }?.let { out.add(Para(it)) }
        }
    }

    private suspend fun bibliography(out: MutableList<Block>, projectId: Long, selected: Set<ExportSection>) {
        val books = if (ExportSection.BOOKS in selected) repo.dao.bibBooksOnce(projectId) else emptyList()
        val mss = if (ExportSection.MANUSCRIPTS in selected) repo.dao.manuscriptsOnce(MsScope.BIB, projectId) else emptyList()
        val arts = if (ExportSection.ARTICLES in selected) repo.dao.bibArticlesOnce(projectId) else emptyList()
        if (books.isEmpty() && mss.isEmpty() && arts.isEmpty()) return
        out.add(H1(s(R.string.bibliography)))
        if (books.isNotEmpty()) { out.add(H2(s(R.string.books))); out.add(bookGrid(books, emptyList())) }
        if (mss.isNotEmpty()) { out.add(H2(s(R.string.mss_manuscripts))); out.add(manuscriptRecords(mss.map { it to emptyList<Pair<String, String>>() })) }
        if (arts.isNotEmpty()) { out.add(H2(s(R.string.articles))); out.add(articleGrid(arts, emptyList())) }
    }

    private suspend fun citations(out: MutableList<Block>, projectId: Long) {
        val cits = repo.dao.citationsOnce(projectId)
        if (cits.isEmpty()) return
        val books = repo.dao.bibBooksOnce(projectId).associateBy { it.id }
        val mss = repo.dao.manuscriptsOnce(MsScope.CIT, projectId).associateBy { it.id }
        val arts = repo.dao.bibArticlesOnce(projectId).associateBy { it.id }
        fun extra(c: org.tribhasa.sodhamitra.data.Citation) = listOf(c.pageFolio, c.contextNote)
        val bookRows = cits.filter { it.sourceType == "BOOK" }.mapNotNull { c -> books[c.sourceId]?.let { it to extra(c) } }
        val mssRows = cits.filter { it.sourceType == "MSS" }.mapNotNull { c -> mss[c.sourceId]?.let { it to listOf(s(R.string.citation_page_folio) to c.pageFolio, s(R.string.citation_note) to c.contextNote) } }
        val artRows = cits.filter { it.sourceType == "ARTICLE" }.mapNotNull { c -> arts[c.sourceId]?.let { it to extra(c) } }
        if (bookRows.isEmpty() && mssRows.isEmpty() && artRows.isEmpty()) return
        out.add(H1(s(R.string.references_citations)))
        if (bookRows.isNotEmpty()) { out.add(H2(s(R.string.books))); out.add(bookGrid(bookRows.map { it.first }, bookRows.map { it.second })) }
        if (mssRows.isNotEmpty()) { out.add(H2(s(R.string.mss_manuscripts))); out.add(manuscriptRecords(mssRows)) }
        if (artRows.isNotEmpty()) { out.add(H2(s(R.string.articles))); out.add(articleGrid(artRows.map { it.first }, artRows.map { it.second })) }
    }

    // ---- tables ----------------------------------------------------------------------------------------------
    private fun no(i: Int) = (i + 1).toString()

    private fun bookGrid(books: List<BibBook>, extra: List<List<String>>): Grid {
        val cited = extra.isNotEmpty()
        val headers = listOf(s(R.string.pdf_no), s(R.string.title_label), s(R.string.author_label), s(R.string.editor_label), s(R.string.edition_label),
            s(R.string.publisher_label), s(R.string.pub_place_label), s(R.string.year_label)) +
            if (cited) listOf(s(R.string.citation_page_folio), s(R.string.citation_note)) else emptyList()
        val weights = if (cited) listOf(0.45f, 1.9f, 1.2f, 1.0f, 0.7f, 1.1f, 0.9f, 0.6f, 0.8f, 1.6f) else listOf(0.45f, 2.2f, 1.5f, 1.3f, 0.9f, 1.4f, 1.1f, 0.7f)
        val rows = books.mapIndexed { i, b ->
            listOf(no(i), b.title, b.author, b.editor, b.edition, b.publisher, b.place, b.year) + if (cited) extra[i] else emptyList()
        }
        return Grid(headers, weights, rows)
    }

    private fun articleGrid(arts: List<BibArticle>, extra: List<List<String>>): Grid {
        val cited = extra.isNotEmpty()
        val headers = listOf(s(R.string.pdf_no), s(R.string.article_title_label), s(R.string.authors_label), s(R.string.journal_label),
            s(R.string.volume_issue_label), s(R.string.year_label), s(R.string.pages_label), s(R.string.issn_label), s(R.string.scopus_label),
            s(R.string.impact_factor_label)) + if (cited) listOf(s(R.string.citation_page_folio), s(R.string.citation_note)) else emptyList()
        val weights = if (cited) listOf(0.4f, 1.7f, 1.0f, 1.0f, 0.7f, 0.5f, 0.6f, 0.8f, 0.9f, 0.6f, 0.7f, 1.3f)
        else listOf(0.45f, 2.0f, 1.3f, 1.3f, 0.9f, 0.6f, 0.8f, 1.0f, 1.1f, 0.8f)
        val rows = arts.mapIndexed { i, a ->
            listOf(no(i), a.title, a.author, a.journal, a.volumeIssue, a.year, a.pages, a.issn, d.scopus(a), a.impactFactor) + if (cited) extra[i] else emptyList()
        }
        return Grid(headers, weights, rows)
    }

    /** portrait: one small field | value table per manuscript, so that all fourteen fields stay readable */
    private fun manuscriptRecords(items: List<Pair<Manuscript, List<Pair<String, String>>>>) = RecordTables(
        items.mapIndexed { i, (m, extra) ->
            RecordTable("${i + 1}.  ${m.title}", d.manuscriptFields(m, onlyFilled = true) + extra.filter { it.second.isNotBlank() })
        },
    )

    /** landscape: the Future Editing List manuscripts as one wide table (14 fields + the three classifications) */
    private fun editingManuscriptGrid(ms: List<Manuscript>): Grid {
        val headers = listOf(s(R.string.pdf_no), s(R.string.ms_serial_no), s(R.string.ms_accession_no), s(R.string.ms_title), s(R.string.stakeholder),
            s(R.string.place), s(R.string.ms_edited_label), s(R.string.ms_author), s(R.string.language_label), s(R.string.script_label),
            s(R.string.material_label), s(R.string.folio_count), s(R.string.completeness), s(R.string.catalogue_ref), s(R.string.remarks),
            listOf(R.string.status_published, R.string.status_unpublished, R.string.to_be_translated).joinToString(" / ") { s(it) })
        val weights = listOf(0.5f, 0.8f, 0.9f, 1.6f, 1.2f, 1.0f, 0.8f, 1.2f, 0.8f, 0.8f, 0.9f, 0.6f, 1.3f, 1.0f, 1.5f, 1.1f)
        val rows = ms.mapIndexed { i, m ->
            listOf(no(i), m.serialNo, m.accessionNo, m.title, m.stakeholder, m.place, d.edited(m.edited), m.author, m.language, m.script,
                d.material(m), m.folioCount, m.completeness, m.catalogueRef, m.remarks, d.classification(m))
        }
        return Grid(headers, weights, rows)
    }

    private fun plannerBookGrid(books: List<org.tribhasa.sodhamitra.data.PlannerBook>): Grid {
        val first = books.firstOrNull()?.let { d.plannerBookFields(it) } ?: emptyList()
        val headers = listOf(s(R.string.pdf_no)) + (first.map { it.first }.ifEmpty {
            listOf(R.string.book_title_field, R.string.author_label, R.string.editor_label, R.string.edition_label, R.string.language_label,
                R.string.publisher_label, R.string.year_label, R.string.to_be_translated, R.string.stakeholder, R.string.place,
                R.string.accession_no, R.string.elibrary, R.string.translation_available, R.string.translation_languages).map { s(it) }
        })
        val weights = listOf(0.4f, 1.8f, 1.1f, 1.0f, 0.7f, 0.8f, 1.1f, 0.55f, 0.7f, 1.0f, 0.8f, 0.8f, 1.4f, 0.7f, 1.1f)
        val rows = books.mapIndexed { i, b -> listOf(no(i)) + d.plannerBookFields(b).map { it.second } }
        return Grid(headers, weights, rows)
    }

    // ---- profile header -----------------------------------------------------------------------------------------
    private suspend fun profileHeader(): ProfileHeader? {
        val p = repo.dao.profileOnce() ?: return null
        if (p.isEmpty) return null
        val lines = ArrayList<String>()
        val name = listOf(p.prefix, p.name).filter { it.isNotBlank() }.joinToString(" ")
        if (name.isNotBlank()) lines.add(name)
        for (v in listOf(p.designation, p.institution, p.address, p.place)) if (v.isNotBlank()) lines.add(v)
        if (p.sex.isNotBlank()) lines.add(s(R.string.profile_sex) + ": " + d.sex(p.sex))
        return ProfileHeader(lines, ProfilePhotos.load(repo.photoDir, p.photoFile))
    }

    // ---- writing --------------------------------------------------------------------------------------------------
    /** lays the document out twice (first to count the pages) and writes the PDF. Runs off the main thread. */
    suspend fun write(doc: PdfDoc, fileTitle: String): File = withContext(Dispatchers.Default) {
        val fonts = PdfFonts(context)
        val counter = CountingSink()
        PdfRenderer(fonts, doc, counter, 1).render()
        val pdf = PdfDocument()
        try {
            PdfRenderer(fonts, doc, PdfFileSink(pdf), counter.pages).render()
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val safe = fileTitle.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("").trim('_').take(40).ifBlank { "sodhamitra" }
            val file = File(PdfFiles.exportDir(context), "$safe-$stamp.pdf")
            file.outputStream().use { pdf.writeTo(it) }
            file
        } finally { pdf.close() }
    }
}
