package com.suvojeet.notenext.ui.project

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.core.util.SortType
import com.suvojeet.notenext.data.RepeatOption
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.core.model.NoteType
import java.time.LocalDate
import java.time.LocalTime

sealed class ProjectNotesEvent {
    /** Bridge for cross-context events that have no behaviour in the project editor (currently AI suggestion events). */
    object NoOp : ProjectNotesEvent()
    data class DeleteNote(val note: NoteSummaryWithAttachments) : ProjectNotesEvent()
    object RestoreNote : ProjectNotesEvent()
    data class ToggleNoteSelection(val noteId: Int) : ProjectNotesEvent()
    object ClearSelection : ProjectNotesEvent()
    object SelectAllNotes : ProjectNotesEvent()
    object TogglePinForSelectedNotes : ProjectNotesEvent()
    object ToggleLockForSelectedNotes : ProjectNotesEvent()
    object DeleteSelectedNotes : ProjectNotesEvent()
    object ArchiveSelectedNotes : ProjectNotesEvent()
    data class ChangeColorForSelectedNotes(val color: Int) : ProjectNotesEvent()
    object CopySelectedNotes : ProjectNotesEvent()
    object SendSelectedNotes : ProjectNotesEvent()
    data class SetReminderForSelectedNotes(val date: LocalDate, val time: LocalTime, val repeatOption: RepeatOption) : ProjectNotesEvent()
    object ToggleImportantForSelectedNotes : ProjectNotesEvent()
    data class SetLabelForSelectedNotes(val label: String) : ProjectNotesEvent()
    data class ExpandNote(val noteId: Int, val noteType: NoteType = NoteType.TEXT) : ProjectNotesEvent()
    object CollapseNote : ProjectNotesEvent()

    // Checklist Events
    data class OnChecklistItemCheckedChange(val itemId: String, val isChecked: Boolean) : ProjectNotesEvent()
    data class OnChecklistItemTextChange(val itemId: String, val text: String) : ProjectNotesEvent()
    data class OnChecklistItemValueChange(val itemId: String, val value: TextFieldValue) : ProjectNotesEvent()
    data class OnChecklistItemFocus(val itemId: String) : ProjectNotesEvent()
    data class SwapChecklistItems(val fromId: String, val toId: String) : ProjectNotesEvent()
    object AddChecklistItem : ProjectNotesEvent()
    data class AddChecklistItemAfter(val itemId: String) : ProjectNotesEvent()
    data class DeleteChecklistItem(val itemId: String) : ProjectNotesEvent()
    data class IndentChecklistItem(val itemId: String) : ProjectNotesEvent()
    data class OutdentChecklistItem(val itemId: String) : ProjectNotesEvent()

    // Events from AddEditNoteEvent
    data class OnTitleChange(val title: String) : ProjectNotesEvent()
    data class OnContentChange(val content: TextFieldValue) : ProjectNotesEvent()
    data class ApplyStyleToContent(val style: SpanStyle) : ProjectNotesEvent()
    data class ApplyHeadingStyle(val level: Int) : ProjectNotesEvent()
    object ApplyBulletedList : ProjectNotesEvent()
    data class OnColorChange(val color: Int) : ProjectNotesEvent()
    data class OnSaveNoteClick(val shouldCollapse: Boolean = true) : ProjectNotesEvent()
    object OnDeleteNoteClick : ProjectNotesEvent()
    object OnTogglePinClick : ProjectNotesEvent()
    object OnToggleLockClick : ProjectNotesEvent()
    object OnToggleArchiveClick : ProjectNotesEvent()
    object OnUndoClick : ProjectNotesEvent()
    object OnRedoClick : ProjectNotesEvent()
    object OnCopyCurrentNoteClick : ProjectNotesEvent()
    object OnAddLabelsToCurrentNoteClick : ProjectNotesEvent()
    data class OnLabelChange(val label: String) : ProjectNotesEvent()
    data class SetInitialTitle(val title: String) : ProjectNotesEvent()
    object DismissLabelDialog : ProjectNotesEvent()
    data class OnReminderChange(val time: Long?, val repeatOption: String?) : ProjectNotesEvent()
    data class OnExpiryChange(val expiryTime: Long?) : ProjectNotesEvent()
    object AutoSaveNote : ProjectNotesEvent()

    // AI Events
    object SummarizeNote : ProjectNotesEvent()
    data class GenerateChecklist(val topic: String) : ProjectNotesEvent()
    data class InsertGeneratedChecklist(val items: List<String>) : ProjectNotesEvent()
    object ClearGeneratedChecklist : ProjectNotesEvent()
    object ClearSummary : ProjectNotesEvent()
    object FixGrammar : ProjectNotesEvent()
    object ApplyGrammarFix : ProjectNotesEvent()
    object ClearGrammarFix : ProjectNotesEvent()

    data class PickToneRewrite(val tone: com.suvojeet.notenext.data.ai.ToneOption) : ProjectNotesEvent()
    object AcceptToneRewrite : ProjectNotesEvent()
    object RetryToneRewrite : ProjectNotesEvent()
    
    // Utility Events
    data class ExportNote(val uri: android.net.Uri, val format: String) : ProjectNotesEvent()
    object ShareAsText : ProjectNotesEvent()

    data class OnLinkDetected(val url: String) : ProjectNotesEvent()
    data class OnLinkPreviewFetched(val url: String, val title: String?, val description: String?, val imageUrl: String?) : ProjectNotesEvent()
    data class OnRemoveLinkPreview(val url: String) : ProjectNotesEvent()
    object ToggleLayout : ProjectNotesEvent()
    data class SortNotes(val sortType: SortType) : ProjectNotesEvent()
    data class OnInsertLink(val url: String, val displayText: String? = null) : ProjectNotesEvent()
    object ClearNewlyAddedChecklistItemId : ProjectNotesEvent()
    data class AddAttachment(val uri: String, val mimeType: String) : ProjectNotesEvent()
    data class RemoveAttachment(val tempId: String) : ProjectNotesEvent()
    data class OnRestoreVersion(val version: NoteVersion) : ProjectNotesEvent()
    data class NavigateToNoteByTitle(val title: String) : ProjectNotesEvent()
    data class UpdateProjectDescription(val description: String?) : ProjectNotesEvent()
    object OnToggleNoteType : ProjectNotesEvent()
    object ConvertToTodo : ProjectNotesEvent()
    object DeleteAllCheckedItems : ProjectNotesEvent()
    object ToggleCheckedItemsExpanded : ProjectNotesEvent()

    // Search in Note
    object ToggleNoteSearch : ProjectNotesEvent()
    data class OnNoteSearchQueryChange(val query: String) : ProjectNotesEvent()
    object NextSearchResult : ProjectNotesEvent()
    object PreviousSearchResult : ProjectNotesEvent()

    // External file
    data class LoadExternalFile(val uri: android.net.Uri) : ProjectNotesEvent()
    object SaveExternalAsNote : ProjectNotesEvent()

    // Project Todo Events
    data class ToggleTodoComplete(val todo: com.suvojeet.notenext.data.TodoItem) : ProjectNotesEvent()
    data class DeleteTodo(val todo: com.suvojeet.notenext.data.TodoItem) : ProjectNotesEvent()
    data class ShareTodo(val todo: com.suvojeet.notenext.data.TodoWithSubtasks) : ProjectNotesEvent()
}