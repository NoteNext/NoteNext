@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.drawing

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun DrawingScreen(
    windowSizeClass: WindowSizeClass,
    onSave: (Uri) -> Unit,
    onDismiss: () -> Unit,
    viewModel: DrawingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val onEvent = viewModel::onEvent

    // A tool rail and a properties panel only earn their space from Medium up —
    // that is exactly where tablets and unfolded foldables live.
    val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val isWide = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    var showCustomColor by remember { mutableStateOf(false) }
    var showPaperMenu by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    BackHandler(enabled = state.isImmersive) { onEvent(DrawingEvent.ToggleImmersive) }

    val saveDrawing = {
        onEvent(DrawingEvent.SaveDrawing(context) { uri -> if (uri != null) onSave(uri) })
    }

    if (showCustomColor) {
        CustomColorDialog(
            initial = state.color,
            onDismiss = { showCustomColor = false },
            onConfirm = { color ->
                onEvent(DrawingEvent.SelectColor(color))
                showCustomColor = false
            }
        )
    }

    Scaffold(
        modifier = Modifier
            // Outermost so shortcuts still fire when a toolbar button holds focus —
            // key events bubble up the chain to their ancestors.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onKeyEvent false
                when (event.key) {
                    Key.Z -> {
                        onEvent(if (event.isShiftPressed) DrawingEvent.Redo else DrawingEvent.Undo)
                        true
                    }
                    Key.Y -> { onEvent(DrawingEvent.Redo); true }
                    Key.S -> { saveDrawing(); true }
                    Key.Zero -> { onEvent(DrawingEvent.ResetView); true }
                    else -> false
                }
            }
            .focusRequester(focusRequester)
            .focusable(),
        topBar = {
            AnimatedVisibility(
                visible = !state.isImmersive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DrawingTopBar(
                    state = state,
                    showPropertiesToggle = isWide,
                    showPaperMenu = showPaperMenu,
                    onPaperMenuChange = { showPaperMenu = it },
                    onEvent = onEvent,
                    onDismiss = onDismiss,
                    onSave = saveDrawing
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isWide && !state.isImmersive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DrawingBottomBar(
                    state = state,
                    onEvent = onEvent,
                    onPickCustomColor = { showCustomColor = true },
                    showTools = !isTablet
                )
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = isTablet && !state.isImmersive,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                DrawingToolRail(state = state, onEvent = onEvent)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(if (isTablet) 20.dp else 8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 4.dp,
                    color = state.paperTint.paper
                ) {
                    DrawingCanvas(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large)
                    )
                }

                if (state.isEmpty) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(id = R.string.drawing_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = state.paperTint.line
                        )
                        Text(
                            stringResource(id = R.string.drawing_placeholder_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = state.paperTint.line
                        )
                    }
                }

                ViewportControls(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )

                if (state.isImmersive) {
                    FilledTonalIconButton(
                        onClick = { onEvent(DrawingEvent.ToggleImmersive) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .springPress()
                    ) {
                        Icon(
                            Icons.Default.FullscreenExit,
                            contentDescription = stringResource(id = R.string.drawing_exit_fullscreen)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isWide && state.showPropertiesPanel && !state.isImmersive,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                DrawingPropertiesPanel(
                    state = state,
                    onEvent = onEvent,
                    onPickCustomColor = { showCustomColor = true }
                )
            }
        }
    }
}

@Composable
private fun DrawingTopBar(
    state: DrawingState,
    showPropertiesToggle: Boolean,
    showPaperMenu: Boolean,
    onPaperMenuChange: (Boolean) -> Unit,
    onEvent: (DrawingEvent) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.drawing),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        navigationIcon = {
            IconButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.drawing_close_cd))
            }
        },
        actions = {
            IconButton(
                onClick = { onEvent(DrawingEvent.Undo) },
                enabled = state.canUndo,
                modifier = Modifier.springPress()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(id = R.string.drawing_undo_cd)
                )
            }
            IconButton(
                onClick = { onEvent(DrawingEvent.Redo) },
                enabled = state.canRedo,
                modifier = Modifier.springPress()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(id = R.string.drawing_redo_cd)
                )
            }
            IconButton(
                onClick = { onEvent(DrawingEvent.ClearAll) },
                enabled = !state.isEmpty,
                modifier = Modifier.springPress()
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = stringResource(id = R.string.drawing_clear_all_cd)
                )
            }

            Box {
                IconButton(onClick = { onPaperMenuChange(true) }, modifier = Modifier.springPress()) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = stringResource(id = R.string.drawing_paper)
                    )
                }
                PaperMenu(
                    expanded = showPaperMenu,
                    state = state,
                    onEvent = onEvent,
                    onDismiss = { onPaperMenuChange(false) }
                )
            }

            if (showPropertiesToggle) {
                IconButton(
                    onClick = { onEvent(DrawingEvent.TogglePropertiesPanel) },
                    modifier = Modifier.springPress()
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = stringResource(
                            id = if (state.showPropertiesPanel) {
                                R.string.drawing_hide_properties
                            } else {
                                R.string.drawing_properties
                            }
                        )
                    )
                }
            }

            IconButton(
                onClick = { onEvent(DrawingEvent.ToggleImmersive) },
                modifier = Modifier.springPress()
            ) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = stringResource(id = R.string.drawing_fullscreen)
                )
            }

            Spacer(Modifier.width(4.dp))

            if (state.isSaving) {
                LoadingIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
            } else {
                Button(
                    onClick = onSave,
                    enabled = !state.isEmpty,
                    modifier = Modifier.padding(end = 8.dp).springPress(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.save))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun PaperMenu(
    expanded: Boolean,
    state: DrawingState,
    onEvent: (DrawingEvent) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        Text(
            stringResource(id = R.string.drawing_paper),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        PaperStyle.entries.forEach { style ->
            DropdownMenuItem(
                text = { Text(stringResource(id = style.labelRes())) },
                onClick = { onEvent(DrawingEvent.SetPaperStyle(style)) },
                leadingIcon = {
                    RadioButton(selected = state.paperStyle == style, onClick = null)
                }
            )
        }
        HorizontalDivider()
        Text(
            stringResource(id = R.string.drawing_paper_tint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        PaperTint.entries.forEach { tint ->
            DropdownMenuItem(
                text = { Text(stringResource(id = tint.labelRes())) },
                onClick = { onEvent(DrawingEvent.SetPaperTint(tint)) },
                leadingIcon = {
                    ColorWell(
                        color = tint.paper,
                        selected = state.paperTint == tint,
                        onClick = { onEvent(DrawingEvent.SetPaperTint(tint)) },
                        size = 22.dp
                    )
                }
            )
        }
    }
}

@Composable
private fun ViewportControls(
    state: DrawingState,
    onEvent: (DrawingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val pivot = Offset(state.viewport.width / 2f, state.viewport.height / 2f)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            IconButton(onClick = { onEvent(DrawingEvent.ZoomBy(1f / 1.25f, pivot)) }) {
                Icon(
                    Icons.Default.ZoomOut,
                    contentDescription = stringResource(id = R.string.drawing_zoom_out),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                stringResource(id = R.string.drawing_zoom_label, state.zoomPercent),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(52.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { onEvent(DrawingEvent.ZoomBy(1.25f, pivot)) }) {
                Icon(
                    Icons.Default.ZoomIn,
                    contentDescription = stringResource(id = R.string.drawing_zoom_in),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = { onEvent(DrawingEvent.FitToContent) },
                enabled = !state.isEmpty
            ) {
                Icon(
                    Icons.Default.CenterFocusStrong,
                    contentDescription = stringResource(id = R.string.drawing_fit),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { onEvent(DrawingEvent.ResetView) }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.drawing_reset_view),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun PaperStyle.labelRes(): Int = when (this) {
    PaperStyle.PLAIN -> R.string.drawing_paper_plain
    PaperStyle.RULED -> R.string.drawing_paper_ruled
    PaperStyle.GRID -> R.string.drawing_paper_grid
    PaperStyle.DOTS -> R.string.drawing_paper_dots
}

private fun PaperTint.labelRes(): Int = when (this) {
    PaperTint.WHITE -> R.string.drawing_tint_white
    PaperTint.CREAM -> R.string.drawing_tint_cream
    PaperTint.SLATE -> R.string.drawing_tint_slate
    PaperTint.BLACK -> R.string.drawing_tint_black
}
