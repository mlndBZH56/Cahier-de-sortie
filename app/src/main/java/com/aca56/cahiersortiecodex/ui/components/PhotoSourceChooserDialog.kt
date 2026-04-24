package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PhotoSourceChooserDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une photo") },
        text = { Text("Choisissez la source de la photo.") },
        confirmButton = {
            TextButton(onClick = onTakePhoto) {
                Text("Prendre une photo")
            }
        },
        dismissButton = {
            TextButton(onClick = onPickFromGallery) {
                Text("Choisir depuis la galerie")
            }
        },
    )
}
