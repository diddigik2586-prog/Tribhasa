package org.tribhasa.sodhamitra

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.layout.fillMaxSize
import org.tribhasa.sodhamitra.i18n.LanguageStore
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.Route
import org.tribhasa.sodhamitra.ui.common.ProvideLocal
import org.tribhasa.sodhamitra.ui.screens.AboutAppScreen
import org.tribhasa.sodhamitra.ui.screens.AboutDvkScreen
import org.tribhasa.sodhamitra.ui.screens.BibArticleFormScreen
import org.tribhasa.sodhamitra.ui.screens.BibBookFormScreen
import org.tribhasa.sodhamitra.ui.screens.BibliographyScreen
import org.tribhasa.sodhamitra.ui.screens.ChapterFormScreen
import org.tribhasa.sodhamitra.ui.screens.ProjectFormScreen
import org.tribhasa.sodhamitra.ui.screens.ChapterScreen
import org.tribhasa.sodhamitra.ui.screens.CitationFormScreen
import org.tribhasa.sodhamitra.ui.screens.EditingListScreen
import org.tribhasa.sodhamitra.ui.screens.ExportScreen
import org.tribhasa.sodhamitra.ui.screens.HomeScreen
import org.tribhasa.sodhamitra.ui.screens.LanguageScreen
import org.tribhasa.sodhamitra.ui.screens.ManuscriptFormScreen
import org.tribhasa.sodhamitra.ui.screens.NoteEditorScreen
import org.tribhasa.sodhamitra.ui.screens.NotesScreen
import org.tribhasa.sodhamitra.ui.screens.OutlineScreen
import org.tribhasa.sodhamitra.ui.screens.PlannerBookFormScreen
import org.tribhasa.sodhamitra.ui.screens.PlannerScreen
import org.tribhasa.sodhamitra.ui.screens.PointEditorScreen
import org.tribhasa.sodhamitra.ui.screens.ProfileScreen
import org.tribhasa.sodhamitra.ui.screens.ProjectScreen
import org.tribhasa.sodhamitra.ui.screens.ProjectsScreen
import org.tribhasa.sodhamitra.ui.screens.SettingsScreen
import org.tribhasa.sodhamitra.ui.screens.SplashScreen
import org.tribhasa.sodhamitra.ui.screens.SupportScreen
import org.tribhasa.sodhamitra.ui.screens.TransListScreen
import org.tribhasa.sodhamitra.ui.theme.SodhamitraTheme

class MainActivity : ComponentActivity() {
    // The repository is handed in explicitly (no hidden global factory state).
    private val vm: AppViewModel by viewModels {
        viewModelFactory { initializer { AppViewModel((application as SodhamitraApp).repository) } }
    }

    /** All strings are bundled in the app; the chosen interface language is applied here. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge with the status bar kept visible: its icons are light because a maroon bar is always drawn behind them;
        // the navigation bar's icons are dark by default and are switched to light on the screens that end in the maroon footer (see Root).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { SodhamitraTheme { Root(vm) } }
    }
}

@Composable
fun Root(vm: AppViewModel) {
    val context = LocalContext.current
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(context.getString(it)) } }

    // The Hero page and the Homepage end in the maroon footer, which reaches under the navigation bar: light icons there.
    val view = LocalView.current
    val activity = LocalActivity.current
    val onMaroonFooter = vm.top.route.let { it == Route.Splash || it == Route.Home }
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = !onMaroonFooter
        }
    }
    ProvideLocal(snack) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { AppNav(vm) }
    }
}

@Composable
fun AppNav(vm: AppViewModel) {
    val holder = rememberSaveableStateHolder()
    BackHandler(enabled = vm.stack.size > 1) { vm.back() }
    val entry = vm.top
    holder.SaveableStateProvider(entry.id) {
        when (val r = entry.route) {
            Route.Splash -> SplashScreen { vm.replaceTop(Route.Home) }
            Route.Home -> HomeScreen(vm)
            Route.Settings -> SettingsScreen(vm)
            Route.Profile -> ProfileScreen(vm)
            Route.Language -> LanguageScreen(vm)
            Route.AboutApp -> AboutAppScreen(vm)
            Route.AboutDvk -> AboutDvkScreen(vm)
            Route.Support -> SupportScreen(vm)
            is Route.Projects -> ProjectsScreen(vm, r.kind)
            is Route.ProjectForm -> ProjectFormScreen(vm, r.kind, r.id)
            is Route.ChapterForm -> ChapterFormScreen(vm, r.projectId, r.id)
            is Route.Project -> ProjectScreen(vm, r.id)
            is Route.Outline -> OutlineScreen(vm, r.projectId)
            is Route.Chapter -> ChapterScreen(vm, r.projectId, r.chapterId)
            is Route.PointEditor -> PointEditorScreen(vm, r.chapterId, r.pointId)
            is Route.Bibliography -> BibliographyScreen(vm, r.projectId)
            is Route.BibBookForm -> BibBookFormScreen(vm, r.projectId, r.id)
            is Route.BibArticleForm -> BibArticleFormScreen(vm, r.projectId, r.id)
            is Route.ManuscriptForm -> ManuscriptFormScreen(vm, r.scope, r.projectId, r.id)
            is Route.Notes -> NotesScreen(vm, r.projectId)
            is Route.NoteEditor -> NoteEditorScreen(vm, r.projectId, r.noteId)
            is Route.CitationForm -> CitationFormScreen(vm, r.projectId, r.type, r.citationId)
            Route.Planner -> PlannerScreen(vm)
            Route.EditingList -> EditingListScreen(vm)
            Route.TransList -> TransListScreen(vm)
            is Route.PlannerBookForm -> PlannerBookFormScreen(vm, r.list, r.id)
            is Route.Export -> ExportScreen(vm, r.target)
        }
    }
}
