package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = "Supprimer",
        message = "Confirmer la suppression ?",
        confirmLabel = "Supprimer",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmColor = MaterialTheme.colorScheme.error,
        confirmContentColor = MaterialTheme.colorScheme.onError,
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    confirmContentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimary,
) {
    AppModalDialog(
        title = title,
        onDismiss = onDismiss,
        accentColor = confirmColor,
        buttons = {
            AppDialogActionRow(
                confirmLabel = confirmLabel,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                confirmContainerColor = confirmColor,
                confirmContentColor = confirmContentColor,
            )
        },
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
