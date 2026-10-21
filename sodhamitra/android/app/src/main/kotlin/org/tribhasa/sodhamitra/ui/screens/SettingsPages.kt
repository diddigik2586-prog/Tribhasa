package org.tribhasa.sodhamitra.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.tribhasa.sodhamitra.BuildConfig
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.i18n.AppLanguage
import org.tribhasa.sodhamitra.i18n.LanguageStore
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.common.AppScaffold
import org.tribhasa.sodhamitra.ui.common.HeadingText
import org.tribhasa.sodhamitra.ui.common.LocalSnack
import org.tribhasa.sodhamitra.ui.common.Txt
import org.tribhasa.sodhamitra.util.Clip

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

// ---- Language ----------------------------------------------------------------------------------------------

@Composable
fun LanguageScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val current = remember { LanguageStore.get(context) }
    AppScaffold(title = stringResource(R.string.settings_language), onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (lang in AppLanguage.entries) {
                Card(
                    onClick = {
                        if (lang != current) {
                            LanguageStore.set(context, lang)
                            context.activity()?.recreate()   // the screens are redrawn in the new language; nothing else changes
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lang == current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(lang.nativeName, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (lang == current) Icon(Icons.Filled.Check, contentDescription = null)
                    }
                }
            }
            Txt(R.string.language_note, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- About App ---------------------------------------------------------------------------------------------

private val LICENCE_FILES = listOf(
    "UFL-Ubuntu-Font-Licence-1.0.txt", "OFL-NotoSans-Latin.txt", "OFL-NotoSansDevanagari.txt", "OFL-NotoSansBengali.txt",
    "OFL-NotoSansGujarati.txt", "OFL-NotoSansOriya.txt", "OFL-NotoSansTamil.txt", "OFL-NotoSansTelugu.txt",
    "OFL-NotoSansKannada.txt", "OFL-NotoSansMalayalam.txt",
)

@Composable
fun AboutAppScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var showLicences by remember { mutableStateOf(false) }
    AppScaffold(title = stringResource(R.string.settings_about_app), onBack = { vm.back() }, centerTitle = true) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandBlock(logoSize = 96.dp)   // logo, Śodhamitra, Research Planner and the one copyright line
            Txt(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (id in listOf(R.string.about_purpose, R.string.about_sections, R.string.about_projects, R.string.about_notes,
                R.string.about_bibliography, R.string.about_planner, R.string.about_languages, R.string.about_storage, R.string.about_pdf)) {
                Txt(id, Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            HeadingText(stringResource(R.string.about_fonts_title), Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Txt(R.string.about_fonts_note, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            TextButton(onClick = { showLicences = !showLicences }) {
                Txt(if (showLicences) R.string.about_hide_licences else R.string.about_show_licences, fontWeight = FontWeight.Medium)
            }
            if (showLicences) {
                val texts = remember {
                    LICENCE_FILES.mapNotNull { f ->
                        runCatching { f to context.assets.open("licenses/$f").bufferedReader().use { it.readText() } }.getOrNull()
                    }
                }
                for ((file, text) in texts) {
                    Txt(file.removeSuffix(".txt"), Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center)
                    // the licence texts themselves are long formatted legal documents: they keep their conventional left alignment
                    Txt(text, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start)
                }
            }
        }
    }
}

// ---- About Dr. DVK -----------------------------------------------------------------------------------------

/**
 * The author-approved biography, reproduced exactly (the strings dvk_name … dvk_bio_5 and the copyright line are fixed, untranslated
 * resources): heading, one line of roles, five paragraphs, and the copyright notice, all centred.
 */
@Composable
fun AboutDvkScreen(vm: AppViewModel) {
    AppScaffold(title = stringResource(R.string.settings_about_dvk), onBack = { vm.back() }, centerTitle = true) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt(R.string.dvk_name, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Txt(R.string.dvk_roles, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center)
            for (id in listOf(R.string.dvk_bio_1, R.string.dvk_bio_2, R.string.dvk_bio_3, R.string.dvk_bio_4, R.string.dvk_bio_5)) {
                Txt(id, Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(4.dp))
            Txt(R.string.copyright_line, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ---- Support Śodhamitra ------------------------------------------------------------------------------------

/** Payment details exactly as supplied by the author. Bundled in the app; nothing is fetched or invented. */
object PaymentDetails {
    const val UPI_ID = "8970500450@kotakbank"
    const val ACCOUNT_HOLDER = "DIDDIGI VAMSI KRISHNA"   // as shown with the supplied UPI QR code
    const val BANK = "Kotak Mahindra Bank"               // from the supplied IFSC prefix KKBK and the Kotak QR screen
    const val HOME_BRANCH = "MYSORE"
    const val ACCOUNT_NUMBER = "1348857820"
    const val IFSC = "KKBK0000425"
}

@Composable
fun SupportScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val copiedMsg = stringResource(R.string.support_upi_copied)
    var bankOpen by remember { mutableStateOf(false) }

    AppScaffold(title = stringResource(R.string.settings_support), onBack = { vm.back() }, centerTitle = true) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt(R.string.support_intro, Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Txt(R.string.support_optional, Modifier.fillMaxWidth(), fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)

            Card(
                Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Txt(R.string.support_scan, Modifier.fillMaxWidth(), fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    // the supplied QR code, centred, square, scaled down on narrow screens so that it is never clipped
                    Box(Modifier.widthIn(max = 272.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(6.dp)) {
                        Image(ImageBitmap.imageResource(R.drawable.upi_qr), contentDescription = "UPI QR", filterQuality = FilterQuality.None,
                            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                    }
                    Txt(PaymentDetails.ACCOUNT_HOLDER, Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Txt(R.string.support_upi_id, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SelectionContainer { Txt(PaymentDetails.UPI_ID, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center) }
                    Button(onClick = {
                        Clip.copy(context, "UPI ID", PaymentDetails.UPI_ID)
                        scope.launch { snack.showSnackbar(copiedMsg) }
                    }) { Txt(R.string.support_copy_upi, fontWeight = FontWeight.Medium) }
                }
            }

            Card(
                Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().clickable { bankOpen = !bankOpen }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(R.string.support_bank_title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Icon(if (bankOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(if (bankOpen) R.string.collapse else R.string.expand))
                    }
                    if (bankOpen) SelectionContainer {
                        Column(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BankRow(R.string.support_holder, PaymentDetails.ACCOUNT_HOLDER)
                            BankRow(R.string.support_bank, PaymentDetails.BANK)
                            BankRow(R.string.support_branch, PaymentDetails.HOME_BRANCH)
                            BankRow(R.string.support_account_no, PaymentDetails.ACCOUNT_NUMBER)
                            BankRow(R.string.support_ifsc, PaymentDetails.IFSC)
                        }
                    }
                }
            }
            Txt(R.string.support_safety, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BankRow(label: Int, value: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Txt(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Txt(value, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}
