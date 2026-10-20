package org.tribhasa.sodhamitra.domain

import org.tribhasa.sodhamitra.data.BibArticle
import org.tribhasa.sodhamitra.data.BibBook
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.PlannerBook

/** Conversions between records and form values. Single-line text is trimmed on save; long text is stored as typed. */
private fun Map<String, String>.t(k: String) = this[k].orEmpty().trim()
private fun Map<String, String>.raw(k: String) = this[k].orEmpty()
private fun Map<String, String>.b(k: String) = this[k] == Forms.CHECKED
private fun flag(b: Boolean) = if (b) Forms.CHECKED else ""

fun Manuscript.toForm(): Map<String, String> = mapOf(
    "serialNo" to serialNo, "accessionNo" to accessionNo, "title" to title, "stakeholder" to stakeholder, "place" to place,
    "edited" to edited, "author" to author, "language" to language, "script" to script, "material" to material,
    "materialOther" to materialOther, "folioCount" to folioCount, "completeness" to completeness,
    "catalogueRef" to catalogueRef, "remarks" to remarks,
    "published" to flag(published), "unpublished" to flag(unpublished), "toBeTranslated" to flag(toBeTranslated),
)

fun Map<String, String>.toManuscript(scope: String, projectId: Long?, id: Long = 0): Manuscript {
    val material = t("material")
    return Manuscript(
        id = id, scope = scope, projectId = projectId,
        serialNo = t("serialNo"), accessionNo = t("accessionNo"), title = t("title"),
        stakeholder = t("stakeholder"), place = t("place"), edited = t("edited"), author = t("author"),
        language = t("language"), script = t("script"), material = material,
        materialOther = if (material == "OTHER") t("materialOther") else "",
        folioCount = t("folioCount"), completeness = raw("completeness").trim(), catalogueRef = t("catalogueRef"),
        remarks = raw("remarks").trim(),
        published = b("published"), unpublished = b("unpublished"), toBeTranslated = b("toBeTranslated"),
    )
}

fun PlannerBook.toForm(): Map<String, String> = mapOf(
    "title" to title, "author" to author, "editor" to editor, "edition" to edition, "language" to language,
    "publisher" to publisher, "year" to year, "toBeTranslated" to flag(toBeTranslated), "stakeholder" to stakeholder,
    "place" to place, "accessionNo" to accessionNo, "eLibrary" to eLibrary,
    "translationAvailable" to flag(translationAvailable), "translationLanguages" to translationLanguages,
)

fun Map<String, String>.toPlannerBook(listType: String, id: Long = 0): PlannerBook {
    val available = b("translationAvailable")
    return PlannerBook(
        id = id, listType = listType, title = t("title"), author = t("author"), editor = t("editor"),
        edition = t("edition"), language = t("language"), publisher = t("publisher"), year = t("year"),
        toBeTranslated = b("toBeTranslated"), stakeholder = t("stakeholder"), place = t("place"),
        accessionNo = t("accessionNo"), eLibrary = t("eLibrary"), translationAvailable = available,
        translationLanguages = if (available) t("translationLanguages") else "",
    )
}

fun BibBook.toForm(): Map<String, String> = mapOf(
    "title" to title, "author" to author, "editor" to editor, "edition" to edition,
    "publisher" to publisher, "place" to place, "year" to year,
)

fun Map<String, String>.toBibBook(projectId: Long, id: Long = 0) = BibBook(
    id = id, projectId = projectId, title = t("title"), author = t("author"), editor = t("editor"),
    edition = t("edition"), publisher = t("publisher"), place = t("place"), year = t("year"),
)

fun BibArticle.toForm(): Map<String, String> = mapOf(
    "title" to title, "author" to author, "journal" to journal, "volumeIssue" to volumeIssue, "year" to year,
    "pages" to pages, "issn" to issn, "scopusIndexed" to flag(scopusIndexed), "scopusDetails" to scopusDetails,
    "impactFactor" to impactFactor,
)

fun Map<String, String>.toBibArticle(projectId: Long, id: Long = 0): BibArticle {
    val scopus = b("scopusIndexed")
    return BibArticle(
        id = id, projectId = projectId, title = t("title"), author = t("author"), journal = t("journal"),
        volumeIssue = t("volumeIssue"), year = t("year"), pages = t("pages"), issn = t("issn"),
        scopusIndexed = scopus, scopusDetails = if (scopus) t("scopusDetails") else "", impactFactor = t("impactFactor"),
    )
}
