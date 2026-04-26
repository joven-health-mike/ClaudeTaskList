package com.lordinatec.claudetasklist.domain.model

import java.time.LocalDate

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val dueDate: LocalDate? = null,
    val priority: Priority = Priority.MEDIUM,
    val creationDate: LocalDate = LocalDate.now(),
    val completedDate: LocalDate? = null,
    val isCompleted: Boolean = false,
    val tags: List<Tag> = emptyList()
)
