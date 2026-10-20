package org.tribhasa.sodhamitra.domain

/** Splits long text into pieces for storage (never inside a surrogate pair) and joins them back unchanged. */
object TextChunks {
    const val MAX_CHARS = 200_000   // at most ~800 KB per database row, far below the 2 MB row limit of Android's cursor window

    fun split(text: String, max: Int = MAX_CHARS): List<String> {
        if (text.isEmpty()) return emptyList()
        val out = ArrayList<String>()
        var i = 0
        while (i < text.length) {
            var end = minOf(i + max, text.length)
            if (end < text.length && Character.isHighSurrogate(text[end - 1])) end--   // keep a surrogate pair together
            out.add(text.substring(i, end)); i = end
        }
        return out
    }

    fun join(chunks: List<String>): String = if (chunks.size == 1) chunks[0] else chunks.joinToString("")
}
