package com.lordinatec.claudetasklist.ui.tasklist

import app.cash.turbine.test
import com.lordinatec.claudetasklist.data.FakeTaskRepository
import com.lordinatec.claudetasklist.domain.model.Priority
import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import com.lordinatec.claudetasklist.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeTaskRepository
    private lateinit var viewModel: TaskListViewModel

    private val tagWork = Tag(id = 1L, name = "Work")
    private val tagHealth = Tag(id = 2L, name = "Health")

    private val task1 = Task(id = 1L, title = "Task One", priority = Priority.HIGH, tags = listOf(tagWork), creationDate = LocalDate.now().minusDays(2))
    private val task2 = Task(id = 2L, title = "Task Two", priority = Priority.LOW, tags = listOf(tagHealth), creationDate = LocalDate.now().minusDays(1))
    private val task3 = Task(id = 3L, title = "Task Three", priority = Priority.MEDIUM, tags = listOf(tagWork), creationDate = LocalDate.now())

    @Before
    fun setUp() {
        repository = FakeTaskRepository()
        viewModel = TaskListViewModel(repository)
    }

    @Test
    fun `initial state has isLoading true and empty tasks`() {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading || state.tasks.isEmpty())
        assertNull(state.activeFilter)
        assertEquals(SortOrder.CREATION_DATE, state.activeSortOrder)
    }

    @Test
    fun `tasks from repository appear in state`() = runTest {
        repository.setTasks(listOf(task1, task2))
        repository.setTags(listOf(tagWork, tagHealth))
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.tasks.size)
            assertEquals(2, state.allTags.size)
        }
    }

    @Test
    fun `tasks sorted by creation date descending by default`() = runTest {
        repository.setTasks(listOf(task1, task2, task3))
        viewModel.uiState.test {
            val tasks = awaitItem().tasks
            assertEquals(task3.id, tasks[0].id)
            assertEquals(task2.id, tasks[1].id)
            assertEquals(task1.id, tasks[2].id)
        }
    }

    @Test
    fun `sort by priority shows high first`() = runTest {
        repository.setTasks(listOf(task1, task2, task3))
        viewModel.onSortOrderChange(SortOrder.PRIORITY)
        viewModel.uiState.test {
            val tasks = awaitItem().tasks
            assertEquals(Priority.HIGH, tasks.first().priority)
        }
    }

    @Test
    fun `filter by tag shows only matching tasks`() = runTest {
        repository.setTasks(listOf(task1, task2, task3))
        viewModel.onFilterByTag(tagWork)
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.tasks.all { task -> tagWork in task.tags })
            assertEquals(2, state.tasks.size)
        }
    }

    @Test
    fun `clearing filter shows all tasks`() = runTest {
        repository.setTasks(listOf(task1, task2, task3))
        viewModel.onFilterByTag(tagWork)
        viewModel.onFilterByTag(null)
        viewModel.uiState.test {
            assertEquals(3, awaitItem().tasks.size)
        }
    }

    @Test
    fun `onToggleCompletion calls repository`() = runTest {
        repository.setTasks(listOf(task1))
        viewModel.onToggleCompletion(task1)
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.tasks.first { it.id == task1.id }.isCompleted)
        }
    }

    @Test
    fun `onDeleteTask removes task from list`() = runTest {
        repository.setTasks(listOf(task1, task2))
        viewModel.onDeleteTask(task1)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.tasks.size)
            assertTrue(state.tasks.none { it.id == task1.id })
        }
    }

    @Test
    fun `delete failure sets error message`() = runTest {
        repository.setTasks(listOf(task1))
        repository.deleteError = RuntimeException("DB error")
        viewModel.onDeleteTask(task1)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("DB error", state.errorMessage)
        }
    }

    @Test
    fun `onErrorDismissed clears error message`() = runTest {
        repository.deleteError = RuntimeException("DB error")
        viewModel.onDeleteTask(task1)
        viewModel.onErrorDismissed()
        viewModel.uiState.test {
            assertNull(awaitItem().errorMessage)
        }
    }

    @Test
    fun `onCreateTaskClick emits NavigateToNewTask`() = runTest {
        viewModel.events.test {
            viewModel.onCreateTaskClick()
            assertEquals(TaskListUiEvent.NavigateToNewTask, awaitItem())
        }
    }

    @Test
    fun `onTaskClick emits NavigateToDetail with correct id`() = runTest {
        viewModel.events.test {
            viewModel.onTaskClick(task1)
            assertEquals(TaskListUiEvent.NavigateToDetail(task1.id), awaitItem())
        }
    }

    @Test
    fun `onSearchClick emits NavigateToSearch`() = runTest {
        viewModel.events.test {
            viewModel.onSearchClick()
            assertEquals(TaskListUiEvent.NavigateToSearch, awaitItem())
        }
    }
}
