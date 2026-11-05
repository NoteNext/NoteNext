package com.suvojeet.notenext.ui.project

import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesUiEvent

fun NotesEvent.toProjectNotesEvent(): ProjectNotesEvent {
    return when (this) {
        is NotesEvent.DeleteNote -> ProjectNotesEvent.DeleteNote(this.note)
        is NotesEvent.RestoreNote -> ProjectNotesEvent.RestoreNote
        is NotesEvent.ToggleNoteSelection -> ProjectNotesEvent.ToggleNoteSelection(this.noteId)
        is NotesEvent.ClearSelection -> ProjectNotesEvent.ClearSelection
        is NotesEvent.TogglePinForSelectedNotes -> ProjectNotesEvent.TogglePinForSelectedNotes
        is NotesEvent.DeleteSelectedNotes -> ProjectNotesEvent.DeleteSelectedNotes
        is NotesEvent.SelectAllNotes -> ProjectNotesEvent.SelectAllNotes
        is NotesEvent.ArchiveSelectedNotes -> ProjectNotesEvent.ArchiveSelectedNotes
        is NotesEvent.ChangeColorForSelectedNotes -> ProjectNotesEvent.ChangeColorForSelectedNotes(this.color)
        is NotesEvent.CopySelectedNotes -> ProjectNotesEvent.CopySelectedNotes
        is NotesEvent.SendSelectedNotes -> ProjectNotesEvent.SendSelectedNotes
        is NotesEvent.ShareSelectedNotesViaLink -> throw IllegalArgumentException("ShareSelectedNotesViaLink event cannot be converted")
        // Link sharing from the editor is only wired into the main notes graph;
        // in a project the editor still offers text share, so this degrades to NoOp.
        is NotesEvent.ShareCurrentNoteViaLink -> ProjectNotesEvent.NoOp
        // Encrypted-link confirmation is only wired into the main notes graph.
        is NotesEvent.ConfirmShareViaLink -> ProjectNotesEvent.NoOp
        // Unshare and UpdateSharedNote are likewise only wired into the main notes graph.
        is NotesEvent.UnshareNote -> ProjectNotesEvent.NoOp
        is NotesEvent.UpdateSharedNote -> ProjectNotesEvent.NoOp
        is NotesEvent.SetReminderForSelectedNotes -> ProjectNotesEvent.SetReminderForSelectedNotes(this.date, this.time, this.repeatOption)
        is NotesEvent.ToggleImportantForSelectedNotes -> ProjectNotesEvent.ToggleImportantForSelectedNotes
        is NotesEvent.SetLabelForSelectedNotes -> ProjectNotesEvent.SetLabelForSelectedNotes(this.label)
        is NotesEvent.ExpandNote -> ProjectNotesEvent.ExpandNote(this.noteId, this.noteType)
        is NotesEvent.CollapseNote -> ProjectNotesEvent.CollapseNote
        is NotesEvent.OnChecklistItemCheckedChange -> ProjectNotesEvent.OnChecklistItemCheckedChange(this.itemId, this.isChecked)
        is NotesEvent.OnChecklistItemTextChange -> ProjectNotesEvent.OnChecklistItemTextChange(this.itemId, this.text)
        is NotesEvent.OnChecklistItemValueChange -> ProjectNotesEvent.OnChecklistItemValueChange(this.itemId, this.value)
        is NotesEvent.OnChecklistItemFocus -> ProjectNotesEvent.OnChecklistItemFocus(this.itemId)
        is NotesEvent.SwapChecklistItems -> ProjectNotesEvent.SwapChecklistItems(this.fromId, this.toId)
        is NotesEvent.AddChecklistItem -> ProjectNotesEvent.AddChecklistItem
        is NotesEvent.AddChecklistItemAfter -> ProjectNotesEvent.AddChecklistItemAfter(this.itemId)
        is NotesEvent.DeleteChecklistItem -> ProjectNotesEvent.DeleteChecklistItem(this.itemId)
        is NotesEvent.IndentChecklistItem -> ProjectNotesEvent.IndentChecklistItem(this.itemId)
        is NotesEvent.OutdentChecklistItem -> ProjectNotesEvent.OutdentChecklistItem(this.itemId)
        is NotesEvent.OnTitleChange -> ProjectNotesEvent.OnTitleChange(this.title)
        is NotesEvent.OnContentChange -> ProjectNotesEvent.OnContentChange(this.content)
        is NotesEvent.ApplyStyleToContent -> ProjectNotesEvent.ApplyStyleToContent(this.style)
        is NotesEvent.ApplyHeadingStyle -> ProjectNotesEvent.ApplyHeadingStyle(this.level)
        is NotesEvent.ApplyBulletedList -> ProjectNotesEvent.ApplyBulletedList
        is NotesEvent.OnColorChange -> ProjectNotesEvent.OnColorChange(this.color)
        is NotesEvent.OnSaveNoteClick -> ProjectNotesEvent.OnSaveNoteClick(shouldCollapse = true)
        is NotesEvent.OnDeleteNoteClick -> ProjectNotesEvent.OnDeleteNoteClick
        is NotesEvent.OnTogglePinClick -> ProjectNotesEvent.OnTogglePinClick
        is NotesEvent.OnToggleLockClick -> ProjectNotesEvent.OnToggleLockClick
        is NotesEvent.OnToggleArchiveClick -> ProjectNotesEvent.OnToggleArchiveClick
        is NotesEvent.OnUndoClick -> ProjectNotesEvent.OnUndoClick
        is NotesEvent.OnRedoClick -> ProjectNotesEvent.OnRedoClick
        is NotesEvent.OnCopyCurrentNoteClick -> ProjectNotesEvent.OnCopyCurrentNoteClick
        is NotesEvent.OnAddLabelsToCurrentNoteClick -> ProjectNotesEvent.OnAddLabelsToCurrentNoteClick
        is NotesEvent.OnLabelChange -> ProjectNotesEvent.OnLabelChange(this.label)
        is NotesEvent.SetInitialTitle -> ProjectNotesEvent.SetInitialTitle(this.title)
        is NotesEvent.DismissLabelDialog -> ProjectNotesEvent.DismissLabelDialog
        is NotesEvent.OnLinkDetected -> ProjectNotesEvent.OnLinkDetected(this.url)
        is NotesEvent.OnLinkPreviewFetched -> ProjectNotesEvent.OnLinkPreviewFetched(this.url, this.title, this.description, this.imageUrl)
        is NotesEvent.OnRemoveLinkPreview -> ProjectNotesEvent.OnRemoveLinkPreview(this.url)
        is NotesEvent.ToggleLayout -> ProjectNotesEvent.ToggleLayout
        is NotesEvent.SortNotes -> ProjectNotesEvent.SortNotes(this.sortType)
        is NotesEvent.OnInsertLink -> ProjectNotesEvent.OnInsertLink(this.url, this.displayText)
        is NotesEvent.ClearNewlyAddedChecklistItemId -> ProjectNotesEvent.ClearNewlyAddedChecklistItemId
        is NotesEvent.AddAttachment -> ProjectNotesEvent.AddAttachment(this.uri, this.mimeType)
        is NotesEvent.RemoveAttachment -> ProjectNotesEvent.RemoveAttachment(this.tempId)
        is NotesEvent.OnRestoreVersion -> ProjectNotesEvent.OnRestoreVersion(this.version)
        is NotesEvent.NavigateToNoteByTitle -> ProjectNotesEvent.NavigateToNoteByTitle(this.title)
        is NotesEvent.CreateProject -> throw IllegalArgumentException("CreateProject event cannot be converted")
        is NotesEvent.FilterByLabel -> throw IllegalArgumentException("FilterByLabel event cannot be converted")
        is NotesEvent.FilterByProject -> throw IllegalArgumentException("FilterByProject event cannot be converted")
        is NotesEvent.OnReminderChange -> ProjectNotesEvent.OnReminderChange(this.time, this.repeatOption)
        is NotesEvent.OnExpiryChange -> ProjectNotesEvent.OnExpiryChange(this.expiryTime)
        is NotesEvent.MoveSelectedNotesToProject -> throw IllegalArgumentException("MoveSelectedNotesToProject event cannot be converted")
        is NotesEvent.ToggleLockForSelectedNotes -> ProjectNotesEvent.ToggleLockForSelectedNotes
        is NotesEvent.CreateNoteFromSharedText -> throw IllegalArgumentException("CreateNoteFromSharedText event cannot be converted")

        is NotesEvent.OnSearchQueryChange -> throw IllegalArgumentException("OnSearchQueryChange event cannot be converted")
        is NotesEvent.OnToggleNoteType -> ProjectNotesEvent.OnToggleNoteType
        is NotesEvent.ConvertToTodo -> ProjectNotesEvent.ConvertToTodo
        is NotesEvent.DeleteAllCheckedItems -> ProjectNotesEvent.DeleteAllCheckedItems
        is NotesEvent.ToggleCheckedItemsExpanded -> ProjectNotesEvent.ToggleCheckedItemsExpanded
        is NotesEvent.SummarizeNote -> ProjectNotesEvent.SummarizeNote
        is NotesEvent.GenerateChecklist -> ProjectNotesEvent.GenerateChecklist(this.topic)
        is NotesEvent.InsertGeneratedChecklist -> ProjectNotesEvent.InsertGeneratedChecklist(this.items)
        is NotesEvent.ClearGeneratedChecklist -> ProjectNotesEvent.ClearGeneratedChecklist
        is NotesEvent.ClearSummary -> ProjectNotesEvent.ClearSummary
        is NotesEvent.FixGrammar -> ProjectNotesEvent.FixGrammar
        is NotesEvent.ApplyGrammarFix -> ProjectNotesEvent.ApplyGrammarFix
        is NotesEvent.ClearGrammarFix -> ProjectNotesEvent.ClearGrammarFix
        is NotesEvent.AutoSaveNote -> ProjectNotesEvent.AutoSaveNote
        is NotesEvent.ImportImage -> throw IllegalArgumentException("ImportImage event cannot be converted")
        is NotesEvent.ExportNote -> ProjectNotesEvent.ExportNote(this.uri, this.format)
        
        is NotesEvent.OnMentionSearchQueryChange -> throw IllegalArgumentException("OnMentionSearchQueryChange event cannot be converted")
        is NotesEvent.InsertMention -> throw IllegalArgumentException("InsertMention event cannot be converted")
        is NotesEvent.CloseMentionPopup -> throw IllegalArgumentException("CloseMentionPopup event cannot be converted")

        is NotesEvent.ToggleNoteSearch -> ProjectNotesEvent.ToggleNoteSearch
        is NotesEvent.OnNoteSearchQueryChange -> ProjectNotesEvent.OnNoteSearchQueryChange(this.query)
        is NotesEvent.NextSearchResult -> ProjectNotesEvent.NextSearchResult
        is NotesEvent.PreviousSearchResult -> ProjectNotesEvent.PreviousSearchResult
        is NotesEvent.LoadExternalFile -> ProjectNotesEvent.LoadExternalFile(this.uri)
        is NotesEvent.SaveExternalAsNote -> ProjectNotesEvent.SaveExternalAsNote

        // AI advanced features
        is NotesEvent.PickToneRewrite -> ProjectNotesEvent.PickToneRewrite(this.tone)
        is NotesEvent.AcceptToneRewrite -> ProjectNotesEvent.AcceptToneRewrite
        is NotesEvent.RetryToneRewrite -> ProjectNotesEvent.RetryToneRewrite

        is NotesEvent.ReorderPinnedNotes, // pinned reorder is only wired into the main notes list
        is NotesEvent.ArchiveNote, // swipe-to-archive is only wired into the main notes list
        is NotesEvent.AcceptSuggestedLabel,
        is NotesEvent.DismissSuggestedLabels,
        is NotesEvent.AcceptExtractedReminder,
        is NotesEvent.DismissExtractedReminder,
        is NotesEvent.OpenLinkedNote,
        is NotesEvent.ExtractActionItems,
        is NotesEvent.SaveActionItemsToTodo,
        is NotesEvent.ClearExtractedActionItems -> ProjectNotesEvent.NoOp
    }
}

fun ProjectNotesUiEvent.toNotesUiEvent(): NotesUiEvent {
    return when (this) {
        is ProjectNotesUiEvent.SendNotes -> NotesUiEvent.SendNotes(this.title, this.content)
        is ProjectNotesUiEvent.ShowSnackbar -> NotesUiEvent.ShowSnackbar(this.message, this.actionLabel, this.onAction)
        is ProjectNotesUiEvent.LinkPreviewRemoved -> NotesUiEvent.LinkPreviewRemoved
        is ProjectNotesUiEvent.NavigateToNoteByTitle -> NotesUiEvent.NavigateToNoteByTitle(this.title)
        is ProjectNotesUiEvent.ScrollToSearchResult -> NotesUiEvent.ScrollToSearchResult(this.index)
    }
}