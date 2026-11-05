@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The note editor's "⋮" sheet, styled like Google Keep: a flat, monochrome
 * grid of icon + label actions. Only the actions people reach for most are
 * shown up front; the trailing "More" cell reveals the rest without opening a
 * second screen, and toggles back to the compact set.
 */
@Composable
fun MoreOptionsSheet(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    showDeleteDialog: (Boolean) -> Unit,
    showSaveAsDialog: (Boolean) -> Unit,
    showHistoryDialog: (Boolean) -> Unit,
    showExpiryDialog: (Boolean) -> Unit,
    onPrint: () -> Unit,
    onToggleLock: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showAll by remember { mutableStateOf(false) }

    val lockLabel = stringResource(id = if (state.editingIsLocked) R.string.unlock else R.string.lock)
    val lockIcon = if (state.editingIsLocked) Icons.Default.LockOpen else Icons.Default.Lock
    val shareLabel = stringResource(id = R.string.share)
    val labelsLabel = stringResource(id = R.string.labels)
    val makeCopyLabel = stringResource(id = R.string.make_a_copy)
    val exportLabel = stringResource(id = R.string.export)
    val printLabel = stringResource(id = R.string.print)
    val selfDestructLabel = stringResource(id = R.string.self_destruct)
    val deleteLabel = stringResource(id = R.string.delete_note)
    val searchLabel = stringResource(id = R.string.search)
    val historyLabel = stringResource(id = R.string.history)
    val extractTasksLabel = stringResource(id = R.string.extract_tasks)
    val toTodoLabel = stringResource(id = R.string.convert_to_todo)
    val undoLabel = stringResource(id = R.string.undo)
    val redoLabel = stringResource(id = R.string.redo)
    val moreLabel = stringResource(id = R.string.show_more)
    val lessLabel = stringResource(id = R.string.show_less)
    val viewLabel = stringResource(
        id = if (state.editingNoteType == NoteType.TEXT) R.string.convert_to_list else R.string.convert_to_text
    )

    val primaryOptions = listOf(
        OptionItem(shareLabel, Icons.Default.Share) { onShare() },
        OptionItem(labelsLabel, Icons.AutoMirrored.Filled.Label) { onEvent(NotesEvent.OnAddLabelsToCurrentNoteClick) },
        OptionItem(makeCopyLabel, Icons.Default.ContentCopy) { onEvent(NotesEvent.OnCopyCurrentNoteClick) },
        OptionItem(lockLabel, lockIcon) { onToggleLock() },
        OptionItem(exportLabel, Icons.Default.FileDownload) { showSaveAsDialog(true) },
        OptionItem(printLabel, Icons.Default.Print) { onPrint() },
        OptionItem(selfDestructLabel, Icons.Default.Timer) { showExpiryDialog(true) },
        OptionItem(deleteLabel, Icons.Default.Delete, destructive = true) { showDeleteDialog(true) }
    )

    val advancedOptions = buildList {
        add(OptionItem(viewLabel, Icons.Default.SwapHoriz) { onEvent(NotesEvent.OnToggleNoteType) })
        add(OptionItem(searchLabel, Icons.Default.Search) { onEvent(NotesEvent.ToggleNoteSearch) })
        if (!state.editingIsNewNote) {
            add(OptionItem(historyLabel, Icons.Default.History) { showHistoryDialog(true) })
        }
        add(OptionItem(extractTasksLabel, Icons.Default.AutoFixHigh) { onEvent(NotesEvent.ExtractActionItems) })
        add(OptionItem(toTodoLabel, Icons.Default.PlaylistAddCheck) { onEvent(NotesEvent.ConvertToTodo) })
        if (state.canUndo) {
            add(OptionItem(undoLabel, Icons.AutoMirrored.Rounded.Undo) { onEvent(NotesEvent.OnUndoClick) })
        }
        if (state.canRedo) {
            add(OptionItem(redoLabel, Icons.AutoMirrored.Rounded.Redo) { onEvent(NotesEvent.OnRedoClick) })
        }
    }

    val options = if (showAll) primaryOptions + advancedOptions else primaryOptions

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            if (!state.editingIsNewNote && state.editingLastEdited != null && state.editingLastEdited != 0L) {
                Text(
                    text = stringResource(id = R.string.edited, dateFormat.format(Date(state.editingLastEdited))),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    GridOptionItem(option) {
                        onDismiss()
                        option.action()
                    }
                }
                GridOptionItem(
                    option = OptionItem(
                        label = if (showAll) lessLabel else moreLabel,
                        icon = if (showAll) Icons.Default.ExpandLess else Icons.Default.MoreHoriz
                    ),
                    onClick = { showAll = !showAll }
                )
            }
        }
    }
}

private data class OptionItem(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val action: () -> Unit = {}
)

@Composable
private fun GridOptionItem(
    option: OptionItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = if (option.destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (option.destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
