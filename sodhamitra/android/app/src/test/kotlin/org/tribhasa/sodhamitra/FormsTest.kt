package org.tribhasa.sodhamitra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tribhasa.sodhamitra.domain.FieldType
import org.tribhasa.sodhamitra.domain.Forms
import org.tribhasa.sodhamitra.domain.toBibArticle
import org.tribhasa.sodhamitra.domain.toForm
import org.tribhasa.sodhamitra.domain.toManuscript
import org.tribhasa.sodhamitra.domain.toPlannerBook

class FormsTest {
    private val manuscriptOrder = listOf(
        R.string.ms_serial_no, R.string.ms_accession_no, R.string.ms_title, R.string.stakeholder, R.string.place,
        R.string.ms_edited_label, R.string.ms_author, R.string.language_label, R.string.script_label, R.string.material_label,
        R.string.folio_count, R.string.completeness, R.string.catalogue_ref, R.string.remarks,
    )

    @Test fun manuscript_form_has_the_fourteen_approved_fields_in_order() {
        val fields = Forms.manuscript.filter { it.key != "materialOther" }   // materialOther is the "Other" detail of field 10
        assertEquals(14, fields.size)
        assertEquals(manuscriptOrder, fields.map { it.label })
        // the three planning checkboxes are separate and independent
        assertEquals(listOf("published", "unpublished", "toBeTranslated"), Forms.manuscriptPlanning.map { it.key })
        assertTrue(Forms.manuscriptPlanning.all { it.type == FieldType.CHECK })
        assertEquals(manuscriptOrder + listOf(R.string.status_published, R.string.status_unpublished, R.string.to_be_translated),
            Forms.manuscriptForm(true).filter { it.key != "materialOther" }.map { it.label })
    }

    @Test fun planner_book_form_has_the_fourteen_approved_fields_in_order() {
        assertEquals(
            listOf(
                R.string.book_title_field, R.string.author_label, R.string.editor_label, R.string.edition_label, R.string.language_label,
                R.string.publisher_label, R.string.year_label, R.string.to_be_translated, R.string.stakeholder, R.string.place,
                R.string.accession_no, R.string.elibrary, R.string.translation_available, R.string.translation_languages,
            ),
            Forms.plannerBook.map { it.label },
        )
        assertEquals(FieldType.CHECK, Forms.plannerBook[7].type)    // 8 To be translated
        assertEquals(FieldType.CHECK, Forms.plannerBook[12].type)   // 13 Translation Available
        assertEquals(listOf("title"), Forms.plannerBook.filter { it.required }.map { it.key })   // only Title is mandatory
    }

    @Test fun title_only_saves_and_stakeholder_empty_means_place_optional() {
        for (spec in listOf(Forms.manuscript, Forms.plannerBook)) {
            assertTrue(Forms.validate(spec, mapOf("title" to "Rāmāyaṇa")).isEmpty())
            assertTrue(Forms.validate(spec, mapOf("title" to "X", "stakeholder" to "   ")).isEmpty())   // blanks count as empty
        }
    }

    @Test fun stakeholder_without_place_is_rejected_with_the_place_message() {
        for (spec in listOf(Forms.manuscript, Forms.plannerBook)) {
            val v = mapOf("title" to "X", "stakeholder" to "ORI Mysuru")
            assertEquals(mapOf("place" to R.string.validation_place_required), Forms.validate(spec, v))
            assertEquals(mapOf("place" to R.string.validation_place_required), Forms.validate(spec, v + ("place" to "  ")))
            assertTrue(Forms.validate(spec, v + ("place" to "Mysuru")).isEmpty())
            // clearing the stakeholder makes the place optional again
            assertTrue(Forms.validate(spec, v + ("stakeholder" to "")).isEmpty())
        }
    }

    @Test fun title_is_the_only_unconditional_requirement() {
        assertEquals(mapOf("title" to R.string.required_field), Forms.validate(Forms.manuscript, emptyMap()))
        assertEquals(mapOf("title" to R.string.required_field), Forms.validate(Forms.plannerBook, emptyMap()))
        assertEquals(setOf("title"), Forms.manuscript.filter { it.required }.map { it.key }.toSet())
    }

    @Test fun place_marker_follows_the_stakeholder() {
        val place = Forms.plannerBook.first { it.key == "place" }
        assertFalse(Forms.isMandatoryNow(place, mapOf("stakeholder" to "")))
        assertTrue(Forms.isMandatoryNow(place, mapOf("stakeholder" to "x")))
    }

    @Test fun translation_languages_appear_only_when_translation_available_is_checked() {
        val langs = Forms.plannerBook.last()
        assertFalse(langs.visibleIf!!(mapOf("translationAvailable" to "")))
        assertTrue(langs.visibleIf!!(mapOf("translationAvailable" to Forms.CHECKED)))
        // optional even when shown
        assertTrue(Forms.validate(Forms.plannerBook, mapOf("title" to "T", "translationAvailable" to Forms.CHECKED)).isEmpty())
        // text of a hidden languages box is not stored
        val b = mapOf("title" to "T", "translationAvailable" to "", "translationLanguages" to "Kannada").toPlannerBook("EDITING")
        assertEquals("", b.translationLanguages)
    }

    @Test fun article_requires_issn_but_not_scopus_or_impact_factor() {
        assertEquals(mapOf("issn" to R.string.issn_required), Forms.validate(Forms.bibArticle, mapOf("title" to "A")))
        assertTrue(Forms.validate(Forms.bibArticle, mapOf("title" to "A", "issn" to "1234-5678")).isEmpty())
        val a = mapOf("title" to "A", "issn" to "1234-5678", "scopusIndexed" to "", "scopusDetails" to "Q1").toBibArticle(1)
        assertEquals("", a.scopusDetails)   // details exist only when Scopus indexing is ticked
        assertTrue(Forms.bibArticle.first { it.key == "scopusDetails" }.visibleIf!!(mapOf("scopusIndexed" to Forms.CHECKED)))
    }

    @Test fun records_round_trip_through_the_form_without_changing_text() {
        val m = mapOf(
            "title" to "ರಾಮಾಯಣ", "stakeholder" to "ORI", "place" to "Mysuru", "script" to "ಕನ್ನಡ", "material" to "OTHER",
            "materialOther" to "Birch bark", "remarks" to "line one\nline two", "published" to "1",
        ).toManuscript("EDIT", null)
        assertEquals("ರಾಮಾಯಣ", m.title); assertEquals("Birch bark", m.materialOther); assertTrue(m.published)
        assertEquals("line one\nline two", m.remarks)
        assertEquals("ರಾಮಾಯಣ", m.toForm()["title"])
        // "Other" detail is dropped when another material is chosen
        assertEquals("", mapOf("title" to "x", "material" to "PAPER", "materialOther" to "z").toManuscript("BIB", 1).materialOther)
    }
}
