package org.tribhasa.sodhamitra.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.BibArticle
import org.tribhasa.sodhamitra.data.BibBook
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.SourceType
import org.tribhasa.sodhamitra.domain.FieldSpec
import org.tribhasa.sodhamitra.domain.Forms
import org.tribhasa.sodhamitra.domain.toBibArticle
import org.tribhasa.sodhamitra.domain.toBibBook
import org.tribhasa.sodhamitra.domain.toForm
import org.tribhasa.sodhamitra.domain.toManuscript
import org.tribhasa.sodhamitra.domain.toPlannerBook
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.common.FormScreen
import org.tribhasa.sodhamitra.ui.common.Txt

/**
 * Loads the record being edited (if any) and then shows the form. [load] returns null for a new record.
 * The form is shown only once loaded, so the typed values can never be overwritten by a late load.
 */
@Composable
private fun RecordForm(
    vm: AppViewModel, title: String, spec: List<FieldSpec>, isEdit: Boolean,
    load: suspend () -> Map<String, String>?, save: suspend (Map<String, String>) -> Unit,
    topContent: @Composable androidx.compose.foundation.layout.ColumnScope.(SnapshotStateMap<String, String>) -> Unit = {},
) {
    val initial by produceState<Map<String, String>?>(null) { value = if (isEdit) load() ?: emptyMap() else emptyMap() }
    initial?.let { init ->
        FormScreen(
            title = title, spec = spec, initial = init, isEdit = isEdit, onCancel = { vm.back() },
            onSave = { values -> vm.launch { save(values) }; vm.back() },
            topContent = topContent,
        )
    }
}

@Composable
fun ProjectFormScreen(vm: AppViewModel, kind: String, id: Long?) {
    val phd = kind == org.tribhasa.sodhamitra.data.ProjectKind.PHD
    RecordForm(
        vm, stringResource(if (phd) (if (id == null) R.string.new_phd_project else R.string.edit_phd_project)
                           else (if (id == null) R.string.new_book_project else R.string.edit_book_project)),
        if (phd) Forms.researchTitle else Forms.bookTitle, id != null,
        load = { vm.repo.dao.projectOnce(id!!)?.let { mapOf("title" to it.title) } },
        save = { v -> vm.repo.saveProject(kind, id, v["title"].orEmpty()) },
    )
}

@Composable
fun ChapterFormScreen(vm: AppViewModel, projectId: Long, id: Long?) {
    RecordForm(
        vm, stringResource(if (id == null) R.string.add_chapter_title else R.string.edit_chapter_title), Forms.chapterTitle, id != null,
        load = { vm.repo.dao.chaptersOnce(projectId).firstOrNull { it.id == id }?.let { mapOf("title" to it.title) } },
        save = { v -> vm.repo.saveChapter(projectId, id, v["title"].orEmpty()) },
    )
}

@Composable
fun ManuscriptFormScreen(vm: AppViewModel, scope: String, projectId: Long?, id: Long?) {
    RecordForm(
        vm, stringResource(if (id == null) R.string.add_manuscript else R.string.edit_manuscript),
        Forms.manuscriptForm(withPlanning = scope == MsScope.EDIT), id != null,
        load = { vm.repo.manuscript(id!!)?.toForm() },
        save = { v -> vm.repo.saveManuscript(v.toManuscript(scope, projectId, id ?: 0)) },
    )
}

@Composable
fun BibBookFormScreen(vm: AppViewModel, projectId: Long, id: Long?) {
    RecordForm(
        vm, stringResource(if (id == null) R.string.add_book else R.string.edit_book), Forms.bibBook, id != null,
        load = { vm.repo.bibBook(id!!)?.toForm() },
        save = { v -> vm.repo.saveBibBook(v.toBibBook(projectId, id ?: 0)) },
    )
}

@Composable
fun BibArticleFormScreen(vm: AppViewModel, projectId: Long, id: Long?) {
    RecordForm(
        vm, stringResource(if (id == null) R.string.add_article else R.string.edit_article), Forms.bibArticle, id != null,
        load = { vm.repo.bibArticle(id!!)?.toForm() },
        save = { v -> vm.repo.saveBibArticle(v.toBibArticle(projectId, id ?: 0)) },
    )
}

@Composable
fun PlannerBookFormScreen(vm: AppViewModel, list: String, id: Long?) {
    RecordForm(
        vm, stringResource(if (id == null) R.string.add_book else R.string.edit_book), Forms.plannerBook, id != null,
        load = { vm.repo.plannerBook(id!!)?.toForm() },
        save = { v -> vm.repo.savePlannerBook(v.toPlannerBook(list, id ?: 0)) },
    )
}

@Composable
fun PointEditorScreen(vm: AppViewModel, chapterId: Long, pointId: Long?) {
    RecordForm(
        vm, stringResource(if (pointId == null) R.string.add_point_title else R.string.edit_point_title), Forms.point, pointId != null,
        load = { vm.repo.point(pointId!!)?.let { mapOf("title" to it.title, "description" to vm.repo.pointDescriptionOnce(it.id)) } },
        save = { v -> vm.repo.savePoint(chapterId, pointId, v["title"].orEmpty(), v["description"].orEmpty()) },
    )
}

@Composable
fun NoteEditorScreen(vm: AppViewModel, projectId: Long, noteId: Long?) {
    RecordForm(
        vm, stringResource(if (noteId == null) R.string.add_note_title else R.string.edit_note_title), Forms.note, noteId != null,
        load = { vm.repo.note(noteId!!)?.let { mapOf("title" to it.title, "description" to vm.repo.noteDescriptionOnce(it.id)) } },
        save = { v -> vm.repo.saveNote(projectId, noteId, v["title"].orEmpty(), v["description"].orEmpty()) },
    )
}

// ---- citations: a source (shared, stored once) + page / folio + contextual note -----------------------------------

private class SourceOption(val id: Long, val label: String, val form: Map<String, String>)

@Composable
fun CitationFormScreen(vm: AppViewModel, projectId: Long, type: String, citationId: Long?) {
    val sourceSpec: List<FieldSpec> = when (type) {
        SourceType.BOOK -> Forms.bibBook
        SourceType.MSS -> Forms.manuscript
        else -> Forms.bibArticle
    }
    val spec = sourceSpec + Forms.citationExtra

    class Loaded(val options: List<SourceOption>, val selected: Long?, val initial: Map<String, String>, val useCount: Int)

    val loaded by produceState<Loaded?>(null) {
        val options: List<SourceOption> = when (type) {
            SourceType.BOOK -> vm.repo.dao.bibBooksOnce(projectId).map { SourceOption(it.id, it.title, it.toForm()) }
            SourceType.MSS -> vm.repo.dao.manuscriptsOnce(MsScope.CIT, projectId).map { SourceOption(it.id, it.title, it.toForm()) }
            else -> vm.repo.dao.bibArticlesOnce(projectId).map { SourceOption(it.id, it.title, it.toForm()) }
        }
        val cit = citationId?.let { vm.repo.citation(it) }
        val opt = cit?.let { c -> options.firstOrNull { it.id == c.sourceId } }
        value = Loaded(
            options, opt?.id,
            (opt?.form ?: emptyMap()) + mapOf("pageFolio" to cit?.pageFolio.orEmpty(), "contextNote" to cit?.contextNote.orEmpty()),
            if (cit != null) vm.repo.citationUseCount(type, cit.sourceId) else 0,
        )
    }

    loaded?.let { l ->
        var selected by remember { mutableStateOf(l.selected) }
        FormScreen(
            title = stringResource(if (citationId == null) R.string.add_citation else R.string.edit_citation),
            spec = spec, initial = l.initial, isEdit = citationId != null, onCancel = { vm.back() },
            onSave = { v ->
                vm.launch {
                    vm.repo.saveCitation(
                        projectId, type, citationId, selected,
                        book = if (type == SourceType.BOOK) v.toBibBook(projectId) else null,
                        manuscript = if (type == SourceType.MSS) v.toManuscript(MsScope.CIT, projectId) else null,
                        article = if (type == SourceType.ARTICLE) v.toBibArticle(projectId) else null,
                        pageFolio = v["pageFolio"].orEmpty(), contextNote = v["contextNote"].orEmpty(),
                    )
                }
                vm.back()
            },
            topContent = { values ->
                if (l.options.isNotEmpty()) SourcePicker(l.options, selected) { opt ->
                    selected = opt?.id
                    // choosing a stored source fills the source fields; "new source" empties them (page / note are kept)
                    for (f in sourceSpec) values[f.key] = opt?.form?.get(f.key).orEmpty()
                }
                if (selected != null && l.useCount > 1 && selected == l.selected)
                    Txt(stringResource(R.string.citation_shared, l.useCount), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
        )
    }
}

@Composable
private fun SourcePicker(options: List<SourceOption>, selected: Long?, onPick: (SourceOption?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Txt(R.string.citation_source, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Txt(options.firstOrNull { it.id == selected }?.label ?: stringResource(R.string.citation_new_source))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Txt(R.string.citation_new_source, fontWeight = FontWeight.Medium) }, onClick = { open = false; onPick(null) })
            for (o in options) DropdownMenuItem(text = { Txt(o.label) }, onClick = { open = false; onPick(o) })
        }
    }
}
