@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Timer
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.theme.*
import kotlinx.coroutines.launch

/**
 * A hyperlink the user tapped in the editor. [start] and [end] are offsets into
 * the whole note body (not the current editor chunk), so the caller can select
 * the link range and open the link editor with it prefilled.
 */
data class ClickedLink(
    val url: String,
    val label: String,
    val start: Int,
    val end: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteTitleEditor(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onReminderClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        val titleTextColor = MaterialTheme.colorScheme.onSurface

        TextField(
            value = state.editingTitle,
            onValueChange = { newTitle: String -> onEvent(NotesEvent.OnTitleChange(newTitle)) },
            placeholder = { 
                Text(
                    stringResource(id = R.string.title), 
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                selectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ),
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = titleTextColor,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.25).sp
            ),
            singleLine = false,
            maxLines = 3
        )

        ReminderDisplay(
            reminderTime = state.editingReminderTime,
            repeatOption = state.editingRepeatOption,
            onClick = onReminderClick
        )

        ExpiryDisplay(
            expiryTime = state.editingExpiryTime,
            onClick = { /* Could open expiry dialog if needed */ }
        )
    }
}

fun LazyListScope.NoteContentItems(
    state: NotesEditState,
    splitOffsets: List<Int>,
    onEvent: (NotesEvent) -> Unit,
    onUrlClick: (ClickedLink) -> Unit,
    onSlashCommand: () -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val globalContent = state.editingContent
    val text = globalContent.text

    itemsIndexed(
        items = splitOffsets,
        key = { _, startOffset -> startOffset }
    ) { index, startOffset ->
        val endOffset = if (index + 1 < splitOffsets.size) splitOffsets[index + 1] else text.length
        
        NoteContentChunkEditor(
            index = index,
            startOffset = startOffset,
            endOffset = endOffset,
            state = state,
            onEvent = onEvent,
            onUrlClick = onUrlClick,
            onSlashCommand = onSlashCommand,
            onTextLayout = onTextLayout
        )
    }

    item {
        MentionPopup(
            isVisible = state.isMentionPopupVisible,
            notes = state.mentionableNotes,
            onNoteClick = { id, title -> onEvent(NotesEvent.InsertMention(id, title)) },
            onDismiss = { onEvent(NotesEvent.CloseMentionPopup) }
        )
    }
}

@Composable
fun NoteContentChunkEditor(
    index: Int,
    startOffset: Int,
    endOffset: Int,
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onUrlClick: (ClickedLink) -> Unit,
    onSlashCommand: () -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Hold latest references
    val currentOnUrlClick by rememberUpdatedState(onUrlClick)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val cursorColor = MaterialTheme.colorScheme.primary

    val contentTextColor = MaterialTheme.colorScheme.onSurface
    val contentTextStyle = when (state.activeHeadingStyle) {
        1 -> MaterialTheme.typography.headlineLarge.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        2 -> MaterialTheme.typography.headlineMedium.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        3 -> MaterialTheme.typography.headlineSmall.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        4 -> MaterialTheme.typography.titleLarge.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        5 -> MaterialTheme.typography.titleMedium.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        6 -> MaterialTheme.typography.titleSmall.copy(color = contentTextColor, fontWeight = FontWeight.Bold)
        else -> MaterialTheme.typography.bodyLarge.copy(color = contentTextColor, lineHeight = 28.sp)
    }

    var chunkLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    // Auto-scroll to cursor
    LaunchedEffect(state.editingContent.selection) {
        val selection = state.editingContent.selection
        if (selection.collapsed) {
            val globalCursor = selection.start
            if (globalCursor >= startOffset && globalCursor <= endOffset) {
                chunkLayoutResult?.let { layout ->
                    val localCursor = globalCursor - startOffset
                    if (localCursor >= 0 && localCursor <= layout.layoutInput.text.length) {
                        try {
                            val cursorRect = layout.getCursorRect(localCursor)
                            scope.launch {
                                bringIntoViewRequester.bringIntoView(cursorRect)
                            }
                        } catch (e: Exception) {
                            // getCursorRect can throw transiently while the layout is mid-update
                            // (stale cursor index). The bring-into-view is purely cosmetic, so
                            // skipping it for one frame is harmless — no need to crash or log spam.
                        }
                    }
                }
            }
        }
    }

    // Derive the chunk from global state
    val chunk = remember(state.editingContent, startOffset, endOffset) {
        val globalContent = state.editingContent
        val text = globalContent.text
        val safeEnd = endOffset.coerceAtMost(text.length)
        val safeStart = startOffset.coerceAtMost(safeEnd)
        
        val chunkAnnotated = try {
            globalContent.annotatedString.subSequence(safeStart, safeEnd)
        } catch (e: Exception) {
            androidx.compose.ui.text.AnnotatedString("")
        }
        
        val globalSelection = globalContent.selection
        val localSelection = androidx.compose.ui.text.TextRange(
            (globalSelection.start - startOffset).coerceIn(0, safeEnd - safeStart),
            (globalSelection.end - startOffset).coerceIn(0, safeEnd - safeStart)
        )
        
        TextFieldValue(chunkAnnotated, localSelection)
    }

    // Search highlight colours come from the theme. The fixed amber background
    // with black text was chosen against a light surface and turned into a
    // glaring, barely readable band in dark mode.
    val currentMatchBackground = MaterialTheme.colorScheme.primary
    val currentMatchForeground = MaterialTheme.colorScheme.onPrimary
    val otherMatchBackground = MaterialTheme.colorScheme.primaryContainer
    val otherMatchForeground = MaterialTheme.colorScheme.onPrimaryContainer

    // isSearchingInNote is a key as well: it gates the branch below, so leaving
    // it out kept stale highlights on screen after search was dismissed.
    val highlightedChunk = remember(
        chunk,
        state.isSearchingInNote,
        state.noteSearchQuery,
        state.searchResultIndices,
        state.currentSearchResultIndex,
        startOffset,
        currentMatchBackground,
        otherMatchBackground
    ) {
        if (state.isSearchingInNote && state.noteSearchQuery.isNotBlank()) {
            val annotatedString = chunk.annotatedString
            val builder = androidx.compose.ui.text.AnnotatedString.Builder(annotatedString)
            
            val query = state.noteSearchQuery
            val chunkText = chunk.text
            val chunkEndOffset = startOffset + chunkText.length
            
            state.searchResultIndices.forEachIndexed { resultIndex, globalIndex ->
                if (globalIndex >= startOffset && globalIndex < chunkEndOffset) {
                    val localIndex = globalIndex - startOffset
                    val isCurrent = resultIndex == state.currentSearchResultIndex
                    
                    builder.addStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            background = if (isCurrent) currentMatchBackground else otherMatchBackground,
                            color = if (isCurrent) currentMatchForeground else otherMatchForeground
                        ),
                        start = localIndex,
                        end = (localIndex + query.length).coerceAtMost(chunkText.length)
                    )
                }
            }
            chunk.copy(annotatedString = builder.toAnnotatedString())
        } else {
            chunk
        }
    }

    // The tap detector below is installed with pointerInput(Unit), so it must read
    // the *current* chunk through rememberUpdatedState. Capturing the value directly
    // froze the annotations from the first composition, which is why links inserted
    // later in the session were not clickable.
    val currentChunk by rememberUpdatedState(chunk)

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        BasicTextField(
            value = highlightedChunk,
            onValueChange = { newHighlightedChunk ->
                val globalContent = state.editingContent
                val originalAnnotatedString = globalContent.annotatedString
                
                // Calculate global selection
                val newGlobalSelection = androidx.compose.ui.text.TextRange(
                    newHighlightedChunk.selection.start + startOffset,
                    newHighlightedChunk.selection.end + startOffset
                )

                // If text changed, merge it back
                if (newHighlightedChunk.text != chunk.text) {
                    val updatedAnnotatedString = try {
                        originalAnnotatedString.subSequence(0, startOffset) + 
                        newHighlightedChunk.annotatedString + 
                        originalAnnotatedString.subSequence(endOffset.coerceAtMost(originalAnnotatedString.length), originalAnnotatedString.length)
                    } catch (e: Exception) {
                        originalAnnotatedString
                    }
                    
                    onEvent(NotesEvent.OnContentChange(TextFieldValue(
                        annotatedString = updatedAnnotatedString,
                        selection = newGlobalSelection
                    )))
                } else {
                    // Only selection changed
                    onEvent(NotesEvent.OnContentChange(globalContent.copy(selection = newGlobalSelection)))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .pointerInput(Unit) {
                    // Watch the Initial pass: BasicTextField handles taps in the Main
                    // pass and would otherwise place the caret before a regular tap
                    // detector ever sees the gesture. Nothing is consumed until we
                    // know this is a click on a link, so list scrolling still works
                    // when the finger starts on a link and then drags.
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        var movedDistance = 0f
                        var upChange: PointerInputChange? = null

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                upChange = change
                                break
                            }
                            movedDistance += (change.position - change.previousPosition).getDistance()
                            if (movedDistance > viewConfiguration.touchSlop) {
                                // It's a scroll/fling, not a click — leave it alone.
                                break
                            }
                        }

                        val up = upChange ?: return@awaitEachGesture
                        if (movedDistance > viewConfiguration.touchSlop) return@awaitEachGesture

                        val layoutResult = chunkLayoutResult ?: return@awaitEachGesture
                        val position = layoutResult.getOffsetForPosition(up.position)
                        val annotatedString = currentChunk.annotatedString

                        val urlAnnotation = annotatedString
                            .getStringAnnotations("URL", position, position).firstOrNull()
                            ?: annotatedString
                                .getStringAnnotations("EMAIL", position, position).firstOrNull()
                            ?: annotatedString
                                .getStringAnnotations("PHONE", position, position).firstOrNull()

                        if (urlAnnotation != null) {
                            // Consume only now: the text field will not get the up
                            // event, so the caret stays put and the keyboard stays
                            // closed while the link dialog opens.
                            up.consume()
                            val localStart = urlAnnotation.start
                                .coerceIn(0, currentChunk.text.length)
                            val localEnd = urlAnnotation.end
                                .coerceIn(localStart, currentChunk.text.length)
                            currentOnUrlClick(
                                ClickedLink(
                                    url = urlAnnotation.item,
                                    label = currentChunk.text.substring(localStart, localEnd),
                                    start = startOffset + localStart,
                                    end = startOffset + localEnd
                                )
                            )
                        } else {
                            annotatedString
                                .getStringAnnotations("NOTE_LINK", position, position)
                                .firstOrNull()?.let { annotation ->
                                    up.consume()
                                    currentOnEvent(NotesEvent.NavigateToNoteByTitle(annotation.item))
                                }
                        }
                    }
                },
            onTextLayout = { 
                chunkLayoutResult = it 
                onTextLayout(it)
            },
            textStyle = contentTextStyle,
            cursorBrush = SolidColor(cursorColor),
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = chunk.text,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = { 
                        if (index == 0 && chunk.text.isEmpty()) {
                            Text(stringResource(id = R.string.note), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), style = contentTextStyle)
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = cursorColor,
                        selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                            handleColor = cursorColor,
                            backgroundColor = cursorColor.copy(alpha = 0.3f)
                        )
                    ),
                    contentPadding = PaddingValues(horizontal = 0.dp) 
                )
            }
        )
    }
}

@Composable
fun ReminderDisplay(
    reminderTime: Long?,
    repeatOption: String?,
    onClick: () -> Unit
) {
    if (reminderTime != null) {
        val dateTime = java.time.Instant.ofEpochMilli(reminderTime).atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm")
        val formattedTime = formatter.format(dateTime)
        val repeatText = if (repeatOption != null && repeatOption != "NEVER") ", $repeatOption" else ""

        AssistChip(
            onClick = onClick,
            label = { Text(text = "$formattedTime$repeatText") },
            leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = "Reminder") },
            shape = MaterialTheme.shapes.medium,
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.primary,
                leadingIconContentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.springPress()
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun ExpiryDisplay(
    expiryTime: Long?,
    onClick: () -> Unit
) {
    if (expiryTime == null) return

    // The clock used to be read once while composing and then left alone, so the
    // chip kept advertising whatever was true when the editor opened — a note
    // could still claim "2h" left well after it had self-destructed. Re-read it
    // on a ticker so the countdown actually counts down.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(expiryTime) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(30_000L)
        }
    }

    val remaining = expiryTime - now
    if (remaining <= 0) return

    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(remaining)
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(remaining)

    val remainingText = when {
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }

    AssistChip(
        onClick = onClick,
        label = { Text(text = "Self-destruct: $remainingText") },
        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = "Self-destruct") },
        shape = MaterialTheme.shapes.medium,
        colors = AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.error,
            leadingIconContentColor = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        modifier = Modifier.springPress()
    )
    Spacer(modifier = Modifier.height(12.dp))
}
