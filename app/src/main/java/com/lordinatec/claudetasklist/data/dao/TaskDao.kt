package com.lordinatec.claudetasklist.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lordinatec.claudetasklist.data.model.TaskEntity
import com.lordinatec.claudetasklist.data.model.TaskTagCrossRef
import com.lordinatec.claudetasklist.data.model.TaskWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Transaction
    @Query("SELECT * FROM tasks")
    fun getAllTasksWithTags(): Flow<List<TaskWithTags>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskWithTagsById(id: Long): Flow<TaskWithTags?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasksWithTags(query: String): Flow<List<TaskWithTags>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTagCrossRef(crossRef: TaskTagCrossRef)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun deleteTaskTagCrossRefsForTask(taskId: Long)
}
