package org.tribhasa.sodhamitra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerList
import org.tribhasa.sodhamitra.domain.Display
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.ExportTarget
import org.tribhasa.sodhamitra.ui.Route
import org.tribhasa.sodhamitra.ui.common.AppScaffold
import org.tribhasa.sodhamitra.ui.common.rememberFlow
import org.tribhasa.sodhamitra.ui.common.BarTextButton
import org.tribhasa.sodhamitra.ui.common.ConfirmDeleteDialog
import org.tribhasa.sodhamitra.ui.common.EmptyHint
import org.tribhasa.sodhamitra.ui.common.HintLine
import org.tribhasa.sodhamitra.ui.common.SectionHeader
import org.tribhasa.sodhamitra.ui.common.TableColumn
import org.tribhasa.sodhamitra.ui.common.TableRowData
import org.tribhasa.sodhamitra.ui.common.recordTable

/** Editing / Translation Planner: exactly two options. */
@Composable
fun PlannerScreen(vm: AppViewModel) {
    AppScaffold(title = stringResource(R.string.home_planner), onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BigOption(R.string.future_editing_list) { vm.go(Route.EditingList) }
            BigOption(R.string.future_trans_list) { vm.go(Route.TransList) }
        }
    }
}

private fun manuscriptColumns(d: Display): List<TableColumn> = listOf(
    TableColumn(d.s(R.string.pdf_no), 44.dp),
    TableColumn(d.s(R.string.ms_serial_no), 96.dp), TableColumn(d.s(R.string.ms_accession_no), 104.dp),
    TableColumn(d.s(R.string.ms_title), 190.dp), TableColumn(d.s(R.string.stakeholder), 140.dp),
    TableColumn(d.s(R.string.place), 110.dp), TableColumn(d.s(R.string.ms_edited_label), 120.dp),
    TableColumn(d.s(R.string.ms_author), 150.dp), TableColumn(d.s(R.string.language_label), 100.dp),
    TableColumn(d.s(R.string.script_label), 100.dp), TableColumn(d.s(R.string.material_label), 130.dp),
    TableColumn(d.s(R.string.folio_count), 90.dp), TableColumn(d.s(R.string.completeness), 170.dp),
    TableColumn(d.s(R.string.catalogue_ref), 140.dp), TableColumn(d.s(R.string.remarks), 190.dp),
    TableColumn(listOf(R.string.status_published, R.string.status_unpublished, R.string.to_be_translated).joinToString(" / ") { d.s(it) }, 170.dp),
)

private fun bookColumns(d: Display): List<TableColumn> {
    val widths = listOf(190, 130, 130, 90, 100, 130, 70, 100, 140, 110, 110, 190, 110, 180).map { it.dp }
    val labels = listOf(
        R.string.book_title_field, R.string.author_label, R.string.editor_label, R.string.edition_label, R.string.language_label,
        R.string.publisher_label, R.string.year_label, R.string.to_be_translated, R.string.stakeholder, R.string.place,
        R.string.accession_no, R.string.elibrary, R.string.translation_available, R.string.translation_languages,
    )
    return listOf(TableColumn(d.s(R.string.pdf_no), 44.dp)) + labels.mapIndexed { i, l -> TableColumn(d.s(l), widths[i]) }
}

@Composable
fun EditingListScreen(vm: AppViewModel) {
    val d = Display(LocalContext.current)
    val mss by rememberFlow { vm.repo.editingManuscripts() }.collectAsState(initial = emptyList())
    val books by rememberFlow(PlannerList.EDITING) { vm.repo.plannerBooks(PlannerList.EDITING) }.collectAsState(initial = emptyList())
    val msScroll = androidx.compose.foundation.rememberScrollState()
    val bookScroll = rememberScrollState()
    var pending by remember { mutableStateOf<PendingDelete?>(null) }

    AppScaffold(
        title = stringResource(R.string.future_editing_list), onBack = { vm.back() },
        actions = { BarTextButton(R.string.export) { vm.go(Route.Export(ExportTarget.EditingList)) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item { HintLine(R.string.table_hint, Modifier.padding(bottom = 8.dp)) }

            item(key = "h-ms") { SectionHeader(R.string.heading_manuscript, R.string.add_plus, { vm.go(Route.ManuscriptForm(MsScope.EDIT, null, null)) }, Modifier.padding(bottom = 8.dp)) }
            if (mss.isEmpty()) item(key = "e-ms") { EmptyHint(R.string.empty_records) }
            else recordTable(
                "ms", manuscriptColumns(d),
                mss.mapIndexed { i, m ->
                    TableRowData(m.id, listOf((i + 1).toString(), m.serialNo, m.accessionNo, m.title, m.stakeholder, m.place, d.edited(m.edited),
                        m.author, m.language, m.script, d.material(m), m.folioCount, m.completeness, m.catalogueRef, m.remarks, d.classification(m)))
                },
                msScroll,
                onEdit = { vm.go(Route.ManuscriptForm(MsScope.EDIT, null, it)) },
                onDelete = { id -> pending = PendingDelete(R.string.delete_record_msg) { vm.launch { vm.repo.deleteManuscript(id) } } },
            )

            item(key = "h-book") { SectionHeader(R.string.heading_book, R.string.add_plus, { vm.go(Route.PlannerBookForm(PlannerList.EDITING, null)) }, Modifier.padding(top = 24.dp, bottom = 8.dp)) }
            if (books.isEmpty()) item(key = "e-book") { EmptyHint(R.string.empty_records) }
            else recordTable(
                "bk", bookColumns(d),
                books.mapIndexed { i, b -> TableRowData(b.id, listOf((i + 1).toString()) + d.plannerBookFields(b).map { it.second }) },
                bookScroll,
                onEdit = { vm.go(Route.PlannerBookForm(PlannerList.EDITING, it)) },
                onDelete = { id -> pending = PendingDelete(R.string.delete_record_msg) { vm.launch { vm.repo.deletePlannerBook(id) } } },
            )
        }
    }
    pending?.let { p -> ConfirmDeleteDialog(p.message, onConfirm = { p.action(); pending = null }, onDismiss = { pending = null }) }
}

@Composable
fun TransListScreen(vm: AppViewModel) {
    val d = Display(LocalContext.current)
    val books by rememberFlow(PlannerList.TRANS) { vm.repo.plannerBooks(PlannerList.TRANS) }.collectAsState(initial = emptyList())
    val scroll = rememberScrollState()
    var pending by remember { mutableStateOf<PendingDelete?>(null) }

    AppScaffold(
        title = stringResource(R.string.future_trans_list), onBack = { vm.back() },
        actions = { BarTextButton(R.string.export) { vm.go(Route.Export(ExportTarget.TransList)) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item { HintLine(R.string.table_hint, Modifier.padding(bottom = 8.dp)) }
            item(key = "h-book") { SectionHeader(R.string.heading_book, R.string.add_plus, { vm.go(Route.PlannerBookForm(PlannerList.TRANS, null)) }, Modifier.padding(bottom = 8.dp)) }
            if (books.isEmpty()) item(key = "e-book") { EmptyHint(R.string.empty_records) }
            else recordTable(
                "bk", bookColumns(d),
                books.mapIndexed { i, b -> TableRowData(b.id, listOf((i + 1).toString()) + d.plannerBookFields(b).map { it.second }) },
                scroll,
                onEdit = { vm.go(Route.PlannerBookForm(PlannerList.TRANS, it)) },
                onDelete = { id -> pending = PendingDelete(R.string.delete_record_msg) { vm.launch { vm.repo.deletePlannerBook(id) } } },
            )
        }
    }
    pending?.let { p -> ConfirmDeleteDialog(p.message, onConfirm = { p.action(); pending = null }, onDismiss = { pending = null }) }
}
