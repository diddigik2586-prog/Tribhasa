package org.tribhasa.sodhamitra

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Checks the bundled translations as they are packaged: parses the generated strings.xml of all ten languages. */
class StringsTest {
    private val res = File("src/main/res")
    private val folders = linkedMapOf(
        "en" to "values", "kn" to "values-kn", "sa" to "values-sa", "te" to "values-te", "hi" to "values-hi",
        "ta" to "values-ta", "bn" to "values-bn", "gu" to "values-gu", "ml" to "values-ml", "or" to "values-or",
    )

    private fun load(folder: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$folder/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }.associate { it.getAttribute("name") to it.textContent }
    }

    private val all = folders.mapValues { load(it.value) }
    private val en = all.getValue("en")

    @Test fun exactly_ten_languages_are_bundled() {
        assertEquals(10, folders.size)
        folders.values.forEach { assertTrue("$it/strings.xml missing", File(res, "$it/strings.xml").exists()) }
    }

    @Test fun every_language_has_every_translatable_string() {
        val fixed = setOf("app_name", "brand_name", "brand_tagline", "copyright_line", "pdf_footer_line1", "pdf_footer_line2", "feedback_email", "feedback_subject")
        val keys = en.keys - fixed
        assertTrue(keys.size > 200)
        for ((lang, map) in all) {
            if (lang == "en") continue
            assertEquals("missing in $lang: ${keys - map.keys}", emptySet<String>(), keys - map.keys)
            assertEquals("unexpected in $lang: ${map.keys - keys}", emptySet<String>(), map.keys - keys)
            assertTrue("fixed strings must not be translated ($lang)", (map.keys intersect fixed).isEmpty())
        }
    }

    @Test fun non_english_strings_are_really_translated() {
        // words that are legitimately the same in every language (abbreviations and Latin-script technical terms)
        val sameOk = setOf("issn_label", "support_upi_id")
        val range = mapOf(
            "kn" to 0x0C80..0x0CFF, "sa" to 0x0900..0x097F, "te" to 0x0C00..0x0C7F, "hi" to 0x0900..0x097F, "ta" to 0x0B80..0x0BFF,
            "bn" to 0x0980..0x09FF, "gu" to 0x0A80..0x0AFF, "ml" to 0x0D00..0x0D7F, "or" to 0x0B00..0x0B7F,
        )
        for ((lang, r) in range) {
            for ((k, v) in all.getValue(lang)) {
                assertTrue("$lang/$k has no $lang-script text: '$v'", v.any { it.code in r } || k in sameOk)
                if (k !in sameOk) assertTrue("$lang/$k is just the English text", v != en[k])
            }
        }
    }

    @Test fun english_labels_are_the_approved_wording() {
        assertEquals("Script", en["script_label"]); assertEquals("Stakeholder", en["stakeholder"]); assertEquals("Place", en["place"])
        assertEquals("Mss Serial No.", en["ms_serial_no"]); assertEquals("Mss Accession No.", en["ms_accession_no"])
        assertEquals("Mss Title", en["ms_title"]); assertEquals("Edited / Not Edited", en["ms_edited_label"])
        assertEquals("Author / Attributed Author", en["ms_author"]); assertEquals("Material (Palm Leaf / Paper / Other)", en["material_label"])
        assertEquals("Folio Count", en["folio_count"]); assertEquals("Completeness and Condition", en["completeness"])
        assertEquals("Catalogue Reference", en["catalogue_ref"]); assertEquals("Remarks", en["remarks"])
        assertEquals("Place is required when Stakeholder is provided.", en["validation_place_required"])
        assertEquals("Title of Book", en["book_title_field"]); assertEquals("Accession No.", en["accession_no"])
        assertEquals("E-Library Name / Paste Link of the Book", en["elibrary"])
        assertEquals("Language(s) in which translation is available", en["translation_languages"])
        assertEquals("+ Add Chapter", en["add_chapter"]); assertEquals("+ Add Point", en["add_point"]); assertEquals("+ Add Note", en["add_note"])
        assertTrue(en.values.none { it.contains("Stakeholding") })   // "Stakeholding Institution" is replaced everywhere
        assertEquals("ಲಿಪಿ", all.getValue("kn")["script_label"])
    }

    @Test fun settings_and_branding_strings() {
        assertEquals("Profile", en["settings_profile"]); assertEquals("Language", en["settings_language"])
        assertEquals("About App", en["settings_about_app"]); assertEquals("About Dr. DVK", en["settings_about_dvk"])
        assertEquals("Support Śodhamitra", en["settings_support"]); assertEquals("Feedback", en["settings_feedback"])
        assertEquals("Śodhamitra — Research Planner", en["app_name"])
        assertEquals("Śodhamitra", en["brand_name"]); assertEquals("Research Planner", en["brand_tagline"])
        assertEquals("©Dr. Diddigi Vamsi Krishna", en["copyright_line"])
        assertEquals("Śodhamitra — Research Planner", en["pdf_footer_line1"])
        assertEquals("Created by © Dr. Diddigi Vamsi Krishna", en["pdf_footer_line2"])
        assertEquals("dvkrishna92@gmail.com", en["feedback_email"]); assertEquals("Śodhamitra App Feedback", en["feedback_subject"])
        // the creator-attribution phrases are not used on screens: the PDF footer is the only "Created by"
        for ((lang, m) in all) for ((k, v) in m) {
            if (k == "pdf_footer_line2") continue
            assertTrue("$lang/$k contains a creator phrase", !v.contains("Created by") && !v.contains("by Dr. Diddigi"))
        }
    }

    @Test fun biography_uses_only_supplied_facts() {
        val bio = listOf("dvk_p1", "dvk_p2", "dvk_p3", "dvk_p4", "dvk_p5", "dvk_recognition_title", "dvk_recognition")
        for ((lang, m) in all) {
            val text = bio.joinToString(" ") { m.getValue(it) }
            assertTrue("$lang: Granthapuraskāra 2020", text.contains("Granthapuraskāra (ग्रन्थपुरस्कार)") && text.contains("2020"))
            assertTrue("$lang: Karnataka Samskrit University", text.contains("Karnataka Samskrit University"))
            assertTrue("$lang: ORI", text.contains("Oriental Research Institute, University of Mysore"))
            assertTrue("$lang: founding director institution", text.contains("Shimshumara Shodha Pratishthanam"))
            assertTrue("$lang: unsupported awards must not appear", !text.contains("Veda Peetha") && !text.contains("Literary Saviours") && !text.contains("2024") && !text.contains("2025"))
        }
    }

    @Test fun every_string_referenced_in_xml_is_escaped_safely() {
        for ((lang, map) in all) for ((k, v) in map) {
            assertTrue("$lang/$k has an unbalanced quote", !v.contains("\\\\"))
        }
    }
}
