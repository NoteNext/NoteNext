@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.ui.add_edit_note.components.MentionPopup
import com.suvojeet.notenext.ui.add_edit_note.components.*
import com.suvojeet.notenext.ui.components.AiThinkingIndicator
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.notes.NotesListState
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.notes.NotesUiEvent
import com.suvojeet.notenext.ui.notes.ShareStatusState
import com.suvojeet.notenext.ui.theme.NoteGradients
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.ui.reminder.ReminderSheetContent
import com.suvojeet.notenext.data.RepeatOption
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ImageViewerData(val uri: Uri, val tempId: String)

@Composable
fun AddEditNoteScreen(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    events: SharedFlow<NotesUiEvent>,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass? = null
) {
    // Local UI State
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val colorPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showFormatBar by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var showSlashCommandSheet by remember { mutableStateOf(false) }
    val slashCommandSheetState = rememberModalBottomSheetState()

    var showMoreOptions by remember { mutableStateOf(false) }
    var showShareOptions by remember { mutableStateOf(false) }
    var shareLinkReady by remember { mutableStateOf<NotesUiEvent.ShareLinkReady?>(null) }
    var showShareLinkConfig by remember { mutableStateOf(false) }
    var shareLinkChecking by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showExpiryDialog by remember { mutableStateOf(false) }
    var showInsertLinkDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageData by remember { mutableStateOf<ImageViewerData?>(null) }
    var isFocusMode by remember { mutableStateOf(false) }
    var showToneRewriteSheet by remember { mutableStateOf(false) }

    var aiButtonOffsetX by remember { mutableStateOf(0f) }
    var aiButtonOffsetY by remember { mutableStateOf(0f) }
    var isAiButtonDismissed by remember { mutableStateOf(false) }

    var clickedUrl by remember { mutableStateOf<String?>(null) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val enableRichLinkPreview by settingsRepository.enableRichLinkPreview.collectAsStateWithLifecycle(initialValue = false)

    // Auto-save on background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onEvent(NotesEvent.AutoSaveNote)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission Logic
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
             if (isGranted) {
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                     val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                     if (alarmManager.canScheduleExactAlarms()) {
                         showReminderDialog = true
                     } else {
                         showExactAlarmDialog = true
                     }
                 } else {
                     showReminderDialog = true
                 }
             } else {
                 Toast.makeText(context, "Notifications are required for reminders", Toast.LENGTH_LONG).show()
             }
        }
    )

    val checkAndRequestReminderPermissions: () -> Unit = {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                     val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                     if (alarmManager.canScheduleExactAlarms()) {
                         showReminderDialog = true
                     } else {
                         showExactAlarmDialog = true
                     }
                 } else {
                     showReminderDialog = true
                 }
             } else {
                 notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
             }
        } else {
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                 val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                 if (alarmManager.canScheduleExactAlarms()) {
                     showReminderDialog = true
                 } else {
                     showExactAlarmDialog = true
                 }
             } else {
                 showReminderDialog = true
             }
        }
    }

    // Image/Photo Pickers
    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            onEvent(NotesEvent.ImportImage(uri))
        }
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { uri ->
                onEvent(NotesEvent.ImportImage(uri))
            }
        }
    }

    // Helper to launch camera after permission is granted
    val launchCamera: () -> Unit = {
        try {
            val uri = createImageFile(context)
            photoUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Voice notes
    var showAudioRecorder by remember { mutableStateOf(false) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                showAudioRecorder = true
            } else {
                Toast.makeText(
                    context,
                    context.getString(com.suvojeet.notenext.R.string.audio_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    )
    val startAudioRecording: () -> Unit = {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            showAudioRecorder = true
        } else {
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showAudioRecorder) {
        AudioRecorderSheet(
            onDismiss = { showAudioRecorder = false },
            onSaved = { uri, mimeType ->
                showAudioRecorder = false
                onEvent(NotesEvent.AddAttachment(uri.toString(), mimeType))
            }
        )
    }

    PredictiveBackHandler { progress ->
        try {
            progress.collectLatest { /* handle progress */ }
            if (showToneRewriteSheet) {
                showToneRewriteSheet = false
            } else if (showImageViewer) {
                showImageViewer = false
            } else if (isFocusMode) {
                isFocusMode = false
            } else if (state.isMentionPopupVisible) {
                onEvent(NotesEvent.CloseMentionPopup)
            } else {
                onDismiss()
            }
        } catch (e: Exception) {
            onDismiss()
        }
    }

    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is NotesUiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.onAction?.invoke()
                    }
                }
                is NotesUiEvent.LinkPreviewRemoved -> {
                    snackbarHostState.showSnackbar("Link preview removed")
                }
                is NotesUiEvent.ShareLinkReady -> {
                    shareLinkReady = event
                }
                is NotesUiEvent.ShareLinkStatusUpdated -> {
                    if (event.statusState is ShareStatusState.Expired) {
                        shareLinkReady = null
                    } else if (shareLinkReady?.shareId == event.shareId) {
                        shareLinkReady = shareLinkReady?.copy(statusState = event.statusState)
                    }
                }
                is NotesUiEvent.ShowShareOptions -> {
                    showShareLinkConfig = true
                }
                is NotesUiEvent.ShareLinkChecking -> {
                    shareLinkChecking = event.checking
                }
                is NotesUiEvent.ScrollToSearchResult -> {
                    val globalIndex = event.index
                    val textValue = state.editingContent.text
                    
                    // Re-calculate chunks to find which item to scroll to
                    var startOffset = 0
                    var lineCount = 0
                    var chunkIndex = 0
                    
                    // Account for NoteAttachmentsList and NoteTitleEditor items (2 items)
                    val baseItemCount = 2 

                    for (i in textValue.indices) {
                        if (globalIndex >= startOffset && globalIndex <= i) {
                            // Found it!
                            scope.launch {
                                lazyListState.animateScrollToItem(baseItemCount + chunkIndex)
                            }
                            break
                        }
                        
                        if (textValue[i] == '\n') lineCount++
                        if (lineCount >= 50 || (i - startOffset) >= 5000) {
                            startOffset = i + 1
                            lineCount = 0
                            chunkIndex++
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Theme calculations
    val systemInDarkTheme = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
        else -> false
    }
    val colors = remember(isDarkTheme) { NoteGradients.getNoteColors(isDarkTheme) }
    val adaptiveColor = remember(state.editingColor, isDarkTheme) { NoteGradients.getAdaptiveColor(state.editingColor, isDarkTheme) }
    val backgroundColor = remember(adaptiveColor) { if (adaptiveColor != 0) Color(adaptiveColor) else null } ?: MaterialTheme.colorScheme.surface
    val contentColor = remember(adaptiveColor) { if (adaptiveColor != 0) NoteGradients.getContentColor(adaptiveColor) else null } ?: MaterialTheme.colorScheme.onSurface

    val splitOffsets = remember(state.editingContent.text) {
        val textValue = state.editingContent.text
        val offsets = mutableListOf<Int>()
        var currentStart = 0
        var lineCount = 0
        for (i in textValue.indices) {
            if (textValue[i] == '\n') lineCount++
            if (lineCount >= 50 || (i - currentStart) >= 5000) {
                offsets.add(currentStart)
                currentStart = i + 1
                lineCount = 0
            }
        }
        offsets.add(currentStart)
        offsets
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            // Single owner for the bottom insets: the whole editor lifts above the
            // keyboard (or the navigation bar when it is closed). Children below
            // therefore never add their own IME padding, which is what left a
            // keyboard-sized gap behind after the keyboard was dismissed.
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
    ) {
        Scaffold(
            containerColor = backgroundColor,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                AnimatedVisibility(
                    visible = !isFocusMode,
                    enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing))
                ) {
                    AddEditNoteTopAppBar(
                        state = state,
                        onEvent = onEvent,
                        onDismiss = onDismiss,
                        onToneRewriteClick = { showToneRewriteSheet = true },
                        editingNoteType = state.editingNoteType,
                        onToggleFocusMode = { isFocusMode = !isFocusMode },
                        isFocusMode = isFocusMode,
                        onReminderClick = { checkAndRequestReminderPermissions() },
                        backgroundColor = backgroundColor,
                        contentColor = contentColor
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !isFocusMode,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing))
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shadowElevation = 6.dp
                    ) {
                        AddEditNoteBottomAppBar(
                            showColorPicker = { showColorPicker = !showColorPicker },
                            showFormatBar = { showFormatBar = !showFormatBar },
                            showMoreOptions = { showMoreOptions = it },
                            onAttachmentClick = { showAttachmentSheet = true },
                            backgroundColor = Color.Transparent
                        )
                    }
                }
            }
        ) { padding ->
            // A note that runs the full width of a 12" tablet is unreadable, so the
            // writing column is capped and centred while the note colour stays full-bleed.
            val editorMaxWidth = when (windowSizeClass?.widthSizeClass) {
                WindowWidthSizeClass.Expanded -> 860.dp
                WindowWidthSizeClass.Medium -> 720.dp
                else -> Dp.Unspecified
            }
            SelectionContainer {
                Box(
                    modifier = Modifier.fillMaxSize().background(backgroundColor),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (editorMaxWidth != Dp.Unspecified) {
                                    Modifier.widthIn(max = editorMaxWidth)
                                } else {
                                    Modifier
                                }
                            )
                            .padding(padding)
                    ) {
                        AnimatedVisibility(
                            visible = state.isSearchingInNote,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                        ) {
                            NoteSearchBar(
                                query = state.noteSearchQuery,
                                onQueryChange = { onEvent(NotesEvent.OnNoteSearchQueryChange(it)) },
                                onNext = { onEvent(NotesEvent.NextSearchResult) },
                                onPrevious = { onEvent(NotesEvent.PreviousSearchResult) },
                                onClose = { onEvent(NotesEvent.ToggleNoteSearch) },
                                currentResult = state.currentSearchResultIndex + 1,
                                totalResults = state.searchResultIndices.size,
                                backgroundColor = backgroundColor,
                                contentColor = contentColor
                            )
                        }

                        if (state.editingNoteType == NoteType.TEXT) {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(backgroundColor)
                            ) {
                                item {
                                    NoteAttachmentsList(
                                        attachments = state.editingAttachments,
                                        onEvent = onEvent,
                                        onImageClick = { data ->
                                            selectedImageData = data
                                            showImageViewer = true
                                        }
                                    )
                                }

                                item {
                                    NoteTitleEditor(
                                        state = state,
                                        onEvent = onEvent,
                                        onReminderClick = { checkAndRequestReminderPermissions() }
                                    )
                                }

                                // ─── AI suggestions (Auto-tag + Smart Reminder) ──────
                                item {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        AiSuggestedLabelsRow(
                                            visible = state.suggestedLabels.isNotEmpty(),
                                            suggestions = state.suggestedLabels,
                                            onAcceptLabel = { onEvent(NotesEvent.AcceptSuggestedLabel(it)) },
                                            onDismiss = { onEvent(NotesEvent.DismissSuggestedLabels) }
                                        )
                                        if (state.suggestedLabels.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
                                        AiSmartReminderChip(
                                            visible = state.extractedReminder != null,
                                            reminder = state.extractedReminder,
                                            onSetReminder = { onEvent(NotesEvent.AcceptExtractedReminder) },
                                            onDismiss = { onEvent(NotesEvent.DismissExtractedReminder) }
                                        )
                                        if (state.extractedReminder != null) Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                NoteContentItems(
                                    state = state,
                                    splitOffsets = splitOffsets,
                                    onEvent = onEvent,
                                    onUrlClick = { url -> clickedUrl = url },
                                    onSlashCommand = { showSlashCommandSheet = true },
                                    onTextLayout = { textLayoutResult = it }
                                )

                                if (enableRichLinkPreview && state.linkPreviews.isNotEmpty()) {
                                    item { Spacer(modifier = Modifier.height(16.dp)) }
                                    items(items = state.linkPreviews, key = { it.url }) { linkPreview ->
                                        LinkPreviewCard(linkPreview = linkPreview, onEvent = onEvent)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                // ─── Linked Notes section ────────────────────────────
                                item {
                                    AiLinkedNotesSection(
                                        visible = state.linkedNotes.isNotEmpty(),
                                        notes = state.linkedNotes,
                                        onOpenNote = { note -> onEvent(NotesEvent.OpenLinkedNote(note.id)) }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(120.dp))
                                }
                            }
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(backgroundColor)
                            ) {
                                item {
                                    NoteTitleEditor(
                                        state = state,
                                        onEvent = onEvent,
                                        onReminderClick = { checkAndRequestReminderPermissions() }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                item {
                                     NoteAttachmentsList(
                                        attachments = state.editingAttachments,
                                        onEvent = onEvent,
                                        onImageClick = { data ->
                                            selectedImageData = data
                                            showImageViewer = true
                                        }
                                    )
                                }

                                if (state.editingNoteType == NoteType.CHECKLIST) {
                                    ChecklistEditor(
                                        state = state,
                                        onEvent = onEvent,
                                        isCheckedItemsExpanded = state.isCheckedItemsExpanded,
                                        onToggleCheckedItems = { onEvent(NotesEvent.ToggleCheckedItemsExpanded) },
                                        backgroundColor = backgroundColor
                                    )
                                } else {
                                    NoteContentItems(
                                        state = state,
                                        splitOffsets = splitOffsets,
                                        onEvent = onEvent,
                                        onUrlClick = { url -> clickedUrl = url },
                                        onSlashCommand = { showSlashCommandSheet = true },
                                        onTextLayout = { textLayoutResult = it }
                                    )

                                    // A text note can carry tick boxes appended below its body.
                                    // They only appear once the user actually adds one, so plain
                                    // notes look unchanged.
                                    if (state.editingChecklist.isNotEmpty()) {
                                        item { Spacer(modifier = Modifier.height(8.dp)) }
                                        ChecklistEditor(
                                            state = state,
                                            onEvent = onEvent,
                                            isCheckedItemsExpanded = state.isCheckedItemsExpanded,
                                            onToggleCheckedItems = { onEvent(NotesEvent.ToggleCheckedItemsExpanded) },
                                            backgroundColor = backgroundColor
                                        )
                                    }
                                }

                                if (enableRichLinkPreview && state.linkPreviews.isNotEmpty()) {
                                    item { Spacer(modifier = Modifier.height(16.dp)) }
                                    items(items = state.linkPreviews, key = { it.url }) { linkPreview ->
                                        LinkPreviewCard(linkPreview = linkPreview, onEvent = onEvent)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(120.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showFormatBar && (state.editingNoteType == NoteType.TEXT || state.editingNoteType == NoteType.CHECKLIST),
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)) + androidx.compose.animation.scaleIn(initialScale = 0.9f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing)) + androidx.compose.animation.scaleOut(targetScale = 0.9f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        ) {
            Surface(
                shadowElevation = 12.dp,
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                FormatToolbar(
                    state = state, 
                    onEvent = onEvent, 
                    onInsertLinkClick = { showInsertLinkDialog = true }, 
                    onGrammarFixClick = { onEvent(NotesEvent.FixGrammar) },
                    isFixingGrammar = state.isFixingGrammar,
                    themeMode = themeMode,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        var showAiChecklistSheet by remember { mutableStateOf(false) }
        val showAiButton = (state.editingNoteType == NoteType.TEXT && state.editingContent.text.isEmpty()) || 
                           (state.editingNoteType == NoteType.CHECKLIST && state.editingChecklist.isEmpty())
                           
        AnimatedVisibility(
            visible = showAiButton && !isFocusMode && !isAiButtonDismissed,
            enter = fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)) + slideInVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) { it } + androidx.compose.animation.scaleIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing), initialScale = 0.8f),
            exit = fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing)) + slideOutVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) { it } + androidx.compose.animation.scaleOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing), targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(aiButtonOffsetX.roundToInt(), aiButtonOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        aiButtonOffsetX += dragAmount.x
                        aiButtonOffsetY += dragAmount.y
                    }
                }
                .padding(bottom = 88.dp, end = 20.dp)
        ) {
            AiAssistantButton(
                onClick = { showAiChecklistSheet = true },
                onDismiss = { isAiButtonDismissed = true }
            )
        }

        AnimatedVisibility(
            visible = state.fixedContentPreview != null,
            enter = androidx.compose.animation.scaleIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            exit = androidx.compose.animation.scaleOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 84.dp, end = 16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    IconButton(onClick = { onEvent(NotesEvent.ApplyGrammarFix) }, modifier = Modifier.springPress()) {
                        Icon(Icons.Filled.Check, contentDescription = "Accept", tint = Color(0xFF4CAF50))
                    }
                    IconButton(onClick = { onEvent(NotesEvent.ClearGrammarFix) }, modifier = Modifier.springPress()) {
                        Icon(Icons.Filled.Close, contentDescription = "Discard", tint = Color(0xFFE57373))
                    }
                }
            }
        }

        val aiPromptHistory by settingsRepository.aiPromptHistory.collectAsStateWithLifecycle(initialValue = emptyList<String>())

        AiChecklistSheet(
            isVisible = showAiChecklistSheet,
            isGenerating = state.isGeneratingChecklist,
            generatedItems = state.generatedChecklistPreview,
            promptHistory = aiPromptHistory,
            onDismiss = { 
                showAiChecklistSheet = false
                onEvent(NotesEvent.ClearGeneratedChecklist)
            },
            onGenerate = { topic -> onEvent(NotesEvent.GenerateChecklist(topic)) },
            onInsert = { editedItems -> onEvent(NotesEvent.InsertGeneratedChecklist(editedItems)) },
            onRegenerate = { topic -> onEvent(NotesEvent.GenerateChecklist(topic)) },
            onShowError = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        )
    }

    val createTxtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { onEvent(NotesEvent.ExportNote(it, "TXT")) }
    }
    val createMdLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        uri?.let { onEvent(NotesEvent.ExportNote(it, "MD")) }
    }

    AddEditNoteDialogs(
        state = state,
        onEvent = onEvent,
        showDeleteDialog = showDeleteDialog,
        onShowDeleteDialogChange = { showDeleteDialog = it },
        showMoreOptions = showMoreOptions,
        onShowMoreOptionsChange = { showMoreOptions = it },
        onShare = { showShareOptions = true },
        showLabelDialog = showLabelDialog,
        onShowLabelDialogChange = { showLabelDialog = it },
        showSaveAsDialog = showSaveAsDialog,
        onShowSaveAsDialogChange = { showSaveAsDialog = it },
        showHistoryDialog = showHistoryDialog,
        onShowHistoryDialogChange = { showHistoryDialog = it },
        showExpiryDialog = showExpiryDialog,
        onShowExpiryDialogChange = { showExpiryDialog = it },
        showInsertLinkDialog = showInsertLinkDialog,
        onShowInsertLinkDialogChange = { showInsertLinkDialog = it },
        clickedUrl = clickedUrl,
        onClickedUrlChange = { clickedUrl = it },
        showExactAlarmDialog = showExactAlarmDialog,
        onShowExactAlarmDialogChange = { showExactAlarmDialog = it },
        settingsRepository = settingsRepository,
        scope = scope,
        onSaveAsPdf = {
            scope.launch {
                val fullHtml = com.suvojeet.notenext.util.NoteHtmlGenerator.generateNoteHtml(
                    context,
                    state.editingTitle,
                    state.editingContent.annotatedString,
                    state.editingAttachments
                )
                com.suvojeet.notenext.util.printNote(context, fullHtml, state.editingTitle.ifBlank { "Note Document" })
            }
        },
        onSaveAsTxt = {
            createTxtLauncher.launch("${state.editingTitle.ifBlank { "Untitled" }}.txt")
        },
        onSaveAsMd = {
             createMdLauncher.launch("${state.editingTitle.ifBlank { "Untitled" }}.md")
        }
    )

    if (showShareOptions) {
        com.suvojeet.notenext.ui.components.ShareOptionsDialog(
            onDismiss = { showShareOptions = false },
            onShareAsText = {
                val shareContent = buildString {
                    if (state.editingNoteType != com.suvojeet.notenext.core.model.NoteType.CHECKLIST) {
                        append(state.editingContent.text)
                    }
                    // Tick boxes belong to both note types — a text note's checkboxes
                    // used to be dropped from the shared text entirely.
                    if (state.editingChecklist.isNotEmpty()) {
                        if (isNotEmpty()) append('\n')
                        state.editingChecklist.forEach { item ->
                            append(if (item.isChecked) "[x] " else "[ ] ")
                            append(item.text)
                            append('\n')
                        }
                    }
                }
                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, state.editingTitle + "\n\n" + shareContent)
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                showShareOptions = false
            },
            onShareViaLink = {
                onEvent(NotesEvent.ShareCurrentNoteViaLink)
                showShareOptions = false
            }
        )
    }

    if (showShareLinkConfig) {
        com.suvojeet.notenext.ui.components.ShareLinkConfigDialog(
            onDismiss = { showShareLinkConfig = false },
            onCreate = { expiry, burn, maxReads ->
                showShareLinkConfig = false
                onEvent(NotesEvent.ConfirmShareViaLink(expiry, burn, maxReads))
            }
        )
    }

    if (shareLinkChecking) {
        com.suvojeet.notenext.ui.components.ShareLinkCheckingDialog()
    }

    shareLinkReady?.let { link ->
        com.suvojeet.notenext.ui.components.ShareLinkDialog(
            url = link.url,
            statusState = link.statusState,
            noteToken = link.noteToken,
            onDismiss = { shareLinkReady = null },
            onShare = {
                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, link.url)
                }
                context.startActivity(
                    android.content.Intent.createChooser(sendIntent, context.getString(com.suvojeet.notenext.R.string.share_via_link))
                )
                shareLinkReady = null
            },
            onOpen = {
                // Open the link — App Links route it back into the in-app collaborative
                // editor when verified, otherwise it opens in the browser.
                val viewIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(link.url)
                )
                context.startActivity(viewIntent)
                shareLinkReady = null
            },
            onUpdateSharedNote = link.noteToken?.let { token ->
                {
                    onEvent(NotesEvent.UpdateSharedNote(link.shareId, token))
                    shareLinkReady = null
                }
            },
            onStopSharing = link.noteToken?.let { token ->
                {
                    onEvent(NotesEvent.UnshareNote(link.shareId, token))
                    shareLinkReady = null
                }
            }
        )
    }

    if (showImageViewer) {
        selectedImageData?.let { data ->
            ImageViewerScreen(
                imageUri = data.uri,
                attachmentTempId = data.tempId,
                onDismiss = { showImageViewer = false },
                onEvent = onEvent
            )
        }
    }
    
    if (state.showSummaryDialog) {
        AiSummarySheet(
            summary = state.summaryResult,
            isSummarizing = state.isSummarizing,
            onDismiss = { onEvent(NotesEvent.ClearSummary) },
            onClearSummary = { onEvent(NotesEvent.ClearSummary) }
        )
    }

    AiActionItemsSheet(
        isVisible = state.showActionItemsSheet,
        isExtracting = state.isExtractingTasks,
        extractedTasks = state.extractedTasksPreview,
        onDismiss = { onEvent(NotesEvent.ClearExtractedActionItems) },
        onSaveToTodo = { tasks -> onEvent(NotesEvent.SaveActionItemsToTodo(tasks)) }
    )

    AnimatedVisibility(
        visible = showToneRewriteSheet,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        ToneRewriteScreen(
            state = state,
            onEvent = onEvent,
            onBack = { showToneRewriteSheet = false }
        )
    }

    if (showReminderDialog) {        ModalBottomSheet(
            onDismissRequest = { showReminderDialog = false },
            sheetState = reminderSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            val currentReminderDateTime: java.time.ZonedDateTime? = state.editingReminderTime?.let {
                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault())
            }
            ReminderSheetContent(
                initialDate = currentReminderDateTime?.toLocalDate(),
                initialTime = currentReminderDateTime?.toLocalTime(),
                initialRepeatOption = state.editingRepeatOption?.let { name ->
                    RepeatOption.entries.find { it.name == name }
                } ?: RepeatOption.NEVER,
                onDismissRequest = { showReminderDialog = false },
                onConfirm = { date: LocalDate, time: LocalTime, repeat: RepeatOption ->
                    val reminderMillis = LocalDateTime.of(date, time)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onEvent(NotesEvent.OnReminderChange(reminderMillis, repeat.name))
                    showReminderDialog = false
                }
            )
        }
    }

    if (showSlashCommandSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSlashCommandSheet = false },
            sheetState = slashCommandSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            SlashCommandSheetContent(
                onDismissRequest = { showSlashCommandSheet = false },
                onCommandSelected = { command ->
                    showSlashCommandSheet = false
                    when (command.title) {
                        "Heading 1" -> onEvent(NotesEvent.ApplyHeadingStyle(1))
                        // Appends a single tick box below the note. Converting the whole
                        // note to a checklist is a separate, explicit action in the
                        // "More options" sheet ("List View").
                        "Checklist" -> onEvent(NotesEvent.AddChecklistItem)
                        "Image" -> getContent.launch("image/*")
                        "Bulleted List" -> onEvent(NotesEvent.ApplyBulletedList)
                    }
                }
            )
        }
    }

    if (showColorPicker) {
        ModalBottomSheet(
            onDismissRequest = { showColorPicker = false },
            sheetState = colorPickerSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Text(
                text = "Color",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ColorPicker(
                colors = colors,
                editingColor = state.editingColor,
                onEvent = onEvent
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showAttachmentSheet) {
        AttachmentSheet(
            onDismiss = { showAttachmentSheet = false },
            onTakePhotoClick = {
                val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (hasCameraPermission) {
                    launchCamera()
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            },
            onImageClick = { getContent.launch("image/*") },
            // Drawing is a standalone canvas reachable from the home FAB. It can't
            // be inserted into the note currently being edited, so the row is
            // hidden rather than left as a button that only shows a "not
            // implemented" toast.
            onAudioClick = startAudioRecording,
            onTickBoxesClick = { onEvent(NotesEvent.AddChecklistItem) }
        )
    }
}
