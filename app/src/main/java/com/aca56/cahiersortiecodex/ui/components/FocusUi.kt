package com.aca56.cahiersortiecodex.ui.components

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView

@Composable
fun ProvideAppKeyboardFocusManager(content: @Composable () -> Unit) {
    val dismissKeyboard = rememberDismissKeyboardAction()
    val imeVisible = WindowInsets.isImeVisible

    LaunchedEffect(imeVisible) {
        if (!imeVisible) {
            dismissKeyboard()
        }
    }

    content()
}

@Composable
fun rememberDismissKeyboardAction(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current

    return remember(focusManager, keyboardController, view) {
        {
            focusManager.clearFocus(force = true)
            view.clearFocus()
            keyboardController?.hide()
            val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

@Composable
fun AppTapToDismissKeyboard(
    content: @Composable () -> Unit,
) {
    val dismissKeyboard = rememberDismissKeyboardAction()

    Box(
        modifier = Modifier
            .pointerInput(dismissKeyboard) {
                detectTapGestures(onTap = { dismissKeyboard() })
            }
            .safeDrawingPadding(),
    ) {
        content()
    }
}

fun Modifier.dismissKeyboardOnTap(): Modifier = composed {
    val dismissKeyboard = rememberDismissKeyboardAction()
    pointerInput(dismissKeyboard) {
        detectTapGestures(onTap = { dismissKeyboard() })
    }
}

@Composable
fun rememberDoneKeyboardActions(): KeyboardActions {
    val dismissKeyboard = rememberDismissKeyboardAction()
    return remember(dismissKeyboard) {
        KeyboardActions(
            onDone = { dismissKeyboard() },
            onGo = { dismissKeyboard() },
            onNext = { dismissKeyboard() },
            onSearch = { dismissKeyboard() },
            onSend = { dismissKeyboard() },
        )
    }
}
