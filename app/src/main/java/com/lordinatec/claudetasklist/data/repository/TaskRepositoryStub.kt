package com.lordinatec.claudetasklist.data.repository

import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class TaskRepositoryStub : TaskRepository {
    override fun getAllTasks(): Flow<List<Task>> = flowOf(emptyList())
    override fun getTaskById(id: Long): Flow<Task?> = flowOf(null)
    override fun searchTasks(query: String): Flow<List<Task>> = flowOf(emptyList())
    override fun getAllTags(): Flow<List<Tag>> = flowOf(emptyList())

    override suspend fun insertTask(task: Task): Result<Long> = Result.success(0L)
    override suspend fun updateTask(task: Task): Result<Unit> = Result.success(Unit)
    override suspend fun deleteTask(task: Task): Result<Unit> = Result.success(Unit)
    override suspend fun toggleTaskCompletion(task: Task): Result<Unit> = Result.success(Unit)
    override suspend fun copyAsNewTask(task: Task): Result<Long> = Result.success(0L)
}
