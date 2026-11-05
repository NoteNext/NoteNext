@file:OptIn(ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress

/**
 * Google Keep–style attachment bottom sheet shown from the editor's "+" button.
 * Reuses the same ModalBottomSheet config as [MoreOptionsSheet] so the slide-up
 * animation matches exactly. Rows: Take photo, Add image, Drawing (only when
 * the caller can handle it), Recording, Tick boxes.
 */
@Composable
fun AttachmentSheet(
    onDismiss: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onImageClick: () -> Unit,
    onDrawingClick: (() -> Unit)? = null,
    onAudioClick: () -> Unit,
    onTickBoxesClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

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
                .padding(bottom = 24.dp)
        ) {
            AttachmentRow(Icons.Outlined.PhotoCamera, stringResource(id = R.string.take_photo)) {
                onDismiss(); onTakePhotoClick()
            }
            AttachmentRow(Icons.Outlined.Image, stringResource(id = R.string.add_image)) {
                onDismiss(); onImageClick()
            }
            if (onDrawingClick != null) {
                AttachmentRow(Icons.Outlined.Brush, stringResource(id = R.string.drawing)) {
                    onDismiss(); onDrawingClick()
                }
            }
            AttachmentRow(Icons.Outlined.Mic, stringResource(id = R.string.audio_recording)) {
                onDismiss(); onAudioClick()
            }
            AttachmentRow(Icons.Outlined.CheckBox, stringResource(id = R.string.tick_boxes)) {
                onDismiss(); onTickBoxesClick()
            }
        }
    }
}

@Composable
private fun AttachmentRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .springPress()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
