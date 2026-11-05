@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.theme.NoteGradients
import com.suvojeet.notenext.util.HtmlConverter

@Composable
fun NoteItem(
    modifier: Modifier = Modifier,
    note: NoteSummaryWithAttachments,
    isSelected: Boolean,
    searchQuery: String = "",
    onNoteClick: () -> Unit,
    onNoteLongClick: () -> Unit,
    binnedDaysRemaining: Int? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val colorScheme = MaterialTheme.colorScheme
    val onSurface = colorScheme.onSurface
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    // Long-press = enter selection mode → give a tactile confirmation. Wrapping the
    // callback here covers every long-press path (card + text tap-gesture) in one place.
    val haptic = LocalHapticFeedback.current
    val onNoteLongClickHaptic = remember(onNoteLongClick, haptic) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onNoteLongClick()
        }
    }

    // The text body installs a pointerInput(Unit) tap detector. Without reading the
    // latest callbacks through state, that detector kept the first composition's
    // lambda — which still thought multi-select was off, so tapping a selected note
    // opened it instead of toggling the selection.
    val currentOnNoteClick by rememberUpdatedState(onNoteClick)
    val currentOnNoteLongClick by rememberUpdatedState(onNoteLongClickHaptic)

    val adaptiveColor = remember(note.note.color, isDarkTheme) {
        NoteGradients.getAdaptiveColor(note.note.color, isDarkTheme)
    }
    val isDefaultColor = adaptiveColor == 0

    val contentColor = remember(isDefaultColor, adaptiveColor, onSurface) {
        if (isDefaultColor) {
            onSurface
        } else {
            NoteGradients.getContentColor(adaptiveColor)
        }
    }
    
    val tintColor = remember(isDefaultColor, contentColor, onSurfaceVariant) {
        if (isDefaultColor) {
            onSurfaceVariant
        } else {
            contentColor.copy(alpha = 0.7f)
        }
    }

    val decryptedNote = remember(note.note.title, note.note.content, note.note.isEncrypted) {
        if (note.note.isEncrypted) {
            if (note.note.isLocked) {
                // Never attempt to decrypt locked notes without auth.
                // Tap-to-unlock is handled by the click handler in NotesScreen.
                note.note
            } else {
                // Non-locked encrypted notes use the non-auth key — safe to decrypt here.
                com.suvojeet.notenext.util.CryptoUtils.decryptNote(note.note)
            }
        } else {
            note.note
        }
    }
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val cardShape = MaterialTheme.shapes.medium

    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, primary)
    } else if (isDefaultColor) {
        BorderStroke(1.dp, outlineVariant)
    } else {
        null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onNoteClick,
                onLongClick = onNoteLongClickHaptic
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isDefaultColor) surfaceContainer else Color(adaptiveColor),
            contentColor = contentColor
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (note.note.isPinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = stringResource(id = R.string.pinned_note_description),
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.End),
                        tint = tintColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (decryptedNote.title.isNotEmpty()) {
                    val unescapedTitle = remember(decryptedNote.title) {
                        androidx.core.text.HtmlCompat.fromHtml(decryptedNote.title, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                    }
                    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
                    
                    val titleText = remember(unescapedTitle, searchQuery, primaryContainer, onPrimaryContainer) {
                        if (searchQuery.isNotEmpty()) {
                            buildAnnotatedString {
                                val text = unescapedTitle
                                append(text)
                                val lowerText = text.lowercase()
                                val lowerQuery = searchQuery.lowercase()
                                var index = lowerText.indexOf(lowerQuery)
                                while (index >= 0) {
                                    addStyle(
                                        style = SpanStyle(
                                            background = primaryContainer,
                                            color = onPrimaryContainer
                                        ),
                                        start = index,
                                        end = index + searchQuery.length
                                    )
                                    index = lowerText.indexOf(lowerQuery, index + searchQuery.length)
                                }
                            }
                        } else {
                            androidx.compose.ui.text.AnnotatedString(unescapedTitle)
                        }
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (decryptedNote.isLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(id = com.suvojeet.notenext.core.R.string.note_locked_content_cd),
                            modifier = Modifier.size(24.dp),
                            tint = tintColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = com.suvojeet.notenext.core.R.string.note_locked_message),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    // A TEXT note can carry tick boxes below its body, so checklist items
                    // alone are enough to render a preview for either note type.
                    if ((decryptedNote.noteType == NoteType.TEXT && decryptedNote.content.isNotEmpty()) || note.checklistItems.isNotEmpty()) {
                        if (decryptedNote.noteType == NoteType.TEXT && decryptedNote.content.isNotEmpty()) {
                            val rawContentLength = decryptedNote.content.length
                            
                            val typography = MaterialTheme.typography
                            val headlineSmall = typography.headlineSmall
                            val bodyLarge = typography.bodyLarge
                            val bodyMedium = typography.bodyMedium

                            val (textStyle, maxLines, fontWeight) = remember(rawContentLength, decryptedNote.title, headlineSmall, bodyLarge, bodyMedium) {
                                val style = when {
                                    rawContentLength < 100 -> headlineSmall to 6
                                    rawContentLength < 250 -> bodyLarge to 8
                                    else -> bodyMedium to 10
                                }
                                val weight = if (decryptedNote.title.isEmpty() && rawContentLength < 100) FontWeight.SemiBold else FontWeight.Normal
                                Triple(style.first, style.second, weight)
                            }
    
                            val annotatedContent = remember(decryptedNote.content) {
                                val plainText = HtmlConverter.cleanHtmlToPlain(decryptedNote.content)
                                androidx.compose.ui.text.AnnotatedString(plainText)
                            }

                            val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                            val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

                            val highlightedContent = remember(annotatedContent, searchQuery, primaryContainer, onPrimaryContainer) {
                                if (searchQuery.isNotEmpty()) {
                                    buildAnnotatedString {
                                        append(annotatedContent)
                                        val lowerText = annotatedContent.text.lowercase()
                                        val lowerQuery = searchQuery.lowercase()
                                        var index = lowerText.indexOf(lowerQuery)
                                        while (index >= 0) {
                                            addStyle(
                                                style = SpanStyle(
                                                    background = primaryContainer,
                                                    color = onPrimaryContainer
                                                ),
                                                start = index,
                                                end = index + searchQuery.length
                                            )
                                            index = lowerText.indexOf(lowerQuery, index + searchQuery.length)
                                        }
                                    }
                                } else {
                                    annotatedContent
                                }
                            }
    
                            val uriHandler = LocalUriHandler.current
                            val currentHighlightedContent by rememberUpdatedState(highlightedContent)
                            var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

                            Text(
                                text = highlightedContent,
                                style = textStyle.copy(
                                    fontWeight = fontWeight,
                                    color = if (isDefaultColor) MaterialTheme.colorScheme.onSurfaceVariant else contentColor.copy(alpha = 0.9f)
                                ),
                                maxLines = maxLines,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textLayoutResult = it },
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            currentOnNoteLongClick()
                                        },
                                        onTap = { pos ->
                                            val layoutResult = textLayoutResult ?: return@detectTapGestures
                                            val offset = layoutResult.getOffsetForPosition(pos)
                                            
                                            var isLink = false
                                            currentHighlightedContent.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let { annotation ->
                                                isLink = true
                                                try { uriHandler.openUri(annotation.item) } catch (e: Exception) { e.printStackTrace() }
                                            }
                                            if (!isLink) {
                                                currentHighlightedContent.getStringAnnotations(tag = "EMAIL", start = offset, end = offset).firstOrNull()?.let { annotation ->
                                                    isLink = true
                                                    try { uriHandler.openUri(annotation.item) } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }
                                            if (!isLink) {
                                                currentHighlightedContent.getStringAnnotations(tag = "PHONE", start = offset, end = offset).firstOrNull()?.let { annotation ->
                                                    isLink = true
                                                    try { uriHandler.openUri(annotation.item) } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }
                                            
                                            if (!isLink) {
                                                currentOnNoteClick()
                                            }
                                        }
                                    )
                                }
                            )
                        } else {
                            ChecklistPreview(note.checklistItems, if (isDefaultColor) MaterialTheme.colorScheme.onSurface else contentColor, searchQuery)
                        }

                        // Text body was rendered above — append its tick boxes underneath.
                        if (decryptedNote.noteType == NoteType.TEXT &&
                            decryptedNote.content.isNotEmpty() &&
                            note.checklistItems.isNotEmpty()
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ChecklistPreview(note.checklistItems, if (isDefaultColor) MaterialTheme.colorScheme.onSurface else contentColor, searchQuery)
                        }
                    }

                    if (note.note.linkPreviews.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinkPreviewDisplay(
                            linkPreview = note.note.linkPreviews.first(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (note.attachments.isNotEmpty() || !note.note.label.isNullOrEmpty() || note.note.reminderTime != null || binnedDaysRemaining != null || note.note.expiryTime != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (note.attachments.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Attachment,
                                    contentDescription = stringResource(id = R.string.attachment_icon_description),
                                    modifier = Modifier.size(16.dp),
                                    tint = tintColor
                                )
                            }

                            note.note.reminderTime?.let {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = stringResource(id = R.string.reminder_icon_description),
                                    modifier = Modifier.size(16.dp),
                                    tint = tintColor
                                )
                            }

                            note.note.expiryTime?.let { expiry ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = stringResource(id = com.suvojeet.notenext.core.R.string.note_self_destruct_cd),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                    
                                    val expiredLabel = stringResource(id = com.suvojeet.notenext.core.R.string.note_expired)
                                    val remainingText = remember(expiry, expiredLabel) {
                                        val remaining = expiry - System.currentTimeMillis()
                                        if (remaining <= 0) {
                                            expiredLabel
                                        } else {
                                            val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(remaining)
                                            val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
                                            val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(remaining)

                                            when {
                                                days > 0 -> "${days}d"
                                                hours > 0 -> "${hours}h"
                                                else -> "${minutes}m"
                                            }
                                        }
                                    }
                                    
                                    Text(
                                        text = remainingText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            val label = note.note.label
                            if (!label.isNullOrEmpty()) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isDefaultColor) MaterialTheme.colorScheme.secondaryContainer else contentColor.copy(alpha = 0.15f)
                                ) {
                                    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                                    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

                                    val labelText = remember(label, searchQuery, primaryContainer, onPrimaryContainer) {
                                        if (searchQuery.isNotEmpty()) {
                                            buildAnnotatedString {
                                                append(label)
                                                val lowerText = label.lowercase()
                                                val lowerQuery = searchQuery.lowercase()
                                                var index = lowerText.indexOf(lowerQuery)
                                                while (index >= 0) {
                                                    addStyle(
                                                        style = SpanStyle(
                                                            background = primaryContainer,
                                                            color = onPrimaryContainer
                                                        ),
                                                        start = index,
                                                        end = index + searchQuery.length
                                                    )
                                                    index = lowerText.indexOf(lowerQuery, index + searchQuery.length)
                                                }
                                            }
                                        } else {
                                            androidx.compose.ui.text.AnnotatedString(label)
                                        }
                                    }

                                    Text(
                                        text = labelText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDefaultColor) MaterialTheme.colorScheme.onSecondaryContainer else contentColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (binnedDaysRemaining != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.days_left, binnedDaysRemaining),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistPreview(checklistItems: List<ChecklistItem>, contentColor: Color, searchQuery: String = "") {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        checklistItems.take(5).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (item.isChecked) colorScheme.primary else contentColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                val unescapedItemText = remember(item.text) {
                    androidx.core.text.HtmlCompat.fromHtml(item.text, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                }
                
                val primaryContainer = colorScheme.primaryContainer
                val onPrimaryContainer = colorScheme.onPrimaryContainer

                val itemText = remember(unescapedItemText, searchQuery, primaryContainer, onPrimaryContainer) {
                    if (searchQuery.isNotEmpty()) {
                        buildAnnotatedString {
                            val text = unescapedItemText
                            append(text)
                            val lowerText = text.lowercase()
                            val lowerQuery = searchQuery.lowercase()
                            var index = lowerText.indexOf(lowerQuery)
                            while (index >= 0) {
                                addStyle(
                                    style = SpanStyle(
                                        background = primaryContainer,
                                        color = onPrimaryContainer
                                    ),
                                    start = index,
                                    end = index + searchQuery.length
                                )
                                index = lowerText.indexOf(lowerQuery, index + searchQuery.length)
                            }
                        }
                    } else {
                        androidx.compose.ui.text.AnnotatedString(unescapedItemText)
                    }
                }

                Text(
                    text = itemText,
                    fontSize = 14.sp,
                    color = if (item.isChecked) contentColor.copy(alpha = 0.6f) else contentColor.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (item.isChecked) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle()
                )
            }
        }
        if (checklistItems.size > 5) {
            Text(
                text = "...",
                fontSize = 14.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
