package com.lordinatec.claudetasklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lordinatec.claudetasklist.ui.search.SearchScreen
import com.lordinatec.claudetasklist.ui.search.SearchUiEvent
import com.lordinatec.claudetasklist.ui.search.SearchViewModel
import com.lordinatec.claudetasklist.ui.taskdetail.NAV_ARG_TASK_ID
import com.lordinatec.claudetasklist.ui.taskdetail.TaskDetailScreen
import com.lordinatec.claudetasklist.ui.taskdetail.TaskDetailUiEvent
import com.lordinatec.claudetasklist.ui.taskdetail.TaskDetailViewModel
import com.lordinatec.claudetasklist.ui.tasklist.TaskListScreen
import com.lordinatec.claudetasklist.ui.tasklist.TaskListUiEvent
import com.lordinatec.claudetasklist.ui.tasklist.TaskListViewModel
import com.lordinatec.claudetasklist.ui.theme.ClaudeTaskListTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.LaunchedEffect

private const val ROUTE_TASK_LIST = "task_list"
private const val ROUTE_TASK_DETAIL = "task_detail"
private const val ROUTE_SEARCH = "search"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClaudeTaskListTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = ROUTE_TASK_LIST) {

                    composable(ROUTE_TASK_LIST) {
                        val viewModel: TaskListViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            viewModel.events.collect { event ->
                                when (event) {
                                    TaskListUiEvent.NavigateToNewTask ->
                                        navController.navigate(ROUTE_TASK_DETAIL)
                                    is TaskListUiEvent.NavigateToDetail ->
                                        navController.navigate("$ROUTE_TASK_DETAIL?$NAV_ARG_TASK_ID=${event.taskId}")
                                    TaskListUiEvent.NavigateToSearch ->
                                        navController.navigate(ROUTE_SEARCH)
                                }
                            }
                        }
                        TaskListScreen(
                            uiState = uiState,
                            onCreateTaskClick = viewModel::onCreateTaskClick,
                            onTaskClick = viewModel::onTaskClick,
                            onToggleCompletion = viewModel::onToggleCompletion,
                            onDeleteTask = viewModel::onDeleteTask,
                            onSearchClick = viewModel::onSearchClick,
                            onFilterByTag = viewModel::onFilterByTag,
                            onSortOrderChange = viewModel::onSortOrderChange,
                            onErrorDismissed = viewModel::onErrorDismissed
                        )
                    }

                    composable(
                        route = "$ROUTE_TASK_DETAIL?$NAV_ARG_TASK_ID={$NAV_ARG_TASK_ID}",
                        arguments = listOf(
                            navArgument(NAV_ARG_TASK_ID) {
                                type = NavType.LongType
                                defaultValue = -1L
                            }
                        )
                    ) {
                        val viewModel: TaskDetailViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            viewModel.events.collect { event ->
                                when (event) {
                                    TaskDetailUiEvent.NavigateBack -> navController.popBackStack()
                                }
                            }
                        }
                        TaskDetailScreen(
                            uiState = uiState,
                            onTitleChange = viewModel::onTitleChange,
                            onDescriptionChange = viewModel::onDescriptionChange,
                            onDueDateChange = viewModel::onDueDateChange,
                            onPriorityChange = viewModel::onPriorityChange,
                            onTagToggle = viewModel::onTagToggle,
                            onSaveClick = viewModel::onSaveClick,
                            onBackClick = viewModel::onBackClick,
                            onDiscardConfirmed = viewModel::onDiscardConfirmed,
                            onDiscardCancelled = viewModel::onDiscardCancelled,
                            onErrorDismissed = viewModel::onErrorDismissed
                        )
                    }

                    composable(ROUTE_SEARCH) {
                        val viewModel: SearchViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            viewModel.events.collect { event ->
                                when (event) {
                                    is SearchUiEvent.NavigateToDetail ->
                                        navController.navigate("$ROUTE_TASK_DETAIL?$NAV_ARG_TASK_ID=${event.taskId}")
                                }
                            }
                        }
                        SearchScreen(
                            uiState = uiState,
                            onQueryChange = viewModel::onQueryChange,
                            onTaskClick = viewModel::onTaskClick,
                            onToggleCompletion = viewModel::onToggleCompletion,
                            onCopyTask = viewModel::onCopyTask,
                            onCopySuccessDismissed = viewModel::onCopySuccessDismissed,
                            onNavigateBack = { navController.popBackStack() },
                            onErrorDismissed = viewModel::onErrorDismissed
                        )
                    }
                }
            }
        }
    }
}
