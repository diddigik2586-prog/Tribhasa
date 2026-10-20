package org.tribhasa.sodhamitra.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TextButton
import androidx.compose.material3.Snackbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.selection.SelectionContainer
import kotlinx.coroutines.delay
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.ProjectKind
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.Route
import org.tribhasa.sodhamitra.ui.common.AppScaffold
import org.tribhasa.sodhamitra.ui.common.LocalSnack
import org.tribhasa.sodhamitra.ui.common.Txt
import org.tribhasa.sodhamitra.util.Clip
import org.tribhasa.sodhamitra.util.Feedback

/** The genuine Tribhāṣā logo, "Śodhamitra", "Research Planner" and the copyright line, in the approved order. */
@Composable
fun BrandBlock(logoSize: androidx.compose.ui.unit.Dp = 132.dp) {
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

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { delay(1400); onDone() }
    Box(Modifier.fillMaxSize().clickable { onDone() }.padding(24.dp), contentAlignment = Alignment.Center) {
        BrandBlock(logoSize = 168.dp)
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    Scaffold(snackbarHost = { SnackbarHost(LocalSnack.current) { Snackbar { Txt(it.visuals.message) } } }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BrandBlock()
            Spacer(Modifier.height(8.dp))
            HomeSection(R.string.home_phd) { vm.go(Route.Projects(ProjectKind.PHD)) }
            HomeSection(R.string.home_individual) { vm.go(Route.Projects(ProjectKind.BOOK)) }
            HomeSection(R.string.home_planner) { vm.go(Route.Planner) }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = { vm.go(Route.Settings) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Txt(R.string.settings_title, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HomeSection(label: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Txt(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var noMail by remember { mutableStateOf(false) }
    val snack = LocalSnack.current

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

    if (noMail) {
        val copied = stringResource(R.string.feedback_email_copied)
        var message by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(message) { message?.let { snack.showSnackbar(it) } }
        AlertDialog(
            onDismissRequest = { noMail = false },
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
            dismissButton = { TextButton(onClick = { noMail = false }) { Txt(R.string.close) } },
        )
    }
}

@Composable
private fun SettingsRow(label: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
