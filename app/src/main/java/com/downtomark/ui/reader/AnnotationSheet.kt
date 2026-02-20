package com.downtomark.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.downtomark.data.model.Highlight
import com.downtomark.data.model.HighlightColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnnotationSheet(
    selection: TextSelection? = null,
    existingHighlight: Highlight? = null,
    allTags: List<String> = emptyList(),
    onSave: (HighlightColor, String?, List<String>) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedColor by remember {
        mutableStateOf(existingHighlight?.color ?: HighlightColor.YELLOW)
    }
    var comment by remember {
        mutableStateOf(existingHighlight?.comment ?: "")
    }
    var tags by remember {
        mutableStateOf(existingHighlight?.tags ?: emptyList())
    }
    var tagInput by remember { mutableStateOf("") }

    val previewText = existingHighlight?.highlightedText ?: selection?.selectedText ?: ""

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (existingHighlight != null) "Edit Highlight" else "New Highlight",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Selected text preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Color picker
            Text(
                text = "Color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HighlightColor.entries.forEach { color ->
                    ColorCircle(
                        color = highlightColorToCompose(color),
                        selected = selectedColor == color,
                        onClick = { selectedColor = color }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Comment field
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Comment (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Tags
            Text(
                text = "Tags",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { tags = tags.filter { it != tag } },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove tag",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Add tag") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val trimmed = tagInput.trim().lowercase()
                            if (trimmed.isNotEmpty() && trimmed !in tags) {
                                tags = tags + trimmed
                                tagInput = ""
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val trimmed = tagInput.trim().lowercase()
                        if (trimmed.isNotEmpty() && trimmed !in tags) {
                            tags = tags + trimmed
                            tagInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add tag")
                }
            }

            // Suggestions from existing tags
            val suggestions = allTags.filter {
                it !in tags && (tagInput.isBlank() || it.contains(tagInput, ignoreCase = true))
            }.take(5)
            if (suggestions.isNotEmpty() && tagInput.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        TextButton(onClick = {
                            tags = tags + suggestion
                            tagInput = ""
                        }) {
                            Text(suggestion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        onSave(selectedColor, comment.ifBlank { null }, tags)
                    }
                ) {
                    Text(if (existingHighlight != null) "Update" else "Save")
                }
            }
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            )
            .clickable(onClick = onClick)
    )
}

fun highlightColorToCompose(color: HighlightColor): Color = when (color) {
    HighlightColor.YELLOW -> Color(0xFFDBBC7F)
    HighlightColor.GREEN -> Color(0xFFA7C080)
    HighlightColor.BLUE -> Color(0xFF7FBBB3)
    HighlightColor.PINK -> Color(0xFFD699B6)
    HighlightColor.ORANGE -> Color(0xFFE69875)
}
