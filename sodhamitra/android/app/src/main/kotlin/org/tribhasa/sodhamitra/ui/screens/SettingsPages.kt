package org.tribhasa.sodhamitra.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.tribhasa.sodhamitra.BuildConfig
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.Profile
import org.tribhasa.sodhamitra.i18n.AppLanguage
import org.tribhasa.sodhamitra.i18n.LanguageStore
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.common.AppScaffold
import org.tribhasa.sodhamitra.ui.common.HeadingText
import org.tribhasa.sodhamitra.ui.common.LocalSnack
import org.tribhasa.sodhamitra.ui.common.ScriptTextField
import org.tribhasa.sodhamitra.ui.common.Txt
import org.tribhasa.sodhamitra.util.Clip
import org.tribhasa.sodhamitra.util.ProfilePhotos

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

// ---- Profile -------------------------------------------------------------------------------------------------

val PROFILE_PREFIXES = listOf("Mr.", "Mrs.", "Ms.", "Vid.", "Dr.", "Prof.")
private val SEX_CHOICES = listOf("MALE" to R.string.sex_male, "FEMALE" to R.string.sex_female, "TRANSGENDER" to R.string.sex_transgender)

@Composable
fun ProfileScreen(vm: AppViewModel) {
    val loaded by produceState<Profile?>(null) { value = vm.repo.profileOnce() }
    AppScaffold(title = stringResource(R.string.settings_profile), onBack = { vm.back() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { loaded?.let { ProfileForm(vm, it) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileForm(vm: AppViewModel, initial: Profile) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = LocalSnack.current
    val savedMsg = stringResource(R.string.profile_saved)

    var prefix by remember { mutableStateOf(initial.prefix) }
    var name by remember { mutableStateOf(initial.name) }
    var sex by remember { mutableStateOf(initial.sex) }
    var designation by remember { mutableStateOf(initial.designation) }
    var institution by remember { mutableStateOf(initial.institution) }
    var address by remember { mutableStateOf(initial.address) }
    var place by remember { mutableStateOf(initial.place) }
    var photo by remember { mutableStateOf(initial.photoFile) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val file = ProfilePhotos.import(context, vm.repo.photoDir, uri)
            if (file != null) {
                val current = vm.repo.profileOnce()
                ProfilePhotos.delete(vm.repo.photoDir, current.photoFile)
                vm.repo.saveProfile(current.copy(photoFile = file))
                photo = file
            }
        }
    }
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, photo) {
        value = ProfilePhotos.load(vm.repo.photoDir, photo)?.asImageBitmap()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Txt(R.string.profile_optional_note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                bitmap?.let { Image(it, contentDescription = stringResource(R.string.profile_photo), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt(R.string.profile_photo, fontWeight = FontWeight.Medium)
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Txt(R.string.profile_choose_photo)
                }
                if (photo.isNotBlank()) TextButton(onClick = {
                    scope.launch {
                        val current = vm.repo.profileOnce()
                        ProfilePhotos.delete(vm.repo.photoDir, current.photoFile)
                        vm.repo.saveProfile(current.copy(photoFile = "")); photo = ""
                    }
                }) { Txt(R.string.profile_remove_photo, color = MaterialTheme.colorScheme.error) }
            }
        }

        Txt(R.string.profile_prefix, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (p in PROFILE_PREFIXES) FilterChip(selected = prefix == p, onClick = { prefix = if (prefix == p) "" else p }, label = { Txt(p) })
        }
        ScriptTextField(name, { name = it }, stringResource(R.string.profile_name))
        Txt(R.string.profile_sex, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((code, label) in SEX_CHOICES) FilterChip(selected = sex == code, onClick = { sex = if (sex == code) "" else code }, label = { Txt(label) })
        }
        ScriptTextField(designation, { designation = it }, stringResource(R.string.profile_designation))
        ScriptTextField(institution, { institution = it }, stringResource(R.string.profile_institution))
        ScriptTextField(address, { address = it }, stringResource(R.string.profile_address), singleLine = false, minLines = 2)
        ScriptTextField(place, { place = it }, stringResource(R.string.place))

        Button(
            onClick = {
                scope.launch {
                    val current = vm.repo.profileOnce()
                    vm.repo.saveProfile(current.copy(prefix = prefix, name = name.trim(), sex = sex, designation = designation.trim(),
                        institution = institution.trim(), address = address.trim(), place = place.trim()))
                    snack.showSnackbar(savedMsg)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Txt(R.string.save_changes, fontWeight = FontWeight.Medium) }
    }
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
    AppScaffold(title = stringResource(R.string.settings_about_app), onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandBlock(logoSize = 96.dp)   // logo, Śodhamitra, Research Planner and the one copyright line
            Txt(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (id in listOf(R.string.about_purpose, R.string.about_sections, R.string.about_projects, R.string.about_notes,
                R.string.about_bibliography, R.string.about_planner, R.string.about_languages, R.string.about_storage, R.string.about_pdf)) {
                Txt(id)
            }
            HeadingText(stringResource(R.string.about_fonts_title))
            Txt(R.string.about_fonts_note, style = MaterialTheme.typography.bodyMedium)
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
                    Txt(file.removeSuffix(".txt"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Txt(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ---- About Dr. DVK -----------------------------------------------------------------------------------------

@Composable
fun AboutDvkScreen(vm: AppViewModel) {
    AppScaffold(title = stringResource(R.string.settings_about_dvk), onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Txt("Dr. Diddigi Vamsi Krishna", Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            for (id in listOf(R.string.dvk_p1, R.string.dvk_p2, R.string.dvk_p3, R.string.dvk_p4, R.string.dvk_p5)) Txt(id)
            HeadingText(stringResource(R.string.dvk_recognition_title))
            Txt(R.string.dvk_recognition)
            Spacer(Modifier.height(8.dp))
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

    AppScaffold(title = stringResource(R.string.settings_support), onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Txt(R.string.support_intro)
            Txt(R.string.support_optional, fontWeight = FontWeight.Medium)

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Txt(R.string.support_scan, fontWeight = FontWeight.Medium)
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(6.dp)) {
                        Image(ImageBitmap.imageResource(R.drawable.upi_qr), contentDescription = "UPI QR", filterQuality = FilterQuality.None,
                            modifier = Modifier.size(260.dp))
                    }
                    Txt(PaymentDetails.ACCOUNT_HOLDER, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Txt(R.string.support_upi_id, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SelectionContainer { Txt(PaymentDetails.UPI_ID, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) }
                    Button(onClick = {
                        Clip.copy(context, "UPI ID", PaymentDetails.UPI_ID)
                        scope.launch { snack.showSnackbar(copiedMsg) }
                    }) { Txt(R.string.support_copy_upi, fontWeight = FontWeight.Medium) }
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().clickable { bankOpen = !bankOpen }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt(R.string.support_bank_title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Icon(if (bankOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(if (bankOpen) R.string.collapse else R.string.expand))
                    }
                    if (bankOpen) SelectionContainer {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BankRow(R.string.support_holder, PaymentDetails.ACCOUNT_HOLDER)
                            BankRow(R.string.support_bank, PaymentDetails.BANK)
                            BankRow(R.string.support_branch, PaymentDetails.HOME_BRANCH)
                            BankRow(R.string.support_account_no, PaymentDetails.ACCOUNT_NUMBER)
                            BankRow(R.string.support_ifsc, PaymentDetails.IFSC)
                        }
                    }
                }
            }
            Txt(R.string.support_safety, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BankRow(label: Int, value: String) {
    Column {
        Txt(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Txt(value, fontWeight = FontWeight.Medium)
    }
}
