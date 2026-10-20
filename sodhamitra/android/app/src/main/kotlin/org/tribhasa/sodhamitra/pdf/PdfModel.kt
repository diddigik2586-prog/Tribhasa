package org.tribhasa.sodhamitra.pdf

import android.graphics.Bitmap

/** What a PDF contains, independent of how it is drawn. */
sealed interface Block
data class H1(val text: String) : Block
data class H2(val text: String) : Block
data class H3(val text: String) : Block
/** running text of any length; it flows from page to page */
data class Para(val text: String) : Block
/** a table with a repeated header row; the rows flow from page to page. [weights] give the relative column widths. */
data class Grid(val headers: List<String>, val weights: List<Float>, val rows: List<List<String>>) : Block
/** one small two-column (field | value) table per record, used where a record has too many fields for a portrait row */
data class RecordTables(val records: List<RecordTable>) : Block
data class RecordTable(val caption: String, val rows: List<Pair<String, String>>)

data class ProfileHeader(val lines: List<String>, val photo: Bitmap?)

data class PdfDoc(
    val title: String,
    val subtitle: String,
    val landscape: Boolean,
    val blocks: List<Block>,
    val profile: ProfileHeader?,
    /** localised "Page %1$d of %2$d" */
    val pageOfFormat: String,
    val footerLine1: String,
    val footerLine2: String,
)
