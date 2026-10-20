package org.tribhasa.sodhamitra.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.tribhasa.sodhamitra.domain.FieldSpec
import org.tribhasa.sodhamitra.domain.FieldType
import org.tribhasa.sodhamitra.domain.Forms

/**
 * One generic form screen. The fields appear in exactly the order of [spec]. Validation happens on Save: the form is not
 * saved while a mandatory field (including a Place that the Stakeholder makes mandatory) is empty.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormScreen(
    title: String,
    spec: List<FieldSpec>,
    initial: Map<String, String>,
    isEdit: Boolean,
    onCancel: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
    topContent: @Composable ColumnScope.(SnapshotStateMap<String, String>) -> Unit = {},
) {
    val values: SnapshotStateMap<String, String> = remember { mutableStateMapOf<String, String>().apply { putAll(initial) } }
    var attempted by remember { mutableStateOf(false) }
    val errors = Forms.validate(spec, values)

    AppScaffold(
        title = title, onBack = onCancel, modifier = Modifier.imePadding(),
        bottomBar = {
            SaveCancelBar(isEdit, onCancel, onSave = {
                attempted = true
                if (Forms.validate(spec, values).isEmpty()) onSave(values.toMap())
            })
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            topContent(values)
            for (f in spec) {
                if (f.visibleIf?.invoke(values) == false) continue
                val label = stringResource(f.label) + if (Forms.isMandatoryNow(f, values)) " *" else ""
                when (f.type) {
                    FieldType.TEXT, FieldType.MULTILINE -> ScriptTextField(
                        value = values[f.key].orEmpty(), onValueChange = { values[f.key] = it }, label = label,
                        singleLine = f.type == FieldType.TEXT, minLines = if (f.type == FieldType.MULTILINE) f.minLines else 1,
                        error = if (attempted) errors[f.key]?.let { stringResource(it) } else null,
                    )
                    FieldType.CHECK -> LabeledCheckbox(f.label, values[f.key] == Forms.CHECKED, onChange = {
                        values[f.key] = if (it) Forms.CHECKED else ""
                    })
                    FieldType.CHOICE -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Txt(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (c in f.choices) {
                                val selected = values[f.key] == c.code
                                FilterChip(
                                    selected = selected,
                                    // tapping the chosen option again clears the choice: the field stays optional
                                    onClick = { values[f.key] = if (selected) "" else c.code },
                                    label = { Txt(c.label) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
