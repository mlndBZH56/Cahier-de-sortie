package com.aca56.cahiersortiecodex.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeRoute(
    contentPadding: PaddingValues,
    onOpenNewSession: () -> Unit,
    onOpenOngoingSessions: () -> Unit,
    onOpenBoats: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRemarks: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    HomeScreen(
        contentPadding = contentPadding,
        onOpenNewSession = onOpenNewSession,
        onOpenOngoingSessions = onOpenOngoingSessions,
        onOpenBoats = onOpenBoats,
        onOpenHistory = onOpenHistory,
        onOpenRemarks = onOpenRemarks,
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenNewSession: () -> Unit,
    onOpenOngoingSessions: () -> Unit,
    onOpenBoats: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRemarks: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val dashboardItems = listOf(
        DashboardAction(
            title = "Nouvelle session",
            description = "Démarrer rapidement une nouvelle sortie.",
            badge = "01",
            onClick = onOpenNewSession,
        ),
        DashboardAction(
            title = "Sessions en cours",
            description = "Reprendre et terminer les sorties actives.",
            badge = "02",
            onClick = onOpenOngoingSessions,
        ),
        DashboardAction(
            title = "Bateaux",
            description = "Consulter l’état, les réparations et les détails des bateaux.",
            badge = "03",
            onClick = onOpenBoats,
        ),
        DashboardAction(
            title = "Historique",
            description = "Consulter les sessions passées et leurs détails.",
            badge = "04",
            onClick = onOpenHistory,
        ),
        DashboardAction(
            title = "Remarques",
            description = "Noter les réparations, incidents et observations.",
            badge = "05",
            onClick = onOpenRemarks,
        ),
        DashboardAction(
            title = "Statistiques",
            description = "Consulter l’activité et les totaux d’utilisation.",
            badge = "06",
            onClick = onOpenStats,
        ),
        DashboardAction(
            title = "Paramètres",
            description = "Gérer les rameurs, bateaux et options de l’application.",
            badge = "07",
            onClick = onOpenSettings,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = 28.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 560.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Tableau de bord",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Choisissez une action pour ouvrir rapidement la bonne zone de l’application.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 220.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(dashboardItems) { item ->
                DashboardCard(item = item)
            }
        }
    }
}

private data class DashboardAction(
    val title: String,
    val description: String,
    val badge: String,
    val onClick: () -> Unit,
)

@Composable
private fun DashboardCard(
    item: DashboardAction,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clickable(onClick = item.onClick),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.badge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
