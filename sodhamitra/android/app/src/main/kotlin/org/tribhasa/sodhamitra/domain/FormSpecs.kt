package org.tribhasa.sodhamitra.domain

import androidx.annotation.StringRes
import org.tribhasa.sodhamitra.R

enum class FieldType { TEXT, MULTILINE, CHECK, CHOICE }

data class Choice(val code: String, @StringRes val label: Int)

/**
 * One field of a form. The order of the lists below IS the approved field order of each form; the screens render them
 * top to bottom exactly as listed. Values live in a Map<String, String>; a checked checkbox is "1", unchecked is "".
 */
data class FieldSpec(
    val key: String,
    @StringRes val label: Int,
    val type: FieldType = FieldType.TEXT,
    /** always mandatory (the Title of a record, the ISSN of an article) */
    val required: Boolean = false,
    @StringRes val requiredMessage: Int = R.string.required_field,
    /** becomes mandatory as soon as the named field contains text (Stakeholder -> Place) */
    val requiredIfFilled: String? = null,
    @StringRes val requiredIfFilledMessage: Int = R.string.validation_place_required,
    val choices: List<Choice> = emptyList(),
    /** shown only while this returns true (e.g. the translation languages once "Translation Available" is checked) */
    val visibleIf: ((Map<String, String>) -> Boolean)? = null,
    val minLines: Int = 3,
)

object Forms {
    const val CHECKED = "1"

    // ---- shared manuscript form: fourteen fields in the approved order ------------------------------------------
    val manuscript: List<FieldSpec> = listOf(
        FieldSpec("serialNo", R.string.ms_serial_no),                                                    // 1
        FieldSpec("accessionNo", R.string.ms_accession_no),                                              // 2
        FieldSpec("title", R.string.ms_title, required = true),                                          // 3
        FieldSpec("stakeholder", R.string.stakeholder),                                                  // 4
        FieldSpec("place", R.string.place, requiredIfFilled = "stakeholder"),                            // 5
        FieldSpec("edited", R.string.ms_edited_label, FieldType.CHOICE, choices = listOf(                // 6
            Choice("EDITED", R.string.ms_edited), Choice("NOT_EDITED", R.string.ms_not_edited))),
        FieldSpec("author", R.string.ms_author),                                                         // 7
        FieldSpec("language", R.string.language_label),                                                  // 8
        FieldSpec("script", R.string.script_label),                                                      // 9
        FieldSpec("material", R.string.material_label, FieldType.CHOICE, choices = listOf(               // 10
            Choice("PALM_LEAF", R.string.material_palm), Choice("PAPER", R.string.material_paper),
            Choice("OTHER", R.string.material_other))),
        FieldSpec("materialOther", R.string.material_other_detail, visibleIf = { it["material"] == "OTHER" }), // detail of 10
        FieldSpec("folioCount", R.string.folio_count),                                                   // 11
        FieldSpec("completeness", R.string.completeness, FieldType.MULTILINE),                           // 12
        FieldSpec("catalogueRef", R.string.catalogue_ref),                                               // 13
        FieldSpec("remarks", R.string.remarks, FieldType.MULTILINE),                                     // 14
    )

    /** the three independent planning classifications (Future Editing List manuscripts only) */
    val manuscriptPlanning: List<FieldSpec> = listOf(
        FieldSpec("published", R.string.status_published, FieldType.CHECK),
        FieldSpec("unpublished", R.string.status_unpublished, FieldType.CHECK),
        FieldSpec("toBeTranslated", R.string.to_be_translated, FieldType.CHECK),
    )

    fun manuscriptForm(withPlanning: Boolean): List<FieldSpec> = if (withPlanning) manuscript + manuscriptPlanning else manuscript

    // ---- Future Editing List and Future Trans List: the identical fourteen-field Book form ----------------------
    val plannerBook: List<FieldSpec> = listOf(
        FieldSpec("title", R.string.book_title_field, required = true),                                  // 1
        FieldSpec("author", R.string.author_label),                                                      // 2
        FieldSpec("editor", R.string.editor_label),                                                      // 3
        FieldSpec("edition", R.string.edition_label),                                                    // 4
        FieldSpec("language", R.string.language_label),                                                  // 5
        FieldSpec("publisher", R.string.publisher_label),                                                // 6
        FieldSpec("year", R.string.year_label),                                                          // 7
        FieldSpec("toBeTranslated", R.string.to_be_translated, FieldType.CHECK),                         // 8
        FieldSpec("stakeholder", R.string.stakeholder),                                                  // 9
        FieldSpec("place", R.string.place, requiredIfFilled = "stakeholder"),                            // 10
        FieldSpec("accessionNo", R.string.accession_no),                                                 // 11
        FieldSpec("eLibrary", R.string.elibrary),                                                        // 12
        FieldSpec("translationAvailable", R.string.translation_available, FieldType.CHECK),              // 13
        FieldSpec("translationLanguages", R.string.translation_languages,                                // 14
            visibleIf = { it["translationAvailable"] == CHECKED }),
    )

    // ---- bibliography / citation sources ------------------------------------------------------------------------
    val bibBook: List<FieldSpec> = listOf(
        FieldSpec("title", R.string.title_label, required = true),
        FieldSpec("author", R.string.author_label),
        FieldSpec("editor", R.string.editor_label),
        FieldSpec("edition", R.string.edition_label),
        FieldSpec("publisher", R.string.publisher_label),
        FieldSpec("place", R.string.pub_place_label),
        FieldSpec("year", R.string.year_label),
    )

    val bibArticle: List<FieldSpec> = listOf(
        FieldSpec("title", R.string.article_title_label, required = true),
        FieldSpec("author", R.string.authors_label),
        FieldSpec("journal", R.string.journal_label),
        FieldSpec("volumeIssue", R.string.volume_issue_label),
        FieldSpec("year", R.string.year_label),
        FieldSpec("pages", R.string.pages_label),
        FieldSpec("issn", R.string.issn_label, required = true, requiredMessage = R.string.issn_required),
        FieldSpec("scopusIndexed", R.string.scopus_label, FieldType.CHECK),
        FieldSpec("scopusDetails", R.string.scopus_details_label, visibleIf = { it["scopusIndexed"] == CHECKED }),
        FieldSpec("impactFactor", R.string.impact_factor_label),
    )

    /** the citation-specific fields that follow the source fields in a citation form */
    val citationExtra: List<FieldSpec> = listOf(
        FieldSpec("pageFolio", R.string.citation_page_folio),
        FieldSpec("contextNote", R.string.citation_note, FieldType.MULTILINE),
    )

    // ---- project titles and chapter titles: one mandatory line -------------------------------------------------
    val researchTitle: List<FieldSpec> = listOf(FieldSpec("title", R.string.research_title_label, required = true))
    val bookTitle: List<FieldSpec> = listOf(FieldSpec("title", R.string.book_title_label, required = true))
    val chapterTitle: List<FieldSpec> = listOf(FieldSpec("title", R.string.chapter_title_label, required = true))

    // ---- outline points and research notes: a title plus a description of any length ---------------------------
    val point: List<FieldSpec> = listOf(
        FieldSpec("title", R.string.point_title_label, required = true),
        FieldSpec("description", R.string.description_label, FieldType.MULTILINE, minLines = 12),
    )
    val note: List<FieldSpec> = listOf(
        FieldSpec("title", R.string.note_title_label, required = true),
        FieldSpec("description", R.string.description_label, FieldType.MULTILINE, minLines = 12),
    )

    // ---- validation -------------------------------------------------------------------------------------------
    private fun isVisible(f: FieldSpec, values: Map<String, String>) = f.visibleIf?.invoke(values) ?: true

    /** key -> string resource of the message to show; empty when the form may be saved. */
    fun validate(spec: List<FieldSpec>, values: Map<String, String>): Map<String, Int> {
        val errors = LinkedHashMap<String, Int>()
        for (f in spec) {
            if (!isVisible(f, values) || f.type == FieldType.CHECK || f.type == FieldType.CHOICE) continue
            val blank = values[f.key].orEmpty().isBlank()
            if (f.required && blank) errors[f.key] = f.requiredMessage
            else if (f.requiredIfFilled != null && blank && values[f.requiredIfFilled].orEmpty().isNotBlank())
                errors[f.key] = f.requiredIfFilledMessage
        }
        return errors
    }

    /** whether the mandatory marker (*) is currently shown next to this field */
    fun isMandatoryNow(f: FieldSpec, values: Map<String, String>): Boolean =
        f.required || (f.requiredIfFilled != null && values[f.requiredIfFilled].orEmpty().isNotBlank())
}
