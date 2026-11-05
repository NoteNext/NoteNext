@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.*
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.NoteHtmlGenerator
import com.suvojeet.notenext.util.printNote
import com.suvojeet.notenext.util.findActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AddEditNoteDialogs(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    showDeleteDialog: Boolean,
    onShowDeleteDialogChange: (Boolean) -> Unit,
    showMoreOptions: Boolean,
    onShowMoreOptionsChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    showLabelDialog: Boolean,
    onShowLabelDialogChange: (Boolean) -> Unit,
    showSaveAsDialog: Boolean,
    onShowSaveAsDialogChange: (Boolean) -> Unit,
    showHistoryDialog: Boolean,
    onShowHistoryDialogChange: (Boolean) -> Unit,
    showExpiryDialog: Boolean,
    onShowExpiryDialogChange: (Boolean) -> Unit,
    showInsertLinkDialog: Boolean,
    onShowInsertLinkDialogChange: (Boolean) -> Unit,
    clickedLink: ClickedLink?,
    onClickedLinkChange: (ClickedLink?) -> Unit,
    showExactAlarmDialog: Boolean,
    onShowExactAlarmDialogChange: (Boolean) -> Unit,
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    onSaveAsPdf: () -> Unit,
    onSaveAsTxt: () -> Unit,
    onSaveAsMd: () -> Unit
) {
    val context = LocalContext.current

    // Set when the user chooses "Edit link": the insert dialog is reused with
    // the tapped link's label and URL prefilled, and the link range selected.
    var editingLink by remember { mutableStateOf<ClickedLink?>(null) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { onShowDeleteDialogChange(false) },
            shape = MaterialTheme.shapes.extraLarge,
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(id = R.string.delete_note)) },
            text = { Text(stringResource(id = R.string.move_to_bin_question)) },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(NotesEvent.OnDeleteNoteClick)
                        onShowDeleteDialogChange(false)
                    },
                    modifier = Modifier.springPress(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.move_to_bin), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDeleteDialogChange(false) }, modifier = Modifier.springPress()) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showMoreOptions) {
        MoreOptionsSheet(
            state = state,
            onEvent = { event ->
                if (event is NotesEvent.OnAddLabelsToCurrentNoteClick) {
                    onShowLabelDialogChange(true)
                } else {
                    onEvent(event)
                }
            },
            onDismiss = { onShowMoreOptionsChange(false) },
            showDeleteDialog = { onShowDeleteDialogChange(it) },
            showSaveAsDialog = { onShowSaveAsDialogChange(it) },
            showHistoryDialog = { onShowHistoryDialogChange(it) },
            showExpiryDialog = { onShowExpiryDialogChange(it) },
            onPrint = {
                scope.launch {
                    val fullHtml = NoteHtmlGenerator.generateNoteHtml(
                        context,
                        state.editingTitle,
                        state.editingContent.annotatedString,
                        state.editingAttachments
                    )
                    printNote(context, fullHtml, state.editingTitle.ifBlank { "Note Document" })
                }
            },
            onToggleLock = {
                val activity = context.findActivity() as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    val biometricAuthManager = BiometricAuthManager(context, activity)
                    biometricAuthManager.showBiometricPrompt(
                        onAuthSuccess = { onEvent(NotesEvent.OnToggleLockClick) },
                        onAuthError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    )
                } else {
                    Toast.makeText(context, "Authentication unavailable", Toast.LENGTH_SHORT).show()
                }
            },
            onShare = onShare
        )
    }

    if (showLabelDialog) {
        com.suvojeet.notenext.ui.components.LabelDialog(
            onDismiss = { onShowLabelDialogChange(false) },
            onConfirm = { label ->
                onEvent(NotesEvent.OnLabelChange(label))
                onShowLabelDialogChange(false)
            },
            labels = state.labels
        )
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            onDismiss = { onShowSaveAsDialogChange(false) },
            onSaveAsPdf = {
                onSaveAsPdf()
                onShowSaveAsDialogChange(false)
            },
            onSaveAsTxt = {
                onSaveAsTxt()
                onShowSaveAsDialogChange(false)
            },
            onSaveAsMd = {
                onSaveAsMd()
                onShowSaveAsDialogChange(false)
            },
            onSaveAsNote = if (state.externalUri != null) {
                {
                    onEvent(NotesEvent.SaveExternalAsNote)
                    onShowSaveAsDialogChange(false)
                }
            } else null
        )
    }

    if (showHistoryDialog) {
        NoteHistoryDialog(
            versions = state.editingNoteVersions,
            isLocked = state.editingIsLocked,
            onVersionClick = { version ->
                onEvent(NotesEvent.OnRestoreVersion(version))
                onShowHistoryDialogChange(false)
            },
            onDismiss = { onShowHistoryDialogChange(false) }
        )
    }

    if (showExpiryDialog) {
        ExpiryTimerDialog(
            currentExpiryTime = state.editingExpiryTime,
            onDismiss = { onShowExpiryDialogChange(false) },
            onExpirySelected = { expiryTime ->
                onEvent(NotesEvent.OnExpiryChange(expiryTime))
                onShowExpiryDialogChange(false)
            }
        )
    }

    if (showInsertLinkDialog) {
        val selection = state.editingContent.selection
        val selectedText = if (!selection.collapsed) {
            state.editingContent.text
                .substring(
                    selection.min.coerceIn(0, state.editingContent.text.length),
                    selection.max.coerceIn(0, state.editingContent.text.length)
                )
        } else {
            ""
        }
        InsertLinkDialog(
            onDismiss = {
                editingLink = null
                onShowInsertLinkDialogChange(false)
            },
            initialText = editingLink?.label ?: selectedText,
            initialUrl = editingLink?.url ?: "",
            onInsertLink = { text, url ->
                onEvent(NotesEvent.OnInsertLink(url, text))
                editingLink = null
                onShowInsertLinkDialogChange(false)
            }
        )
    }

    clickedLink?.let { link ->
        LinkActionDialog(
            url = link.url,
            onDismiss = { onClickedLinkChange(null) },
            onOpenLink = {
                onClickedLinkChange(null)
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(link.url)
                )
                context.startActivity(intent)
            },
            onEditLink = {
                onClickedLinkChange(null)
                editingLink = link
                // Select the existing link so insertLink replaces exactly that
                // range instead of appending a second link at the cursor.
                onEvent(
                    NotesEvent.OnContentChange(
                        state.editingContent.copy(
                            selection = TextRange(link.start, link.end)
                        )
                    )
                )
                onShowInsertLinkDialogChange(true)
            },
            onCopyLink = {
                onClickedLinkChange(null)
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("URL", link.url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    context,
                    context.getString(R.string.link_dialog_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { onShowExactAlarmDialogChange(false) },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(id = R.string.exact_alarm_permission)) },
            text = { Text(stringResource(id = R.string.exact_alarm_permission_description)) },
            confirmButton = {
                Button(
                    onClick = {
                        onShowExactAlarmDialogChange(false)
                        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.springPress()
                ) {
                    Text(stringResource(id = R.string.request))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowExactAlarmDialogChange(false) }, modifier = Modifier.springPress()) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ExpiryTimerDialog(
    currentExpiryTime: Long?,
    onDismiss: () -> Unit,
    onExpirySelected: (Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) { Text(stringResource(id = R.string.note_search_next_cd)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val expiryDateTime = selectedDate.atTime(timePickerState.hour, timePickerState.minute)
                    val expiryMillis = expiryDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onExpirySelected(expiryMillis)
                    showTimePicker = false
                }) { Text(stringResource(id = R.string.note_dialog_set)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(id = R.string.cancel)) }
            },
            title = { Text(stringResource(id = R.string.note_dialog_select_time)) },
            text = { TimePicker(state = timePickerState) }
        )
    }

    val options = listOf(
        (1 * 60 * 60 * 1000L) to "1 Hour",
        (24 * 60 * 60 * 1000L) to "24 Hours",
        (7 * 24 * 60 * 60 * 1000L) to "7 Days"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.note_dialog_self_destruct_title)) },
        text = {
            Column {
                options.forEach { (duration, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { 
                                val expiryTime = System.currentTimeMillis() + duration
                                onExpirySelected(expiryTime) 
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = false, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(label)
                    }
                }

                // Custom Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { showDatePicker = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = false, onClick = null)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(id = R.string.note_dialog_custom))
                }

                if (currentExpiryTime != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onExpirySelected(null) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(id = R.string.note_dialog_remove_timer), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
