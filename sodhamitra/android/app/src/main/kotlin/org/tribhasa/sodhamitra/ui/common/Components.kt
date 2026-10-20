package org.tribhasa.sodhamitra.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.tribhasa.sodhamitra.R
import org.tribhasa.sodhamitra.ui.theme.ScriptVisualTransformation
import org.tribhasa.sodhamitra.ui.theme.rememberScriptText

val LocalSnack = compositionLocalOf { SnackbarHostState() }

/**
 * A database Flow created once per set of [keys]. Room returns a NEW Flow object on every call, and collectAsState restarts
 * whenever it is handed a different Flow, so a Flow must never be created afresh on each recomposition.
 */
@Composable
fun <T> rememberFlow(vararg keys: Any?, flow: () -> kotlinx.coroutines.flow.Flow<T>): kotlinx.coroutines.flow.Flow<T> =
    remember(*keys, calculation = flow)

/** Text drawn with the bundled font of each script it contains (Ubuntu for English). All app text goes through here. */
@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = rememberScriptText(text), modifier = modifier, style = style, color = color, fontWeight = fontWeight,
        textAlign = textAlign, maxLines = maxLines, overflow = overflow,
    )
}

@Composable
fun Txt(
    @StringRes id: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) = Txt(stringResource(id), modifier, style, color, fontWeight, textAlign, maxLines, overflow)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val snack = LocalSnack.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Txt(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snack) { data -> Snackbar { Txt(data.visuals.message, color = Color.Unspecified) } } },
        content = content,
    )
}

/** A text button for the top bar, in the bar's own colours. */
@Composable
fun BarTextButton(@StringRes label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Txt(label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium) }
}

@Composable
fun ConfirmDeleteDialog(@StringRes message: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Txt(R.string.delete_title, style = MaterialTheme.typography.titleLarge) },
        text = { Txt(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Txt(R.string.delete, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Txt(R.string.cancel) } },
    )
}

@Composable
fun HeadingText(text: String, modifier: Modifier = Modifier) =
    Txt(text, modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

/** A heading with its own "+ Add" action at the right. */
@Composable
fun SectionHeader(@StringRes title: Int, @StringRes addLabel: Int, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Txt(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Button(onClick = onAdd) { Txt(addLabel, fontWeight = FontWeight.Medium) }
    }
}

@Composable
fun EmptyHint(@StringRes text: Int, modifier: Modifier = Modifier) =
    Txt(text, modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun HintLine(@StringRes text: Int, modifier: Modifier = Modifier) =
    Txt(text, modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

data class MenuAction(@StringRes val label: Int, val destructive: Boolean = false, val onClick: () -> Unit)

/** A card that opens on tap and shows a menu (Edit, Delete, ...) on long-press. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongPressCard(
    onClick: () -> Unit,
    actions: List<MenuAction>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        Card(
            Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { menu = true }),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) { Column(Modifier.fillMaxWidth().padding(14.dp), content = content) }
        ActionMenu(menu, { menu = false }, actions)
    }
}

@Composable
fun ActionMenu(expanded: Boolean, onDismiss: () -> Unit, actions: List<MenuAction>) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        actions.forEach { a ->
            DropdownMenuItem(
                text = { Txt(a.label, color = if (a.destructive) MaterialTheme.colorScheme.error else Color.Unspecified) },
                onClick = { onDismiss(); a.onClick() },
            )
        }
    }
}

@Composable
fun LabeledCheckbox(@StringRes label: Int, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Txt(label, Modifier.padding(start = 8.dp).weight(1f))
    }
}

/** A text field whose content is drawn with the right bundled font per script; the text itself is never altered. */
@Composable
fun ScriptTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        label = { Txt(label, style = MaterialTheme.typography.bodyMedium) },
        singleLine = singleLine, minLines = minLines,
        isError = error != null,
        supportingText = if (error != null) ({ Txt(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }) else null,
        visualTransformation = ScriptVisualTransformation,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
fun SaveCancelBar(isEdit: Boolean, onCancel: () -> Unit, onSave: () -> Unit, enabled: Boolean = true) {
    androidx.compose.material3.Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Txt(R.string.cancel) }
            Button(onClick = onSave, enabled = enabled, modifier = Modifier.weight(1f)) {
                Txt(if (isEdit) R.string.save_changes else R.string.save, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ProvideLocal(snack: SnackbarHostState, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalSnack provides snack, content = content)
