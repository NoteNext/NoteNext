package com.suvojeet.notenext.data

import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData
import com.suvojeet.notenext.core.util.SortType

interface NoteRepository {
    // Note operations
    fun getNotes(searchQuery: String = "", sortType: SortType = SortType.DATE_MODIFIED, projectId: Int? = null, isDecoy: Boolean = false): Flow<List<NoteWithAttachments>>
    fun getPinnedNotes(searchQuery: String = "", projectId: Int? = null, isDecoy: Boolean = false): Flow<List<NoteWithAttachments>>
    fun getOtherNotesPaged(searchQuery: String = "", sortType: SortType = SortType.DATE_MODIFIED, projectId: Int? = null, isDecoy: Boolean = false): Flow<PagingData<NoteWithAttachments>>

    // Optimized Note Summary operations
    fun getPinnedNoteSummaries(searchQuery: String = "", projectId: Int? = null, isDecoy: Boolean = false): Flow<List<NoteSummaryWithAttachments>>
    fun getOtherNoteSummariesPaged(searchQuery: String = "", sortType: SortType = SortType.DATE_MODIFIED, projectId: Int? = null, isDecoy: Boolean = false): Flow<PagingData<NoteSummaryWithAttachments>>
    fun getArchivedNoteSummaries(isDecoy: Boolean = false): Flow<List<NoteSummaryWithAttachments>>
    fun getBinnedNoteSummaries(isDecoy: Boolean = false): Flow<List<NoteSummaryWithAttachments>>
    fun getNoteSummariesByProjectId(projectId: Int, isDecoy: Boolean = false): Flow<List<NoteSummaryWithAttachments>>
    suspend fun getAllNoteIds(searchQuery: String = "", projectId: Int? = null, isDecoy: Boolean = false): List<Int>

    fun getArchivedNotes(isDecoy: Boolean = false): Flow<List<NoteWithAttachments>>
    fun getBinnedNotes(isDecoy: Boolean = false): Flow<List<NoteWithAttachments>>
    fun getNotesByProjectId(projectId: Int, isDecoy: Boolean = false): Flow<List<NoteWithAttachments>>
    fun getNotesModifiedSince(timestamp: Long): Flow<List<NoteWithAttachments>>
    suspend fun getNoteById(id: Int): NoteWithAttachments?
    suspend fun getNoteByShareId(shareId: String): Note?
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun updateNotePosition(id: Int, position: Int)
    suspend fun deleteNote(note: Note)
    suspend fun emptyBin()
    
    // Attachment operations
    suspend fun insertAttachment(attachment: Attachment)
    suspend fun deleteAttachment(attachment: Attachment)
    suspend fun deleteAttachmentById(attachmentId: Int)
    
    // Label operations
    fun getLabels(): Flow<List<Label>>
    fun getLabelsWithParent(parentName: String): Flow<List<Label>>
    fun getRootLabels(): Flow<List<Label>>
    suspend fun insertLabel(label: Label)
    suspend fun updateLabel(label: Label)
    suspend fun deleteLabel(label: Label)
    suspend fun updateLabelName(oldName: String, newName: String)
    suspend fun removeLabelFromNotes(labelName: String)

    // Project operations
    fun getProjects(): Flow<List<Project>>
    fun getRootProjects(): Flow<List<Project>>
    fun getSubProjects(parentId: Int): Flow<List<Project>>
    fun getProjectHierarchy(): Flow<List<Project>>
    suspend fun insertProject(project: Project): Long
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(projectId: Int)
    suspend fun getProjectById(projectId: Int): Project?
    fun getProjectNoteCounts(): Flow<Map<Int, Int>>
    suspend fun moveProject(projectId: Int, newParentId: Int?)
    suspend fun reorderProject(projectId: Int, newOrder: Int)

    // Reminder operations
    fun getNotesWithReminders(currentTime: Long): Flow<List<Note>>
    fun getAllReminders(): Flow<List<Note>>

    // Checklist operations
    suspend fun insertChecklistItems(items: List<ChecklistItem>)
    suspend fun updateChecklistItem(item: ChecklistItem)
    suspend fun updateChecklistItemStatus(id: String, isChecked: Boolean)
    suspend fun updateChecklistItems(items: List<ChecklistItem>)
    suspend fun deleteChecklistItem(item: ChecklistItem)
    suspend fun deleteChecklistForNote(noteId: Int)

    // Versioning operations
    suspend fun insertNoteVersion(version: NoteVersion)
    fun getNoteVersions(noteId: Int): Flow<List<NoteVersion>>
    suspend fun limitNoteVersions(noteId: Int, limit: Int)
    suspend fun getNoteIdByTitle(title: String): Int?
    suspend fun getNoteByTitleAndCreatedAt(title: String, createdAt: Long): Note?
    fun getAllNotes(): Flow<List<NoteWithAttachments>>
    fun getAllNotesModifiedSince(timestamp: Long): Flow<List<NoteWithAttachments>>

    // Database transaction
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}
