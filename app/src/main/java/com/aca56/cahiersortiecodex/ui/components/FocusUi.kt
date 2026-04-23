package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun rememberDismissKeyboardAction(): () -> Unit {
    val focusManager = LocalFocusManager.current

    return remember(focusManager) {
        {
            focusManager.clearFocus(force = true)
        }
    }
}

@Composable
fun rememberDoneKeyboardActions(): KeyboardActions {
    val dismissKeyboard = rememberDismissKeyboardAction()
    return remember(dismissKeyboard) {
        KeyboardActions(
            onDone = { dismissKeyboard() },
            onGo = { dismissKeyboard() },
            onSearch = { dismissKeyboard() },
            onSend = { dismissKeyboard() },
        )
    }
}
