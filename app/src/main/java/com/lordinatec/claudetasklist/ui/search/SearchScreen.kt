package com.lordinatec.claudetasklist.ui.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lordinatec.claudetasklist.domain.model.Task
import com.lordinatec.claudetasklist.ui.components.TaskItem
import com.lordinatec.claudetasklist.ui.theme.ClaudeTaskListTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onTaskClick: (Task) -> Unit,
    onToggleCompletion: (Task) -> Unit,
    onCopyTask: (Task) -> Unit,
    onNavigateBack: () -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search tasks...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            items(uiState.results, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onTaskClick = onTaskClick,
                    onToggleCompletion = onToggleCompletion,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    trailingContent = if (task.isCompleted) {
                        {
                            IconButton(onClick = { onCopyTask(task) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy as new task")
                            }
                        }
                    } else null
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    ClaudeTaskListTheme {
        SearchScreen(
            uiState = SearchUiState(),
            onQueryChange = {},
            onTaskClick = {},
            onToggleCompletion = {},
            onCopyTask = {},
            onNavigateBack = {},
            onErrorDismissed = {}
        )
    }
}
