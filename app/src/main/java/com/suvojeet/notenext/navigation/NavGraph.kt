@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.navigation

import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesViewModel
import com.suvojeet.notenext.ui.MainViewModel
import com.suvojeet.notenext.ui.theme.ThemeMode
import com.suvojeet.notenext.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import com.suvojeet.notenext.R
import com.suvojeet.notenext.util.BiometricAuthManager
import com.suvojeet.notenext.util.findActivity
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.suvojeet.notenext.ui.notes.NotesListState

@Composable
fun NavGraph(
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository,
    mainViewModel: MainViewModel,
    startNoteId: Int = -1,
    startProjectId: Int = -1,
    startAddNote: Boolean = false,
    sharedText: String? = null,
    initialTitle: String? = null,
    searchQuery: String? = null,
    externalUri: android.net.Uri? = null,
    sharedNoteId: String? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val notesViewModel: NotesViewModel = hiltViewModel()
    val notesState by notesViewModel.listState.collectAsState()
    val editState by notesViewModel.editState.collectAsState()

    val isDecoySession by mainViewModel.isDecoySession.collectAsState()

    LaunchedEffect(isDecoySession) {
        notesViewModel.setDecoyMode(isDecoySession)
    }

    val activity = context.findActivity() as? FragmentActivity
    val biometricAuthManager = if (activity != null) {
        remember(activity) {
            BiometricAuthManager(context, activity)
        }
    } else {
        null
    }

    LaunchedEffect(startNoteId) {
        if (startNoteId != -1) {
            val isLocked = notesViewModel.getNoteLockStatus(startNoteId)
            if (isLocked) {
                biometricAuthManager?.showBiometricPrompt(
                    onAuthSuccess = {
                        notesViewModel.onEvent(NotesEvent.ExpandNote(startNoteId))
                        navController.navigate(Destination.Notes()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onAuthError = {
                        Toast.makeText(context, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                ) ?: Toast.makeText(context, "Biometrics not available", Toast.LENGTH_SHORT).show()
            } else {
                notesViewModel.onEvent(NotesEvent.ExpandNote(startNoteId))
                navController.navigate(Destination.Notes()) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(startProjectId) {
        if (startProjectId != -1) {
            navController.navigate(Destination.ProjectNotes(startProjectId)) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(startAddNote) {
        if (startAddNote) {
            notesViewModel.onEvent(NotesEvent.ExpandNote(-1))
        }
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            notesViewModel.onEvent(NotesEvent.CreateNoteFromSharedText(sharedText))
        }
    }

    LaunchedEffect(initialTitle) {
        if (initialTitle != null) {
            notesViewModel.onEvent(NotesEvent.SetInitialTitle(initialTitle))
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery != null) {
            notesViewModel.onEvent(NotesEvent.OnSearchQueryChange(searchQuery))
        }
    }

    LaunchedEffect(externalUri) {
        if (externalUri != null) {
            notesViewModel.onEvent(NotesEvent.LoadExternalFile(externalUri))
        }
    }

    LaunchedEffect(sharedNoteId) {
        if (!sharedNoteId.isNullOrBlank()) {
            // The decryption key rides along in the deep link's fragment / intent extra.
            navController.navigate(Destination.SharedNote(sharedNoteId, mainViewModel.sharedNoteKey.value)) {
                launchSingleTop = true
            }
        }
    }

    val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    if (isExpandedScreen) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    // A flat percentage is too narrow on a 10" tablet and too wide on a
                    // desktop-class display, so clamp it to a readable band.
                    modifier = Modifier.widthIn(min = 248.dp, max = 320.dp),
                    drawerContainerColor = Color.Black,
                    drawerContentColor = Color.White,
                    drawerShape = RectangleShape
                ) {
                    DrawerContent(
                        navController = navController,
                        notesState = notesState,
                        notesViewModel = notesViewModel,
                        onCloseDrawer = { /* no-op for permanent drawer */ }
                    )
                }
            }
        ) {
            AppNavHost(
                navController = navController,
                notesViewModel = notesViewModel,
                themeMode = themeMode,
                windowSizeClass = windowSizeClass,
                settingsRepository = settingsRepository,
                onMenuClick = { scope.launch { drawerState.open() } },
                isCompact = false
            )
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = editState.expandedNoteId == null,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    drawerContainerColor = Color.Black,
                    drawerContentColor = Color.White,
                    drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                ) {
                    DrawerContent(
                        navController = navController,
                        notesState = notesState,
                        notesViewModel = notesViewModel,
                        onCloseDrawer = { action ->
                            scope.launch {
                                drawerState.close()
                                action()
                            }
                        }
                    )                }
            }
        ) {
            AppNavHost(
                navController = navController,
                notesViewModel = notesViewModel,
                themeMode = themeMode,
                windowSizeClass = windowSizeClass,
                settingsRepository = settingsRepository,
                onMenuClick = { scope.launch { drawerState.open() } },
                isCompact = true
            )
        }
    }
}

// ─── Shared drawer content ───────────────────────────────────────────

@Composable
private fun DrawerContent(
    navController: NavHostController,
    notesState: NotesListState,
    notesViewModel: NotesViewModel,
    onCloseDrawer: (onClosed: () -> Unit) -> Unit
) {
    val drawerItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = Color.White.copy(alpha = 0.12f),
        unselectedContainerColor = Color.Transparent,
        selectedIconColor = Color.White,
        unselectedIconColor = Color.White.copy(alpha = 0.68f),
        selectedTextColor = Color.White,
        unselectedTextColor = Color.White.copy(alpha = 0.9f)
    )

    Text(
        text = stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 14.dp)
    )

    DrawerSectionLabel(text = stringResource(id = R.string.menu))

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    data class DrawerItem(val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector, val isSelected: Boolean, val onClick: () -> Unit)

    val items = listOf(
        DrawerItem(
            R.string.notes, 
            Icons.AutoMirrored.Filled.Label, 
            currentDestination?.hasRoute<Destination.Notes>() == true && notesState.filteredLabel == null
        ) {
            onCloseDrawer {
                if (currentDestination?.hasRoute<Destination.Notes>() != true || notesState.filteredLabel != null) {
                    notesViewModel.onEvent(NotesEvent.FilterByLabel(null))
                    navController.navigate(Destination.Notes()) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
        DrawerItem(
            R.string.projects, 
            Icons.Default.CreateNewFolder, 
            currentDestination?.hasRoute<Destination.Projects>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Projects) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.archive, 
            Icons.Default.Archive, 
            currentDestination?.hasRoute<Destination.Archive>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Archive) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.reminders, 
            Icons.Default.Notifications, 
            currentDestination?.hasRoute<Destination.Reminder>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Reminder) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.todos, 
            Icons.Default.PlaylistAddCheck, 
            currentDestination?.hasRoute<Destination.Todo>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Todo) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.bin, 
            Icons.Default.Delete, 
            currentDestination?.hasRoute<Destination.Bin>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Bin) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        DrawerItem(
            R.string.settings, 
            Icons.Default.Settings, 
            currentDestination?.hasRoute<Destination.Settings>() == true
        ) {
            onCloseDrawer {
                navController.navigate(Destination.Settings) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    )

    items.forEach { item ->
        NavigationDrawerItem(
            icon = { Icon(item.icon, contentDescription = stringResource(id = item.label)) },
            label = { Text(stringResource(id = item.label), fontWeight = FontWeight.Medium) },
            selected = item.isSelected,
            onClick = item.onClick,
            colors = drawerItemColors,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp),
        color = Color.White.copy(alpha = 0.12f)
    )

    if (notesState.labels.isEmpty()) {
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(id = R.string.create_new_label)) },
            label = { Text(stringResource(id = R.string.create_new_label), fontWeight = FontWeight.Medium) },
            selected = false,
            onClick = {
                onCloseDrawer {
                    navController.navigate(Destination.EditLabels) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = drawerItemColors,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.labels_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.55f)
            )
            IconButton(onClick = {
                onCloseDrawer {
                    navController.navigate(Destination.EditLabels) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit_labels),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        notesState.labels.forEach { label ->
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = label) },
                label = { Text(label, fontWeight = FontWeight.Normal) },
                selected = notesState.filteredLabel == label,
                onClick = {
                    onCloseDrawer {
                        notesViewModel.onEvent(NotesEvent.FilterByLabel(label))
                        if (currentDestination?.hasRoute<Destination.Notes>() != true) {
                            navController.navigate(Destination.Notes()) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                colors = drawerItemColors,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 4.dp, bottom = 6.dp)
    )
}

// ─── Shared NavHost ──────────────────────────────────────────────────

@Composable
private fun AppNavHost(
    navController: NavHostController,
    notesViewModel: NotesViewModel,
    themeMode: ThemeMode,
    windowSizeClass: WindowSizeClass,
    settingsRepository: SettingsRepository,
    onMenuClick: () -> Unit,
    isCompact: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Notes(),
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        notesGraph(navController, notesViewModel, themeMode, settingsRepository, onMenuClick, isCompact, windowSizeClass)
        projectGraph(navController, themeMode, settingsRepository, onMenuClick)
        settingsGraph(navController)
    }
}
