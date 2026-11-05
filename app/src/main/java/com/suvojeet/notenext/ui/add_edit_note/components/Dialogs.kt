@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun SaveAsDialog(
    onDismiss: () -> Unit,
    onSaveAsPdf: () -> Unit,
    onSaveAsTxt: () -> Unit,
    onSaveAsMd: () -> Unit,
    onSaveAsNote: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.save_as)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSaveAsNote != null) {
                    SaveAsOption(
                        icon = Icons.Rounded.Description,
                        text = stringResource(id = R.string.save_as_note),
                        onClick = onSaveAsNote
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                SaveAsOption(
                    icon = Icons.Rounded.PictureAsPdf,
                    text = "PDF Document",
                    onClick = onSaveAsPdf
                )
                SaveAsOption(
                    icon = Icons.Rounded.Description,
                    text = "Plain Text (.txt)",
                    onClick = onSaveAsTxt
                )
                SaveAsOption(
                    icon = Icons.Rounded.Description,
                    text = "Markdown (.md)",
                    onClick = onSaveAsMd
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun SaveAsOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .springPress()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun InsertLinkDialog(
    onDismiss: () -> Unit,
    onInsertLink: (String, String) -> Unit,
    initialText: String = ""
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.link_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(id = R.string.link_dialog_display_text)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(id = R.string.link_dialog_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onInsertLink(text, url) },
                // A label is optional: when it is empty the URL becomes the link text.
                enabled = url.isNotBlank(),
                modifier = Modifier.springPress()
            ) {
                Text(stringResource(id = R.string.link_dialog_insert))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun LinkActionDialog(
    url: String,
    onDismiss: () -> Unit,
    onOpenLink: () -> Unit,
    onCopyLink: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.link_dialog_section_options)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                Spacer(modifier = Modifier.height(8.dp))
                
                SaveAsOption(icon = Icons.Rounded.OpenInNew, text = "Open in Browser", onClick = onOpenLink)
                SaveAsOption(icon = Icons.Rounded.ContentCopy, text = "Copy URL", onClick = onCopyLink)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
