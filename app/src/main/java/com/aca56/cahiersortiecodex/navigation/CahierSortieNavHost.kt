package com.aca56.cahiersortiecodex.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.feature.history.presentation.HistoryDetailRoute
import com.aca56.cahiersortiecodex.feature.history.presentation.HistoryRoute
import com.aca56.cahiersortiecodex.feature.history.presentation.HistoryViewModel
import com.aca56.cahiersortiecodex.feature.newsession.presentation.EditSessionRoute
import com.aca56.cahiersortiecodex.feature.home.presentation.HomeRoute
import com.aca56.cahiersortiecodex.feature.newsession.presentation.NewSessionRoute
import com.aca56.cahiersortiecodex.feature.ongoingsessions.presentation.OngoingSessionsRoute
import com.aca56.cahiersortiecodex.feature.remarks.presentation.RemarksRoute
import com.aca56.cahiersortiecodex.feature.settings.presentation.SettingsRoute
import com.aca56.cahiersortiecodex.feature.settings.presentation.SettingsBoatsRoute
import com.aca56.cahiersortiecodex.feature.settings.presentation.SettingsDestinationsRoute
import com.aca56.cahiersortiecodex.feature.settings.presentation.SettingsRowersRoute
import com.aca56.cahiersortiecodex.feature.settings.presentation.SettingsViewModel
import com.aca56.cahiersortiecodex.feature.stats.presentation.StatsRoute
import com.aca56.cahiersortiecodex.ui.shell.CahierSortieScaffold
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus

@Composable
fun CahierSortieNavHost(navController: NavHostController) {
    val application = navController.context.applicationContext as CahierSortieApplication
    val appContainer = application.appContainer
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            application = application,
            backupManager = appContainer.backupManager,
            pinCodeStore = appContainer.pinCodeStore,
            appPreferencesStore = appContainer.appPreferencesStore,
            rowerRepository = appContainer.rowerRepository,
            boatRepository = appContainer.boatRepository,
            destinationRepository = appContainer.destinationRepository,
            sessionRepository = appContainer.sessionRepository,
        ),
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(currentRoute, settingsViewModel) {
        settingsViewModel.onNavigationDestinationChanged(currentRoute)
    }

    CahierSortieScaffold(navController = navController) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
        ) {
            composable(AppDestination.Home.route) {
                HomeRoute(
                    contentPadding = innerPadding,
                    onOpenNewSession = {
                        navController.navigate(AppDestination.NewSession.route) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onOpenOngoingSessions = {
                        navController.navigate(AppDestination.OngoingSessions.route) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onOpenHistory = {
                        navController.navigate(AppDestination.History.route) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onOpenRemarks = {
                        navController.navigate(AppDestination.Remarks.route) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onOpenStats = {
                        navController.navigate(AppDestination.Stats.route) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onOpenSettings = {
                        navController.navigate(AppDestination.Settings.route) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                )
            }
            composable(AppDestination.NewSession.route) {
                NewSessionRoute(
                    contentPadding = innerPadding,
                    onSessionSaved = {
                        navController.navigate(AppDestination.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                )
            }
            composable(AppDestination.OngoingSessions.route) {
                OngoingSessionsRoute(
                    contentPadding = innerPadding,
                    onEditSession = { sessionId ->
                        navController.navigate(AppDestination.EditSession.createRoute(sessionId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppDestination.History.route) {
                HistoryRoute(
                    contentPadding = innerPadding,
                    onOpenSession = { sessionId ->
                        navController.navigate(AppDestination.HistoryDetail.createRoute(sessionId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = AppDestination.HistoryDetail.route,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                val historyBackStackEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AppDestination.History.route)
                }
                val appContainer =
                    (navController.context.applicationContext as CahierSortieApplication).appContainer
                val historyViewModel: HistoryViewModel = viewModel(
                    viewModelStoreOwner = historyBackStackEntry,
                    factory = HistoryViewModel.factory(
                        sessionRepository = appContainer.sessionRepository,
                    ),
                )
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                HistoryDetailRoute(
                    contentPadding = innerPadding,
                    sessionId = sessionId,
                    historyViewModel = historyViewModel,
                    onEditSession = { editableSessionId ->
                        navController.navigate(AppDestination.EditSession.createRoute(editableSessionId)) {
                            launchSingleTop = true
                        }
                    },
                    onCloseDetail = {
                        navController.navigateUp()
                    },
                )
            }
            composable(AppDestination.Stats.route) {
                StatsRoute(contentPadding = innerPadding)
            }
            composable(
                route = AppDestination.EditSession.route,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                EditSessionRoute(
                    contentPadding = innerPadding,
                    sessionId = sessionId,
                    onSessionSaved = { savedStatus ->
                        when (savedStatus) {
                            SessionStatus.ONGOING -> {
                                navController.navigate(AppDestination.OngoingSessions.route) {
                                    popUpTo(AppDestination.OngoingSessions.route) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                            SessionStatus.COMPLETED -> {
                                navController.navigate(AppDestination.Home.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        }
                    },
                )
            }
            composable(AppDestination.Remarks.route) {
                RemarksRoute(contentPadding = innerPadding)
            }
            composable(AppDestination.Settings.route) {
                SettingsRoute(
                    contentPadding = innerPadding,
                    viewModel = settingsViewModel,
                    onOpenRowers = {
                        navController.navigate(AppDestination.SettingsRowers.route) { launchSingleTop = true }
                    },
                    onOpenBoats = {
                        navController.navigate(AppDestination.SettingsBoats.route) { launchSingleTop = true }
                    },
                    onOpenDestinations = {
                        navController.navigate(AppDestination.SettingsDestinations.route) { launchSingleTop = true }
                    },
                )
            }
            composable(AppDestination.SettingsRowers.route) {
                SettingsRowersRoute(contentPadding = innerPadding, viewModel = settingsViewModel)
            }
            composable(AppDestination.SettingsBoats.route) {
                SettingsBoatsRoute(contentPadding = innerPadding, viewModel = settingsViewModel)
            }
            composable(AppDestination.SettingsDestinations.route) {
                SettingsDestinationsRoute(contentPadding = innerPadding, viewModel = settingsViewModel)
            }
        }
    }
}
