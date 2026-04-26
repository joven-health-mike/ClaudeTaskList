package com.lordinatec.claudetasklist.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lordinatec.claudetasklist.domain.model.Tag

@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier
    )
}
