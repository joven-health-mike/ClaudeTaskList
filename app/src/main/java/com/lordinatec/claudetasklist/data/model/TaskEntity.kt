package com.lordinatec.claudetasklist.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val priority: String,
    val creationDate: Long,
    val completedDate: Long?,
    val isCompleted: Boolean
)
