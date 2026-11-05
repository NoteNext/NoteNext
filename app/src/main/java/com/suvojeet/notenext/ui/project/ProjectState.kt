package com.suvojeet.notenext.ui.project

import androidx.compose.runtime.Immutable
import com.suvojeet.notenext.data.Project

@Immutable
data class ProjectState(
    val projects: List<Project> = emptyList(),
    /** projectId → number of active (not archived/binned) notes inside it. */
    val noteCounts: Map<Int, Int> = emptyMap()
)
