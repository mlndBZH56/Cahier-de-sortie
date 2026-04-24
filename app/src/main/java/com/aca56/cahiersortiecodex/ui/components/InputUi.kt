package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

enum class AppTextFieldType {
    SEARCH,
    SIMPLE,
    NUMERIC,
    LONG_TEXT,
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    type: AppTextFieldType = AppTextFieldType.SIMPLE,
) {
    val keyboardOptions = when (type) {
        AppTextFieldType.SEARCH -> KeyboardOptions(keyboardType = KeyboardType.Text)
        AppTextFieldType.SIMPLE -> KeyboardOptions(keyboardType = KeyboardType.Text)
        AppTextFieldType.NUMERIC -> KeyboardOptions(keyboardType = KeyboardType.Number)
        AppTextFieldType.LONG_TEXT -> KeyboardOptions(keyboardType = KeyboardType.Text)
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        singleLine = type != AppTextFieldType.LONG_TEXT,
        maxLines = if (type == AppTextFieldType.LONG_TEXT) Int.MAX_VALUE else 1,
        shape = MaterialTheme.shapes.medium,
        colors = appOutlinedTextFieldColors(),
    )
}

@Composable
fun appOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)
