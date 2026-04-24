package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val InitialVisibleSelectionCount = 5
private const val SelectionPageSize = 5

@Composable
private fun selectionSecondaryLabelColor(
    secondaryLabel: String,
    isSelected: Boolean,
): androidx.compose.ui.graphics.Color {
    val colorScheme = MaterialTheme.colorScheme
    if (isSelected) return colorScheme.onPrimaryContainer
    return when {
        secondaryLabel.contains("réparation", ignoreCase = true) ||
            secondaryLabel.contains("reparation", ignoreCase = true) -> colorScheme.error
        secondaryLabel.contains("utilis", ignoreCase = true) ||
            secondaryLabel.contains("cours", ignoreCase = true) -> colorScheme.tertiary
        secondaryLabel.contains("disponible", ignoreCase = true) -> colorScheme.primary
        else -> colorScheme.onSurfaceVariant
    }
}

@Composable
private fun SelectionStatusLabel(
    text: String,
    isSelected: Boolean,
) {
    val statusColor = selectionSecondaryLabelColor(
        secondaryLabel = text,
        isSelected = isSelected,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Spacer(
            modifier = Modifier
                .size(8.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(statusColor),
        )
        Text(
            text = text.removePrefix("● ").trim(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
        )
    }
}

data class SearchableSelectableOption(
    val key: String,
    val label: String,
    val secondaryLabel: String? = null,
    val usageCount: Int = 0,
)

private fun visibleSelectionOptions(
    options: List<SearchableSelectableOption>,
    visibleCount: Int,
): List<SearchableSelectableOption> {
    return options.take(visibleCount.coerceAtMost(options.size))
}

private fun reorderMultiSelectOptions(
    options: List<SearchableSelectableOption>,
    selectedKeys: Set<String>,
    searchQuery: String,
): List<SearchableSelectableOption> {
    val deduplicated = options.distinctBy { it.key }
    val query = searchQuery.trim()
    val selected = deduplicated.filter { it.key in selectedKeys }
    val remaining = deduplicated.filterNot { it.key in selectedKeys }
    val matching = if (query.isBlank()) {
        emptyList()
    } else {
        remaining.filter { option ->
            option.label.contains(query, ignoreCase = true)
        }
    }
    val matchingKeys = matching.mapTo(mutableSetOf()) { it.key }
    val fallback = remaining.filterNot { it.key in matchingKeys }
    return selected + matching + fallback
}

private fun reorderSingleSelectOptions(
    options: List<SearchableSelectableOption>,
    selectedKey: String?,
    searchQuery: String,
): List<SearchableSelectableOption> {
    val deduplicated = options.distinctBy { it.key }
    val selected = selectedKey?.let { key -> deduplicated.filter { it.key == key } }.orEmpty()
    val remaining = deduplicated.filterNot { it.key == selectedKey }
    val query = searchQuery.trim()
    val matching = if (query.isBlank()) {
        emptyList()
    } else {
        remaining.filter { option ->
            option.label.contains(query, ignoreCase = true)
        }
    }
    val matchingKeys = matching.mapTo(mutableSetOf()) { it.key }
    val fallback = remaining.filterNot { it.key in matchingKeys }
    return selected + matching + fallback
}

@Composable
fun SearchableSelectableList(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    searchLabel: String,
    selectedKeys: Set<String>,
    options: List<SearchableSelectableOption>,
    emptyLabel: String,
    noResultsLabel: String,
    onOptionToggled: (String) -> Unit,
) {
    val trackedOnSearchQueryChanged = rememberInteractionAwareValueChange(onSearchQueryChanged)
    var visibleCount by remember(searchQuery) { mutableStateOf(InitialVisibleSelectionCount) }
    val orderedOptions = reorderMultiSelectOptions(
        options = options,
        selectedKeys = selectedKeys,
        searchQuery = searchQuery,
    )
    val visibleOptions = visibleSelectionOptions(options = orderedOptions, visibleCount = visibleCount)

    LaunchedEffect(selectedKeys) {
        visibleCount = InitialVisibleSelectionCount
    }

    AppTextField(
        value = searchQuery,
        onValueChange = trackedOnSearchQueryChanged,
        label = searchLabel,
        modifier = Modifier.fillMaxWidth(),
        type = AppTextFieldType.SEARCH,
    )

    if (orderedOptions.isEmpty()) {
        Text(
            text = if (searchQuery.isBlank()) emptyLabel else noResultsLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    visibleOptions.forEach { option ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            tonalElevation = if (option.key in selectedKeys) 2.dp else 0.dp,
            color = if (option.key in selectedKeys) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOptionToggled(option.key)
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Checkbox(
                    checked = option.key in selectedKeys,
                    onCheckedChange = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (option.key in selectedKeys) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                option.secondaryLabel?.takeIf { it.isNotBlank() }?.let { secondaryLabel ->
                    Spacer(modifier = Modifier.width(8.dp))
                    SelectionStatusLabel(
                        text = secondaryLabel,
                        isSelected = option.key in selectedKeys,
                    )
                }
            }
        }
    }

    if (orderedOptions.size > InitialVisibleSelectionCount) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (visibleCount < orderedOptions.size) {
                TextButton(
                    onClick = { visibleCount += SelectionPageSize },
                ) {
                    Text("Voir plus")
                }
            }
            if (visibleCount > InitialVisibleSelectionCount) {
                TextButton(
                    onClick = { visibleCount = InitialVisibleSelectionCount },
                ) {
                    Text("Voir moins")
                }
            }
        }
    }
}

@Composable
fun SearchableSingleSelectList(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    searchLabel: String,
    selectedKey: String?,
    options: List<SearchableSelectableOption>,
    emptyLabel: String,
    noResultsLabel: String,
    onOptionSelected: (String) -> Unit,
) {
    val trackedOnSearchQueryChanged = rememberInteractionAwareValueChange(onSearchQueryChanged)
    var visibleCount by remember(searchQuery) { mutableStateOf(InitialVisibleSelectionCount) }
    val orderedOptions = reorderSingleSelectOptions(
        options = options,
        selectedKey = selectedKey,
        searchQuery = searchQuery,
    )
    val visibleOptions = visibleSelectionOptions(options = orderedOptions, visibleCount = visibleCount)

    LaunchedEffect(selectedKey) {
        visibleCount = InitialVisibleSelectionCount
    }

    AppTextField(
        value = searchQuery,
        onValueChange = trackedOnSearchQueryChanged,
        label = searchLabel,
        modifier = Modifier.fillMaxWidth(),
        type = AppTextFieldType.SEARCH,
    )

    if (orderedOptions.isEmpty()) {
        Text(
            text = if (searchQuery.isBlank()) emptyLabel else noResultsLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    visibleOptions.forEach { option ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            tonalElevation = if (option.key == selectedKey) 2.dp else 0.dp,
            color = if (option.key == selectedKey) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOptionSelected(option.key)
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                RadioButton(
                    selected = option.key == selectedKey,
                    onClick = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (option.key == selectedKey) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                option.secondaryLabel?.takeIf { it.isNotBlank() }?.let { secondaryLabel ->
                    Spacer(modifier = Modifier.width(8.dp))
                    SelectionStatusLabel(
                        text = secondaryLabel,
                        isSelected = option.key == selectedKey,
                    )
                }
            }
        }
    }

    if (orderedOptions.size > InitialVisibleSelectionCount) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (visibleCount < orderedOptions.size) {
                TextButton(
                    onClick = { visibleCount += SelectionPageSize },
                ) {
                    Text("Voir plus")
                }
            }
            if (visibleCount > InitialVisibleSelectionCount) {
                TextButton(
                    onClick = { visibleCount = InitialVisibleSelectionCount },
                ) {
                    Text("Voir moins")
                }
            }
        }
    }
}
