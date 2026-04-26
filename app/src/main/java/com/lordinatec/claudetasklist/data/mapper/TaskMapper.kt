package com.lordinatec.claudetasklist.data.mapper

import com.lordinatec.claudetasklist.data.model.TagEntity
import com.lordinatec.claudetasklist.data.model.TaskEntity
import com.lordinatec.claudetasklist.data.model.TaskWithTags
import com.lordinatec.claudetasklist.domain.model.Priority
import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import java.time.LocalDate

fun TaskWithTags.toDomain(): Task = Task(
    id = task.id,
    title = task.title,
    description = task.description,
    dueDate = task.dueDate?.let { LocalDate.ofEpochDay(it) },
    priority = Priority.valueOf(task.priority),
    creationDate = LocalDate.ofEpochDay(task.creationDate),
    completedDate = task.completedDate?.let { LocalDate.ofEpochDay(it) },
    isCompleted = task.isCompleted,
    tags = tags.map { it.toDomain() }
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate?.toEpochDay(),
    priority = priority.name,
    creationDate = creationDate.toEpochDay(),
    completedDate = completedDate?.toEpochDay(),
    isCompleted = isCompleted
)

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name)
