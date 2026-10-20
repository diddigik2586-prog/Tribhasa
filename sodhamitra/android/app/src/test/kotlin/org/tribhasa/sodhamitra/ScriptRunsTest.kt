package org.tribhasa.sodhamitra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tribhasa.sodhamitra.ui.theme.Script
import org.tribhasa.sodhamitra.ui.theme.ScriptRuns

class ScriptRunsTest {
    private fun scripts(s: String) = ScriptRuns.runs(s).map { it.script }
    private fun pieces(s: String) = ScriptRuns.runs(s).map { s.substring(it.start, it.end) to it.script }

    @Test fun english_is_one_latin_run() {
        assertEquals(listOf(Script.LATIN), scripts("Research Planner — Śodhamitra ©Dr. Diddigi Vamsi Krishna"))
    }

    @Test fun each_indian_script_gets_its_own_font() {
        assertEquals(Script.KANNADA, scripts("ಲಿಪಿ").single())
        assertEquals(Script.DEVANAGARI, scripts("संस्कृतम्").single())
        assertEquals(Script.TELUGU, scripts("తెలుగు").single())
        assertEquals(Script.TAMIL, scripts("தமிழ்").single())
        assertEquals(Script.BENGALI, scripts("বাংলা").single())
        assertEquals(Script.GUJARATI, scripts("ગુજરાતી").single())
        assertEquals(Script.MALAYALAM, scripts("മലയാളം").single())
        assertEquals(Script.ODIA, scripts("ଓଡ଼ିଆ").single())
    }

    @Test fun mixed_text_is_split_per_script_and_nothing_is_lost() {
        val text = "ಹಸ್ತಪ್ರತಿ Śodhamitra संस्कृत 123"
        val runs = ScriptRuns.runs(text)
        assertEquals(text, runs.joinToString("") { text.substring(it.start, it.end) })
        assertEquals(listOf(Script.KANNADA, Script.LATIN, Script.DEVANAGARI, Script.LATIN), runs.map { it.script })
    }

    @Test fun iast_letters_that_ubuntu_lacks_use_the_latin_extension_font() {
        // ṭ ḍ ṇ ṣ ṃ ḥ ṛ are in Latin Extended Additional; Ubuntu has none of them
        val p = pieces("Rāmāyaṇa")
        assertTrue(p.any { it.second == Script.LATIN_EXT && it.first == "ṇ" })
        assertTrue(p.any { it.second == Script.LATIN && it.first.startsWith("Rāmāya") })
        assertEquals(Script.LATIN, ScriptRuns.classify('Ś'.code))   // Ś is in Ubuntu
        assertEquals(Script.LATIN, ScriptRuns.classify('ā'.code))
    }

    @Test fun danda_follows_the_script_it_ends() {
        assertEquals(listOf(Script.BENGALI), scripts("অনুবাদ।"))
        assertEquals(listOf(Script.DEVANAGARI), scripts("अस्तु।"))
    }

    @Test fun empty_and_neutral_only_text() {
        assertTrue(ScriptRuns.runs("").isEmpty())
        assertEquals(listOf(Script.LATIN), scripts("   "))
    }

    @Test fun runs_cover_text_without_gaps() {
        val text = "a ಕ b ക c ଓ d ட e ග"
        val runs = ScriptRuns.runs(text)
        assertEquals(0, runs.first().start); assertEquals(text.length, runs.last().end)
        for (i in 1 until runs.size) assertEquals(runs[i - 1].end, runs[i].start)
    }
}
