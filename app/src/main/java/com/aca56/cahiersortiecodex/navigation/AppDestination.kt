package com.aca56.cahiersortiecodex.navigation

sealed class AppDestination(
    val route: String,
    val title: String,
) {
    data object Home : AppDestination("home", "Accueil")
    data object NewSession : AppDestination("new_session", "Nouvelle session")
    data object OngoingSessions : AppDestination("ongoing_sessions", "Sessions en cours")
    data object History : AppDestination("history", "Historique")
    data object HistoryDetail : AppDestination("history_detail/{sessionId}", "Détails de la session") {
        fun createRoute(sessionId: Long): String = "history_detail/$sessionId"
    }
    data object EditSession : AppDestination("edit_session/{sessionId}", "Modifier la session") {
        fun createRoute(sessionId: Long): String = "edit_session/$sessionId"
    }
    data object Remarks : AppDestination("remarks", "Remarques")
    data object Stats : AppDestination("stats", "Statistiques")
    data object Settings : AppDestination("settings", "Paramètres")
    data object SettingsRowers : AppDestination("settings/rowers", "Gérer les rameurs")
    data object SettingsBoats : AppDestination("settings/boats", "Gérer les bateaux")
    data object SettingsDestinations : AppDestination("settings/destinations", "Gérer les destinations")
}

val allDestinations = listOf(
    AppDestination.Home,
    AppDestination.NewSession,
    AppDestination.OngoingSessions,
    AppDestination.History,
    AppDestination.HistoryDetail,
    AppDestination.EditSession,
    AppDestination.Remarks,
    AppDestination.Stats,
    AppDestination.Settings,
    AppDestination.SettingsRowers,
    AppDestination.SettingsBoats,
    AppDestination.SettingsDestinations,
)

val topLevelDestinations = listOf(
    AppDestination.Home,
    AppDestination.NewSession,
    AppDestination.OngoingSessions,
    AppDestination.History,
    AppDestination.Remarks,
    AppDestination.Stats,
    AppDestination.Settings,
)
