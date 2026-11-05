package com.suvojeet.notenext.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.suvojeet.notenext.ui.project.toNotesUiEvent
import com.suvojeet.notenext.ui.notes.NotesUiEvent
import com.suvojeet.notenext.ui.project.ProjectScreen
import com.suvojeet.notenext.ui.project.ProjectNotesScreen
import com.suvojeet.notenext.ui.project.ProjectNotesViewModel
import com.suvojeet.notenext.ui.project.toNotesEditState
import com.suvojeet.notenext.ui.project.toProjectNotesEvent
import com.suvojeet.notenext.ui.add_edit_note.AddEditNoteScreen
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.repository.SettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted

fun NavGraphBuilder.projectGraph(
    navController: NavHostController,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    isDecoySession: Boolean = false
) {
    val slideEnter = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
    val slideExit = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))

    composable<Destination.Projects>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ProjectScreen(
            onMenuClick = onMenuClick,
            onProjectClick = { projectId -> navController.navigate(Destination.ProjectNotes(projectId)) },
            navController = navController,
            settingsRepository = settingsRepository
        )
    }

    composable<Destination.ProjectNotes>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ProjectNotesScreen(
            navController = navController,
            onBackClick = { navController.popBackStack() },
            themeMode = themeMode,
            settingsRepository = settingsRepository,
            isDecoySession = isDecoySession
        )
    }

    composable<Destination.AddEditNote>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        val viewModel: ProjectNotesViewModel = hiltViewModel()
        val scope = rememberCoroutineScope()
        
        androidx.compose.runtime.LaunchedEffect(isDecoySession) {
            viewModel.setDecoyMode(isDecoySession)
        }

        AddEditNoteScreen(
            state = viewModel.state.collectAsState().value.toNotesEditState(),
            onEvent = { viewModel.onEvent(it.toProjectNotesEvent()) },
            onDismiss = { navController.popBackStack() },
            themeMode = themeMode,
            settingsRepository = settingsRepository,
            events = viewModel.events.map { it.toNotesUiEvent() }.shareIn(scope, SharingStarted.WhileSubscribed())
        )
    }
}
