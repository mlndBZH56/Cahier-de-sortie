package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PhotoSourceChooserDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    AppModalDialog(
        title = "Ajouter une photo",
        onDismiss = onDismiss,
        accentColor = MaterialTheme.colorScheme.primary,
        buttons = {
            AppDialogActionRow(
                confirmLabel = "Prendre une photo",
                onConfirm = onTakePhoto,
                dismissLabel = "Choisir depuis la galerie",
                onDismiss = onPickFromGallery,
            )
        },
    ) {
        Text("Choisissez la source de la photo.")
    }
}
