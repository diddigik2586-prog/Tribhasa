package org.tribhasa.sodhamitra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tribhasa.sodhamitra.domain.TextChunks

class TextChunksTest {
    @Test fun empty_text_has_no_chunks_and_joins_back_to_empty() {
        assertTrue(TextChunks.split("").isEmpty()); assertEquals("", TextChunks.join(emptyList()))
    }

    @Test fun any_text_round_trips_exactly_and_never_splits_a_surrogate_pair() {
        val text = "ab😀".repeat(1000) + "ಕ".repeat(77)
        for (max in listOf(1, 2, 3, 7, 100, 4001)) {
            val parts = TextChunks.split(text, max.coerceAtLeast(2))
            assertEquals(text, TextChunks.join(parts))
            parts.forEach { p -> assertTrue("chunk starts with a lone low surrogate", p.isEmpty() || !Character.isLowSurrogate(p[0])) ; assertTrue(!Character.isHighSurrogate(p.last()) || p == parts.last()) }
        }
    }

    @Test fun chunks_stay_far_below_the_two_megabyte_row_limit() {
        val big = "ಕ".repeat(1_000_000)   // 3 bytes per character in UTF-8
        val parts = TextChunks.split(big)
        assertTrue(parts.size >= 5)
        assertTrue(parts.all { it.toByteArray().size < 1_000_000 })
        assertEquals(big, TextChunks.join(parts))
    }
}
