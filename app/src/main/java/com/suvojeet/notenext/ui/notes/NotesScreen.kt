@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as StaggeredGridItems
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.compose.ui.platform.LocalFocusManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.add_edit_note.components.PinnedReorderSheet
import com.suvojeet.notenext.ui.components.*
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.core.util.SortType
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import com.suvojeet.notenext.ui.reminder.ReminderSetDialog
import com.suvojeet.notenext.util.findActivity
import kotlinx.coroutines.flow.SharedFlow
import com.suvojeet.notenext.navigation.Destination

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onSettingsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onEditLabelsClick: () -> Unit = {},
    onBinClick: () -> Unit = {},
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit = {},
    onDrawingClick: () -> Unit = {},
    onTodoClick: () -> Unit = {},
    onOpenSharedNote: (String, String?) -> Unit = { _, _ -> },
    events: SharedFlow<NotesUiEvent>,
    windowSizeClass: WindowSizeClass? = null
) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val lastSeenVersion by viewModel.lastSeenVersion.collectAsStateWithLifecycle()
    var isFabExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showFeatureGuide by remember { mutableStateOf(false) }
    
    val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
        else -> false
    }

    val isSelectionModeActive = listState.selectedNoteIds.isNotEmpty()

    // Tablets and unfolded foldables get more note columns and a capped reading
    // width, so a wide screen stops looking like a stretched phone.
    val widthClass = windowSizeClass?.widthSizeClass
    val gridColumns = when (widthClass) {
        WindowWidthSizeClass.Expanded -> 4
        WindowWidthSizeClass.Medium -> 3
        else -> 2
    }
    val listMaxWidth = when (widthClass) {
        WindowWidthSizeClass.Expanded -> 720.dp
        WindowWidthSizeClass.Medium -> 640.dp
        else -> Dp.Unspecified
    }
    val contentPadding = if (widthClass == WindowWidthSizeClass.Compact || widthClass == null) 8.dp else 16.dp

    var showLabelDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReminderSetDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showMoveToProjectDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var showPinnedReorderSheet by remember { mutableStateOf(false) }
    var shareLinkReady by remember { mutableStateOf<NotesUiEvent.ShareLinkReady?>(null) }
    var showShareLinkConfig by remember { mutableStateOf(false) }
    var shareLinkChecking by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val activity = context.findActivity() as? androidx.fragment.app.FragmentActivity
    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            com.suvojeet.notenext.util.BiometricAuthManager(context, activity)
        }
    } else {
        null
    }

    LaunchedEffect(lastSeenVersion) {
        if (lastSeenVersion != -1 && lastSeenVersion < 39) {
            showFeatureGuide = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotesUiEvent.SendNotes -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, event.title)
                        putExtra(Intent.EXTRA_TEXT, event.content)
                    }
                    val chooser = Intent.createChooser(intent, context.getString(R.string.send_notes_via))
                    context.startActivity(chooser)
                }

                // The editor is composed on top of this screen and collects the
                // same flow. When it's open, let it own snackbars too — otherwise
                // this host queues messages that surface later, after the editor
                // closes, as stale duplicates.
                is NotesUiEvent.ShowSnackbar -> {
                    if (viewModel.editState.value.expandedNoteId == null) {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            event.onAction?.invoke()
                        }
                    }
                }
                is NotesUiEvent.LinkPreviewRemoved -> {
                    if (viewModel.editState.value.expandedNoteId == null) {
                        snackbarHostState.showSnackbar(context.getString(R.string.link_preview_removed))
                    }
                }
                is NotesUiEvent.ProjectCreated -> {
                    if (viewModel.editState.value.expandedNoteId == null) {
                        snackbarHostState.showSnackbar(context.getString(R.string.project_created, event.projectName))
                    }
                }
                is NotesUiEvent.NavigateToNoteByTitle -> {
                    val noteId = viewModel.getNoteIdByTitle(event.title)
                    if (noteId != null) {
                        viewModel.onEvent(NotesEvent.ExpandNote(noteId as Int))
                    } else {
                        snackbarHostState.showSnackbar("Note \"${event.title}\" not found")
                    }
                }
                is NotesUiEvent.ScrollToSearchResult -> {}
                // The editor (AddEditNoteScreen) is composed on top of this screen and
                // collects the SAME events flow. When it's open, let IT own the share
                // dialogs — otherwise both surfaces render them and the dialog appears on
                // the home screen behind the editor. Only handle share UI here when the
                // editor is collapsed (i.e. sharing was triggered from the list).
                is NotesUiEvent.ShareLinkReady -> {
                    if (viewModel.editState.value.expandedNoteId == null) shareLinkReady = event
                }
                is NotesUiEvent.ShareLinkStatusUpdated -> {
                    if (viewModel.editState.value.expandedNoteId == null) {
                        if (event.statusState is ShareStatusState.Expired) {
                            shareLinkReady = null
                        } else if (shareLinkReady?.shareId == event.shareId) {
                            shareLinkReady = shareLinkReady?.copy(statusState = event.statusState)
                        }
                    }
                }
                is NotesUiEvent.ShowShareOptions -> {
                    if (viewModel.editState.value.expandedNoteId == null) showShareLinkConfig = true
                }
                is NotesUiEvent.ShareLinkChecking -> {
                    if (viewModel.editState.value.expandedNoteId == null) shareLinkChecking = event.checking
                }
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchActive || isSelectionModeActive || editState.expandedNoteId != null) {
        when {
            isSearchActive -> {
                isSearchActive = false
                focusManager.clearFocus()
            }
            isSelectionModeActive -> viewModel.onEvent(NotesEvent.ClearSelection)
            editState.expandedNoteId != null -> viewModel.onEvent(NotesEvent.CollapseNote)
        }
    }

    val gridState = rememberLazyStaggeredGridState()
    val lazyListState = rememberLazyListState()

    // Disable item placement animation while the list is being scrolled. When the screen
    // returns from the note editor (AnimatedContent recomposes the disposed list), scrolling
    // re-measures off-screen items and the staggered grid recomputes their lanes; with the
    // placement spring active this manifests as notes visibly shuffling before settling.
    // Reordering/pinning still animates because those happen while the list is idle.
    val isListScrolling by remember {
        derivedStateOf { gridState.isScrollInProgress || lazyListState.isScrollInProgress }
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = editState.expandedNoteId,
            label = "NoteTransition",
            transitionSpec = {
                val springSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)
                if (targetState != null) {
                    (fadeIn(spring()) + scaleIn(initialScale = 0.85f, animationSpec = springSpec))
                        .togetherWith(fadeOut(spring()))
                } else {
                    fadeIn(spring())
                        .togetherWith(fadeOut(spring()) + scaleOut(targetScale = 0.85f, animationSpec = springSpec))
                }
            }
        ) { expandedId ->
            if (expandedId == null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            AnimatedContent(
                                targetState = isSelectionModeActive,
                                transitionSpec = {
                                    fadeIn(animationSpec = spring()).togetherWith(fadeOut(animationSpec = spring()))
                                },
                                label = "TopAppBar Animation"
                            ) { targetState ->
                                if (targetState) {
                                    val isAllPinned = listState.selectedNoteIds.all { id ->
                                        listState.pinnedNotes.any { it.note.id == id }
                                    }
                                    ContextualTopAppBar(
                                        selectedItemCount = listState.selectedNoteIds.size,
                                        isPinned = isAllPinned,
                                        onClearSelection = { viewModel.onEvent(NotesEvent.ClearSelection) },
                                        onTogglePinClick = { viewModel.onEvent(NotesEvent.TogglePinForSelectedNotes) },
                                        onReminderClick = { showReminderSetDialog = true },
                                        onColorClick = { showColorPickerDialog = true },
                                        onArchiveClick = { viewModel.onEvent(NotesEvent.ArchiveSelectedNotes) },
                                        onDeleteClick = { showDeleteDialog = true },
                                        onCopyClick = { viewModel.onEvent(NotesEvent.CopySelectedNotes) },
                                        onSendClick = { showShareOptionsDialog = true },
                                        onLabelClick = { showLabelDialog = true },
                                        onMoveToProjectClick = { showMoveToProjectDialog = true },
                                        onLockClick = { 
                                            biometricAuthManager?.showBiometricPrompt(
                                                onAuthSuccess = { viewModel.onEvent(NotesEvent.ToggleLockForSelectedNotes) },
                                                onAuthError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                            )
                                        },
                                        onSelectAllClick = { viewModel.onEvent(NotesEvent.SelectAllNotes) }
                                    )
                                } else {
                                    TopAppBar(
                                        title = {
                                            SearchBar(
                                                searchQuery = listState.searchQuery,
                                                onSearchQueryChange = { viewModel.onEvent(NotesEvent.OnSearchQueryChange(it)) },
                                                isSearchActive = isSearchActive,
                                                onSearchActiveChange = { isSearchActive = it },
                                                onLayoutToggleClick = { viewModel.onEvent(NotesEvent.ToggleLayout) },
                                                onSortClick = { showSortMenu = true },
                                                layoutType = listState.layoutType,
                                                sortMenuExpanded = showSortMenu,
                                                onSortMenuDismissRequest = { showSortMenu = false },
                                                onSortOptionClick = { sortType ->
                                                    val newSortType = if (sortType == listState.sortType) {
                                                        SortType.DATE_MODIFIED
                                                    } else {
                                                        sortType
                                                    }
                                                    viewModel.onEvent(NotesEvent.SortNotes(newSortType))
                                                },
                                                currentSortType = listState.sortType
                                            )
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = onMenuClick) {
                                                Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                                            }
                                        },
                                        scrollBehavior = scrollBehavior,
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            val isFabScrollExpanded by remember {
                                derivedStateOf {
                                    when (listState.layoutType) {
                                        LayoutType.GRID -> gridState.firstVisibleItemIndex == 0
                                        LayoutType.LIST -> lazyListState.firstVisibleItemIndex == 0
                                    }
                                }
                            }

                            MultiActionFab(
                                isExpanded = isFabExpanded,
                                onExpandedChange = { isFabExpanded = it },
                                onNoteClick = {
                                    viewModel.onEvent(NotesEvent.ExpandNote(-1))
                                    isFabExpanded = false
                                },
                                onChecklistClick = {
                                    viewModel.onEvent(NotesEvent.ExpandNote(-1, NoteType.CHECKLIST))
                                    isFabExpanded = false
                                },
                                onProjectClick = {
                                    showCreateProjectDialog = true
                                    isFabExpanded = false
                                },
                                onDrawingClick = {
                                    onDrawingClick()
                                    isFabExpanded = false
                                },
                                onTodoClick = {
                                    onTodoClick()
                                    isFabExpanded = false
                                },
                                themeMode = themeMode,
                                isScrollExpanded = isFabScrollExpanded
                            )
                        }
                    ) { padding ->
                        val autoDeleteDays by settingsRepository.autoDeleteDays.collectAsStateWithLifecycle(initialValue = 7)
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                shape = MaterialTheme.shapes.extraLarge,
                                title = { Text(stringResource(id = R.string.move_to_bin_question)) },
                                text = { Text(stringResource(id = R.string.move_to_bin_message, autoDeleteDays)) },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.onEvent(NotesEvent.DeleteSelectedNotes)
                                            showDeleteDialog = false
                                        },
                                        modifier = Modifier.springPress()
                                    ) {
                                        Text(stringResource(id = R.string.move_to_bin))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }, modifier = Modifier.springPress()) {
                                        Text(stringResource(id = R.string.cancel))
                                    }
                                }
                            )
                        }
                        if (showLabelDialog) {
                            LabelDialog(
                                labels = listState.labels,
                                onDismiss = { showLabelDialog = false },
                                onConfirm = { label ->
                                    viewModel.onEvent(NotesEvent.SetLabelForSelectedNotes(label))
                                    showLabelDialog = false
                                }
                            )
                        }
                        if (showReminderSetDialog) {
                            ReminderSetDialog(
                                onDismissRequest = { showReminderSetDialog = false },
                                onConfirm = { date, time, repeatOption ->
                                    viewModel.onEvent(NotesEvent.SetReminderForSelectedNotes(date, time, repeatOption))
                                    showReminderSetDialog = false
                                }
                            )
                        }

                        if (showColorPickerDialog) {
                            ColorSelectionDialog(
                                onDismiss = { showColorPickerDialog = false },
                                onColorSelected = { color ->
                                    viewModel.onEvent(NotesEvent.ChangeColorForSelectedNotes(color))
                                    showColorPickerDialog = false
                                },
                                themeMode = themeMode
                            )
                        }

                        if (showCreateProjectDialog) {
                            CreateProjectDialog(
                                onDismiss = { showCreateProjectDialog = false },
                                onConfirm = { projectName ->
                                    viewModel.onEvent(NotesEvent.CreateProject(projectName))
                                    showCreateProjectDialog = false
                                }
                            )
                        }

                        if (showMoveToProjectDialog) {
                            MoveToProjectDialog(
                                projects = listState.projects,
                                onDismiss = { showMoveToProjectDialog = false },
                                onConfirm = { projectId ->
                                    viewModel.onEvent(NotesEvent.MoveSelectedNotesToProject(projectId))
                                    showMoveToProjectDialog = false
                                }
                            )
                        }

                        if (showShareOptionsDialog) {
                            ShareOptionsDialog(
                                onDismiss = {
                                    showShareOptionsDialog = false
                                },
                                onShareAsText = {
                                    viewModel.onEvent(NotesEvent.SendSelectedNotes)
                                    showShareOptionsDialog = false
                                },
                                onShareViaLink = {
                                    viewModel.onEvent(NotesEvent.ShareSelectedNotesViaLink)
                                    showShareOptionsDialog = false
                                }
                            )
                        }

                        if (showShareLinkConfig) {
                            ShareLinkConfigDialog(
                                onDismiss = { showShareLinkConfig = false },
                                onCreate = { expiry, burn, maxReads ->
                                    showShareLinkConfig = false
                                    viewModel.onEvent(NotesEvent.ConfirmShareViaLink(expiry, burn, maxReads))
                                }
                            )
                        }

                        if (shareLinkChecking) {
                            ShareLinkCheckingDialog()
                        }

                        shareLinkReady?.let { link ->
                            ShareLinkDialog(
                                url = link.url,
                                statusState = link.statusState,
                                noteToken = link.noteToken,
                                onDismiss = { shareLinkReady = null },
                                onShare = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, link.title)
                                        putExtra(Intent.EXTRA_TEXT, link.url)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, context.getString(R.string.share_via_link))
                                    )
                                    shareLinkReady = null
                                },
                                onOpen = {
                                    val id = link.shareId
                                    // The decryption key rides in the link fragment (#...).
                                    val key = link.url.substringAfter('#', "").takeIf { it.isNotBlank() }
                                    shareLinkReady = null
                                    onOpenSharedNote(id, key)
                                },
                                onUpdateSharedNote = link.noteToken?.let { token ->
                                    {
                                        viewModel.onEvent(NotesEvent.UpdateSharedNote(link.shareId, token))
                                        shareLinkReady = null
                                    }
                                },
                                onStopSharing = link.noteToken?.let { token ->
                                    {
                                        viewModel.onEvent(NotesEvent.UnshareNote(link.shareId, token))
                                        shareLinkReady = null
                                    }
                                }
                            )
                        }

                        if (showFeatureGuide) {
                            com.suvojeet.notenext.ui.components.ShareFeatureGuideDialog(
                                onDismiss = {
                                    showFeatureGuide = false
                                    viewModel.onFeatureGuideDismissed(39)
                                }
                            )
                        }

                        if (showPinnedReorderSheet && listState.pinnedNotes.size >= 2) {
                            PinnedReorderSheet(
                                pinnedNotes = listState.pinnedNotes,
                                onDismiss = { showPinnedReorderSheet = false },
                                onConfirm = { orderedIds ->
                                    viewModel.onEvent(NotesEvent.ReorderPinnedNotes(orderedIds))
                                    showPinnedReorderSheet = false
                                }
                            )
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            val pagedNotes = listState.pagedNotes.collectAsLazyPagingItems()
                            val pinnedNotes = listState.pinnedNotes

                            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val isNotesEmpty = pinnedNotes.isEmpty() && 
                                                 pagedNotes.itemCount == 0 && 
                                                 pagedNotes.loadState.refresh is androidx.paging.LoadState.NotLoading
                                
                                val isLoading = listState.isLoading || pagedNotes.loadState.refresh is androidx.paging.LoadState.Loading

                                if (isLoading) {
                                    ExpressiveLoading()
                                } else if (isNotesEmpty) {
                                    val currentLabel = listState.filteredLabel
                                    val emptyMessage = if (currentLabel != null) {
                                        stringResource(id = R.string.no_notes_found_label, currentLabel)
                                    } else if (listState.searchQuery.isNotEmpty()) {
                                        stringResource(id = R.string.no_notes_found)
                                    } else {
                                        stringResource(id = R.string.no_notes_yet)
                                    }
                                    
                                    val emptyIcon = if (listState.searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.Note

                                    EmptyState(
                                        icon = emptyIcon,
                                        message = emptyMessage,
                                        description = if (listState.searchQuery.isEmpty()) stringResource(id = R.string.create_your_first_note) else null
                                    )
                                } else {
                                    val onNoteClickAction: (com.suvojeet.notenext.data.NoteSummaryWithAttachments) -> Unit = { note ->
                                        if (isSelectionModeActive) {
                                            viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id))
                                        } else {
                                            if (note.note.isLocked) {
                                                // The NoteNext key uses TIME-BASED auth (validity = 60s via
                                                // setUserAuthenticationParameters). For time-based keys, cipher.init()
                                                // throws UserNotAuthenticatedException BEFORE auth — so we must NOT
                                                // pre-init a cipher or wrap it in a CryptoObject here.
                                                // The correct flow: authenticate → key unlocked for 60s →
                                                // ExpandNote calls decryptNote → cipher.init() succeeds in that window.
                                                biometricAuthManager?.showBiometricPrompt(
                                                    onAuthSuccess = {
                                                        viewModel.onEvent(NotesEvent.ExpandNote(note.note.id))
                                                    },
                                                    onAuthError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                                ) ?: Toast.makeText(context, "Biometrics not available", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.onEvent(NotesEvent.ExpandNote(note.note.id))
                                            }
                                        }
                                    }

                                    when (listState.layoutType) {
                                        LayoutType.GRID -> {
                                            LazyVerticalStaggeredGrid(
                                                columns = StaggeredGridCells.Fixed(gridColumns),
                                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                                state = gridState,
                                                contentPadding = PaddingValues(contentPadding),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalItemSpacing = 8.dp
                                            ) {
                                                if (pinnedNotes.isNotEmpty()) {
                                                    item(span = StaggeredGridItemSpan.FullLine) {
                                                        PinnedSectionHeader(
                                                            count = pinnedNotes.size,
                                                            onReorderClick = { showPinnedReorderSheet = true }
                                                        )
                                                    }
                                                    StaggeredGridItems(
                                                        items = pinnedNotes,
                                                        key = { it.note.id },
                                                        contentType = { it.note.noteType }
                                                    ) { note ->
                                                        val noteModifier = Modifier
                                                            .animateItem(placementSpec = if (isListScrolling) null else androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                                                            .sharedElement(
                                                                rememberSharedContentState(key = "note-${note.note.id}"),
                                                                animatedVisibilityScope = this@AnimatedContent
                                                            )
                                                        
                                                        val onNoteClick = remember(note, onNoteClickAction) {
                                                            { onNoteClickAction(note) }
                                                        }
                                                        val onNoteLongClick = remember(note.note.id) {
                                                            { viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id)) }
                                                        }

                                                        SwipeableNoteItem(
                                                            modifier = noteModifier,
                                                            note = note,
                                                            isSelected = listState.selectedNoteIds.contains(note.note.id),
                                                            searchQuery = listState.searchQuery,
                                                            isDarkTheme = isDarkTheme,
                                                            swipeEnabled = listState.layoutType == LayoutType.LIST,
                                                            isSelectionModeActive = isSelectionModeActive,
                                                            onNoteClick = onNoteClick,
                                                            onNoteLongClick = onNoteLongClick,
                                                            onArchive = { viewModel.onEvent(NotesEvent.ArchiveNote(note)) },
                                                            onBin = { viewModel.onEvent(NotesEvent.DeleteNote(note)) }
                                                        )
                                                    }
                                                }

                                                if (pagedNotes.itemCount > 0) {
                                                    if (pinnedNotes.isNotEmpty()) {
                                                        item(span = StaggeredGridItemSpan.FullLine) {
                                                            Text(
                                                                text = stringResource(id = R.string.others),
                                                                modifier = Modifier.padding(8.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    
                                                    // Paged items
                                                    items(
                                                        count = pagedNotes.itemCount,
                                                        key = pagedNotes.itemKey { it.note.id },
                                                        contentType = pagedNotes.itemContentType { it.note.noteType }
                                                    ) { index ->
                                                        pagedNotes[index]?.let { note ->
                                                            val noteModifier = Modifier
                                                                .animateItem(placementSpec = if (isListScrolling) null else androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                                                                .sharedElement(
                                                                    rememberSharedContentState(key = "note-${note.note.id}"),
                                                                    animatedVisibilityScope = this@AnimatedContent
                                                                )
                                                            
                                                            val onNoteClick = remember(note, onNoteClickAction) {
                                                                { onNoteClickAction(note) }
                                                            }
                                                            val onNoteLongClick = remember(note.note.id) {
                                                                { viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id)) }
                                                            }

                                                            SwipeableNoteItem(
                                                                modifier = noteModifier,
                                                                note = note,
                                                                isSelected = listState.selectedNoteIds.contains(note.note.id),
                                                                searchQuery = listState.searchQuery,
                                                                isDarkTheme = isDarkTheme,
                                                                swipeEnabled = listState.layoutType == LayoutType.LIST,
                                                                isSelectionModeActive = isSelectionModeActive,
                                                                onNoteClick = onNoteClick,
                                                                onNoteLongClick = onNoteLongClick,
                                                                onArchive = { viewModel.onEvent(NotesEvent.ArchiveNote(note)) },
                                                                onBin = { viewModel.onEvent(NotesEvent.DeleteNote(note)) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        LayoutType.LIST -> {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .then(
                                                        if (listMaxWidth != Dp.Unspecified) {
                                                            Modifier.widthIn(max = listMaxWidth)
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .fillMaxWidth()
                                                    .align(Alignment.CenterHorizontally),
                                                state = lazyListState,
                                                contentPadding = PaddingValues(contentPadding),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (pinnedNotes.isNotEmpty()) {
                                                    item {
                                                        PinnedSectionHeader(
                                                            count = pinnedNotes.size,
                                                            onReorderClick = { showPinnedReorderSheet = true }
                                                        )
                                                    }
                                                    items(
                                                        items = pinnedNotes,
                                                        key = { it.note.id },
                                                        contentType = { it.note.noteType }
                                                    ) { note ->
                                                        val noteModifier = Modifier
                                                            .animateItem(placementSpec = if (isListScrolling) null else androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                                                            .sharedElement(
                                                                rememberSharedContentState(key = "note-${note.note.id}"),
                                                                animatedVisibilityScope = this@AnimatedContent
                                                            )
                                                        
                                                        val onNoteClick = remember(note, onNoteClickAction) {
                                                            { onNoteClickAction(note) }
                                                        }
                                                        val onNoteLongClick = remember(note.note.id) {
                                                            { viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id)) }
                                                        }

                                                        SwipeableNoteItem(
                                                            modifier = noteModifier,
                                                            note = note,
                                                            isSelected = listState.selectedNoteIds.contains(note.note.id),
                                                            searchQuery = listState.searchQuery,
                                                            isDarkTheme = isDarkTheme,
                                                            swipeEnabled = listState.layoutType == LayoutType.LIST,
                                                            isSelectionModeActive = isSelectionModeActive,
                                                            onNoteClick = onNoteClick,
                                                            onNoteLongClick = onNoteLongClick,
                                                            onArchive = { viewModel.onEvent(NotesEvent.ArchiveNote(note)) },
                                                            onBin = { viewModel.onEvent(NotesEvent.DeleteNote(note)) }
                                                        )
                                                    }
                                                }

                                                if (pagedNotes.itemCount > 0) {
                                                    if (pinnedNotes.isNotEmpty()) {
                                                        item {
                                                            Text(
                                                                text = stringResource(id = R.string.others),
                                                                modifier = Modifier.padding(8.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    
                                                    // Paged items
                                                    items(
                                                        count = pagedNotes.itemCount,
                                                        key = pagedNotes.itemKey { it.note.id },
                                                        contentType = pagedNotes.itemContentType { it.note.noteType }
                                                    ) { index ->
                                                        pagedNotes[index]?.let { note ->
                                                            val noteModifier = Modifier
                                                                .animateItem(placementSpec = if (isListScrolling) null else androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                                                                .sharedElement(
                                                                    rememberSharedContentState(key = "note-${note.note.id}"),
                                                                    animatedVisibilityScope = this@AnimatedContent
                                                                )
                                                            
                                                            val onNoteClick = remember(note, onNoteClickAction) {
                                                                { onNoteClickAction(note) }
                                                            }
                                                            val onNoteLongClick = remember(note.note.id) {
                                                                { viewModel.onEvent(NotesEvent.ToggleNoteSelection(note.note.id)) }
                                                            }

                                                            SwipeableNoteItem(
                                                                modifier = noteModifier,
                                                                note = note,
                                                                isSelected = listState.selectedNoteIds.contains(note.note.id),
                                                                searchQuery = listState.searchQuery,
                                                                isDarkTheme = isDarkTheme,
                                                                swipeEnabled = listState.layoutType == LayoutType.LIST,
                                                                isSelectionModeActive = isSelectionModeActive,
                                                                onNoteClick = onNoteClick,
                                                                onNoteLongClick = onNoteLongClick,
                                                                onArchive = { viewModel.onEvent(NotesEvent.ArchiveNote(note)) },
                                                                onBin = { viewModel.onEvent(NotesEvent.DeleteNote(note)) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (isFabExpanded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            onClick = { isFabExpanded = false },
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                 // Flush any pending (debounced) auto-save when the app is backgrounded
                 // while the editor is open, so edits made in the last ~2s aren't lost if
                 // the process is killed before the debounce timer fires.
                 val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                 DisposableEffect(lifecycleOwner) {
                     val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                         if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                             viewModel.onEvent(NotesEvent.AutoSaveNote)
                         }
                     }
                     lifecycleOwner.lifecycle.addObserver(observer)
                     onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                 }
                 AddEditNoteScreen(
                    state = editState,
                    onEvent = viewModel::onEvent,
                    onDismiss = { viewModel.onEvent(NotesEvent.CollapseNote) },
                    themeMode = themeMode,
                    settingsRepository = settingsRepository,
                    events = viewModel.events,
                    windowSizeClass = windowSizeClass,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "note-${expandedId}"),
                        animatedVisibilityScope = this@AnimatedContent
                    )
                )
            }
        }
    }
}

/**
 * Header for the "Pinned" section. Shows the section label and, when there are
 * at least two pinned notes, a reorder affordance that opens [PinnedReorderSheet].
 */
@Composable
private fun PinnedSectionHeader(
    count: Int,
    onReorderClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.pinned),
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        if (count >= 2) {
            IconButton(
                onClick = onReorderClick,
                modifier = Modifier.size(32.dp).springPress()
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Reorder pinned notes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Wraps a [NoteItem] with swipe-to-act gestures (LIST layout only):
 *  - swipe right (StartToEnd) → archive
 *  - swipe left  (EndToStart) → move to bin
 * Both surface an Undo snackbar from the ViewModel. Gestures are disabled while
 * multi-selecting so they don't fight tap-to-select.
 */
@Composable
private fun SwipeableNoteItem(
    note: com.suvojeet.notenext.data.NoteSummaryWithAttachments,
    isSelected: Boolean,
    searchQuery: String,
    isDarkTheme: Boolean,
    swipeEnabled: Boolean,
    isSelectionModeActive: Boolean,
    onNoteClick: () -> Unit,
    onNoteLongClick: () -> Unit,
    onArchive: () -> Unit,
    onBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Swipe is meaningful only in the LIST layout. In GRID (or while selecting)
    // render the bare card so there's no behavior change there.
    if (!swipeEnabled) {
        NoteItem(
            modifier = modifier,
            note = note,
            isSelected = isSelected,
            searchQuery = searchQuery,
            onNoteClick = onNoteClick,
            onNoteLongClick = onNoteLongClick,
            isDarkTheme = isDarkTheme
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onArchive(); true }
                SwipeToDismissBoxValue.EndToStart -> { onBin(); true }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = !isSelectionModeActive,
        enableDismissFromEndToStart = !isSelectionModeActive,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isArchiveDir = direction == SwipeToDismissBoxValue.StartToEnd
            val bg = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            val fg = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onTertiaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(bg)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isArchiveDir) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = if (isArchiveDir) Icons.Filled.Archive else Icons.Filled.Delete,
                    contentDescription = if (isArchiveDir)
                        stringResource(id = R.string.archive)
                    else
                        stringResource(id = R.string.move_to_bin),
                    tint = fg
                )
            }
        }
    ) {
        NoteItem(
            note = note,
            isSelected = isSelected,
            searchQuery = searchQuery,
            onNoteClick = onNoteClick,
            onNoteLongClick = onNoteLongClick,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.create_new_project)) },
        text = {
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text(stringResource(id = R.string.project_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraSmall
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(projectName) },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.springPress()
            ) {
                Text(stringResource(id = R.string.create))
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
private fun MoveToProjectDialog(
    projects: kotlinx.collections.immutable.ImmutableList<com.suvojeet.notenext.data.Project>,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var selectedProject by remember { mutableStateOf<com.suvojeet.notenext.data.Project?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(id = R.string.move_to_project)) },
        text = {
            // Cap the height and make it scrollable so a long project list can't
            // overflow the dialog off-screen.
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                projects.forEach { project ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProject = project }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedProject == project),
                            onClick = { selectedProject = project }
                        )
                        // Project color dot for quick visual identification.
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    project.color?.let { Color(it) }
                                        ?: MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                        Text(text = project.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedProject = null }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedProject == null),
                        onClick = { selectedProject = null }
                    )
                    Text(text = stringResource(id = R.string.none_remove_from_project), modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedProject?.id) },
                modifier = Modifier.springPress()
            ) {
                Text(stringResource(id = R.string.move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

