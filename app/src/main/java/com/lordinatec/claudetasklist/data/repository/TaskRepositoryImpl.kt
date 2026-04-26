package com.lordinatec.claudetasklist.data.repository

import com.lordinatec.claudetasklist.data.dao.TagDao
import com.lordinatec.claudetasklist.data.dao.TaskDao
import com.lordinatec.claudetasklist.data.mapper.toDomain
import com.lordinatec.claudetasklist.data.mapper.toEntity
import com.lordinatec.claudetasklist.data.model.TaskTagCrossRef
import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val tagDao: TagDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> =
        taskDao.getAllTasksWithTags().map { list -> list.map { it.toDomain() } }

    override fun getTaskById(id: Long): Flow<Task?> =
        taskDao.getTaskWithTagsById(id).map { it?.toDomain() }

    override fun searchTasks(query: String): Flow<List<Task>> =
        taskDao.searchTasksWithTags(query).map { list -> list.map { it.toDomain() } }

    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insertTask(task: Task): Result<Long> = runCatching {
        withContext(Dispatchers.IO) {
            val entity = task.copy(creationDate = LocalDate.now()).toEntity()
            val id = taskDao.insert(entity)
            task.tags.forEach { tag ->
                taskDao.insertTaskTagCrossRef(TaskTagCrossRef(taskId = id, tagId = tag.id))
            }
            id
        }
    }

    override suspend fun updateTask(task: Task): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            taskDao.update(task.toEntity())
            taskDao.deleteTaskTagCrossRefsForTask(task.id)
            task.tags.forEach { tag ->
                taskDao.insertTaskTagCrossRef(TaskTagCrossRef(taskId = task.id, tagId = tag.id))
            }
        }
    }

    override suspend fun deleteTask(task: Task): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { taskDao.delete(task.toEntity()) }
    }

    override suspend fun toggleTaskCompletion(task: Task): Result<Unit> {
        val updated = if (task.isCompleted) task.copy(isCompleted = false, completedDate = null)
                      else task.copy(isCompleted = true, completedDate = LocalDate.now())
        return updateTask(updated)
    }

    override suspend fun copyAsNewTask(task: Task): Result<Long> =
        insertTask(task.copy(id = 0L, isCompleted = false, completedDate = null))
}
