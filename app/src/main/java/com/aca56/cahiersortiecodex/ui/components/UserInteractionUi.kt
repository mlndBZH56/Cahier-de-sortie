package com.aca56.cahiersortiecodex.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

val LocalUserInteractionNotifier = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun ProvideUserInteractionNotifier(
    onUserInteraction: () -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalUserInteractionNotifier provides onUserInteraction,
        content = content,
    )
}

@Composable
fun rememberUserInteractionNotifier(): () -> Unit {
    return LocalUserInteractionNotifier.current
}

@Composable
fun rememberInteractionAwareValueChange(
    onValueChange: (String) -> Unit,
): (String) -> Unit {
    val notifyUserInteraction = rememberUserInteractionNotifier()

    return remember(onValueChange, notifyUserInteraction) {
        { value ->
            notifyUserInteraction()
            onValueChange(value)
        }
    }
}
