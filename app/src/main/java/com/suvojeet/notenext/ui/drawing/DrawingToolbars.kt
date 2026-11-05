@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package com.suvojeet.notenext.ui.drawing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress

internal data class ToolSpec(val tool: DrawingTool, val icon: ImageVector, val label: Int)

internal val InkTools = listOf(
    ToolSpec(DrawingTool.PEN, Icons.Default.Create, R.string.drawing_tool_pen),
    ToolSpec(DrawingTool.PENCIL, Icons.Default.Edit, R.string.drawing_tool_pencil),
    ToolSpec(DrawingTool.MARKER, Icons.Default.Brush, R.string.drawing_tool_marker),
    ToolSpec(DrawingTool.HIGHLIGHTER, Icons.Default.BorderColor, R.string.drawing_tool_highlighter),
    ToolSpec(DrawingTool.ERASER, Icons.Default.AutoFixHigh, R.string.drawing_tool_eraser)
)

internal val ShapeTools = listOf(
    ToolSpec(DrawingTool.LINE, Icons.Default.HorizontalRule, R.string.drawing_tool_line),
    ToolSpec(DrawingTool.ARROW, Icons.Default.NorthEast, R.string.drawing_tool_arrow),
    ToolSpec(DrawingTool.RECTANGLE, Icons.Default.CropSquare, R.string.drawing_tool_rectangle),
    ToolSpec(DrawingTool.OVAL, Icons.Default.RadioButtonUnchecked, R.string.drawing_tool_oval)
)

// ── Tablet: the vertical tool rail ────────────────────────────────────────

@Composable
fun DrawingToolRail(
    state: DrawingState,
    onEvent: (DrawingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(96.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RailSectionLabel(R.string.drawing_section_tools)
            InkTools.forEach { spec ->
                ToolRailItem(
                    spec = spec,
                    selected = state.tool == spec.tool,
                    accent = if (spec.tool.isEraser) null else state.color,
                    onClick = { onEvent(DrawingEvent.SelectTool(spec.tool)) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            RailSectionLabel(R.string.drawing_section_shapes)
            ShapeTools.forEach { spec ->
                ToolRailItem(
                    spec = spec,
                    selected = state.tool == spec.tool,
                    accent = state.color,
                    onClick = { onEvent(DrawingEvent.SelectTool(spec.tool)) }
                )
            }
        }
    }
}

@Composable
private fun RailSectionLabel(label: Int) {
    Text(
        text = stringResource(id = label),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun ToolRailItem(
    spec: ToolSpec,
    selected: Boolean,
    accent: Color?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(spec.icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(id = spec.label),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selected && accent != null) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

// ── Tablet: the properties panel ──────────────────────────────────────────

@Composable
fun DrawingPropertiesPanel(
    state: DrawingState,
    onEvent: (DrawingEvent) -> Unit,
    onPickCustomColor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(288.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PanelHeader(R.string.drawing_color, Icons.Default.Palette)
            ColorGrid(
                selected = state.color,
                isEraser = state.tool.isEraser,
                onSelect = { onEvent(DrawingEvent.SelectColor(it)) },
                onCustom = onPickCustomColor
            )

            if (state.recentColors.isNotEmpty()) {
                Text(
                    stringResource(id = R.string.drawing_recent_colors),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.recentColors.forEach { color ->
                        ColorWell(
                            color = color,
                            selected = state.color == color && !state.tool.isEraser,
                            onClick = { onEvent(DrawingEvent.SelectColor(color)) },
                            size = 36.dp
                        )
                    }
                }
            }

            HorizontalDivider()

            PanelHeader(R.string.drawing_size, Icons.Default.LineWeight)
            WidthControls(state, onEvent)

            if (!state.tool.isEraser) {
                PanelHeader(R.string.drawing_opacity, Icons.Default.Opacity)
                Slider(
                    value = state.activeAlpha,
                    onValueChange = { onEvent(DrawingEvent.SetOpacity(it)) },
                    valueRange = 0.05f..1f
                )
            }

            if (state.tool.isEraser) {
                PanelHeader(R.string.drawing_eraser_mode, Icons.Default.AutoFixHigh)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.eraserMode == EraserMode.PIXEL,
                        onClick = { onEvent(DrawingEvent.SetEraserMode(EraserMode.PIXEL)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text(stringResource(id = R.string.drawing_eraser_pixel), maxLines = 1) }
                    SegmentedButton(
                        selected = state.eraserMode == EraserMode.STROKE,
                        onClick = { onEvent(DrawingEvent.SetEraserMode(EraserMode.STROKE)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text(stringResource(id = R.string.drawing_eraser_stroke), maxLines = 1) }
                }
            }

            HorizontalDivider()

            PanelHeader(R.string.drawing_stylus, Icons.Default.Tune)
            SwitchRow(
                title = stringResource(id = R.string.drawing_palm_rejection),
                subtitle = stringResource(id = R.string.drawing_palm_rejection_desc),
                checked = state.stylusOnly,
                onCheckedChange = { onEvent(DrawingEvent.SetStylusOnly(it)) }
            )
            Text(
                stringResource(id = R.string.drawing_pressure),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = state.pressureSensitivity,
                onValueChange = { onEvent(DrawingEvent.SetPressureSensitivity(it)) },
                valueRange = 0f..1f
            )
            SwitchRow(
                title = stringResource(id = R.string.drawing_smoothing),
                subtitle = null,
                checked = state.smoothing,
                onCheckedChange = { onEvent(DrawingEvent.SetSmoothing(it)) }
            )

            HorizontalDivider()

            PanelHeader(R.string.drawing_export, Icons.Default.Check)
            SwitchRow(
                title = stringResource(id = R.string.drawing_trim),
                subtitle = null,
                checked = state.trimToContent,
                onCheckedChange = { onEvent(DrawingEvent.SetTrimToContent(it)) }
            )
            SwitchRow(
                title = stringResource(id = R.string.drawing_transparent_bg),
                subtitle = null,
                checked = state.transparentBackground,
                onCheckedChange = { onEvent(DrawingEvent.SetTransparentBackground(it)) }
            )

            Text(
                stringResource(id = R.string.drawing_shortcut_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PanelHeader(label: Int, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(id = label),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun WidthControls(state: DrawingState, onEvent: (DrawingEvent) -> Unit) {
    val presets = if (state.tool.isEraser) EraserWidthPresets else BrushWidthPresets
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { preset ->
            val selected = kotlin.math.abs(state.activeWidth - preset) < 0.5f
            Surface(
                onClick = { onEvent(DrawingEvent.SetWidth(preset)) },
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size((6 + preset / 6f).dp.coerceAtMost(28.dp))
                            .clip(CircleShape)
                            .background(
                                if (state.tool.isEraser) MaterialTheme.colorScheme.onSurfaceVariant
                                else state.color
                            )
                    )
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = state.activeWidth,
            onValueChange = { onEvent(DrawingEvent.SetWidth(it)) },
            valueRange = 1f..if (state.tool.isEraser) 160f else 80f,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(id = R.string.drawing_stroke_size_label, state.activeWidth.toInt()),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun ColorGrid(
    selected: Color,
    isEraser: Boolean,
    onSelect: (Color) -> Unit,
    onCustom: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DrawingPalette.forEach { color ->
            ColorWell(
                color = color,
                selected = !isEraser && selected == color,
                onClick = { onSelect(color) },
                size = 40.dp
            )
        }
        Surface(
            onClick = onCustom,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Colorize,
                    contentDescription = stringResource(id = R.string.drawing_custom_color),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ColorWell(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp = 34.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .springPress()
    )
}

// ── Phone: the bottom bar ─────────────────────────────────────────────────

@Composable
fun DrawingBottomBar(
    state: DrawingState,
    onEvent: (DrawingEvent) -> Unit,
    onPickCustomColor: () -> Unit,
    /** Medium widths already show the vertical rail, so the tool row is dropped there. */
    showTools: Boolean = true
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            AnimatedVisibility(
                visible = state.showBrushSettings,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WidthControls(state, onEvent)
                    if (state.tool.isEraser) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = state.eraserMode == EraserMode.PIXEL,
                                onClick = { onEvent(DrawingEvent.SetEraserMode(EraserMode.PIXEL)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text(stringResource(id = R.string.drawing_eraser_pixel), maxLines = 1) }
                            SegmentedButton(
                                selected = state.eraserMode == EraserMode.STROKE,
                                onClick = { onEvent(DrawingEvent.SetEraserMode(EraserMode.STROKE)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text(stringResource(id = R.string.drawing_eraser_stroke), maxLines = 1) }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Opacity,
                                contentDescription = stringResource(id = R.string.drawing_opacity),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Slider(
                                value = state.activeAlpha,
                                onValueChange = { onEvent(DrawingEvent.SetOpacity(it)) },
                                valueRange = 0.05f..1f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = stringResource(id = R.string.drawing_pressure),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = state.pressureSensitivity,
                            onValueChange = { onEvent(DrawingEvent.SetPressureSensitivity(it)) },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SwitchRow(
                        title = stringResource(id = R.string.drawing_palm_rejection),
                        subtitle = null,
                        checked = state.stylusOnly,
                        onCheckedChange = { onEvent(DrawingEvent.SetStylusOnly(it)) }
                    )
                    SwitchRow(
                        title = stringResource(id = R.string.drawing_smoothing),
                        subtitle = null,
                        checked = state.smoothing,
                        onCheckedChange = { onEvent(DrawingEvent.SetSmoothing(it)) }
                    )
                }
            }

            // Tools
            if (showTools) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (InkTools + ShapeTools).forEach { spec ->
                        CompactToolButton(
                            spec = spec,
                            selected = state.tool == spec.tool,
                            onClick = { onEvent(DrawingEvent.SelectTool(spec.tool)) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
            }

            // Ink
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(items = DrawingPalette, key = { it.toArgb() }) { color ->
                        ColorWell(
                            color = color,
                            selected = state.color == color && !state.tool.isEraser,
                            onClick = { onEvent(DrawingEvent.SelectColor(color)) },
                            size = 30.dp
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onPickCustomColor,
                    modifier = Modifier.size(44.dp).springPress()
                ) {
                    Icon(
                        Icons.Default.Colorize,
                        contentDescription = stringResource(id = R.string.drawing_custom_color)
                    )
                }
                FilledTonalIconButton(
                    onClick = { onEvent(DrawingEvent.ToggleBrushSettings) },
                    modifier = Modifier.size(44.dp).springPress()
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = stringResource(id = R.string.drawing_settings_cd)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactToolButton(spec: ToolSpec, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.springPress()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(spec.icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                stringResource(id = spec.label),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Custom colour picker ──────────────────────────────────────────────────

@Composable
fun CustomColorDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val startHsv = remember(initial) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initial.toArgb(), hsv)
        hsv
    }
    var hue by remember { mutableFloatStateOf(startHsv[0]) }
    var saturation by remember { mutableFloatStateOf(startHsv[1]) }
    var value by remember { mutableFloatStateOf(startHsv[2]) }

    val preview = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.drawing_custom_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(preview)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                )
                LabelledSlider(R.string.drawing_hue, hue, 0f, 360f) { hue = it }
                LabelledSlider(R.string.drawing_saturation, saturation, 0f, 1f) { saturation = it }
                LabelledSlider(R.string.drawing_brightness, value, 0f, 1f) { value = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(preview) }) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun LabelledSlider(
    label: Int,
    value: Float,
    from: Float,
    to: Float,
    onChange: (Float) -> Unit
) {
    Column {
        Text(
            stringResource(id = label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(value = value, onValueChange = onChange, valueRange = from..to)
    }
}
