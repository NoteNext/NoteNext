package com.suvojeet.notenext.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Immutable
@Serializable
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val parentId: Int? = null,
    val orderIndex: Int = 0,
    val color: Int? = null
)

/** Aggregated note count for a single project, used by the project list. */
@Immutable
data class ProjectNoteCount(
    val projectId: Int,
    val noteCount: Int
)
