package org.tribhasa.sodhamitra.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.tribhasa.sodhamitra.R

data class TableColumn(val header: String, val width: Dp)
data class TableRowData(val id: Long, val cells: List<String>)

/**
 * Adds a table (header row + one row per record) to a LazyColumn. All rows share one horizontal scroll state, so the
 * whole table slides sideways together on a phone. A plain tap does nothing (no accidental edits); long-press a row for the
 * contextual menu, where Edit and Delete are chosen explicitly.
 */
fun LazyListScope.recordTable(
    keyPrefix: String,
    columns: List<TableColumn>,
    rows: List<TableRowData>,
    scroll: ScrollState,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    item(key = "$keyPrefix-header") { TableRow(columns, columns.map { it.header }, scroll, header = true) }
    items(rows.size, key = { "$keyPrefix-${rows[it].id}" }) { i ->
        val r = rows[i]
        var menu by remember { mutableStateOf(false) }
        Box {
            TableRow(columns, r.cells, scroll, header = false, onLongClick = { menu = true })
            ActionMenu(menu, { menu = false }, listOf(
                MenuAction(R.string.edit) { onEdit(r.id) },
                MenuAction(R.string.delete, destructive = true) { onDelete(r.id) },
            ))
        }
    }
}

@Composable
private fun TableRow(
    columns: List<TableColumn>,
    cells: List<String>,
    scroll: ScrollState,
    header: Boolean,
    onLongClick: (() -> Unit)? = null,
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val bg = if (header) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    var m: Modifier = Modifier.horizontalScroll(scroll).height(IntrinsicSize.Min)
    if (onLongClick != null) {
        // long-press only: no tap action, no tap ripple, and the long-press is also offered to accessibility services
        m = Modifier.pointerInput(onLongClick) { detectTapGestures(onLongPress = { onLongClick() }) }
            .semantics { onLongClick(action = { onLongClick(); true }) }
            .then(m)
    }
    Row(m) {
        columns.forEachIndexed { i, col ->
            Box(Modifier.width(col.width).fillMaxHeight().background(bg).border(0.5.dp, line).padding(horizontal = 6.dp, vertical = 5.dp)) {
                Txt(
                    cells.getOrElse(i) { "" },
                    style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = if (header) FontWeight.Bold else null,
                    maxLines = if (header) 4 else 6, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
