package com.suvojeet.notenext.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.ProjectDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProjectScreenEvent {
    data class CreateNewNote(val projectId: Int) : ProjectScreenEvent
    data class CreateNewChecklist(val projectId: Int) : ProjectScreenEvent
    data class CreateProject(
        val name: String,
        val description: String?,
        val parentId: Int? = null,
        val color: Int? = null
    ) : ProjectScreenEvent
    data class UpdateProject(
        val projectId: Int,
        val name: String,
        val description: String?,
        val color: Int? = null
    ) : ProjectScreenEvent
    data class MoveProject(val projectId: Int, val newParentId: Int?) : ProjectScreenEvent
    data class ReorderProject(val projectId: Int, val newOrder: Int) : ProjectScreenEvent
    data class DeleteProject(val projectId: Int) : ProjectScreenEvent
}

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val repository: com.suvojeet.notenext.data.NoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectState())
    val state = _state.asStateFlow()

    private val _events = Channel<ProjectScreenEvent>()
    val events = _events.receiveAsFlow()

    init {
        repository.getProjectHierarchy().onEach { projects ->
            _state.value = _state.value.copy(projects = projects)
        }.launchIn(viewModelScope)

        repository.getProjectNoteCounts().onEach { counts ->
            _state.value = _state.value.copy(noteCounts = counts)
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: ProjectScreenEvent) {
        when (event) {
            is ProjectScreenEvent.CreateNewNote -> {
                viewModelScope.launch {
                    _events.send(ProjectScreenEvent.CreateNewNote(event.projectId))
                }
            }
            is ProjectScreenEvent.CreateNewChecklist -> {
                viewModelScope.launch {
                    _events.send(ProjectScreenEvent.CreateNewChecklist(event.projectId))
                }
            }
            is ProjectScreenEvent.CreateProject -> {
                viewModelScope.launch {
                    if (event.name.isNotBlank()) {
                        val order = _state.value.projects.count { it.parentId == event.parentId }
                        val project = Project(
                            name = event.name,
                            description = event.description,
                            parentId = event.parentId,
                            orderIndex = order,
                            color = event.color
                        )
                        repository.insertProject(project)
                    }
                }
            }
            is ProjectScreenEvent.UpdateProject -> {
                viewModelScope.launch {
                    repository.getProjectById(event.projectId)?.let { existing ->
                        if (event.name.isNotBlank()) {
                            repository.updateProject(
                                existing.copy(
                                    name = event.name,
                                    description = event.description,
                                    color = event.color
                                )
                            )
                        }
                    }
                }
            }
            is ProjectScreenEvent.MoveProject -> {
                viewModelScope.launch {
                    repository.moveProject(event.projectId, event.newParentId)
                }
            }
            is ProjectScreenEvent.ReorderProject -> {
                viewModelScope.launch {
                    repository.reorderProject(event.projectId, event.newOrder)
                }
            }
            is ProjectScreenEvent.DeleteProject -> {
                viewModelScope.launch {
                    repository.deleteProject(event.projectId)
                }
            }
        }
    }
}
