package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun rememberMessageSnackbarHostState(message: String?): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    return snackbarHostState
}

@Composable
fun MessageSnackbarHost(
    message: String?,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = rememberMessageSnackbarHostState(message)
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
    )
}
