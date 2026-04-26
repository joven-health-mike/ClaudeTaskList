package com.lordinatec.claudetasklist.ui.search

import app.cash.turbine.test
import com.lordinatec.claudetasklist.data.FakeTaskRepository
import com.lordinatec.claudetasklist.domain.model.Task
import com.lordinatec.claudetasklist.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeTaskRepository
    private lateinit var viewModel: SearchViewModel

    private val completedTask = Task(
        id = 1L, title = "Buy milk", isCompleted = true,
        completedDate = LocalDate.now().minusDays(1), creationDate = LocalDate.now().minusDays(2)
    )
    private val activeTask = Task(
        id = 2L, title = "Write report", isCompleted = false,
        creationDate = LocalDate.now()
    )

    @Before
    fun setUp() {
        repository = FakeTaskRepository()
        repository.setTasks(listOf(completedTask, activeTask))
        viewModel = SearchViewModel(repository)
    }

    @Test
    fun `initial state has empty query and no results`() {
        val state = viewModel.uiState.value
        assertTrue(state.query.isEmpty())
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun `query change updates state immediately`() {
        viewModel.onQueryChange("Buy")
        assertEquals("Buy", viewModel.uiState.value.query)
    }

    @Test
    fun `search results appear after debounce`() = runTest {
        viewModel.onQueryChange("Buy")
        advanceTimeBy(350L)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.results.size)
            assertEquals("Buy milk", state.results.first().title)
        }
    }

    @Test
    fun `blank query clears results`() = runTest {
        viewModel.onQueryChange("Buy")
        advanceTimeBy(350L)
        viewModel.onQueryChange("")
        advanceTimeBy(350L)
        viewModel.uiState.test {
            assertTrue(awaitItem().results.isEmpty())
        }
    }

    @Test
    fun `onTaskClick emits NavigateToDetail`() = runTest {
        viewModel.events.test {
            viewModel.onTaskClick(activeTask)
            assertEquals(SearchUiEvent.NavigateToDetail(activeTask.id), awaitItem())
        }
    }

    @Test
    fun `onCopyTask sets copySuccessMessage on success`() = runTest {
        viewModel.onCopyTask(completedTask)
        assertEquals("Task copied to active list", viewModel.uiState.value.copySuccessMessage)
    }

    @Test
    fun `onCopySuccessDismissed clears copySuccessMessage`() = runTest {
        viewModel.onCopyTask(completedTask)
        viewModel.onCopySuccessDismissed()
        assertNull(viewModel.uiState.value.copySuccessMessage)
    }

    @Test
    fun `onCopyTask creates new active task in repository`() = runTest {
        viewModel.onCopyTask(completedTask)
        val allTasks = repository.getAllTasks()
        allTasks.test {
            val tasks = awaitItem()
            val copies = tasks.filter { it.title == completedTask.title && !it.isCompleted }
            assertEquals(1, copies.size)
            assertNull(copies.first().completedDate)
        }
    }

    @Test
    fun `copy failure sets error message`() = runTest {
        repository.insertError = RuntimeException("Copy failed")
        viewModel.onCopyTask(completedTask)
        assertEquals("Copy failed", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onErrorDismissed clears error`() = runTest {
        repository.insertError = RuntimeException("Copy failed")
        viewModel.onCopyTask(completedTask)
        viewModel.onErrorDismissed()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
