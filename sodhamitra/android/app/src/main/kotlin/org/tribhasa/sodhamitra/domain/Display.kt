package org.tribhasa.sodhamitra.domain

import android.content.Context
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.BibArticle
import org.tribhasa.sodhamitra.data.BibBook
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.PlannerBook

/** Turns stored records into the localised text shown in lists, tables and PDFs. Stored values are never translated. */
class Display(private val ctx: Context) {
    fun s(id: Int): String = ctx.getString(id)

    fun edited(code: String) = when (code) { "EDITED" -> s(R.string.ms_edited); "NOT_EDITED" -> s(R.string.ms_not_edited); else -> "" }

    fun material(m: Manuscript) = when (m.material) {
        "PALM_LEAF" -> s(R.string.material_palm)
        "PAPER" -> s(R.string.material_paper)
        "OTHER" -> s(R.string.material_other) + if (m.materialOther.isNotBlank()) " (${m.materialOther})" else ""
        else -> ""
    }

    fun sex(code: String) = when (code) {
        "MALE" -> s(R.string.sex_male); "FEMALE" -> s(R.string.sex_female); "TRANSGENDER" -> s(R.string.sex_transgender); else -> ""
    }

    fun yes(b: Boolean) = if (b) s(R.string.pdf_yes) else ""

    /** (label, value) pairs of the fourteen manuscript fields, in the approved order. Optionally only the filled ones. */
    fun manuscriptFields(m: Manuscript, onlyFilled: Boolean): List<Pair<String, String>> {
        val all = listOf(
            R.string.ms_serial_no to m.serialNo, R.string.ms_accession_no to m.accessionNo, R.string.ms_title to m.title,
            R.string.stakeholder to m.stakeholder, R.string.place to m.place, R.string.ms_edited_label to edited(m.edited),
            R.string.ms_author to m.author, R.string.language_label to m.language, R.string.script_label to m.script,
            R.string.material_label to material(m), R.string.folio_count to m.folioCount, R.string.completeness to m.completeness,
            R.string.catalogue_ref to m.catalogueRef, R.string.remarks to m.remarks,
        ).map { s(it.first) to it.second }
        return if (onlyFilled) all.filter { it.second.isNotBlank() } else all
    }

    /** the planning classifications that are ticked, e.g. "Published / To be translated" */
    fun classification(m: Manuscript) = listOfNotNull(
        s(R.string.status_published).takeIf { m.published }, s(R.string.status_unpublished).takeIf { m.unpublished },
        s(R.string.to_be_translated).takeIf { m.toBeTranslated },
    ).joinToString(" / ")

    fun bookFields(b: BibBook, onlyFilled: Boolean = true): List<Pair<String, String>> {
        val all = listOf(
            R.string.title_label to b.title, R.string.author_label to b.author, R.string.editor_label to b.editor,
            R.string.edition_label to b.edition, R.string.publisher_label to b.publisher, R.string.pub_place_label to b.place,
            R.string.year_label to b.year,
        ).map { s(it.first) to it.second }
        return if (onlyFilled) all.filter { it.second.isNotBlank() } else all
    }

    fun scopus(a: BibArticle) = when {
        !a.scopusIndexed -> ""
        a.scopusDetails.isBlank() -> s(R.string.pdf_yes)
        else -> s(R.string.pdf_yes) + ": " + a.scopusDetails
    }

    fun articleFields(a: BibArticle, onlyFilled: Boolean = true): List<Pair<String, String>> {
        val all = listOf(
            R.string.article_title_label to a.title, R.string.authors_label to a.author, R.string.journal_label to a.journal,
            R.string.volume_issue_label to a.volumeIssue, R.string.year_label to a.year, R.string.pages_label to a.pages,
            R.string.issn_label to a.issn, R.string.scopus_label to scopus(a), R.string.impact_factor_label to a.impactFactor,
        ).map { s(it.first) to it.second }
        return if (onlyFilled) all.filter { it.second.isNotBlank() } else all
    }

    /** the fourteen fields of a planner book, in the approved order */
    fun plannerBookFields(b: PlannerBook): List<Pair<String, String>> = listOf(
        R.string.book_title_field to b.title, R.string.author_label to b.author, R.string.editor_label to b.editor,
        R.string.edition_label to b.edition, R.string.language_label to b.language, R.string.publisher_label to b.publisher,
        R.string.year_label to b.year, R.string.to_be_translated to yes(b.toBeTranslated), R.string.stakeholder to b.stakeholder,
        R.string.place to b.place, R.string.accession_no to b.accessionNo, R.string.elibrary to b.eLibrary,
        R.string.translation_available to yes(b.translationAvailable), R.string.translation_languages to b.translationLanguages,
    ).map { s(it.first) to it.second }
}
