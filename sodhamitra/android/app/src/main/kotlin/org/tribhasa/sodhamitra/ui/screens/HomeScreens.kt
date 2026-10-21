package org.tribhasa.sodhamitra.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.ProjectKind
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.Route
import org.tribhasa.sodhamitra.ui.common.AppScaffold
import org.tribhasa.sodhamitra.ui.common.CreatorFooter
import org.tribhasa.sodhamitra.ui.common.LocalSnack
import org.tribhasa.sodhamitra.ui.common.Txt
import org.tribhasa.sodhamitra.util.Clip
import org.tribhasa.sodhamitra.util.Feedback
import kotlin.math.abs

/** The genuine Tribhāṣā logo, "Śodhamitra", "Research Planner" and the copyright line, in the approved order. */
@Composable
fun BrandBlock(logoSize: Dp = 132.dp) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painterResource(R.drawable.tribhasa_logo), contentDescription = "Tribhāṣā",
            modifier = Modifier.size(logoSize), contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(12.dp))
        Txt(R.string.brand_name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Txt(R.string.brand_tagline, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Txt(R.string.copyright_line, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/**
 * The slim maroon bar of the Hero (splash) page and the Homepage. On the Homepage it carries the hamburger menu at the right;
 * on the Hero page [onMenu] is null, so the two pages look the same except for the menu icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onMenu: (() -> Unit)?) {
    TopAppBar(
        title = {},
        actions = {
            if (onMenu != null) IconButton(onClick = onMenu) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu_open))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

/** The Hero page: the brand block between the maroon bar and the creator footer. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { delay(1400); onDone() }
    Column(Modifier.fillMaxSize()) {
        HomeTopBar(onMenu = null)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clickable { onDone() }.padding(24.dp), contentAlignment = Alignment.Center) {
            // the logo takes what is left after the three text lines, so that the whole brand block also fits a landscape phone
            val logo = (maxHeight - 150.dp).coerceIn(48.dp, 168.dp)
            BrandBlock(logoSize = logo)
        }
        CreatorFooter()
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var noMail by remember { mutableStateOf(false) }
    val context = LocalContext.current
    BackHandler(enabled = drawerOpen) { drawerOpen = false }   // Back closes the menu before anything else

    fun go(route: Route) { drawerOpen = false; vm.go(route) }

    // The right-edge swipe lives on the Homepage only: no other screen composes it.
    val rightInset = WindowInsets.safeDrawing.getRight(LocalDensity.current, LocalLayoutDirection.current)
    Box(Modifier.fillMaxSize().rightEdgeSwipe(enabled = !drawerOpen, rightInsetPx = rightInset) { drawerOpen = true }) {
        Scaffold(
            topBar = { HomeTopBar(onMenu = { drawerOpen = true }) },
            bottomBar = { CreatorFooter() },
            snackbarHost = { SnackbarHost(LocalSnack.current) { Snackbar { Txt(it.visuals.message) } } },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BrandBlock()
                Spacer(Modifier.height(8.dp))
                HomeCard(R.string.home_phd_plan) { go(Route.Projects(ProjectKind.PHD)) }
                HomeCard(R.string.home_individual) { go(Route.Projects(ProjectKind.BOOK)) }
                HomeCard(R.string.home_planner) { go(Route.Planner) }
            }
        }
        // A transparent, non-interactive marker that asks Android not to treat a swipe from this part of the right edge as "Back".
        Box(Modifier.align(Alignment.CenterEnd).width(EDGE_ZONE).height(200.dp).systemGestureExclusion())

        RightDrawer(open = drawerOpen, onClose = { drawerOpen = false }) {
            DrawerItem(R.string.home_phd_plan) { go(Route.Projects(ProjectKind.PHD)) }
            DrawerItem(R.string.home_individual) { go(Route.Projects(ProjectKind.BOOK)) }
            DrawerItem(R.string.home_planner) { go(Route.Planner) }
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            DrawerItem(R.string.settings_profile) { go(Route.Profile) }
            DrawerItem(R.string.settings_language) { go(Route.Language) }
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            DrawerItem(R.string.settings_about_app) { go(Route.AboutApp) }
            DrawerItem(R.string.settings_about_dvk) { go(Route.AboutDvk) }
            DrawerItem(R.string.settings_support) { go(Route.Support) }
            DrawerItem(R.string.settings_feedback) { drawerOpen = false; if (!Feedback.open(context)) noMail = true }
        }
    }
    if (noMail) FeedbackNoAppDialog { noMail = false }
}

// ---- Homepage feature cards -------------------------------------------------------------------------------------------

/** Moderate corner rounding of the Homepage cards (a pill would be half the card height, i.e. 52 dp or more). */
internal val HomeCardCorner = 16.dp

/** A large rectangular feature card (moderate corner rounding, never a pill): maroon, ivory text, a right-facing arrow. */
@Composable
private fun HomeCard(label: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
        shape = RoundedCornerShape(HomeCardCorner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Txt(label, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ---- right-side navigation drawer ---------------------------------------------------------------------------------------

private val EDGE_ZONE = 28.dp

/**
 * A navigation drawer that slides in from the RIGHT edge. It closes with its close control, a tap on the dimmed area outside it,
 * or Back (the caller registers the BackHandler).
 */
@Composable
private fun BoxScope.RightDrawer(open: Boolean, onClose: () -> Unit, items: @Composable () -> Unit) {
    val menuTitle = stringResource(R.string.menu_title)
    val closeLabel = stringResource(R.string.menu_close)
    AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).testTag("menu-scrim")
                .semantics { contentDescription = closeLabel; onClick(label = closeLabel) { onClose(); true } }
                .pointerInput(Unit) { detectTapGestures { onClose() } },
        )
    }
    AnimatedVisibility(
        visible = open, modifier = Modifier.align(Alignment.CenterEnd),
        enter = slideInHorizontally { it }, exit = slideOutHorizontally { it },
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight().widthIn(max = 320.dp).fillMaxWidth(0.84f).testTag("menu-panel")
                .semantics { paneTitle = menuTitle }
                .pointerInput(Unit) { detectTapGestures { } },   // taps inside the panel never reach the dimmed area
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                // Maroon where the panel runs under the status bar and the navigation bar: their (light) icons stay readable
                // on the ivory panel exactly as they are on the maroon bar and footer of the page behind it.
                Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))) {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(R.string.menu_title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = onClose, modifier = Modifier.testTag("menu-close")) { Icon(Icons.Filled.Close, contentDescription = closeLabel) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) { items() }
                }
                Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
private fun DrawerItem(label: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Opens [onOpen] when a finger that went down within [EDGE_ZONE] of the right edge moves clearly towards the left. The pointer
 * events are only observed (never consumed) until the gesture is recognised, so scrolling, taps, buttons and text fields keep
 * working; a mostly vertical movement cancels the recognition.
 */
private fun Modifier.rightEdgeSwipe(enabled: Boolean, rightInsetPx: Int, onOpen: () -> Unit): Modifier = pointerInput(enabled, rightInsetPx) {
    if (!enabled) return@pointerInput
    val edgePx = EDGE_ZONE.toPx()
    val openDistance = 40.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.position.x < size.width - rightInsetPx - edgePx) return@awaitEachGesture   // the edge of the usable area, not of the cut-out / side bar
        var dx = 0f
        var dy = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val move = change.positionChange()
            dx += move.x; dy += move.y
            if (-dx >= openDistance && -dx > abs(dy) * 1.5f) { change.consume(); onOpen(); break }
            if (abs(dy) > viewConfiguration.touchSlop && abs(dy) > abs(dx)) break   // a vertical scroll, not our gesture
        }
    }
}

// ---- Settings -----------------------------------------------------------------------------------------------------------

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var noMail by remember { mutableStateOf(false) }

    AppScaffold(title = stringResource(R.string.settings_title), onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // The six options, in the approved order.
                SettingsRow(R.string.settings_profile) { vm.go(Route.Profile) }
                SettingsRow(R.string.settings_language) { vm.go(Route.Language) }
                SettingsRow(R.string.settings_about_app) { vm.go(Route.AboutApp) }
                SettingsRow(R.string.settings_about_dvk) { vm.go(Route.AboutDvk) }
                SettingsRow(R.string.settings_support) { vm.go(Route.Support) }
                SettingsRow(R.string.settings_feedback) { if (!Feedback.open(context)) noMail = true }
            }
            Txt(R.string.copyright_line, Modifier.fillMaxWidth().padding(vertical = 14.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }

    if (noMail) FeedbackNoAppDialog { noMail = false }
}

/** Shown when the device has no email app: the address can still be copied and used from any other mail service. */
@Composable
private fun FeedbackNoAppDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val snack = LocalSnack.current
    val copied = stringResource(R.string.feedback_email_copied)
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Txt(R.string.feedback_no_app_title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Txt(R.string.feedback_no_app_msg)
                SelectionContainer { Txt(Feedback.EMAIL, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            TextButton(onClick = { Clip.copy(context, "Email", Feedback.EMAIL); message = copied }) { Txt(R.string.feedback_copy_email) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Txt(R.string.close) } },
    )
}

@Composable
private fun SettingsRow(label: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
