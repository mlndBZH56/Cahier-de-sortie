package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aca56.cahiersortiecodex.CahierSortieApplication
import kotlinx.coroutines.delay

enum class FeedbackDialogType {
    ERROR,
    SUCCESS,
}

@Composable
fun FeedbackDialog(
    message: String,
    type: FeedbackDialogType,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val appPreferences by (context.applicationContext as CahierSortieApplication)
        .appContainer
        .appPreferencesStore
        .preferencesFlow
        .collectAsState()
    val dismissDelayMillis = if (type == FeedbackDialogType.ERROR) {
        appPreferences.errorPopupDurationMillis
    } else {
        appPreferences.successPopupDurationMillis
    }

    LaunchedEffect(message, type, dismissDelayMillis) {
        delay(dismissDelayMillis)
        onDismiss()
    }

    val titleColor = if (type == FeedbackDialogType.ERROR) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val containerColor = if (type == FeedbackDialogType.ERROR) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (type == FeedbackDialogType.ERROR) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    AppModalDialog(
        title = if (type == FeedbackDialogType.ERROR) "Erreur" else "Confirmation",
        onDismiss = onDismiss,
        accentColor = titleColor,
        buttons = {
            AppDialogActionRow(
                confirmLabel = "OK",
                onConfirm = onDismiss,
                onDismiss = onDismiss,
                dismissLabel = "Fermer",
                confirmContainerColor = titleColor,
                confirmContentColor = if (type == FeedbackDialogType.ERROR) {
                    MaterialTheme.colorScheme.onError
                } else {
                    MaterialTheme.colorScheme.onPrimary
                },
            )
        },
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(),
            color = containerColor,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = message,
                color = contentColor,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
