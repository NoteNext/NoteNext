
package com.suvojeet.notenext.ui.project

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.LabelDao
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteDao
import com.suvojeet.notenext.util.HtmlConverter
import com.suvojeet.notenext.data.LinkPreviewRepository
import com.suvojeet.notenext.data.ProjectDao
import com.suvojeet.notenext.core.util.SortType
import com.suvojeet.notenext.data.ReminderScheduler
import java.time.LocalDateTime
import java.time.ZoneId
import com.suvojeet.notenext.data.RepeatOption
import com.suvojeet.notenext.ui.notes.SaveStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import com.suvojeet.notenext.data.repository.AiRepository
import com.suvojeet.notenext.data.repository.AiResult
import com.suvojeet.notenext.data.repository.onFailure
import com.suvojeet.notenext.data.repository.onSuccess
import com.suvojeet.notenext.data.repository.toUserMessage
import com.suvojeet.notenext.ui.theme.NoteGradients
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.core.model.AttachmentType
import javax.inject.Inject

@HiltViewModel
class ProjectNotesViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository,
    private val todoRepository: com.suvojeet.notenext.data.TodoRepository,
    private val noteUseCases: com.suvojeet.notenext.domain.use_case.NoteUseCases,
    private val linkPreviewRepository: LinkPreviewRepository,
    private val reminderScheduler: ReminderScheduler,
    private val richTextController: com.suvojeet.notenext.ui.notes.RichTextController,
    private val aiRepository: AiRepository,
    private val aiSuggestionsDelegate: com.suvojeet.notenext.ui.notes.delegate.AISuggestionsDelegate,
    private val bulkActionDelegate: com.suvojeet.notenext.ui.notes.delegate.BulkActionDelegate,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectNotesState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ProjectNotesUiEvent>()
    val events = _events.asSharedFlow()

    private var isDecoySession: Boolean = false
    private var recentlyDeletedNote: Note? = null
    private var autoSaveJob: Job? = null
    private var linkDetectionJob: Job? = null

    private val projectId: Int = savedStateHandle.get<Int>("projectId") ?: -1

    private val _sortType = MutableStateFlow(SortType.DATE_MODIFIED)
    private val _isDecoy = MutableStateFlow(false)

    /**
     * Shared plumbing behind every multi-select action: take the current
     * selection, run [action] against it, clear the selection, then show
     * whatever message came back. Returning null means "no snackbar".
     */
    private fun runBulkAction(action: suspend (List<Int>) -> String?) {
        val selectedIds = state.value.selectedNoteIds.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val message = action(selectedIds)
            _state.value = state.value.copy(selectedNoteIds = emptyList())
            message?.let { _events.emit(ProjectNotesUiEvent.ShowSnackbar(it)) }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        _state.value = _state.value.copy(saveStatus = SaveStatus.SAVING)
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            onEvent(ProjectNotesEvent.OnSaveNoteClick(shouldCollapse = false))
            _state.value = _state.value.copy(saveStatus = SaveStatus.SAVED)
        }
    }

    init {
        if (projectId != -1) {
            viewModelScope.launch {
                repository.getProjectById(projectId)?.let { project ->
                    _state.value = _state.value.copy(
                        projectName = project.name,
                        projectDescription = project.description
                    )
                }
            }

            val notesFlow = _isDecoy.flatMapLatest { decoy ->
                repository.getNoteSummariesByProjectId(projectId, decoy)
            }

            combine(
                notesFlow,
                todoRepository.getTodosByProject(projectId),
                repository.getLabels(),
                _sortType
            ) { notes, todos, labels, sortType ->
                val sortedNotes = when (sortType) {
                    SortType.DATE_CREATED -> notes.sortedByDescending { it.note.createdAt }
                    SortType.DATE_MODIFIED -> notes.sortedByDescending { it.note.lastEdited }
                    SortType.TITLE -> notes.sortedBy { it.note.title }
                    SortType.CUSTOM -> notes.sortedBy { it.note.position }
                }
                _state.value = _state.value.copy(
                    notes = sortedNotes,
                    todos = todos,
                    labels = labels.map { it.name },
                    sortType = sortType
                )
            }.launchIn(viewModelScope)
        }
    }

    fun setDecoyMode(isDecoy: Boolean) {
        this.isDecoySession = isDecoy
        _isDecoy.value = isDecoy
    }

    fun onEvent(event: ProjectNotesEvent) {
        when (event) {
            is ProjectNotesEvent.OnRestoreVersion -> {
                viewModelScope.launch {
                    val content = HtmlConverter.htmlToAnnotatedString(event.version.content)
                    _state.value = state.value.copy(
                        editingTitle = event.version.title,
                        editingContent = TextFieldValue(content),
                        editingNoteType = event.version.noteType
                    )
                    _events.emit(ProjectNotesUiEvent.ShowSnackbar("Version restored"))
                }
            }
            is ProjectNotesEvent.NavigateToNoteByTitle -> {
                viewModelScope.launch {
                    _events.emit(ProjectNotesUiEvent.NavigateToNoteByTitle(event.title))
                }
            }
            is ProjectNotesEvent.DeleteNote -> {
                viewModelScope.launch {
                    repository.getNoteById(event.note.note.id)?.let { fullNote ->
                        val noteToBin = fullNote.note.copy(isBinned = true, binnedOn = System.currentTimeMillis())
                        repository.updateNote(noteToBin)
                        recentlyDeletedNote = fullNote.note
                        _events.emit(ProjectNotesUiEvent.ShowSnackbar("Note moved to Bin"))
                    }
                }
            }
            is ProjectNotesEvent.RestoreNote -> {
                viewModelScope.launch {
                    recentlyDeletedNote?.let { restoredNote ->
                        repository.updateNote(restoredNote.copy(isBinned = false))
                        recentlyDeletedNote = null
                    }
                }
            }
            is ProjectNotesEvent.ToggleNoteSelection -> {
                val selectedIds = state.value.selectedNoteIds.toMutableList()
                if (selectedIds.contains(event.noteId)) {
                    selectedIds.remove(event.noteId)
                } else {
                    selectedIds.add(event.noteId)
                }
                _state.value = state.value.copy(selectedNoteIds = selectedIds)
            }
            is ProjectNotesEvent.ClearSelection -> {
                _state.value = state.value.copy(selectedNoteIds = emptyList())
            }
            is ProjectNotesEvent.SelectAllNotes -> {
                viewModelScope.launch {
                    val allIds = repository.getAllNoteIds(
                        projectId = projectId,
                        isDecoy = isDecoySession
                    )
                    _state.value = state.value.copy(selectedNoteIds = allIds)
                }
            }
            is ProjectNotesEvent.TogglePinForSelectedNotes -> {
                runBulkAction { ids -> bulkActionDelegate.togglePin(ids) }
            }
            is ProjectNotesEvent.ToggleLockForSelectedNotes -> {
                runBulkAction { ids -> bulkActionDelegate.toggleLock(ids)?.message }
            }
            is ProjectNotesEvent.DeleteSelectedNotes -> {
                runBulkAction { ids -> bulkActionDelegate.moveToBin(ids) }
            }
            is ProjectNotesEvent.ArchiveSelectedNotes -> {
                runBulkAction { ids -> bulkActionDelegate.toggleArchive(ids); null }
            }
            is ProjectNotesEvent.ToggleImportantForSelectedNotes -> {
                runBulkAction { ids -> bulkActionDelegate.toggleImportant(ids); null }
            }
            is ProjectNotesEvent.ChangeColorForSelectedNotes -> {
                runBulkAction { ids -> bulkActionDelegate.changeColor(ids, event.color) }
            }
            is ProjectNotesEvent.CopySelectedNotes -> {
                runBulkAction { ids -> bulkActionDelegate.copyNotes(ids, isDecoySession) }
            }
            is ProjectNotesEvent.SendSelectedNotes -> {
                runBulkAction { ids ->
                    bulkActionDelegate.buildSharePayload(ids)?.let { payload ->
                        _events.emit(ProjectNotesUiEvent.SendNotes(payload.title, payload.content))
                    }
                    null
                }
            }
            is ProjectNotesEvent.SetReminderForSelectedNotes -> {
                val reminderMillis = LocalDateTime.of(event.date, event.time)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                runBulkAction { ids ->
                    bulkActionDelegate.setReminder(ids, reminderMillis, event.repeatOption.name)
                }
            }
            is ProjectNotesEvent.SetLabelForSelectedNotes -> {
                runBulkAction { ids -> bulkActionDelegate.setLabel(ids, event.label); null }
            }
            is ProjectNotesEvent.ExpandNote -> {
                viewModelScope.launch {
                    if (event.noteId != -1) {
                        repository.getNoteById(event.noteId)?.let { noteWithAttachments ->
                            val note = noteWithAttachments.note
                            val content = if (note.noteType == NoteType.TEXT) {
                                HtmlConverter.htmlToAnnotatedString(note.content)
                            } else {
                                AnnotatedString("")
                            }
                            // Loaded for every note type: a TEXT note can also carry tick
                            // boxes appended below its body, not just CHECKLIST notes.
                            val checklist = noteWithAttachments.checklistItems.sortedBy { it.position }

                            // Fetch versions
                            viewModelScope.launch {
                                repository.getNoteVersions(event.noteId).collect { versions ->
                                    _state.value = _state.value.copy(editingNoteVersions = versions)
                                }
                            }

                            _state.value = state.value.copy(
                                expandedNoteId = event.noteId,
                                editingTitle = note.title,
                                editingContent = TextFieldValue(content),
                                editingColor = note.color,
                                editingIsNewNote = false,
                                editingLastEdited = note.lastEdited,
                                isPinned = note.isPinned,
                                isArchived = note.isArchived,
                                editingLabel = note.label,
                                editingProjectId = note.projectId,
                                editingHistory = listOf(note.title to TextFieldValue(content)),
                                editingHistoryIndex = 0,
                                linkPreviews = note.linkPreviews,
                                editingNoteType = note.noteType,
                                editingChecklist = checklist,
                                checklistInputValues = checklist.associate { item ->
                                    item.id to TextFieldValue(richTextController.parseMarkdownToAnnotatedString(item.text))
                                },
                                focusedChecklistItemId = null,
                                editingAttachments = noteWithAttachments.attachments.map { it.copy(tempId = java.util.UUID.randomUUID().toString()) },
                                editingIsLocked = note.isLocked,
                                editingReminderTime = note.reminderTime,
                                editingRepeatOption = note.repeatOption
                            )
                        }
                    } else {
                        // Reuse one item id for both the model and the text-field map;
                        // generating two different UUIDs left the first row uneditable.
                        val initialChecklistItem = ChecklistItem(text = "", isChecked = false)
                        _state.value = state.value.copy(
                            expandedNoteId = -1,
                            editingTitle = "",
                            editingContent = TextFieldValue(),
                            editingColor = 0,
                            editingIsNewNote = true,
                            editingLastEdited = 0,
                            editingHistory = listOf("" to TextFieldValue()),
                            editingHistoryIndex = 0,
                            editingLabel = null,
                            editingProjectId = if (projectId != -1) projectId else null,
                            linkPreviews = emptyList(),
                            editingNoteType = event.noteType,
                            editingChecklist = if (event.noteType == NoteType.CHECKLIST) listOf(initialChecklistItem) else emptyList(),
                            checklistInputValues = if (event.noteType == NoteType.CHECKLIST) {
                                mapOf(initialChecklistItem.id to TextFieldValue(""))
                            } else emptyMap(),
                            focusedChecklistItemId = null,
                            editingAttachments = emptyList(),
                            editingIsLocked = false,
                            editingNoteVersions = emptyList(),
                            editingReminderTime = null,
                            editingRepeatOption = null
                        )
                    }
                }
            }
            is ProjectNotesEvent.CollapseNote -> {
                onEvent(ProjectNotesEvent.OnSaveNoteClick())
            }
            is ProjectNotesEvent.AddChecklistItem -> {
                if (state.value.editingChecklist.size >= 500) return
                val (updatedChecklist, newItemId) = com.suvojeet.notenext.ui.notes.ChecklistManager.addChecklistItem(state.value.editingChecklist)
                _state.value = state.value.copy(
                    editingChecklist = updatedChecklist,
                    newlyAddedChecklistItemId = newItemId,
                    checklistInputValues = state.value.checklistInputValues + (newItemId to TextFieldValue(""))
                )
                scheduleAutoSave()
            }
            is ProjectNotesEvent.AddChecklistItemAfter -> {
                if (state.value.editingChecklist.size >= 500) return
                val (updatedChecklist, newItemId) = com.suvojeet.notenext.ui.notes.ChecklistManager.addChecklistItemAfter(state.value.editingChecklist, event.itemId)
                _state.value = state.value.copy(
                    editingChecklist = updatedChecklist,
                    newlyAddedChecklistItemId = newItemId,
                    checklistInputValues = state.value.checklistInputValues + (newItemId to TextFieldValue(""))
                )
                scheduleAutoSave()
            }
            is ProjectNotesEvent.SwapChecklistItems -> {
                val updatedList = com.suvojeet.notenext.ui.notes.ChecklistManager.swapItems(state.value.editingChecklist, event.fromId, event.toId)
                if (updatedList != state.value.editingChecklist) {
                    _state.value = state.value.copy(editingChecklist = updatedList)
                    scheduleAutoSave()
                }
            }
            is ProjectNotesEvent.DeleteChecklistItem -> {
                val updatedChecklist = com.suvojeet.notenext.ui.notes.ChecklistManager.deleteItem(state.value.editingChecklist, event.itemId)
                _state.value = state.value.copy(
                    editingChecklist = updatedChecklist,
                    checklistInputValues = state.value.checklistInputValues - event.itemId
                )
                scheduleAutoSave()
            }
            is ProjectNotesEvent.IndentChecklistItem -> {
                val updatedChecklist = com.suvojeet.notenext.ui.notes.ChecklistManager.indentItem(state.value.editingChecklist, event.itemId)
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
                scheduleAutoSave()
            }
            is ProjectNotesEvent.OutdentChecklistItem -> {
                val updatedChecklist = com.suvojeet.notenext.ui.notes.ChecklistManager.outdentItem(state.value.editingChecklist, event.itemId)
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
                scheduleAutoSave()
            }
            is ProjectNotesEvent.OnChecklistItemCheckedChange -> {
                val updatedChecklist = com.suvojeet.notenext.ui.notes.ChecklistManager.changeItemCheckedState(state.value.editingChecklist, event.itemId, event.isChecked)
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
                scheduleAutoSave()
            }
            is ProjectNotesEvent.OnChecklistItemTextChange -> {
                if (event.text.length > 2000) return
                val updatedChecklist = com.suvojeet.notenext.ui.notes.ChecklistManager.changeItemText(state.value.editingChecklist, event.itemId, event.text)
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
                scheduleAutoSave()
            }
            is ProjectNotesEvent.OnChecklistItemValueChange -> {
                if (event.value.text.length > 2000) return
                val currentValues = state.value.checklistInputValues.toMutableMap()
                val oldContent = currentValues[event.itemId] ?: TextFieldValue("")

                val finalContent = richTextController.processContentChange(
                    oldContent = oldContent,
                    newContent = event.value,
                    activeStyles = state.value.activeStyles,
                    activeHeadingStyle = state.value.activeHeadingStyle
                )                
                currentValues[event.itemId] = finalContent
                
                // Sync styles with toolbar
                val selection = finalContent.selection
                val styles = if (selection.collapsed) {
                    if (selection.start > 0) {
                        finalContent.annotatedString.spanStyles.filter {
                            it.start <= selection.start - 1 && it.end >= selection.start
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    finalContent.annotatedString.spanStyles.filter {
                        maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                    }
                }

                _state.value = state.value.copy(
                    checklistInputValues = currentValues,
                    isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                    isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                    isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline }
                )
                
                // Sync text with persistence model (Markdown)
                viewModelScope.launch {
                    val updatedText = HtmlConverter.annotatedStringToHtml(finalContent.annotatedString).let {
                        com.suvojeet.notenext.data.MarkdownExporter.convertHtmlToMarkdown(it)
                    }
                    val updatedChecklist = state.value.editingChecklist.map {
                        if (it.id == event.itemId) it.copy(text = updatedText) else it
                    }
                    _state.value = state.value.copy(editingChecklist = updatedChecklist)
                }
                scheduleAutoSave()
            }
            is ProjectNotesEvent.OnChecklistItemFocus -> {
                _state.value = state.value.copy(focusedChecklistItemId = event.itemId)
            }
            is ProjectNotesEvent.OnTitleChange -> {
                val safeTitle = if (event.title.length > 100) event.title.take(100) else event.title
                val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (safeTitle to state.value.editingContent)
                _state.value = state.value.copy(
                    editingTitle = safeTitle,
                    editingHistory = newHistory,
                    editingHistoryIndex = newHistory.lastIndex
                )
                scheduleAutoSave()
            }
            is ProjectNotesEvent.OnContentChange -> {
                if (state.value.editingNoteType == NoteType.TEXT) {
                    val newContent = event.content
                    val oldContent = state.value.editingContent
                    
                    if (newContent.text.length > 100_000) {
                        _state.value = state.value.copy(
                            editingContent = oldContent.copy(selection = newContent.selection)
                        )
                    } else {
                        val finalContent = if (newContent.text != oldContent.text) {
                        val oldText = oldContent.text
                        val newText = newContent.text

                        // 1. Find common prefix
                        val prefixLength = commonPrefixWith(oldText, newText).length

                        // 2. Find common suffix of the remainder of the strings
                        val oldRemainder = oldText.substring(prefixLength)
                        val newRemainder = newText.substring(prefixLength)
                        val suffixLength = commonSuffixWith(oldRemainder, newRemainder).length

                        // 3. Determine the middle (changed) part of the new text
                        val newChangedPart = newRemainder.substring(0, newRemainder.length - suffixLength)

                        val newAnnotatedString = buildAnnotatedString {
                            // Append the styled prefix from the original string
                            append(oldContent.annotatedString.subSequence(0, prefixLength))

                            // Append the newly typed text with the active styles
                            val styleToApply = state.value.activeStyles.reduceOrNull { a, b -> a.merge(b) } ?: SpanStyle()
                            withStyle(styleToApply) {
                                append(newChangedPart)
                            }

                            // Append the styled suffix from the original string
                            append(oldContent.annotatedString.subSequence(oldText.length - suffixLength, oldText.length))
                        }
                        newContent.copy(annotatedString = newAnnotatedString)
                    } else {
                        // When only the selection changes, trust the old AnnotatedString from our state
                        // and just update the selection from the new value. This prevents the TextField
                        // from stripping styles on selection/deselection.
                        oldContent.copy(selection = newContent.selection)
                    }

                    val selection = finalContent.selection
                    val styles = if (selection.collapsed) {
                        if (selection.start > 0) {
                            finalContent.annotatedString.spanStyles.filter {
                                it.start <= selection.start - 1 && it.end >= selection.start
                            }
                        } else {
                            emptyList()
                        }
                    } else {
                        finalContent.annotatedString.spanStyles.filter {
                            maxOf(selection.start, it.start) < minOf(selection.end, it.end)
                        }
                    }
                    val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to finalContent)
                    _state.value = state.value.copy(
                        editingContent = finalContent,
                        editingHistory = newHistory,
                        editingHistoryIndex = newHistory.lastIndex,
                        isBoldActive = styles.any { style -> style.item.fontWeight == FontWeight.Bold },
                        isItalicActive = styles.any { style -> style.item.fontStyle == FontStyle.Italic },
                        isUnderlineActive = styles.any { style -> style.item.textDecoration == TextDecoration.Underline }
                    )

                    // Link detection
                    val urlRegex = "(https?://[\\w.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?)".toRegex()
                    val detectedUrls = urlRegex.findAll(finalContent.text).map { it.value }.toSet() // Use Set for efficient lookup

                    detectedUrls.forEach { url ->
                        onEvent(ProjectNotesEvent.OnLinkDetected(url))
                    }
                    scheduleAutoSave()
                }
            }
        }
            is ProjectNotesEvent.ApplyStyleToContent -> {
                val selection = state.value.editingContent.selection
                if (selection.collapsed) {
                    val styleToAddOrRemove = event.style
                    val activeStyles = state.value.activeStyles.toMutableSet()

                    val isBold = styleToAddOrRemove.fontWeight == FontWeight.Bold
                    val isItalic = styleToAddOrRemove.fontStyle == FontStyle.Italic
                    val isUnderline = styleToAddOrRemove.textDecoration == TextDecoration.Underline

                    val wasBold = activeStyles.any { it.fontWeight == FontWeight.Bold }
                    val wasItalic = activeStyles.any { it.fontStyle == FontStyle.Italic }
                    val wasUnderline = activeStyles.any { it.textDecoration == TextDecoration.Underline }

                    if (isBold) {
                        if (wasBold) activeStyles.removeAll { it.fontWeight == FontWeight.Bold }
                        else activeStyles.add(SpanStyle(fontWeight = FontWeight.Bold))
                    }
                    if (isItalic) {
                        if (wasItalic) activeStyles.removeAll { it.fontStyle == FontStyle.Italic }
                        else activeStyles.add(SpanStyle(fontStyle = FontStyle.Italic))
                    }
                    if (isUnderline) {
                        if (wasUnderline) activeStyles.removeAll { it.textDecoration == TextDecoration.Underline }
                        else activeStyles.add(SpanStyle(textDecoration = TextDecoration.Underline))
                    }

                    _state.value = state.value.copy(
                        activeStyles = activeStyles,
                        isBoldActive = activeStyles.any { it.fontWeight == FontWeight.Bold },
                        isItalicActive = activeStyles.any { it.fontStyle == FontStyle.Italic },
                        isUnderlineActive = activeStyles.any { it.textDecoration == TextDecoration.Underline }
                    )
                } else {
                    val selection = state.value.editingContent.selection
                    val newAnnotatedString = AnnotatedString.Builder(state.value.editingContent.annotatedString).apply {
                        val style = event.style
                        val isApplyingBold = style.fontWeight == FontWeight.Bold
                        val isApplyingItalic = style.fontStyle == FontStyle.Italic
                        val isApplyingUnderline = style.textDecoration == TextDecoration.Underline

                        val selectionIsAlreadyBold = state.value.isBoldActive
                        val selectionIsAlreadyItalic = state.value.isItalicActive
                        val selectionIsAlreadyUnderline = state.value.isUnderlineActive

                        val styleToApply = when {
                            isApplyingBold -> if (selectionIsAlreadyBold) SpanStyle(fontWeight = FontWeight.Normal) else SpanStyle(fontWeight = FontWeight.Bold)
                            isApplyingItalic -> if (selectionIsAlreadyItalic) SpanStyle(fontStyle = FontStyle.Normal) else SpanStyle(fontStyle = FontStyle.Italic)
                            isApplyingUnderline -> if (selectionIsAlreadyUnderline) SpanStyle(textDecoration = TextDecoration.None) else SpanStyle(textDecoration = TextDecoration.Underline)
                            else -> style
                        }
                        addStyle(styleToApply, selection.start, selection.end)
                    }.toAnnotatedString()

                    val newTextFieldValue = state.value.editingContent.copy(annotatedString = newAnnotatedString)
                    val newHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to newTextFieldValue)

                    _state.value = state.value.copy(
                        editingContent = newTextFieldValue,
                        editingHistory = newHistory,
                        editingHistoryIndex = newHistory.lastIndex
                    )
                }
            }
            is ProjectNotesEvent.ApplyBulletedList -> {
                val updatedContent = richTextController.toggleBulletedList(state.value.editingContent)
                val updatedHistory = state.value.editingHistory.take(state.value.editingHistoryIndex + 1) + (state.value.editingTitle to updatedContent)
                _state.value = state.value.copy(
                    editingContent = updatedContent,
                    editingHistory = updatedHistory,
                    editingHistoryIndex = updatedHistory.lastIndex
                )
                scheduleAutoSave()
            }
            is ProjectNotesEvent.ApplyHeadingStyle -> {
                _state.value = state.value.copy(
                    activeHeadingStyle = event.level,
                    isBoldActive = false,
                    isItalicActive = false,
                    isUnderlineActive = false,
                    activeStyles = emptySet()
                )
            }
            is ProjectNotesEvent.OnColorChange -> {
                _state.value = state.value.copy(editingColor = event.color)
            }
            is ProjectNotesEvent.OnLabelChange -> {
                viewModelScope.launch {
                    repository.insertLabel(Label(event.label))
                    _state.value = state.value.copy(editingLabel = event.label)
                }
            }
            is ProjectNotesEvent.SetInitialTitle -> {
                val safeTitle = if (event.title.length > 100) event.title.take(100) else event.title
                _state.value = state.value.copy(editingTitle = safeTitle)
            }
            is ProjectNotesEvent.OnReminderChange -> {
                _state.value = state.value.copy(
                    editingReminderTime = event.time,
                    editingRepeatOption = event.repeatOption
                )
                scheduleAutoSave()
            }
            is ProjectNotesEvent.OnExpiryChange -> {
                _state.value = state.value.copy(
                    editingExpiryTime = event.expiryTime
                )
                scheduleAutoSave()
            }
            is ProjectNotesEvent.AutoSaveNote -> {
                viewModelScope.launch {
                    onEvent(ProjectNotesEvent.OnSaveNoteClick(shouldCollapse = false))
                }
            }
            is ProjectNotesEvent.SummarizeNote -> {
                val content = if (state.value.editingNoteType == NoteType.TEXT) {
                    state.value.editingContent.text
                } else {
                    state.value.editingChecklist.joinToString("\n") { it.text }
                }

                if (content.isBlank()) {
                    viewModelScope.launch { _events.emit(ProjectNotesUiEvent.ShowSnackbar("Note content is empty")) }
                    return
                }

                _state.value = state.value.copy(isSummarizing = true, showSummaryDialog = true)
                viewModelScope.launch {
                    aiRepository.summarizeNote(content).collect { result ->
                        result.onSuccess { summary ->
                            _state.value = _state.value.copy(isSummarizing = false, summaryResult = summary)
                        }.onFailure { failure ->
                            val errorMessage = failure.toUserMessage("Summarization failed.")
                            _state.value = _state.value.copy(isSummarizing = false, showSummaryDialog = false)
                            _events.emit(ProjectNotesUiEvent.ShowSnackbar(errorMessage, actionLabel = "Retry", onAction = { onEvent(ProjectNotesEvent.SummarizeNote) }))
                        }
                    }
                }
            }
            is ProjectNotesEvent.ClearSummary -> {
                _state.value = state.value.copy(summaryResult = null, showSummaryDialog = false)
            }
            is ProjectNotesEvent.GenerateChecklist -> {
                if (event.topic.isBlank()) return

                _state.value = state.value.copy(isGeneratingChecklist = true)
                viewModelScope.launch {
                    aiRepository.generateChecklist(event.topic).collect { result ->
                        result.onSuccess { items ->
                            _state.value = _state.value.copy(
                                isGeneratingChecklist = false,
                                generatedChecklistPreview = items
                            )
                        }.onFailure { failure ->
                            val errorMessage = failure.toUserMessage("Generation failed.")
                            _state.value = _state.value.copy(isGeneratingChecklist = false)
                            _events.emit(ProjectNotesUiEvent.ShowSnackbar(errorMessage, actionLabel = "Retry", onAction = { onEvent(ProjectNotesEvent.GenerateChecklist(event.topic)) }))
                        }
                    }
                }
            }
            is ProjectNotesEvent.InsertGeneratedChecklist -> {
                val currentItems = state.value.editingChecklist
                val newItems = event.items.mapIndexed { index, text ->
                    com.suvojeet.notenext.data.ChecklistItem(
                        text = text,
                        isChecked = false,
                        position = currentItems.size + index
                    )
                }
                _state.value = state.value.copy(
                    editingChecklist = currentItems + newItems,
                    editingNoteType = NoteType.CHECKLIST,
                    generatedChecklistPreview = emptyList()
                )
                scheduleAutoSave()
            }
            is ProjectNotesEvent.ClearGeneratedChecklist -> {
                _state.value = state.value.copy(generatedChecklistPreview = emptyList())
            }
            is ProjectNotesEvent.FixGrammar -> {
                val targetText = state.value.editingContent.text
                if (targetText.isBlank()) return

                _state.value = state.value.copy(isFixingGrammar = true)
                viewModelScope.launch {
                    aiRepository.fixGrammar(targetText).collect { result ->
                        result.onSuccess { fixedText ->
                            _state.value = _state.value.copy(
                                isFixingGrammar = false,
                                fixedContentPreview = fixedText,
                                originalContentBackup = state.value.editingContent
                            )
                        }.onFailure { failure ->
                            val errorMessage = failure.toUserMessage("Grammar fix failed.")
                            _state.value = _state.value.copy(isFixingGrammar = false)
                            _events.emit(ProjectNotesUiEvent.ShowSnackbar(errorMessage, actionLabel = "Retry", onAction = { onEvent(ProjectNotesEvent.FixGrammar) }))
                        }
                    }
                }
            }
            is ProjectNotesEvent.ApplyGrammarFix -> {
                state.value.fixedContentPreview?.let { fixedText ->
                    val newAnnotatedString = AnnotatedString(fixedText)
                    _state.value = state.value.copy(
                        editingContent = TextFieldValue(newAnnotatedString),
                        fixedContentPreview = null,
                        originalContentBackup = null
                    )
                    scheduleAutoSave()
                }
            }
            is ProjectNotesEvent.ClearGrammarFix -> {
                _state.value = state.value.copy(
                    fixedContentPreview = null,
                    originalContentBackup = null
                )
            }
            is ProjectNotesEvent.PickToneRewrite -> {
                runToneRewrite(event.tone)
            }
            is ProjectNotesEvent.RetryToneRewrite -> {
                state.value.toneRewriteSelectedTone?.let { runToneRewrite(it) }
            }
            is ProjectNotesEvent.AcceptToneRewrite -> {
                val rewritten = state.value.toneRewriteResult ?: return
                _state.value = state.value.copy(
                    editingContent = TextFieldValue(rewritten),
                    toneRewriteResult = null,
                    toneRewriteSelectedTone = null
                )
                scheduleAutoSave()
            }
            is ProjectNotesEvent.ExportNote -> {
                viewModelScope.launch {
                    try {
                        val content = if (state.value.editingNoteType == NoteType.TEXT) {
                            state.value.editingContent.text
                        } else {
                            state.value.editingChecklist.joinToString("\n") { if (it.isChecked) "[x] ${it.text}" else "[ ] ${it.text}" }
                        }
                        val fullContent = "${state.value.editingTitle}\n\n$content"
                        
                        context.contentResolver.openOutputStream(event.uri)?.use { outputStream ->
                            outputStream.write(fullContent.toByteArray())
                        }
                        _events.emit(ProjectNotesUiEvent.ShowSnackbar("Note exported successfully"))
                    } catch (e: Exception) {
                        _events.emit(ProjectNotesUiEvent.ShowSnackbar("Export failed: ${e.message}"))
                    }
                }
            }
            is ProjectNotesEvent.ShareAsText -> {
                val content = if (state.value.editingNoteType == NoteType.TEXT) {
                    state.value.editingContent.text
                } else {
                    state.value.editingChecklist.joinToString("\n") { if (it.isChecked) "[x] ${it.text}" else "[ ] ${it.text}" }
                }
                val shareText = "${state.value.editingTitle}\n\n$content"
                viewModelScope.launch {
                    _events.emit(ProjectNotesUiEvent.SendNotes(state.value.editingTitle, shareText))
                }
            }
            is ProjectNotesEvent.OnTogglePinClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let { noteId ->
                        repository.getNoteById(noteId)?.let { note ->
                            val updatedNote = note.note.copy(isPinned = !note.note.isPinned)
                            repository.updateNote(updatedNote)
                            _state.value = state.value.copy(isPinned = updatedNote.isPinned)
                            val message = if (updatedNote.isPinned) "Note pinned" else "Note unpinned"
                            _events.emit(ProjectNotesUiEvent.ShowSnackbar(message))
                        }
                    }
                }
            }
            is ProjectNotesEvent.OnToggleLockClick -> {
                viewModelScope.launch {
                    val currentLockState = state.value.editingIsLocked
                    val newLockState = !currentLockState
                    _state.value = state.value.copy(editingIsLocked = newLockState)
                    // If note exists, update immediately using the current decrypted state
                    state.value.expandedNoteId?.let { noteId ->
                         if (noteId != -1) {
                             onEvent(ProjectNotesEvent.OnSaveNoteClick(shouldCollapse = false))
                             _events.emit(ProjectNotesUiEvent.ShowSnackbar(if (newLockState) "Note locked" else "Note unlocked"))
                         }
                    }
                }
            }
            is ProjectNotesEvent.OnToggleArchiveClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let { noteId ->
                        repository.getNoteById(noteId)?.let { note ->
                            val updatedNote = note.note.copy(isArchived = !note.note.isArchived)
                            repository.updateNote(updatedNote)
                            _state.value = state.value.copy(isArchived = updatedNote.isArchived)
                        }
                    }
                }
            }
            is ProjectNotesEvent.OnUndoClick -> {
                if (state.value.editingHistoryIndex > 0) {
                    val newIndex = state.value.editingHistoryIndex - 1
                    val (title, content) = state.value.editingHistory[newIndex]
                    _state.value = state.value.copy(
                        editingTitle = title,
                        editingContent = content,
                        editingHistoryIndex = newIndex
                    )
                }
            }
            is ProjectNotesEvent.OnRedoClick -> {
                if (state.value.editingHistoryIndex < state.value.editingHistory.lastIndex) {
                    val newIndex = state.value.editingHistoryIndex + 1
                    val (title, content) = state.value.editingHistory[newIndex]
                    _state.value = state.value.copy(
                        editingTitle = title,
                        editingContent = content,
                        editingHistoryIndex = newIndex
                    )
                }
            }
            is ProjectNotesEvent.OnSaveNoteClick -> {
                viewModelScope.launch {
                    val noteId = state.value.expandedNoteId
                    if (noteId == null) return@launch

                    val title = state.value.editingTitle
                    val content = if (state.value.editingNoteType == NoteType.TEXT) {
                        HtmlConverter.annotatedStringToHtml(state.value.editingContent.annotatedString)
                    } else {
                        ""
                    }

                    // A TEXT note is only "empty" when its tick boxes are blank too —
                    // otherwise a note holding nothing but checkboxes would be binned.
                    val checklistIsBlank = state.value.editingChecklist.all { it.text.isBlank() }
                    val bodyIsBlank = if (state.value.editingNoteType == NoteType.CHECKLIST) {
                        checklistIsBlank
                    } else {
                        content.isBlank() && checklistIsBlank
                    }

                    if (title.isBlank() && bodyIsBlank) {
                        if (noteId != -1) { // It's an existing note, so delete it
                            repository.getNoteById(noteId)?.let { repository.updateNote(it.note.copy(isBinned = true, binnedOn = System.currentTimeMillis())) }
                        }
                        if (event.shouldCollapse) {
                            _state.value = state.value.copy(expandedNoteId = null)
                        }
                    } else {
                        val currentTime = System.currentTimeMillis()
                        val note = if (noteId == -1) { // New note
                            Note(
                                title = title,
                                content = content,
                                createdAt = currentTime,
                                lastEdited = currentTime,
                                color = state.value.editingColor,
                                isPinned = state.value.isPinned,
                                isArchived = state.value.isArchived,
                                label = state.value.editingLabel,
                                linkPreviews = state.value.linkPreviews,
                                noteType = state.value.editingNoteType,
                                projectId = projectId,
                                isLocked = state.value.editingIsLocked,
                                reminderTime = state.value.editingReminderTime,
                                repeatOption = state.value.editingRepeatOption,
                                expiryTime = state.value.editingExpiryTime,
                                isDecoy = isDecoySession
                            )
                        } else { // Existing note
                            repository.getNoteById(noteId)?.let { existingNote ->
                                existingNote.note.copy(
                                    title = title,
                                    content = content,
                                    lastEdited = currentTime,
                                    color = state.value.editingColor,
                                    isPinned = state.value.isPinned,
                                    isArchived = state.value.isArchived,
                                    label = state.value.editingLabel,
                                    linkPreviews = state.value.linkPreviews,
                                    noteType = state.value.editingNoteType,
                                    projectId = projectId,
                                    isLocked = state.value.editingIsLocked,
                                    reminderTime = state.value.editingReminderTime,
                                    repeatOption = state.value.editingRepeatOption,
                                    expiryTime = state.value.editingExpiryTime,
                                    aiSummary = state.value.summaryResult,
                                    isEncrypted = false,
                                    iv = null
                                )
                            }
                        }
                        if (note != null) {
                            val currentNoteId = if (noteId == -1) { // New note
                                val newId = repository.insertNote(note).toInt()
                                _state.value = state.value.copy(
                                    expandedNoteId = newId,
                                    editingIsNewNote = false,
                                    editingLastEdited = currentTime
                                )
                                newId.toLong()
                            } else { // Existing note
                                // Before updating, save current state as a version if it's not a new note
                                repository.getNoteById(noteId)?.let { oldNoteWithAttachments ->
                                    val oldNote = oldNoteWithAttachments.note
                                    // Only save version if content or title changed
                                    if (oldNote.title != title || oldNote.content != content) {
                                        repository.insertNoteVersion(
                                            com.suvojeet.notenext.data.NoteVersion(
                                                noteId = noteId,
                                                title = oldNote.title,
                                                content = oldNote.content,
                                                timestamp = oldNote.lastEdited,
                                                noteType = oldNote.noteType
                                            )
                                        )
                                        repository.limitNoteVersions(noteId, 10)
                                    }
                                }
                                repository.updateNote(note)
                                _state.value = state.value.copy(
                                    editingLastEdited = currentTime
                                )
                                noteId.toLong() // Convert Int to Long for consistency
                            }

                            if (state.value.editingReminderTime != null) {
                                reminderScheduler.scheduleNoteReminder(note.copy(id = currentNoteId.toInt()))
                            } else if (noteId != -1) {
                                reminderScheduler.cancelNoteReminder(note.copy(id = currentNoteId.toInt()))
                            }

                            // Per-note exact-alarm self-destruct. See NotesViewModel.kt
                            // for rationale — same fix mirrored here for project notes.
                            val withId = note.copy(id = currentNoteId.toInt())
                            if (note.expiryTime != null) {
                                reminderScheduler.scheduleNoteExpiry(withId)
                            } else if (noteId != -1) {
                                reminderScheduler.cancelNoteExpiry(withId)
                            }

                            // Handle Checklist Items — persisted for TEXT notes too, since
                            // tick boxes can be appended below a text body.
                            run {
                                val checklistItems = state.value.editingChecklist.mapIndexed { index, item ->
                                    item.copy(noteId = currentNoteId.toInt(), position = index)
                                }
                                repository.deleteChecklistForNote(currentNoteId.toInt())
                                repository.insertChecklistItems(checklistItems)
                            }

                            // Handle attachments
                            val existingAttachmentsInDb = if (noteId != -1) {
                                repository.getNoteById(noteId)?.attachments ?: emptyList()
                            } else {
                                emptyList()
                            }

                            val attachmentsToAdd = state.value.editingAttachments.filter { uiAttachment ->
                                existingAttachmentsInDb.none { dbAttachment ->
                                    dbAttachment.uri == uiAttachment.uri && dbAttachment.type == uiAttachment.type
                                }
                            }

                            val attachmentsToRemove = existingAttachmentsInDb.filter { dbAttachment ->
                                state.value.editingAttachments.none { uiAttachment ->
                                    uiAttachment.uri == dbAttachment.uri && uiAttachment.type == dbAttachment.type
                                }
                            }

                            attachmentsToRemove.forEach { attachment ->
                                repository.deleteAttachment(attachment)
                            }

                            attachmentsToAdd.forEach { attachment ->
                                repository.insertAttachment(attachment.copy(noteId = currentNoteId.toInt()))
                            }
                        }
                    }

                    if (event.shouldCollapse) {
                        // Reset editing state and collapse
                        _state.value = state.value.copy(
                            expandedNoteId = null,
                            editingTitle = "",
                            editingContent = TextFieldValue(),
                            editingColor = 0,
                            editingIsNewNote = true,
                            editingLastEdited = 0,
                            editingHistory = listOf("" to TextFieldValue()),
                            editingHistoryIndex = 0,
                            isPinned = false,
                            isArchived = false,
                            editingLabel = null,
                            editingProjectId = null,
                            isBoldActive = false,
                            isItalicActive = false,
                            isUnderlineActive = false,
                            activeStyles = emptySet(),
                            linkPreviews = emptyList(),
                            editingChecklist = emptyList(),
                            editingAttachments = emptyList()
                        )
                    }
                }
            }
            is ProjectNotesEvent.OnDeleteNoteClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let {
                        if (it != -1) {
                            repository.getNoteById(it)?.let { note ->
                                repository.updateNote(note.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                                _events.emit(ProjectNotesUiEvent.ShowSnackbar("Note moved to Bin"))
                            }
                        }
                    }
                    _state.value = state.value.copy(expandedNoteId = null)
                }
            }
            is ProjectNotesEvent.OnCopyCurrentNoteClick -> {
                viewModelScope.launch {
                    state.value.expandedNoteId?.let {
                        repository.getNoteById(it)?.let { noteWithAttachments ->
                            val copiedNote = noteWithAttachments.note.copy(id = 0, title = "${noteWithAttachments.note.title} (Copy)", createdAt = System.currentTimeMillis(), lastEdited = System.currentTimeMillis())
                            val newNoteId = repository.insertNote(copiedNote)
                            
                            noteWithAttachments.attachments.forEach { attachment ->
                                repository.insertAttachment(attachment.copy(id = 0, noteId = newNoteId.toInt()))
                            }
                            
                            val newChecklistItems = noteWithAttachments.checklistItems.map { item ->
                                item.copy(id = java.util.UUID.randomUUID().toString(), noteId = newNoteId.toInt())
                            }
                            repository.insertChecklistItems(newChecklistItems)
                            
                            _events.emit(ProjectNotesUiEvent.ShowSnackbar("Note copied"))
                        }
                    }
                }
            }
            is ProjectNotesEvent.OnAddLabelsToCurrentNoteClick -> {
                _state.value = state.value.copy(showLabelDialog = true)
            }
            is ProjectNotesEvent.DismissLabelDialog -> {
                _state.value = state.value.copy(showLabelDialog = false)
            }
            is ProjectNotesEvent.ToggleLayout -> {
                val newLayout = if (state.value.layoutType == com.suvojeet.notenext.ui.notes.LayoutType.GRID) com.suvojeet.notenext.ui.notes.LayoutType.LIST else com.suvojeet.notenext.ui.notes.LayoutType.GRID
                _state.value = state.value.copy(layoutType = newLayout)
            }
            is ProjectNotesEvent.SortNotes -> {
                _sortType.value = event.sortType
            }
            is ProjectNotesEvent.OnRemoveLinkPreview -> {
                val updatedLinkPreviews = state.value.linkPreviews.filter { it.url != event.url }
                _state.value = state.value.copy(linkPreviews = updatedLinkPreviews)
                viewModelScope.launch {
                    _events.emit(ProjectNotesUiEvent.LinkPreviewRemoved)
                }
            }
            is ProjectNotesEvent.OnInsertLink -> {
                val updatedContent = richTextController.insertLink(
                    content = state.value.editingContent,
                    rawUrl = event.url,
                    displayText = event.displayText
                )
                _state.value = state.value.copy(editingContent = updatedContent)
                scheduleAutoSave()
            }
            is ProjectNotesEvent.ClearNewlyAddedChecklistItemId -> {
                _state.value = state.value.copy(newlyAddedChecklistItemId = null)
            }
            is ProjectNotesEvent.AddAttachment -> {
                val type = when {
                    event.mimeType.startsWith("image") -> AttachmentType.IMAGE
                    event.mimeType.startsWith("video") -> AttachmentType.VIDEO
                    event.mimeType.startsWith("audio") -> AttachmentType.AUDIO
                    else -> AttachmentType.FILE
                }
                val attachment = com.suvojeet.notenext.data.Attachment(
                    noteId = state.value.expandedNoteId ?: -1,
                    uri = event.uri,
                    type = type,
                    mimeType = event.mimeType,
                    tempId = java.util.UUID.randomUUID().toString()
                )
                _state.value = state.value.copy(editingAttachments = state.value.editingAttachments + attachment)
            }
            is ProjectNotesEvent.OnLinkDetected -> {
                linkDetectionJob?.cancel()
                linkDetectionJob = viewModelScope.launch {
                    delay(1000L) // 1s delay - only fetch when user stops typing

                    val existingLinkPreviews = state.value.linkPreviews
                    if (existingLinkPreviews.none { it.url == event.url }) {
                        val preview = linkPreviewRepository.getLinkPreview(event.url)
                        onEvent(ProjectNotesEvent.OnLinkPreviewFetched(
                            url = preview.url,
                            title = preview.title,
                            description = preview.description,
                            imageUrl = preview.imageUrl
                        ))
                    }
                }
            }
            is ProjectNotesEvent.OnLinkPreviewFetched -> {
                val newPreview = com.suvojeet.notenext.data.LinkPreview(event.url, event.title, event.description, event.imageUrl)
                val updatedPreviews = (state.value.linkPreviews + newPreview).distinctBy { it.url }
                _state.value = _state.value.copy(linkPreviews = updatedPreviews)
                scheduleAutoSave()
            }            is ProjectNotesEvent.RemoveAttachment -> {
                viewModelScope.launch {
                    val attachmentToRemove = _state.value.editingAttachments.firstOrNull { it.tempId == event.tempId }
                    attachmentToRemove?.let {
                        if (it.id != 0) { // Only delete from DB if it has a real ID
                            repository.deleteAttachmentById(it.id)
                        }
                        val updatedAttachments = _state.value.editingAttachments.filter { attachment -> attachment.tempId != event.tempId }
                        _state.value = _state.value.copy(editingAttachments = updatedAttachments)
                    }
                }
            }
            is ProjectNotesEvent.UpdateProjectDescription -> {
                viewModelScope.launch {
                    repository.getProjectById(projectId)?.let { project ->
                        val updatedDescription = event.description
                        repository.updateProject(project.copy(description = updatedDescription)) // Assuming updateProject takes a Project object
                        _state.value = _state.value.copy(projectDescription = updatedDescription)
                    }
                }
            }
            is ProjectNotesEvent.OnToggleNoteType -> {
                val currentType = state.value.editingNoteType
                if (currentType == NoteType.TEXT) {
                    // Convert TEXT to CHECKLIST. Tick boxes the note already carried are
                    // kept and the converted body lines are appended after them, so the
                    // conversion never discards existing items.
                    val existingItems = state.value.editingChecklist
                    val lines = state.value.editingContent.text.split("\n")
                    val convertedItems = lines.filter { it.isNotBlank() }.mapIndexed { index, text ->
                        com.suvojeet.notenext.data.ChecklistItem(text = text.trim(), isChecked = false, position = existingItems.size + index)
                    }
                    val merged = existingItems + convertedItems
                    val finalItems = merged.ifEmpty { listOf(com.suvojeet.notenext.data.ChecklistItem(text = "", isChecked = false, position = 0)) }

                    _state.value = state.value.copy(
                        editingNoteType = NoteType.CHECKLIST,
                        editingChecklist = finalItems,
                        editingContent = TextFieldValue("") 
                    )
                } else {
                    // Convert CHECKLIST to TEXT
                    val textContent = state.value.editingChecklist.joinToString("\n") { it.text }
                    _state.value = state.value.copy(
                        editingNoteType = NoteType.TEXT,
                        editingContent = TextFieldValue(textContent),
                        editingChecklist = emptyList()
                    )
                }
            }
            is ProjectNotesEvent.ToggleTodoComplete -> {
                viewModelScope.launch {
                    todoRepository.updateTodo(event.todo.copy(isCompleted = !event.todo.isCompleted))
                }
            }
            is ProjectNotesEvent.DeleteTodo -> {
                viewModelScope.launch {
                    todoRepository.deleteTodo(event.todo)
                }
            }
            is ProjectNotesEvent.ShareTodo -> {
                viewModelScope.launch {
                    val todo = event.todo.todo
                    val subtasks = event.todo.subtasks

                    val sb = StringBuilder()
                    if (todo.description.isNotBlank()) {
                        sb.append(todo.description).append("\n\n")
                    }

                    if (subtasks.isNotEmpty()) {
                        subtasks.forEach { subtask ->
                            val status = if (subtask.isChecked) "[x]" else "[ ]"
                            sb.append("$status ${subtask.text}\n")
                        }
                    }

                    _events.emit(ProjectNotesUiEvent.SendNotes(todo.title, sb.toString()))
                }
            }
            is ProjectNotesEvent.ConvertToTodo -> {
                viewModelScope.launch {
                    val title = state.value.editingTitle
                    val content = state.value.editingContent.text
                    val noteType = state.value.editingNoteType
                    val projectId = this@ProjectNotesViewModel.projectId
                    
                    val maxPos = todoRepository.getMaxPosition()
                    val todo = com.suvojeet.notenext.data.TodoItem(
                        title = if (title.isBlank()) "Converted Note" else title,
                        description = if (noteType == NoteType.TEXT) content else "",
                        projectId = if (projectId != -1) projectId else null,
                        position = maxPos + 1,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    val todoId = todoRepository.insertTodo(todo).toInt()
                    
                    if (noteType == NoteType.CHECKLIST) {
                        val subtasks = state.value.editingChecklist.map { item ->
                            com.suvojeet.notenext.data.TodoSubtask(
                                todoId = todoId,
                                text = item.text,
                                isChecked = item.isChecked,
                                position = item.position
                            )
                        }
                        todoRepository.insertSubtasks(subtasks)
                    }
                    
                    // Bin the note after conversion
                    val currentNoteId = state.value.expandedNoteId
                    if (currentNoteId != null && currentNoteId != -1) {
                        repository.getNoteById(currentNoteId)?.let { noteWithAttachments ->
                            repository.updateNote(noteWithAttachments.note.copy(isBinned = true, binnedOn = System.currentTimeMillis()))
                        }
                    }
                    
                    onEvent(ProjectNotesEvent.CollapseNote)
                    _events.emit(ProjectNotesUiEvent.ShowSnackbar("Converted to Todo successfully"))
                }
            }
            is ProjectNotesEvent.DeleteAllCheckedItems -> {
                val updatedChecklist = state.value.editingChecklist.filter { !it.isChecked }
                _state.value = state.value.copy(editingChecklist = updatedChecklist)
            }
            is ProjectNotesEvent.ToggleCheckedItemsExpanded -> {
                _state.value = state.value.copy(
                    isCheckedItemsExpanded = !state.value.isCheckedItemsExpanded
                )
            }
            is ProjectNotesEvent.ToggleNoteSearch -> {
                _state.value = state.value.copy(isSearchingInNote = !state.value.isSearchingInNote)
                if (!state.value.isSearchingInNote) {
                    _state.value = state.value.copy(noteSearchQuery = "", searchResultIndices = emptyList(), currentSearchResultIndex = -1)
                }
            }
            is ProjectNotesEvent.OnNoteSearchQueryChange -> {
                val query = event.query
                val content = state.value.editingContent.text
                val indices = mutableListOf<Int>()
                if (query.isNotBlank()) {
                    var index = content.indexOf(query, ignoreCase = true)
                    while (index != -1) {
                        indices.add(index)
                        index = content.indexOf(query, index + 1, ignoreCase = true)
                    }
                }
                _state.value = state.value.copy(
                    noteSearchQuery = query,
                    searchResultIndices = indices,
                    currentSearchResultIndex = if (indices.isNotEmpty()) 0 else -1
                )
                if (indices.isNotEmpty()) {
                    viewModelScope.launch {
                        _events.emit(ProjectNotesUiEvent.ScrollToSearchResult(indices[0]))
                    }
                }
            }
            is ProjectNotesEvent.NextSearchResult -> {
                val indices = state.value.searchResultIndices
                if (indices.isNotEmpty()) {
                    val nextIndex = (state.value.currentSearchResultIndex + 1) % indices.size
                    _state.value = state.value.copy(currentSearchResultIndex = nextIndex)
                    viewModelScope.launch {
                        _events.emit(ProjectNotesUiEvent.ScrollToSearchResult(indices[nextIndex]))
                    }
                }
            }
            is ProjectNotesEvent.PreviousSearchResult -> {
                val indices = state.value.searchResultIndices
                if (indices.isNotEmpty()) {
                    val prevIndex = if (state.value.currentSearchResultIndex <= 0) indices.size - 1 else state.value.currentSearchResultIndex - 1
                    _state.value = state.value.copy(currentSearchResultIndex = prevIndex)
                    viewModelScope.launch {
                        _events.emit(ProjectNotesUiEvent.ScrollToSearchResult(indices[prevIndex]))
                    }
                }
            }
            is ProjectNotesEvent.LoadExternalFile -> {
                viewModelScope.launch {
                    try {
                        val uri = event.uri
                        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                        // For ProjectNotesViewModel, we might need to handle this differently, but for now we follow NotesViewModel pattern
                        _state.value = state.value.copy(
                            expandedNoteId = -1,
                            editingTitle = "External Note", // Or get filename if ContextUtils available
                            editingContent = TextFieldValue(content),
                            editingNoteType = NoteType.TEXT,
                            editingIsNewNote = true
                        )
                    } catch (e: Exception) {
                        _events.emit(ProjectNotesUiEvent.ShowSnackbar("Failed to load file: ${e.message}"))
                    }
                }
            }
            is ProjectNotesEvent.SaveExternalAsNote -> {
                viewModelScope.launch {
                    onEvent(ProjectNotesEvent.OnSaveNoteClick(shouldCollapse = false))
                    _events.emit(ProjectNotesUiEvent.ShowSnackbar("Saved as internal note"))
                }
            }
            is ProjectNotesEvent.NoOp -> { /* no-op bridge for AI suggestion events not yet wired into ProjectNotesViewModel */ }
        }
    }

    suspend fun getNoteIdByTitle(title: String): Int? {
        return repository.getNoteIdByTitle(title)
    }

    private fun runToneRewrite(tone: com.suvojeet.notenext.data.ai.ToneOption) {
        val source = state.value.editingContent.text
        if (source.isBlank()) {
            viewModelScope.launch { _events.emit(ProjectNotesUiEvent.ShowSnackbar("Nothing to rewrite")) }
            return
        }
        _state.value = state.value.copy(
            toneRewriteSelectedTone = tone,
            isToneRewriting = true,
            toneRewriteError = null,
            toneRewriteResult = null
        )
        viewModelScope.launch {
            when (val result = aiSuggestionsDelegate.rewriteTone(source, tone)) {
                is com.suvojeet.notenext.data.ai.AIResult.Success -> {
                    _state.value = state.value.copy(
                        isToneRewriting = false,
                        toneRewriteResult = result.data
                    )
                }
                is com.suvojeet.notenext.data.ai.AIResult.AuthError ->
                    _state.value = state.value.copy(isToneRewriting = false, toneRewriteError = result.message)
                is com.suvojeet.notenext.data.ai.AIResult.NetworkError ->
                    _state.value = state.value.copy(isToneRewriting = false, toneRewriteError = "Network: ${result.message}")
                is com.suvojeet.notenext.data.ai.AIResult.RateLimited ->
                    _state.value = state.value.copy(isToneRewriting = false, toneRewriteError = "Rate limited. Try again in ${result.retryAfterSeconds}s.")
                is com.suvojeet.notenext.data.ai.AIResult.ProviderError ->
                    _state.value = state.value.copy(isToneRewriting = false, toneRewriteError = result.message)
                is com.suvojeet.notenext.data.ai.AIResult.AllProvidersFailed ->
                    _state.value = state.value.copy(isToneRewriting = false, toneRewriteError = "All providers failed")
            }
        }
    }

    private fun commonPrefixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[i] != b[i]) {
                return a.substring(0, i)
            }
        }
        return a.substring(0, minLength)
    }

    private fun commonSuffixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[a.length - 1 - i] != b[b.length - 1 - i]) {
                return a.substring(a.length - i)
            }
        }
        return a.substring(a.length - minLength)
    }
}