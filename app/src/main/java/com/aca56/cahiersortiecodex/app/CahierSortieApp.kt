package com.aca56.cahiersortiecodex.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.navigation.AppDestination
import com.aca56.cahiersortiecodex.navigation.CahierSortieNavHost
import com.aca56.cahiersortiecodex.ui.components.AppTapToDismissKeyboard
import com.aca56.cahiersortiecodex.ui.components.ProvideAppKeyboardFocusManager
import com.aca56.cahiersortiecodex.ui.components.ProvideUserInteractionNotifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CahierSortieApp(
    userInteractionEvents: StateFlow<Long>,
    onUserInteraction: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appPreferences by (context.applicationContext as CahierSortieApplication)
        .appContainer
        .appPreferencesStore
        .preferencesFlow
        .collectAsState()
    val interactionEventId by userInteractionEvents.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            onUserInteraction()
        }
    }

    LaunchedEffect(interactionEventId, currentRoute, appPreferences.inactivityTimeoutMillis) {
        if (currentRoute == null) return@LaunchedEffect

        delay(appPreferences.inactivityTimeoutMillis)

        val isOnHome = navController.currentDestination
            ?.route == AppDestination.Home.route

        if (!isOnHome) {
            navController.navigate(AppDestination.Home.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }
    ProvideUserInteractionNotifier(onUserInteraction = onUserInteraction) {
        ProvideAppKeyboardFocusManager {
            AppTapToDismissKeyboard {
                CahierSortieNavHost(navController = navController)
            }
        }
    }
}
