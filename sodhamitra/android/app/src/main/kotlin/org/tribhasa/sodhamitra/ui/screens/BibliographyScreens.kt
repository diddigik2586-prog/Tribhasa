package org.tribhasa.sodhamitra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.BibArticle
import org.tribhasa.sodhamitra.data.BibBook
import org.tribhasa.sodhamitra.data.CitationRow
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.SourceType
import org.tribhasa.sodhamitra.domain.Display
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.Route
import org.tribhasa.sodhamitra.ui.common.AppScaffold
import org.tribhasa.sodhamitra.ui.common.ConfirmDeleteDialog
import org.tribhasa.sodhamitra.ui.common.EmptyHint
import org.tribhasa.sodhamitra.ui.common.HintLine
import org.tribhasa.sodhamitra.ui.common.LongPressCard
import org.tribhasa.sodhamitra.ui.common.MenuAction
import org.tribhasa.sodhamitra.ui.common.SectionHeader
import org.tribhasa.sodhamitra.ui.common.Txt

/** A list entry drawn as a card: bold first line, then "Label: value" lines for the filled fields. */
@Composable
fun RecordCard(lines: List<Pair<String, String>>, onEdit: () -> Unit, onDelete: () -> Unit, extra: List<Pair<String, String>> = emptyList()) {
    val all = lines + extra
    LongPressCard(
        onClick = onEdit,
        actions = listOf(MenuAction(R.string.edit, onClick = onEdit), MenuAction(R.string.delete, destructive = true, onClick = onDelete)),
    ) {
        all.firstOrNull()?.let { Txt(it.second, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) }
        for ((label, value) in all.drop(1)) Txt("$label: $value", style = MaterialTheme.typography.bodyMedium)
    }
}

// ---- PhD Research -> Bibliography: Books, MSS / Manuscripts, Articles -----------------------------------------------

@Composable
fun BibliographyScreen(vm: AppViewModel, projectId: Long) {
    val d = Display(LocalContext.current)
    val books by vm.repo.bibBooks(projectId).collectAsState(initial = emptyList())
    val mss by vm.repo.manuscripts(MsScope.BIB, projectId).collectAsState(initial = emptyList())
    val articles by vm.repo.bibArticles(projectId).collectAsState(initial = emptyList())
    var pending by remember { mutableStateOf<PendingDelete?>(null) }

    AppScaffold(title = stringResource(R.string.bibliography), onBack = { vm.back() }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { HintLine(R.string.hint_longpress) }

            item(key = "h-books") { SectionHeader(R.string.books, R.string.add_plus, { vm.go(Route.BibBookForm(projectId, null)) }) }
            if (books.isEmpty()) item(key = "e-books") { EmptyHint(R.string.empty_books) }
            items(books, key = { "b${it.id}" }) { b ->
                RecordCard(d.bookFields(b), { vm.go(Route.BibBookForm(projectId, b.id)) },
                    { pending = PendingDelete(R.string.delete_record_msg) { vm.launch { vm.repo.deleteBibBook(b.id) } } })
            }

            item(key = "h-mss") { SectionHeader(R.string.mss_manuscripts, R.string.add_plus, { vm.go(Route.ManuscriptForm(MsScope.BIB, projectId, null)) }) }
            if (mss.isEmpty()) item(key = "e-mss") { EmptyHint(R.string.empty_mss) }
            items(mss, key = { "m${it.id}" }) { m ->
                RecordCard(d.manuscriptFields(m, onlyFilled = true).let { fields ->
                    // the title leads the card; the remaining filled fields follow in the approved order
                    val title = fields.first { it.first == d.s(R.string.ms_title) }
                    listOf(title) + fields.filter { it != title }
                }, { vm.go(Route.ManuscriptForm(MsScope.BIB, projectId, m.id)) },
                    { pending = PendingDelete(R.string.delete_record_msg) { vm.launch { vm.repo.deleteManuscript(m.id) } } })
            }

            item(key = "h-art") { SectionHeader(R.string.articles, R.string.add_plus, { vm.go(Route.BibArticleForm(projectId, null)) }) }
            if (articles.isEmpty()) item(key = "e-art") { EmptyHint(R.string.empty_articles) }
            items(articles, key = { "a${it.id}" }) { a ->
                RecordCard(d.articleFields(a), { vm.go(Route.BibArticleForm(projectId, a.id)) },
                    { pending = PendingDelete(R.string.delete_record_msg) { vm.launch { vm.repo.deleteBibArticle(a.id) } } })
            }
        }
    }
    pending?.let { p -> ConfirmDeleteDialog(p.message, onConfirm = { p.action(); pending = null }, onDismiss = { pending = null }) }
}

// ---- Individual Researcher / Author -> Research Notes (notes + References / Citations) -----------------------------

@Composable
fun NotesScreen(vm: AppViewModel, projectId: Long) {
    val d = Display(LocalContext.current)
    val notes by vm.repo.notes(projectId).collectAsState(initial = emptyList())
    val citations by vm.repo.citationRows(projectId).collectAsState(initial = emptyList())
    val expanded = rememberExpanded()
    var pending by remember { mutableStateOf<PendingDelete?>(null) }

    AppScaffold(title = stringResource(R.string.research_notes), onBack = { vm.back() }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { HintLine(R.string.hint_longpress) }

            // -- the notes, each with its own description
            item(key = "h-notes") { SectionHeader(R.string.research_notes, R.string.add_note, { vm.go(Route.NoteEditor(projectId, null)) }) }
            if (notes.isEmpty()) item(key = "e-notes") { EmptyHint(R.string.empty_notes) }
            items(notes, key = { "n${it.id}" }) { n ->
                ExpandableItem(
                    title = n.title, expanded = n.id in expanded,
                    onToggle = { if (n.id in expanded) expanded.remove(n.id) else expanded.add(n.id) },
                    description = vm.repo.noteDescription(n.id),
                    onEdit = { vm.go(Route.NoteEditor(projectId, n.id)) },
                    onDelete = { pending = PendingDelete(R.string.delete_note_msg) { vm.launch { vm.repo.deleteNote(n.id) } } },
                )
            }

            // -- References / Citations, nested inside Research Notes
            item(key = "h-refs") {
                Txt(R.string.references_citations, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            citationSection(vm, projectId, d, SourceType.BOOK, R.string.books, R.string.empty_books, citations.filter { it.citation.sourceType == SourceType.BOOK }) { pending = it }
            citationSection(vm, projectId, d, SourceType.MSS, R.string.mss_manuscripts, R.string.empty_mss, citations.filter { it.citation.sourceType == SourceType.MSS }) { pending = it }
            citationSection(vm, projectId, d, SourceType.ARTICLE, R.string.articles, R.string.empty_articles, citations.filter { it.citation.sourceType == SourceType.ARTICLE }) { pending = it }
        }
    }
    pending?.let { p -> ConfirmDeleteDialog(p.message, onConfirm = { p.action(); pending = null }, onDismiss = { pending = null }) }
}

private fun LazyListScope.citationSection(
    vm: AppViewModel, projectId: Long, d: Display, type: String, heading: Int, empty: Int,
    rows: List<CitationRow>, onDelete: (PendingDelete) -> Unit,
) {
    item(key = "h-c-$type") { SectionHeader(heading, R.string.add_plus, { vm.go(Route.CitationForm(projectId, type, null)) }) }
    if (rows.isEmpty()) item(key = "e-c-$type") { EmptyHint(empty) }
    items(rows, key = { "c${it.citation.id}" }) { r ->
        val source: List<Pair<String, String>> = when {
            r.book != null -> d.bookFields(r.book)
            r.manuscript != null -> d.manuscriptFields(r.manuscript, onlyFilled = true).let { f ->
                val t = f.first { it.first == d.s(R.string.ms_title) }; listOf(t) + f.filter { it != t }
            }
            r.article != null -> d.articleFields(r.article)
            else -> emptyList()
        }
        val cite = listOf(d.s(R.string.citation_page_folio) to r.citation.pageFolio, d.s(R.string.citation_note) to r.citation.contextNote)
            .filter { it.second.isNotBlank() }
        RecordCard(source, { vm.go(Route.CitationForm(projectId, type, r.citation.id)) },
            { onDelete(PendingDelete(R.string.delete_citation_msg) { vm.launch { vm.repo.deleteCitation(r.citation.id) } }) }, extra = cite)
    }
}
