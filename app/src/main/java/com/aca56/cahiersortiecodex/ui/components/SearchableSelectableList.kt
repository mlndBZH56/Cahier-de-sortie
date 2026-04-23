package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val InitialVisibleSelectionCount = 5

data class SearchableSelectableOption(
    val key: String,
    val label: String,
    val secondaryLabel: String? = null,
    val usageCount: Int = 0,
)

private fun visibleSelectionOptions(
    options: List<SearchableSelectableOption>,
    showAll: Boolean,
): List<SearchableSelectableOption> {
    return if (showAll) {
        options
    } else {
        options.take(InitialVisibleSelectionCount)
    }
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
    val dismissKeyboard = rememberDismissKeyboardAction()
    val trackedOnSearchQueryChanged = rememberInteractionAwareValueChange(onSearchQueryChanged)
    var showAll by remember(searchQuery, options) { mutableStateOf(false) }
    val visibleOptions = visibleSelectionOptions(options = options, showAll = showAll)

    OutlinedTextField(
        value = searchQuery,
        onValueChange = trackedOnSearchQueryChanged,
        label = { Text(searchLabel) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardActions = rememberDoneKeyboardActions(),
    )

    if (options.isEmpty()) {
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
                        dismissKeyboard()
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
                    Text(
                        text = secondaryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (option.key in selectedKeys) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }

    if (options.size > InitialVisibleSelectionCount) {
        TextButton(
            onClick = {
                dismissKeyboard()
                showAll = !showAll
            },
        ) {
            Text(if (showAll) "Voir moins" else "Voir plus")
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
    val dismissKeyboard = rememberDismissKeyboardAction()
    val trackedOnSearchQueryChanged = rememberInteractionAwareValueChange(onSearchQueryChanged)
    var showAll by remember(searchQuery, options) { mutableStateOf(false) }
    val visibleOptions = visibleSelectionOptions(options = options, showAll = showAll)

    OutlinedTextField(
        value = searchQuery,
        onValueChange = trackedOnSearchQueryChanged,
        label = { Text(searchLabel) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardActions = rememberDoneKeyboardActions(),
    )

    if (options.isEmpty()) {
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
                        dismissKeyboard()
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
                    Text(
                        text = secondaryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (option.key == selectedKey) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }

    if (options.size > InitialVisibleSelectionCount) {
        TextButton(
            onClick = {
                dismissKeyboard()
                showAll = !showAll
            },
        ) {
            Text(if (showAll) "Voir moins" else "Voir plus")
        }
    }
}
