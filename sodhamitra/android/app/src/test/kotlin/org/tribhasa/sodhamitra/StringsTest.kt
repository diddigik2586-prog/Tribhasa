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
        val fixed = setOf("app_name", "brand_name", "brand_tagline", "copyright_line", "pdf_footer_line1", "pdf_footer_line2", "feedback_email", "feedback_subject",
            "creator_footer", "dvk_name", "dvk_roles", "dvk_bio_1", "dvk_bio_2", "dvk_bio_3", "dvk_bio_4", "dvk_bio_5")
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
        // V2: the creator footer of the Hero page and the Homepage, exactly
        assertEquals("Created by Dr. Diddigi Vamsi Krishna", en["creator_footer"])
        // "Created by" appears only in the PDF footer and in that one footer string
        for ((lang, m) in all) for ((k, v) in m) {
            if (k == "pdf_footer_line2" || k == "creator_footer") continue
            assertTrue("$lang/$k contains a creator phrase", !v.contains("Created by") && !v.contains("by Dr. Diddigi"))
        }
    }

    /** The author-approved biography, character for character as supplied (V2 specification, section 8). */
    private val approvedBiography = listOf(
        "dvk_name" to "Dr. Diddigi Vamsi Krishna",
        "dvk_roles" to "Sanskrit Scholar | Manuscriptologist | Researcher",
        "dvk_bio_1" to "Dr. Diddigi Vamsi Krishna is a Sanskrit scholar and manuscriptologist specialising in Sanskrit textual studies, manuscript preservation, critical editing, cataloguing and Indic script studies. He holds a Ph.D. in Sanskrit from the University of Mysore, with research focused on the Nirukta tradition.",
        "dvk_bio_2" to "He serves as a Research Assistant Grade II at the Oriental Research Institute (ORI), University of Mysore, Mysuru, contributing to manuscript research, scholarly editing, archival documentation and publication. As Coordinator of the ORI Digital Books Project, he is involved in digitisation, digital preservation and the documentation of scholarly collections.",
        "dvk_bio_3" to "His scholarly interests include Sanskrit textual scholarship, multilingual manuscripts, transliteration, traditional knowledge systems and digital heritage preservation. He has also contributed to editorial activities, academic publications and manuscriptology training.",
        "dvk_bio_4" to "Dr. Krishna is the Founding Director of Shimshumara Shodha Pratishthanam, Mysuru, and has served as Honorary Chief Editor at Adishila Publication, Bengaluru. He has also served as a reviewer for the Jahnavi Sanskrit e-Journal.",
        "dvk_bio_5" to "He received the Grantha Puraskara (2020) from Karnataka Samskrit University for his Sanskrit work Niruktacintanam and the Saṃskṛta-Suptanidhi-Ācārya recognition in 2014.",
        "copyright_line" to "©Dr. Diddigi Vamsi Krishna",
    )

    @Test fun biography_is_the_approved_text_verbatim() {
        for ((key, text) in approvedBiography) assertEquals("string $key", text, en[key])
        // the biography is fixed text: not translated, so it reads the same in every interface language
        for ((lang, m) in all) if (lang != "en") for ((key, _) in approvedBiography) assertTrue("$lang must not carry $key", key !in m)
        // nothing from the superseded draft remains
        for (k in listOf("dvk_p1", "dvk_p2", "dvk_p3", "dvk_p4", "dvk_p5", "dvk_recognition", "dvk_recognition_title")) assertTrue("$k removed", k !in en)
        // and nothing the author did not supply
        val all = approvedBiography.joinToString(" ") { it.second }
        assertTrue(!all.contains("Veda Peetha") && !all.contains("2024") && !all.contains("2025"))
        assertTrue(all.contains("Founding Director"))
    }

    /** The About App text is the approved V1 text, byte for byte in all ten languages: the snapshot was taken before the V2 changes. */
    @Test fun about_app_text_is_unchanged() {
        val rows = javaClass.classLoader!!.getResourceAsStream("about_app_v1_snapshot.txt")!!.bufferedReader(Charsets.UTF_8).readLines().filter { it.isNotBlank() }
        assertEquals(14 * 10, rows.size)
        for (row in rows) {
            val (lang, key, text) = row.split('\t', limit = 3)
            assertEquals("$lang/$key", text, all.getValue(lang)[key])
        }
    }

    @Test fun every_string_referenced_in_xml_is_escaped_safely() {
        for ((lang, map) in all) for ((k, v) in map) {
            assertTrue("$lang/$k has an unbalanced quote", !v.contains("\\\\"))
        }
    }
}
