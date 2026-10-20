package org.tribhasa.sodhamitra.ui.theme

/** Writing systems for which a font is bundled. */
enum class Script { LATIN, LATIN_EXT, DEVANAGARI, BENGALI, GUJARATI, ODIA, TAMIL, TELUGU, KANNADA, MALAYALAM }

/**
 * Splits text into runs of one script each, so that every run can be drawn with the bundled font made for it: Ubuntu for
 * English, a Noto Sans face for each Indian script, and Noto Sans for the few Latin letters Ubuntu lacks (the dotted IAST
 * letters of Sanskrit transliteration such as ṭ ḍ ṇ ṣ ṃ ḥ ṛ). Pure Kotlin, so it is unit-tested on a computer.
 */
object ScriptRuns {
    data class Run(val start: Int, val end: Int, val script: Script)

    /** null = neutral: the character simply continues whichever run it follows (spaces, joiners, danda, combining marks). */
    fun classify(cp: Int): Script? = when {
        Character.isWhitespace(cp) || Character.isSpaceChar(cp) -> null
        cp == 0x200C || cp == 0x200D || cp in 0xFE00..0xFE0F -> null
        cp in 0x0300..0x036F -> null
        cp == 0x0964 || cp == 0x0965 -> null
        cp in 0x0900..0x097F || cp in 0xA8E0..0xA8FF || cp in 0x1CD0..0x1CFF -> Script.DEVANAGARI
        cp in 0x0980..0x09FF -> Script.BENGALI
        cp in 0x0A80..0x0AFF -> Script.GUJARATI
        cp in 0x0B00..0x0B7F -> Script.ODIA
        cp in 0x0B80..0x0BFF -> Script.TAMIL
        cp in 0x0C00..0x0C7F -> Script.TELUGU
        cp in 0x0C80..0x0CFF -> Script.KANNADA
        cp in 0x0D00..0x0D7F -> Script.MALAYALAM
        cp in 0x1E00..0x1EFF && cp !in 0x1E80..0x1E85 && cp != 0x1EF2 && cp != 0x1EF3 -> Script.LATIN_EXT
        else -> Script.LATIN
    }

    fun runs(text: CharSequence): List<Run> {
        if (text.isEmpty()) return emptyList()
        val out = ArrayList<Run>()
        var runStart = 0
        var current: Script? = null
        var i = 0
        while (i < text.length) {
            val cp = Character.codePointAt(text, i)
            val len = Character.charCount(cp)
            val s = classify(cp)
            if (s != null) {
                if (current == null) current = s
                else if (s != current) { out.add(Run(runStart, i, current)); runStart = i; current = s }
            }
            i += len
        }
        out.add(Run(runStart, text.length, current ?: Script.LATIN))
        return out
    }
}
