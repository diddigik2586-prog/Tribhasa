package org.tribhasa.sodhamitra.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.pdf.ExportSection
import org.tribhasa.sodhamitra.pdf.PdfExporter
import org.tribhasa.sodhamitra.pdf.landscape
import org.tribhasa.sodhamitra.pdf.sections
import org.tribhasa.sodhamitra.ui.AppViewModel
import org.tribhasa.sodhamitra.ui.ExportTarget
import org.tribhasa.sodhamitra.ui.common.AppScaffold
import org.tribhasa.sodhamitra.ui.common.LabeledCheckbox
import org.tribhasa.sodhamitra.ui.common.LocalSnack
import org.tribhasa.sodhamitra.ui.common.Txt
import org.tribhasa.sodhamitra.util.PdfFiles

/** Choose the parts to include, create the PDF on the device, then save / share / open it. */
@Composable
fun ExportScreen(vm: AppViewModel, target: ExportTarget) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = LocalSnack.current
    val options = remember(target) { target.sections() }
    val selected = remember(target) { mutableStateListOf<ExportSection>().apply { addAll(options) } }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var file by remember { mutableStateOf<File?>(null) }

    val saveMsg = stringResource(R.string.export_saved)
    val noViewer = stringResource(R.string.export_no_viewer)
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val f = file
        if (uri != null && f != null) scope.launch {
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } } }
            snack.showSnackbar(saveMsg)
        }
    }

    AppScaffold(title = stringResource(R.string.export_title), onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Txt(R.string.export_choose, fontWeight = FontWeight.Medium)
            for (s in options) LabeledCheckbox(s.label, s in selected, onChange = { on ->
                file = null; message = null
                if (on) selected.add(s) else selected.remove(s)
            })
            Txt(if (target.landscape()) R.string.export_page_landscape else R.string.export_page_portrait,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = {
                    file = null
                    if (selected.isEmpty()) { message = R.string.export_select_one; return@Button }
                    message = null; busy = true
                    scope.launch {
                        try {
                            val exporter = PdfExporter(context, vm.repo)
                            val doc = exporter.build(target, selected.toSet())
                            if (doc == null) message = R.string.export_nothing           // nothing selected has any content: no PDF
                            else { file = exporter.write(doc, doc.title); message = R.string.export_ready }
                        } catch (e: Exception) { message = R.string.export_failed }
                        busy = false
                    }
                },
            ) { Txt(R.string.export_create, fontWeight = FontWeight.Medium) }

            if (busy) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.padding(0.dp))
                Txt(R.string.export_creating)
            }
            message?.let {
                Txt(it, Modifier.padding(top = 8.dp), fontWeight = FontWeight.Medium,
                    color = if (it == R.string.export_ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            file?.let { f ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { saveLauncher.launch(f.name) }, modifier = Modifier.weight(1f)) { Txt(R.string.export_save) }
                    OutlinedButton(onClick = { context.startActivity(PdfFiles.shareIntent(context, f)) }, modifier = Modifier.weight(1f)) { Txt(R.string.share) }
                    OutlinedButton(
                        onClick = {
                            try { context.startActivity(PdfFiles.viewIntent(context, f)) }
                            catch (e: ActivityNotFoundException) { scope.launch { snack.showSnackbar(noViewer) } }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Txt(R.string.open) }
                }
            }
        }
    }
}
