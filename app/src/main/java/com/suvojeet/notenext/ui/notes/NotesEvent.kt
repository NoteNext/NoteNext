package com.suvojeet.notenext.ui.notes

import android.net.Uri
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.data.LinkPreview
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.data.NoteSummaryWithAttachments
import com.suvojeet.notenext.data.RepeatOption
import com.suvojeet.notenext.core.util.SortType
import java.time.LocalDate
import java.time.LocalTime

sealed class NotesEvent {
    data class DeleteNote(val note: NoteSummaryWithAttachments) : NotesEvent()
    data class ArchiveNote(val note: NoteSummaryWithAttachments) : NotesEvent()
    object RestoreNote : NotesEvent()
    data class ToggleNoteSelection(val noteId: Int) : NotesEvent()
    object ClearSelection : NotesEvent()
    object SelectAllNotes : NotesEvent()
    object TogglePinForSelectedNotes : NotesEvent()
    /** Persist a new manual order for the pinned section. [orderedIds] is top-to-bottom. */
    data class ReorderPinnedNotes(val orderedIds: List<Int>) : NotesEvent()
    object ToggleLockForSelectedNotes : NotesEvent()
    object DeleteSelectedNotes : NotesEvent()
    object ArchiveSelectedNotes : NotesEvent()
    object ToggleImportantForSelectedNotes : NotesEvent()
    data class ChangeColorForSelectedNotes(val color: Int) : NotesEvent()
    object CopySelectedNotes : NotesEvent()
    object SendSelectedNotes : NotesEvent()
    object ShareSelectedNotesViaLink : NotesEvent()
    /** Share the note currently open in the editor as an encrypted link (prompts for options). */
    object ShareCurrentNoteViaLink : NotesEvent()
    /**
     * Confirm creating the encrypted share link after the user picked an expiry and
     * whether it should burn after reading. Acts on the previously-stashed target.
     */
    data class ConfirmShareViaLink(
        val expiry: com.suvojeet.notenext.data.share.ShareExpiry,
        val burnAfterRead: Boolean,
        /** Reads allowed before a burn-after-read note self-destructs (only used when burn is on). */
        val maxReads: Int = 1
    ) : NotesEvent()
    /**
     * Stop sharing (delete from backend) the note behind a share link using noteToken.
     */
    data class UnshareNote(val shareId: String, val noteToken: String? = null) : NotesEvent() {
        val deleteToken: String? get() = noteToken
    }
    /**
     * Update an existing shared note on the backend using its universal noteToken.
     */
    data class UpdateSharedNote(val shareId: String, val noteToken: String? = null) : NotesEvent()
    data class SetReminderForSelectedNotes(val date: LocalDate, val time: LocalTime, val repeatOption: RepeatOption) : NotesEvent()
    data class SetLabelForSelectedNotes(val label: String) : NotesEvent()
    data class ExpandNote(
        val noteId: Int, 
        val noteType: NoteType = NoteType.TEXT,
        val authenticatedCipherTitle: javax.crypto.Cipher? = null,
        val authenticatedCipherContent: javax.crypto.Cipher? = null
    ) : NotesEvent()
    object CollapseNote : NotesEvent()

    // Checklist Events
    data class OnChecklistItemCheckedChange(val itemId: String, val isChecked: Boolean) : NotesEvent()
    data class OnChecklistItemTextChange(val itemId: String, val text: String) : NotesEvent()
    data class OnChecklistItemValueChange(val itemId: String, val value: TextFieldValue) : NotesEvent()
    data class OnChecklistItemFocus(val itemId: String) : NotesEvent()
    data class SwapChecklistItems(val fromId: String, val toId: String) : NotesEvent()
    object AddChecklistItem : NotesEvent()
    data class AddChecklistItemAfter(val itemId: String) : NotesEvent()
    data class DeleteChecklistItem(val itemId: String) : NotesEvent()
    data class IndentChecklistItem(val itemId: String) : NotesEvent()
    data class OutdentChecklistItem(val itemId: String) : NotesEvent()

    // Events from AddEditNoteEvent
    data class OnTitleChange(val title: String) : NotesEvent()
    data class OnContentChange(val content: TextFieldValue) : NotesEvent()
    data class ApplyStyleToContent(val style: SpanStyle) : NotesEvent()
    data class ApplyHeadingStyle(val level: Int) : NotesEvent()
    object ApplyBulletedList : NotesEvent()
    data class OnColorChange(val color: Int) : NotesEvent()
    object OnSaveNoteClick : NotesEvent()
    object OnDeleteNoteClick : NotesEvent()
    object OnTogglePinClick : NotesEvent()
    object OnToggleLockClick : NotesEvent()
    object OnToggleArchiveClick : NotesEvent()
    object OnUndoClick : NotesEvent()
    object OnRedoClick : NotesEvent()
    object OnCopyCurrentNoteClick : NotesEvent()
    object OnAddLabelsToCurrentNoteClick : NotesEvent()
    data class OnLabelChange(val label: String) : NotesEvent()
    object DismissLabelDialog : NotesEvent()
    data class FilterByLabel(val label: String?) : NotesEvent()
    data class FilterByProject(val projectId: Int?) : NotesEvent()
    data class OnReminderChange(val time: Long?, val repeatOption: String?) : NotesEvent()
    data class OnExpiryChange(val expiryTime: Long?) : NotesEvent()

    data class OnLinkDetected(val url: String) : NotesEvent()
    data class OnLinkPreviewFetched(val url: String, val title: String?, val description: String?, val imageUrl: String?) : NotesEvent()
    data class OnRemoveLinkPreview(val url: String) : NotesEvent()
    object ToggleLayout : NotesEvent()
    data class SortNotes(val sortType: SortType) : NotesEvent()
    data class OnInsertLink(val url: String, val displayText: String? = null) : NotesEvent()
    object ClearNewlyAddedChecklistItemId : NotesEvent()
    data class AddAttachment(val uri: String, val mimeType: String) : NotesEvent()
    data class RemoveAttachment(val tempId: String) : NotesEvent()
    data class CreateProject(val name: String) : NotesEvent()
    data class MoveSelectedNotesToProject(val projectId: Int?) : NotesEvent()
    data class CreateNoteFromSharedText(val text: String) : NotesEvent()
    data class SetInitialTitle(val title: String) : NotesEvent()

    data class OnSearchQueryChange(val query: String) : NotesEvent()
    data class OnRestoreVersion(val version: NoteVersion) : NotesEvent()
    data class NavigateToNoteByTitle(val title: String) : NotesEvent()
    object OnToggleNoteType : NotesEvent()
    object ConvertToTodo : NotesEvent()
    object DeleteAllCheckedItems : NotesEvent()
    object ToggleCheckedItemsExpanded : NotesEvent()
    object SummarizeNote : NotesEvent()
    data class GenerateChecklist(val topic: String) : NotesEvent()
    data class InsertGeneratedChecklist(val items: List<String>) : NotesEvent()
    object ClearGeneratedChecklist : NotesEvent()
    object ExtractActionItems : NotesEvent()
    data class SaveActionItemsToTodo(val tasks: List<Pair<String, String>>) : NotesEvent()
    object ClearExtractedActionItems : NotesEvent()
    object ClearSummary : NotesEvent()
    object FixGrammar : NotesEvent()
    object ApplyGrammarFix : NotesEvent()
    object ClearGrammarFix : NotesEvent()
    data class ImportImage(val uri: Uri) : NotesEvent()
    data class ExportNote(val uri: Uri, val format: String) : NotesEvent()
    data class LoadExternalFile(val uri: Uri) : NotesEvent()
    object SaveExternalAsNote : NotesEvent()
    object AutoSaveNote : NotesEvent()

    // Mention events
    data class OnMentionSearchQueryChange(val query: String) : NotesEvent()
    data class InsertMention(val noteId: Int, val noteTitle: String) : NotesEvent()
    object CloseMentionPopup : NotesEvent()

    // Search in Note
    object ToggleNoteSearch : NotesEvent()
    data class OnNoteSearchQueryChange(val query: String) : NotesEvent()
    object NextSearchResult : NotesEvent()
    object PreviousSearchResult : NotesEvent()

    // ─── AI advanced features ──────────────────────────────────────────
    data class PickToneRewrite(val tone: com.suvojeet.notenext.data.ai.ToneOption) : NotesEvent()
    object AcceptToneRewrite : NotesEvent()
    object RetryToneRewrite : NotesEvent()

    data class AcceptSuggestedLabel(val label: String) : NotesEvent()
    object DismissSuggestedLabels : NotesEvent()

    object AcceptExtractedReminder : NotesEvent()
    object DismissExtractedReminder : NotesEvent()

    data class OpenLinkedNote(val noteId: Int) : NotesEvent()
}
