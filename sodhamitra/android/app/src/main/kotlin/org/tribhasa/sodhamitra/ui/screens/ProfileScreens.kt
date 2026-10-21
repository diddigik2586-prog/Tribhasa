package org.tribhasa.sodhamitra.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.data.Profile
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.common.AppScaffold
import org.tribhasa.sodhamitra.ui.common.ScriptTextField
import org.tribhasa.sodhamitra.ui.common.Txt
import org.tribhasa.sodhamitra.ui.common.rememberFlow
import org.tribhasa.sodhamitra.util.ProfilePhotos

val PROFILE_PREFIXES = listOf("Mr.", "Mrs.", "Ms.", "Vid.", "Dr.", "Prof.")
private val SEX_CHOICES = listOf("MALE" to R.string.sex_male, "FEMALE" to R.string.sex_female, "TRANSGENDER" to R.string.sex_transgender)

/** What the database says about the profile: [saved] is null when the user has never saved one. */
private class SavedProfile(val saved: Profile?)

/**
 * Profile. Nothing saved yet: the editable form. A saved profile: a read-only details view with an Edit button at the top right;
 * Edit opens the same form with the saved values; Save returns to the details; Cancel (or a confirmed Back) discards the edits.
 */
@Composable
fun ProfileScreen(vm: AppViewModel) {
    val state = rememberFlow { vm.repo.profile().map { SavedProfile(it) } }.collectAsState(initial = null).value
    var editing by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(R.string.settings_profile)
    val saved = state?.saved
    when {
        state == null -> AppScaffold(title = title, onBack = { vm.back() }) { }          // still reading the database
        saved != null && !editing -> ProfileDetails(vm, saved, onEdit = { editing = true })
        else -> ProfileForm(vm, initial = saved ?: Profile(), isNew = saved == null, onFinished = { editing = false })
    }
}

// ---- read-only details ---------------------------------------------------------------------------------------------------

@Composable
private fun ProfileDetails(vm: AppViewModel, p: Profile, onEdit: () -> Unit) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, p.photoFile) {
        value = ProfilePhotos.load(vm.repo.photoDir, p.photoFile)?.asImageBitmap()
    }
    val sexLabel = SEX_CHOICES.firstOrNull { it.first == p.sex }?.let { stringResource(it.second) }.orEmpty()
    AppScaffold(
        title = stringResource(R.string.settings_profile), onBack = { vm.back() },
        actions = {
            // the Edit button with a pencil, at the top right
            TextButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(6.dp))
                Txt(R.string.edit, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium)
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt(R.string.profile_photo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.size(112.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    bitmap?.let { Image(it, contentDescription = stringResource(R.string.profile_photo), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
            }
            Card(
                Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    DetailRow(R.string.profile_prefix, p.prefix)
                    DetailRow(R.string.profile_name, p.name)
                    DetailRow(R.string.profile_sex, sexLabel)
                    DetailRow(R.string.profile_designation, p.designation)
                    DetailRow(R.string.profile_institution, p.institution)
                    DetailRow(R.string.profile_address, p.address)
                    DetailRow(R.string.place, p.place, last = true)
                }
            }
        }
    }
}

/** One label with its saved value; an empty value is shown as an em dash so that every field is always visible. */
@Composable
private fun DetailRow(label: Int, value: String, last: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Txt(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Txt(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
    if (!last) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// ---- the form (first entry, and Edit) ----------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileForm(vm: AppViewModel, initial: Profile, isNew: Boolean, onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var prefix by rememberSaveable { mutableStateOf(initial.prefix) }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var sex by rememberSaveable { mutableStateOf(initial.sex) }
    var designation by rememberSaveable { mutableStateOf(initial.designation) }
    var institution by rememberSaveable { mutableStateOf(initial.institution) }
    var address by rememberSaveable { mutableStateOf(initial.address) }
    var place by rememberSaveable { mutableStateOf(initial.place) }
    var photo by rememberSaveable { mutableStateOf(initial.photoFile) }   // a newly chosen photo is kept only on Save
    var askDiscard by remember { mutableStateOf(false) }

    val dirty = prefix != initial.prefix || name != initial.name || sex != initial.sex || designation != initial.designation ||
        institution != initial.institution || address != initial.address || place != initial.place || photo != initial.photoFile

    /** removes a photograph that was imported during this editing session but never saved */
    fun dropUnsavedPhoto() { if (photo != initial.photoFile) ProfilePhotos.delete(vm.repo.photoDir, photo) }

    /** leave the form without saving: back to the saved details, or (first entry) back to the previous page */
    fun leave() { dropUnsavedPhoto(); if (isNew) vm.back() else onFinished() }
    fun requestLeave() { if (dirty) askDiscard = true else leave() }

    // Back never silently throws edits away. Without edits it simply leaves the form (the first-entry form with nothing typed
    // keeps the ordinary Back behaviour of the app).
    BackHandler(enabled = dirty || !isNew) { requestLeave() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val file = ProfilePhotos.import(context, vm.repo.photoDir, uri)
            if (file != null) { dropUnsavedPhoto(); photo = file }
        }
    }
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, photo) {
        value = ProfilePhotos.load(vm.repo.photoDir, photo)?.asImageBitmap()
    }

    fun save() {
        val values = Profile(prefix = prefix, name = name.trim(), sex = sex, designation = designation.trim(),
            institution = institution.trim(), address = address.trim(), place = place.trim(), photoFile = photo)
        scope.launch {
            // the write completes even if the screen is left (first entry: the saved details replace the form at once)
            withContext(NonCancellable + Dispatchers.Default) {
                vm.repo.saveProfile(values)
                if (initial.photoFile.isNotBlank() && initial.photoFile != photo) ProfilePhotos.delete(vm.repo.photoDir, initial.photoFile)
                vm.say(R.string.profile_saved)
            }
            onFinished()
        }
    }

    AppScaffold(title = stringResource(R.string.settings_profile), onBack = { requestLeave() }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
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
                    if (photo.isNotBlank()) TextButton(onClick = { dropUnsavedPhoto(); photo = "" }) {
                        Txt(R.string.profile_remove_photo, color = MaterialTheme.colorScheme.error)
                    }
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

            if (isNew) {
                Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Txt(R.string.save_changes, fontWeight = FontWeight.Medium) }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { leave() }, modifier = Modifier.weight(1f)) { Txt(R.string.cancel) }
                    Button(onClick = { save() }, modifier = Modifier.weight(1f)) { Txt(R.string.save_changes, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }

    if (askDiscard) {
        AlertDialog(
            onDismissRequest = { askDiscard = false },
            title = { Txt(R.string.profile_discard_title, style = MaterialTheme.typography.titleLarge) },
            text = { Txt(R.string.profile_discard_msg) },
            confirmButton = {
                TextButton(onClick = { askDiscard = false; leave() }) {
                    Txt(R.string.profile_discard, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = { TextButton(onClick = { askDiscard = false }) { Txt(R.string.profile_keep_editing) } },
        )
    }
}
