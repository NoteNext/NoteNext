package com.suvojeet.notenext.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.suvojeet.notenext.ui.notes.NotesScreen
import com.suvojeet.notenext.ui.notes.NotesViewModel
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.archive.ArchiveScreen
import com.suvojeet.notenext.ui.labels.EditLabelsScreen
import com.suvojeet.notenext.ui.bin.BinScreen
import com.suvojeet.notenext.ui.bin.BinViewModel
import com.suvojeet.notenext.ui.reminder.ReminderScreen
import com.suvojeet.notenext.ui.reminder.AddEditReminderScreen
import com.suvojeet.notenext.todo.TodoScreen
import com.suvojeet.notenext.ui.drawing.DrawingScreen
import com.suvojeet.notenext.ui.shared.SharedNoteScreen
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.repository.SettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted

fun NavGraphBuilder.notesGraph(
    navController: NavHostController,
    notesViewModel: NotesViewModel,
    themeMode: ThemeMode,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    isCompact: Boolean,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    val slideEnter = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
    val slideExit = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))

    composable<Destination.Notes>(
        enterTransition = { fadeIn(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)) },
        exitTransition = { fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)) }
    ) { backStackEntry ->
        if (isCompact) {
            val route: Destination.Notes = backStackEntry.toRoute()
            val noteId = route.noteId
            LaunchedEffect(noteId) {
                if (noteId != -1) {
                    notesViewModel.onEvent(NotesEvent.ExpandNote(noteId))
                }
            }
        }
        NotesScreen(
            viewModel = notesViewModel,
            onSettingsClick = { navController.navigate(Destination.Settings) },
            onArchiveClick = { navController.navigate(Destination.Archive) },
            onEditLabelsClick = { navController.navigate(Destination.EditLabels) },
            onBinClick = { navController.navigate(Destination.Bin) },
            themeMode = themeMode,
            settingsRepository = settingsRepository,
            onMenuClick = onMenuClick,
            onDrawingClick = { navController.navigate(Destination.Drawing) },
            onTodoClick = { navController.navigate(Destination.Todo) },
            onOpenSharedNote = { shareId, key -> navController.navigate(Destination.SharedNote(shareId, key)) },
            events = notesViewModel.events,
            windowSizeClass = windowSizeClass
        )
    }

    composable<Destination.Archive>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ArchiveScreen(onMenuClick = onMenuClick)
    }

    composable<Destination.EditLabels>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        EditLabelsScreen(onBackPressed = { navController.popBackStack() })
    }

    composable<Destination.Bin>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        val binViewModel: BinViewModel = hiltViewModel()
        BinScreen(viewModel = binViewModel, onMenuClick = onMenuClick)
    }

    composable<Destination.Reminder>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        ReminderScreen(
            onBackClick = { navController.popBackStack() },
            onNoteClick = { note ->
                navController.navigate(Destination.Notes(noteId = note.id)) {
                    popUpTo<Destination.Notes> { inclusive = true }
                }
            }
        )
    }

    composable<Destination.AddEditReminder>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        AddEditReminderScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.Todo>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        TodoScreen(onBackClick = { navController.popBackStack() })
    }

    composable<Destination.SharedNote>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) { backStackEntry ->
        val route: Destination.SharedNote = backStackEntry.toRoute()
        SharedNoteScreen(
            shareId = route.shareId,
            key = route.key,
            onBack = {
                if (!navController.popBackStack()) {
                    navController.navigate(Destination.Notes()) {
                        popUpTo<Destination.SharedNote> { inclusive = true }
                    }
                }
            }
        )
    }

    composable<Destination.Drawing>(
        enterTransition = { slideEnter },
        exitTransition = { slideExit }
    ) {
        DrawingScreen(
            windowSizeClass = windowSizeClass,
            onSave = { uri ->
                notesViewModel.onEvent(NotesEvent.ExpandNote(-1))
                notesViewModel.onEvent(NotesEvent.ImportImage(uri))
                navController.popBackStack()
            },
            onDismiss = { navController.popBackStack() }
        )
    }
}
