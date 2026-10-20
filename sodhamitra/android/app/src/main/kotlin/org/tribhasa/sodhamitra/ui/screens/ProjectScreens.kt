package org.tribhasa.sodhamitra.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.Chapter
import org.tribhasa.sodhamitra.data.Point
import org.tribhasa.sodhamitra.domain.TextChunks
import org.tribhasa.sodhamitra.data.Project
import org.tribhasa.sodhamitra.data.ProjectKind
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.ExportTarget
import org.tribhasa.sodhamitra.ui.Route
import org.tribhasa.sodhamitra.ui.common.AppScaffold
import org.tribhasa.sodhamitra.ui.common.rememberFlow
import org.tribhasa.sodhamitra.ui.common.ConfirmDeleteDialog
import org.tribhasa.sodhamitra.ui.common.EmptyHint
import org.tribhasa.sodhamitra.ui.common.HeadingText
import org.tribhasa.sodhamitra.ui.common.HintLine
import org.tribhasa.sodhamitra.ui.common.LongPressCard
import org.tribhasa.sodhamitra.ui.common.MenuAction
import org.tribhasa.sodhamitra.ui.common.Txt

/** A confirmation that is pending: which message to show and what to do when the user confirms. */
class PendingDelete(val message: Int, val action: () -> Unit)

@Composable
fun rememberExpanded(): SnapshotStateList<Long> =
    rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) { mutableStateListOf() }

private fun mutableStateListOf(): SnapshotStateList<Long> = androidx.compose.runtime.mutableStateListOf()

// ---- project list (PhD Research, Individual Researcher / Author) -------------------------------------------------

@Composable
fun ProjectsScreen(vm: AppViewModel, kind: String) {
    val projects by rememberFlow(kind) { vm.repo.projects(kind) }.collectAsState(initial = emptyList())
    var pending by remember { mutableStateOf<PendingDelete?>(null) }
    val isPhd = kind == ProjectKind.PHD

    AppScaffold(
        title = stringResource(if (isPhd) R.string.home_phd else R.string.home_individual),
        onBack = { vm.back() },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Button(onClick = { vm.go(Route.ProjectForm(kind, null)) }, modifier = Modifier.fillMaxWidth()) { Txt(R.string.add_project, fontWeight = FontWeight.Medium) } }
            if (projects.isEmpty()) item { EmptyHint(R.string.empty_projects) } else item { HintLine(R.string.hint_longpress_project) }
            items(projects, key = { it.id }) { p ->
                LongPressCard(
                    onClick = { vm.go(Route.Project(p.id)) },
                    actions = listOf(
                        MenuAction(R.string.edit) { vm.go(Route.ProjectForm(kind, p.id)) },
                        MenuAction(R.string.delete, destructive = true) {
                            pending = PendingDelete(R.string.delete_project_msg) { vm.launch { vm.repo.deleteProject(p.id) } }
                        },
                        MenuAction(R.string.export) { vm.go(Route.Export(if (isPhd) ExportTarget.Phd(p.id) else ExportTarget.Book(p.id))) },
                    ),
                ) { Txt(p.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) }
            }
        }
    }

    pending?.let { d -> ConfirmDeleteDialog(d.message, onConfirm = { d.action(); pending = null }, onDismiss = { pending = null }) }
}

// ---- inside one project: its two parts --------------------------------------------------------------------------

@Composable
fun ProjectScreen(vm: AppViewModel, projectId: Long) {
    val project by rememberFlow(projectId) { vm.repo.project(projectId) }.collectAsState(initial = null)
    val p = project
    AppScaffold(title = p?.title ?: "", onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (p != null) {
                val phd = p.kind == ProjectKind.PHD
                BigOption(if (phd) R.string.thesis_outline else R.string.book_outline) { vm.go(Route.Outline(projectId)) }
                BigOption(if (phd) R.string.bibliography else R.string.research_notes) {
                    vm.go(if (phd) Route.Bibliography(projectId) else Route.Notes(projectId))
                }
            }
        }
    }
}

@Composable
fun BigOption(label: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

// ---- Thesis Outline / Book Outline: chapter list -------------------------------------------------------------------

@Composable
fun OutlineScreen(vm: AppViewModel, projectId: Long) {
    val project by rememberFlow(projectId) { vm.repo.project(projectId) }.collectAsState(initial = null)
    val chapters by rememberFlow(projectId) { vm.repo.chapters(projectId) }.collectAsState(initial = emptyList())
    var pending by remember { mutableStateOf<PendingDelete?>(null) }
    val phd = project?.kind != ProjectKind.BOOK

    AppScaffold(title = stringResource(if (phd) R.string.thesis_outline else R.string.book_outline), onBack = { vm.back() }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Button(onClick = { vm.go(Route.ChapterForm(projectId, null)) }, modifier = Modifier.fillMaxWidth()) { Txt(R.string.add_chapter, fontWeight = FontWeight.Medium) } }
            if (chapters.isEmpty()) item { EmptyHint(R.string.empty_chapters) } else item { HintLine(R.string.hint_longpress) }
            items(chapters.size, key = { chapters[it].id }) { i ->
                val c = chapters[i]
                LongPressCard(
                    onClick = { vm.go(Route.Chapter(projectId, c.id)) },
                    actions = listOf(
                        MenuAction(R.string.edit) { vm.go(Route.ChapterForm(projectId, c.id)) },
                        MenuAction(R.string.delete, destructive = true) {
                            pending = PendingDelete(R.string.delete_chapter_msg) { vm.launch { vm.repo.deleteChapter(c.id) } }
                        },
                    ),
                ) { Txt("${i + 1}. ${c.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) }
            }
        }
    }
    pending?.let { d -> ConfirmDeleteDialog(d.message, onConfirm = { d.action(); pending = null }, onDismiss = { pending = null }) }
}

// ---- one chapter: its title, "+ Add Point" and the expandable points ----------------------------------------------

@Composable
fun ChapterScreen(vm: AppViewModel, projectId: Long, chapterId: Long) {
    val chapter by rememberFlow(chapterId) { vm.repo.chapter(chapterId) }.collectAsState(initial = null)
    val points by rememberFlow(chapterId) { vm.repo.points(chapterId) }.collectAsState(initial = emptyList())
    val expanded = rememberExpanded()
    var pending by remember { mutableStateOf<PendingDelete?>(null) }

    AppScaffold(title = chapter?.title ?: "", onBack = { vm.back() }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { HeadingText(chapter?.title.orEmpty()) }
            item { Button(onClick = { vm.go(Route.PointEditor(chapterId, null)) }, modifier = Modifier.fillMaxWidth()) { Txt(R.string.add_point, fontWeight = FontWeight.Medium) } }
            if (points.isEmpty()) item { EmptyHint(R.string.empty_points) } else item { HintLine(R.string.hint_longpress) }
            items(points, key = { it.id }) { pt ->
                ExpandableItem(
                    title = pt.title, expanded = pt.id in expanded,
                    onToggle = { if (pt.id in expanded) expanded.remove(pt.id) else expanded.add(pt.id) },
                    description = rememberFlow(pt.id) { vm.repo.pointDescription(pt.id) },
                    onEdit = { vm.go(Route.PointEditor(chapterId, pt.id)) },
                    onDelete = { pending = PendingDelete(R.string.delete_point_msg) { vm.launch { vm.repo.deletePoint(pt.id) } } },
                )
            }
        }
    }
    pending?.let { d -> ConfirmDeleteDialog(d.message, onConfirm = { d.action(); pending = null }, onDismiss = { pending = null }) }
}

/** A point or a note: collapsed it shows only its title; expanded it reveals its own description (and only that). */
@Composable
fun ExpandableItem(
    title: String, expanded: Boolean, onToggle: () -> Unit, description: Flow<String?>,
    onEdit: () -> Unit, onDelete: () -> Unit,
) {
    LongPressCard(
        onClick = onToggle,
        actions = listOf(MenuAction(R.string.edit, onClick = onEdit), MenuAction(R.string.delete, destructive = true, onClick = onDelete)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand))
        }
        if (expanded) {
            val text by description.collectAsState(initial = null)
            Spacer(Modifier.size(8.dp))
            if (text.isNullOrBlank()) Txt(R.string.no_description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Column {
                // shown paragraph by paragraph, exactly as typed
                for (line in text!!.split('\n')) {
                    if (line.isEmpty()) Spacer(Modifier.size(8.dp))
                    else for (piece in TextChunks.split(line, 4000)) Txt(piece, style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.padding(top = 10.dp)) { Txt(R.string.edit, fontWeight = FontWeight.Medium) }
        }
    }
}
