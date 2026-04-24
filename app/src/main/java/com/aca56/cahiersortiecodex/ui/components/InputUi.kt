package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

enum class AppTextFieldType {
    SEARCH,
    SIMPLE,
    NUMERIC,
    PIN,
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
        AppTextFieldType.PIN -> KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        AppTextFieldType.LONG_TEXT -> KeyboardOptions(keyboardType = KeyboardType.Text)
    }

    val effectiveOnValueChange: (String) -> Unit = if (type == AppTextFieldType.PIN) {
        { updatedValue -> onValueChange(updatedValue.filter(Char::isDigit)) }
    } else {
        onValueChange
    }

    OutlinedTextField(
        value = value,
        onValueChange = effectiveOnValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (type == AppTextFieldType.PIN) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
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
