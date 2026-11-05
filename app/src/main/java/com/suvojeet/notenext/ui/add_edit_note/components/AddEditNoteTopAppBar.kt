@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState

@Composable
fun AddEditNoteTopAppBar(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    onToneRewriteClick: () -> Unit,
    editingNoteType: NoteType,
    onToggleFocusMode: () -> Unit,
    isFocusMode: Boolean,
    onReminderClick: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            if (state.editingIsNewNote) {
                Text(
                    text = if (editingNoteType == NoteType.CHECKLIST) stringResource(id = R.string.add_checklist) else stringResource(id = R.string.add_note),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .springPress()
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = contentColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        scrollBehavior = scrollBehavior,
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                SavedStatusIndicator(status = state.saveStatus, contentColor = contentColor)

                if (editingNoteType == NoteType.TEXT && !state.editingIsNewNote) {
                    IconButton(
                        onClick = { onEvent(NotesEvent.SummarizeNote) },
                        modifier = Modifier.springPress()
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = "Summarize Note", tint = contentColor)
                    }
                    IconButton(
                        onClick = onToneRewriteClick,
                        modifier = Modifier.springPress()
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = "Rewrite tone",
                            tint = contentColor
                        )
                    }
                }
                
                IconButton(onClick = onToggleFocusMode, modifier = Modifier.springPress()) {
                    Icon(
                        imageVector = if (isFocusMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Focus Mode",
                        tint = contentColor
                    )
                }

                if (!state.editingIsNewNote) {
                    val pinContainerColor by animateColorAsState(
                        targetValue = if (state.isPinned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy),
                        label = "PinContainerColor"
                    )
                    val pinContentColor by animateColorAsState(
                        targetValue = if (state.isPinned) MaterialTheme.colorScheme.primary else contentColor,
                        animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy),
                        label = "PinContentColor"
                    )

                    FilledTonalIconButton(
                        onClick = { onEvent(NotesEvent.OnTogglePinClick) },
                        modifier = Modifier.springPress(),
                        shape = MaterialTheme.shapes.medium,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = pinContainerColor,
                            contentColor = pinContentColor
                        )
                    ) {
                        Icon(
                            imageVector = if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (state.isPinned) stringResource(id = R.string.unpin_note) else stringResource(id = R.string.pin_note)
                        )
                    }
                }

                // Reminder (Keep's bell) — tonal, available for new and existing notes.
                FilledTonalIconButton(
                    onClick = onReminderClick,
                    modifier = Modifier.springPress(),
                    shape = MaterialTheme.shapes.medium,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = contentColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddAlert,
                        contentDescription = stringResource(id = R.string.reminder_icon_description)
                    )
                }

                if (!state.editingIsNewNote) {
                    FilledTonalIconButton(
                        onClick = { onEvent(NotesEvent.OnToggleArchiveClick) },
                        modifier = Modifier.springPress(),
                        shape = MaterialTheme.shapes.medium,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (state.isArchived) MaterialTheme.colorScheme.primary else contentColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Archive,
                            contentDescription = if (state.isArchived) stringResource(id = R.string.unarchive_note) else stringResource(id = R.string.archive_note)
                        )
                    }
                }
            }
        }
    )
}
