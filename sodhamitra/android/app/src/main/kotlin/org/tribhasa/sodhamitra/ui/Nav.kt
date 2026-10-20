package org.tribhasa.sodhamitra.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.tribhasa.sodhamitra.data.Repository

/** What a PDF export covers. The page orientation follows from the target (portrait, or landscape for the planner). */
sealed interface ExportTarget {
    data class Phd(val projectId: Long) : ExportTarget
    data class Book(val projectId: Long) : ExportTarget
    data object EditingList : ExportTarget
    data object TransList : ExportTarget
}

sealed interface Route {
    data object Splash : Route
    data object Home : Route
    data object Settings : Route
    data object Profile : Route
    data object Language : Route
    data object AboutApp : Route
    data object AboutDvk : Route
    data object Support : Route

    data class Projects(val kind: String) : Route
    data class ProjectForm(val kind: String, val id: Long?) : Route
    data class ChapterForm(val projectId: Long, val id: Long?) : Route
    data class Project(val id: Long) : Route
    data class Outline(val projectId: Long) : Route
    data class Chapter(val projectId: Long, val chapterId: Long) : Route
    data class PointEditor(val chapterId: Long, val pointId: Long?) : Route
    data class Bibliography(val projectId: Long) : Route
    data class BibBookForm(val projectId: Long, val id: Long?) : Route
    data class BibArticleForm(val projectId: Long, val id: Long?) : Route
    data class ManuscriptForm(val scope: String, val projectId: Long?, val id: Long?) : Route
    data class Notes(val projectId: Long) : Route
    data class NoteEditor(val projectId: Long, val noteId: Long?) : Route
    data class CitationForm(val projectId: Long, val type: String, val citationId: Long?) : Route

    data object Planner : Route
    data object EditingList : Route
    data object TransList : Route
    data class PlannerBookForm(val list: String, val id: Long?) : Route

    data class Export(val target: ExportTarget) : Route
}

/** A stack entry: the same route can appear twice, and each entry keeps its own screen state. */
data class Entry(val id: Long, val route: Route)

/** Holds the navigation stack and the repository; survives language changes (which recreate the activity). */
class AppViewModel(val repo: Repository) : ViewModel() {

    private var nextId = 0L
    val stack = mutableStateListOf(Entry(nextId++, Route.Splash))

    val top: Entry get() = stack.last()

    fun go(route: Route) { stack.add(Entry(nextId++, route)) }
    fun back(): Boolean { if (stack.size > 1) { stack.removeAt(stack.lastIndex); return true }; return false }
    fun replaceTop(route: Route) { stack[stack.lastIndex] = Entry(nextId++, route) }
    /** back to the entry that is the home screen, dropping everything above it */
    fun toHome() { while (stack.size > 1) stack.removeAt(stack.lastIndex); replaceTop(Route.Home) }

    /** runs a database write to completion even if the screen that started it has already been left */
    fun launch(block: suspend () -> Unit) { viewModelScope.launch(Dispatchers.Default) { block() } }

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<Int> = _messages
    fun say(@StringRes message: Int) { _messages.tryEmit(message) }
}
