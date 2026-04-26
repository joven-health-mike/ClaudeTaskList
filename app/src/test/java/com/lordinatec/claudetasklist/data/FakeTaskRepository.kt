package com.lordinatec.claudetasklist.data

import com.lordinatec.claudetasklist.data.repository.TaskRepository
import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeTaskRepository : TaskRepository {

    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private val tags = MutableStateFlow<List<Tag>>(emptyList())

    var insertError: Throwable? = null
    var updateError: Throwable? = null
    var deleteError: Throwable? = null

    override fun getAllTasks(): Flow<List<Task>> = tasks
    override fun getTaskById(id: Long): Flow<Task?> = tasks.map { list -> list.find { it.id == id } }
    override fun searchTasks(query: String): Flow<List<Task>> = tasks.map { list ->
        list.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.description?.contains(query, ignoreCase = true) == true
        }
    }
    override fun getAllTags(): Flow<List<Tag>> = tags

    override suspend fun insertTask(task: Task): Result<Long> {
        insertError?.let { return Result.failure(it) }
        val id = (tasks.value.maxOfOrNull { it.id } ?: 0L) + 1L
        tasks.value = tasks.value + task.copy(id = id, creationDate = LocalDate.now())
        return Result.success(id)
    }

    override suspend fun updateTask(task: Task): Result<Unit> {
        updateError?.let { return Result.failure(it) }
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
        return Result.success(Unit)
    }

    override suspend fun deleteTask(task: Task): Result<Unit> {
        deleteError?.let { return Result.failure(it) }
        tasks.value = tasks.value.filter { it.id != task.id }
        return Result.success(Unit)
    }

    override suspend fun toggleTaskCompletion(task: Task): Result<Unit> {
        val updated = if (task.isCompleted) task.copy(isCompleted = false, completedDate = null)
                      else task.copy(isCompleted = true, completedDate = LocalDate.now())
        return updateTask(updated)
    }

    override suspend fun copyAsNewTask(task: Task): Result<Long> {
        return insertTask(task.copy(id = 0L, isCompleted = false, completedDate = null))
    }

    fun setTasks(taskList: List<Task>) { tasks.value = taskList }
    fun setTags(tagList: List<Tag>) { tags.value = tagList }
}
