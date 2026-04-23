package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun ProvideAppKeyboardFocusManager(content: @Composable () -> Unit) {
    content()
}

@Composable
fun rememberDismissKeyboardAction(): () -> Unit {
    return remember { {} }
}

@Composable
fun AppTapToDismissKeyboard(
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier) {
        content()
    }
}

fun Modifier.dismissKeyboardOnTap(): Modifier = this

@Composable
fun rememberDoneKeyboardActions(): KeyboardActions {
    return remember { KeyboardActions() }
}
