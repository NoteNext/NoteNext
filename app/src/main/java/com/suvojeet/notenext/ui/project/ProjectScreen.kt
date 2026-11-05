@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.project

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.data.repository.SettingsRepository
import com.suvojeet.notenext.navigation.Destination
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.theme.ThemeMode

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.platform.LocalContext
import com.suvojeet.notenext.util.ShortcutUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectScreen(
    onMenuClick: () -> Unit,
    onProjectClick: (Int) -> Unit,
    navController: androidx.navigation.NavController,
    settingsRepository: SettingsRepository
) {
    val viewModel: ProjectViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showCreateSubProjectDialog by remember { mutableStateOf<Int?>(null) }
    var projectToEdit by remember { mutableStateOf<com.suvojeet.notenext.data.Project?>(null) }
    var projectToDelete by remember { mutableStateOf<Int?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val expandedProjects = remember { mutableStateMapOf<Int, Boolean>() }
    val context = LocalContext.current

    // Build hierarchical project list
    // expandedProjects must be part of the key: without it, toggling a folder
    // recomposed the screen but `remember` kept the old (collapsed) hierarchy.
    val expandedIds = expandedProjects.filterValues { it }.keys
    val hierarchicalProjects = remember(state.projects, expandedIds) {
        val projectMap = state.projects.associateBy { it.id }
        val rootProjects = state.projects.filter { it.parentId == null }

        fun buildHierarchy(project: com.suvojeet.notenext.data.Project, depth: Int): List<HierarchicalProjectEntry> {
            val entry = HierarchicalProjectEntry(project, depth)
            val isExpanded = expandedProjects[project.id] ?: false
            val children = state.projects.filter { it.parentId == project.id }

            return listOf(entry) + if (isExpanded) {
                children.flatMap { child -> buildHierarchy(child, depth + 1) }
            } else {
                emptyList()
            }
        }

        rootProjects.flatMap { buildHierarchy(it, 0) }
    }

    // Search filters the hierarchy flatly so a match is never hidden inside a
    // collapsed parent.
    val visibleProjects = remember(hierarchicalProjects, state.projects, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            hierarchicalProjects
        } else {
            state.projects
                .filter { project ->
                    project.name.contains(query, ignoreCase = true) ||
                        project.description?.contains(query, ignoreCase = true) == true
                }
                .map { HierarchicalProjectEntry(it, 0) }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectScreenEvent.CreateNewNote -> {
                    navController.navigate(
                        Destination.AddEditNote(
                            projectId = event.projectId,
                            noteType = NoteType.TEXT
                        )
                    )
                }
                is ProjectScreenEvent.CreateNewChecklist -> {
                    navController.navigate(
                        Destination.AddEditNote(
                            projectId = event.projectId,
                            noteType = NoteType.CHECKLIST
                        )
                    )
                }
                else -> { }
            }
        }
    }

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onConfirm = { projectName, projectDescription, color ->
                viewModel.onEvent(ProjectScreenEvent.CreateProject(projectName, projectDescription, null, color))
                showCreateProjectDialog = false
            }
        )
    }

    if (showCreateSubProjectDialog != null) {
        CreateProjectDialog(
            onDismiss = { showCreateSubProjectDialog = null },
            onConfirm = { projectName, projectDescription, color ->
                viewModel.onEvent(ProjectScreenEvent.CreateProject(projectName, projectDescription, showCreateSubProjectDialog, color))
                showCreateSubProjectDialog = null
            }
        )
    }

    projectToEdit?.let { project ->
        CreateProjectDialog(
            title = stringResource(id = R.string.projects_edit),
            initialName = project.name,
            initialDescription = project.description.orEmpty(),
            initialColor = project.color,
            onDismiss = { projectToEdit = null },
            onConfirm = { projectName, projectDescription, color ->
                viewModel.onEvent(
                    ProjectScreenEvent.UpdateProject(
                        projectId = project.id,
                        name = projectName,
                        description = projectDescription,
                        color = color
                    )
                )
                projectToEdit = null
            }
        )
    }

    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(id = R.string.projects_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.projects_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete?.let { viewModel.onEvent(ProjectScreenEvent.DeleteProject(it)) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.projects),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.springPress()) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showTopMenu = true }, modifier = Modifier.springPress()) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.projects_more_cd))
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.projects_add_menu)) },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                onClick = {
                                    showTopMenu = false
                                    showCreateProjectDialog = true
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.projects.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Folder,
                    message = stringResource(id = R.string.no_projects_yet),
                    description = stringResource(id = R.string.create_first_project)
                )
            } else {
                ProjectSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (visibleProjects.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Search,
                        message = stringResource(id = R.string.projects_search_empty),
                        description = null
                    )
                } else {
                    ExpressiveSection(
                        title = stringResource(id = R.string.projects_section_title),
                        description = stringResource(id = R.string.projects_section_desc)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(visibleProjects, key = { it.project.id }) { entry ->
                                HierarchicalProjectItem(
                                    project = entry.project,
                                    isExpanded = expandedProjects[entry.project.id] ?: false,
                                    onToggleExpand = {
                                        expandedProjects[entry.project.id] = expandedProjects[entry.project.id] != true
                                    },
                                    onClick = { onProjectClick(entry.project.id) },
                                    onLongClick = {
                                        // Non-destructive: long-press opens the editor. Deleting
                                        // lives in the overflow menu where it is clearly labelled.
                                        projectToEdit = entry.project
                                    },
                                    onCreateSubProject = {
                                        showCreateSubProjectDialog = entry.project.id
                                    },
                                    onAddToHome = {
                                        ShortcutUtils.pinProjectToHome(context, entry.project.id, entry.project.name)
                                    },
                                    onCreateNote = {
                                        viewModel.onEvent(ProjectScreenEvent.CreateNewNote(entry.project.id))
                                    },
                                    onCreateChecklist = {
                                        viewModel.onEvent(ProjectScreenEvent.CreateNewChecklist(entry.project.id))
                                    },
                                    onEdit = {
                                        projectToEdit = entry.project
                                    },
                                    onDelete = {
                                        projectToDelete = entry.project.id
                                    },
                                    noteCount = state.noteCounts[entry.project.id] ?: 0,
                                    subProjectCount = state.projects.count { it.parentId == entry.project.id },
                                    depth = entry.depth
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.projects_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.cancel),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private val ProjectAccentColors = listOf(
    0xFFEF9A9A.toInt(),
    0xFFF48FB1.toInt(),
    0xFFCE93D8.toInt(),
    0xFF9FA8DA.toInt(),
    0xFF90CAF9.toInt(),
    0xFF80CBC4.toInt(),
    0xFFA5D6A7.toInt(),
    0xFFFFCC80.toInt()
)

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Int?) -> Unit,
    title: String? = null,
    initialName: String = "",
    initialDescription: String = "",
    initialColor: Int? = null
) {
    var projectName by remember(initialName) { mutableStateOf(initialName) }
    var projectDescription by remember(initialDescription) { mutableStateOf(initialDescription) }
    var selectedColor by remember(initialColor) { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title ?: stringResource(id = R.string.create_new_project),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(stringResource(id = R.string.project_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text(stringResource(id = R.string.project_description)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.projects_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProjectAccentColors.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = if (isSelected) null else color
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(projectName.trim(), projectDescription.ifBlank { null }, selectedColor) },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.springPress(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    if (title == null) stringResource(id = R.string.create) else stringResource(id = R.string.save),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.springPress()) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

data class HierarchicalProjectEntry(
    val project: com.suvojeet.notenext.data.Project,
    val depth: Int
)
