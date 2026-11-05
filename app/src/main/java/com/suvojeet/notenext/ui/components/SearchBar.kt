@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.notes.LayoutType
import com.suvojeet.notenext.core.util.SortType
import androidx.compose.ui.focus.onFocusChanged

import com.suvojeet.notenext.ui.theme.fullShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onLayoutToggleClick: () -> Unit,
    onSortClick: () -> Unit,
    layoutType: LayoutType,
    sortMenuExpanded: Boolean,
    onSortMenuDismissRequest: () -> Unit,
    onSortOptionClick: (SortType) -> Unit,
    currentSortType: SortType,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = fullShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn(animationSpec = spring()).togetherWith(fadeOut(animationSpec = spring()))
                },
                label = "SearchIconAnimation"
            ) { active ->
                if (active) {
                    IconButton(onClick = { 
                        onSearchActiveChange(false)
                        onSearchQueryChange("")
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                } else {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(id = R.string.search_notes),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            TextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                    if (!isSearchActive) onSearchActiveChange(true)
                },
                placeholder = { 
                    Text(
                        stringResource(id = R.string.search_notes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { if (it.isFocused) onSearchActiveChange(true) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true
            )

            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }

            if (!isSearchActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLayoutToggleClick) {
                        Icon(
                            imageVector = if (layoutType == LayoutType.GRID) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = "Toggle Layout"
                        )
                    }
                    Box {
                        IconButton(onClick = onSortClick) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort"
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = onSortMenuDismissRequest,
                            shape = MaterialTheme.shapes.medium,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            SortOption(
                                label = stringResource(id = com.suvojeet.notenext.core.R.string.date_modified),
                                isSelected = currentSortType == SortType.DATE_MODIFIED,
                                onClick = { onSortOptionClick(SortType.DATE_MODIFIED) }
                            )
                            SortOption(
                                label = stringResource(id = com.suvojeet.notenext.core.R.string.date_created),
                                isSelected = currentSortType == SortType.DATE_CREATED,
                                onClick = { onSortOptionClick(SortType.DATE_CREATED) }
                            )
                            SortOption(
                                label = stringResource(id = com.suvojeet.notenext.core.R.string.sort_by_alpha),
                                isSelected = currentSortType == SortType.TITLE,
                                onClick = { onSortOptionClick(SortType.TITLE) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { 
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ) 
        },
        onClick = onClick,
        trailingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null
    )
}
