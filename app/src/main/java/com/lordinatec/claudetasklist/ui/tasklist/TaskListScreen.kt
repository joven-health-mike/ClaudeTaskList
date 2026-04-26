package com.lordinatec.claudetasklist.ui.tasklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import com.lordinatec.claudetasklist.ui.components.TaskItem
import com.lordinatec.claudetasklist.ui.theme.ClaudeTaskListTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    uiState: TaskListUiState,
    onCreateTaskClick: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onToggleCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onSearchClick: () -> Unit,
    onFilterByTag: (Tag?) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismissed()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("ClaudeTaskList") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Sort") // placeholder icon
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Creation Date") },
                            onClick = { onSortOrderChange(SortOrder.CREATION_DATE); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Due Date") },
                            onClick = { onSortOrderChange(SortOrder.DUE_DATE); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Priority") },
                            onClick = { onSortOrderChange(SortOrder.PRIORITY); showSortMenu = false }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTaskClick) {
                Icon(Icons.Default.Add, contentDescription = "New task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            item {
                FilterRow(
                    allTags = uiState.allTags,
                    activeFilter = uiState.activeFilter,
                    onFilterByTag = onFilterByTag
                )
            }
            items(uiState.tasks, key = { it.id }) { task ->
                SwipeToDismissTaskItem(
                    task = task,
                    onTaskClick = onTaskClick,
                    onToggleCompletion = onToggleCompletion,
                    onDeleteTask = onDeleteTask
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissTaskItem(
    task: Task,
    onTaskClick: (Task) -> Unit,
    onToggleCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDeleteTask(task)
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    ) {
        TaskItem(
            task = task,
            onTaskClick = onTaskClick,
            onToggleCompletion = onToggleCompletion,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FilterRow(
    allTags: List<Tag>,
    activeFilter: Tag?,
    onFilterByTag: (Tag?) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        FilterChip(
            selected = activeFilter == null,
            onClick = { onFilterByTag(null) },
            label = { Text("All") },
            modifier = Modifier.padding(end = 8.dp)
        )
        allTags.forEach { tag ->
            FilterChip(
                selected = activeFilter == tag,
                onClick = { onFilterByTag(tag) },
                label = { Text(tag.name) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListScreenPreview() {
    ClaudeTaskListTheme {
        TaskListScreen(
            uiState = TaskListUiState(),
            onCreateTaskClick = {},
            onTaskClick = {},
            onToggleCompletion = {},
            onDeleteTask = {},
            onSearchClick = {},
            onFilterByTag = {},
            onSortOrderChange = {},
            onErrorDismissed = {}
        )
    }
}
