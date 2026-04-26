package com.lordinatec.claudetasklist.ui.taskdetail

import app.cash.turbine.test
import com.lordinatec.claudetasklist.data.FakeTaskRepository
import com.lordinatec.claudetasklist.domain.model.Priority
import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import com.lordinatec.claudetasklist.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeTaskRepository

    private val tagWork = Tag(id = 1L, name = "Work")
    private val existingTask = Task(
        id = 42L, title = "Existing Task", description = "Some notes",
        priority = Priority.HIGH, tags = listOf(tagWork),
        creationDate = LocalDate.now().minusDays(3)
    )

    @Before
    fun setUp() {
        repository = FakeTaskRepository()
        repository.setTags(listOf(tagWork))
    }

    private fun newTaskViewModel() = TaskDetailViewModel(repository, savedStateHandle(-1L))
    private fun editTaskViewModel() = TaskDetailViewModel(repository, savedStateHandle(42L))

    // -- New task mode --

    @Test
    fun `new task has empty fields and medium priority`() {
        val vm = newTaskViewModel()
        val state = vm.uiState.value
        assertTrue(state.title.isEmpty())
        assertTrue(state.description.isEmpty())
        assertNull(state.dueDate)
        assertEquals(Priority.MEDIUM, state.priority)
        assertTrue(state.selectedTags.isEmpty())
        assertFalse(state.isEditMode)
        assertFalse(state.isDirty)
    }

    @Test
    fun `available tags are populated from repository`() = runTest {
        val vm = newTaskViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.availableTags.size)
            assertEquals(tagWork, state.availableTags.first())
        }
    }

    @Test
    fun `title change sets dirty and clears title error`() {
        val vm = newTaskViewModel()
        vm.onTitleChange("My Task")
        val state = vm.uiState.value
        assertEquals("My Task", state.title)
        assertTrue(state.isDirty)
        assertNull(state.titleError)
    }

    @Test
    fun `priority change updates state`() {
        val vm = newTaskViewModel()
        vm.onPriorityChange(Priority.HIGH)
        assertEquals(Priority.HIGH, vm.uiState.value.priority)
    }

    @Test
    fun `tag toggle adds and removes tag`() {
        val vm = newTaskViewModel()
        vm.onTagToggle(tagWork)
        assertTrue(tagWork in vm.uiState.value.selectedTags)
        vm.onTagToggle(tagWork)
        assertFalse(tagWork in vm.uiState.value.selectedTags)
    }

    @Test
    fun `due date change updates state`() {
        val vm = newTaskViewModel()
        val date = LocalDate.now().plusDays(7)
        vm.onDueDateChange(date)
        assertEquals(date, vm.uiState.value.dueDate)
        assertTrue(vm.uiState.value.isDirty)
    }

    @Test
    fun `save with blank title sets titleError`() = runTest {
        val vm = newTaskViewModel()
        vm.onSaveClick()
        assertNotNull(vm.uiState.value.titleError)
    }

    @Test
    fun `save with valid title calls insertTask and emits NavigateBack`() = runTest {
        val vm = newTaskViewModel()
        vm.onTitleChange("New Task")
        vm.events.test {
            vm.onSaveClick()
            assertEquals(TaskDetailUiEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `back with clean state emits NavigateBack immediately`() = runTest {
        val vm = newTaskViewModel()
        vm.events.test {
            vm.onBackClick()
            assertEquals(TaskDetailUiEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `back with dirty state shows discard dialog`() {
        val vm = newTaskViewModel()
        vm.onTitleChange("something")
        vm.onBackClick()
        assertTrue(vm.uiState.value.showDiscardDialog)
    }

    @Test
    fun `discard confirmed emits NavigateBack`() = runTest {
        val vm = newTaskViewModel()
        vm.onTitleChange("something")
        vm.onBackClick()
        vm.events.test {
            vm.onDiscardConfirmed()
            assertEquals(TaskDetailUiEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `discard cancelled hides dialog`() {
        val vm = newTaskViewModel()
        vm.onTitleChange("something")
        vm.onBackClick()
        vm.onDiscardCancelled()
        assertFalse(vm.uiState.value.showDiscardDialog)
    }

    // -- Edit task mode --

    @Test
    fun `edit mode loads existing task data`() = runTest {
        repository.setTasks(listOf(existingTask))
        val vm = editTaskViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(existingTask.title, state.title)
            assertEquals(existingTask.description, state.description)
            assertEquals(existingTask.priority, state.priority)
            assertTrue(state.isEditMode)
            assertFalse(state.isDirty)
        }
    }

    @Test
    fun `edit mode marks dirty only after change from original`() = runTest {
        repository.setTasks(listOf(existingTask))
        val vm = editTaskViewModel()
        assertFalse(vm.uiState.value.isDirty)
        vm.onTitleChange("Modified Title")
        assertTrue(vm.uiState.value.isDirty)
    }

    @Test
    fun `save in edit mode calls updateTask`() = runTest {
        repository.setTasks(listOf(existingTask))
        val vm = editTaskViewModel()
        vm.events.test {
            vm.onSaveClick()
            assertEquals(TaskDetailUiEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `insert failure sets errorMessage`() = runTest {
        repository.insertError = RuntimeException("Insert failed")
        val vm = newTaskViewModel()
        vm.onTitleChange("Task")
        vm.onSaveClick()
        assertEquals("Insert failed", vm.uiState.value.errorMessage)
    }

    @Test
    fun `onErrorDismissed clears error`() = runTest {
        repository.insertError = RuntimeException("Insert failed")
        val vm = newTaskViewModel()
        vm.onTitleChange("Task")
        vm.onSaveClick()
        vm.onErrorDismissed()
        assertNull(vm.uiState.value.errorMessage)
    }

    private fun savedStateHandle(taskId: Long): androidx.lifecycle.SavedStateHandle =
        androidx.lifecycle.SavedStateHandle(mapOf(NAV_ARG_TASK_ID to taskId))
}
