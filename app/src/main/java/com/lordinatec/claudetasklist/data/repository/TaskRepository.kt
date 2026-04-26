package com.lordinatec.claudetasklist.data.repository

import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTaskById(id: Long): Flow<Task?>
    fun searchTasks(query: String): Flow<List<Task>>
    fun getAllTags(): Flow<List<Tag>>

    suspend fun insertTask(task: Task): Result<Long>
    suspend fun updateTask(task: Task): Result<Unit>
    suspend fun deleteTask(task: Task): Result<Unit>
    suspend fun toggleTaskCompletion(task: Task): Result<Unit>
    suspend fun copyAsNewTask(task: Task): Result<Long>
}
