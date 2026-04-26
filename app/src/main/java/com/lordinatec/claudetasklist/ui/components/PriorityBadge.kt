package com.lordinatec.claudetasklist.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.lordinatec.claudetasklist.domain.model.Priority

@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val (label, color) = when (priority) {
        Priority.HIGH -> "High" to MaterialTheme.colorScheme.error
        Priority.MEDIUM -> "Medium" to MaterialTheme.colorScheme.tertiary
        Priority.LOW -> "Low" to MaterialTheme.colorScheme.secondary
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color.copy(alpha = 0.12f), labelColor = color),
        modifier = modifier
    )
}
