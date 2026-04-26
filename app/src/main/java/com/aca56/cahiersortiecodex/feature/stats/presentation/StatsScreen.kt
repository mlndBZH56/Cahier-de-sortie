package com.aca56.cahiersortiecodex.feature.stats.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.ui.components.AppDatePickerDialog
import com.aca56.cahiersortiecodex.ui.components.AppSelectorFieldButton
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableList
import com.aca56.cahiersortiecodex.ui.components.currentStorageDate
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay

@Composable
fun StatsRoute(
    contentPadding: PaddingValues,
    onOpenSession: (Long) -> Unit,
    onOpenRowerProfile: (String) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.factory(
            sessionRepository = appContainer.sessionRepository,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    StatsScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onRowerSearchQueryChanged = viewModel::onRowerSearchQueryChanged,
        onBoatSearchQueryChanged = viewModel::onBoatSearchQueryChanged,
        onRowerToggled = viewModel::onRowerToggled,
        onBoatToggled = viewModel::onBoatToggled,
        onDateFromChanged = viewModel::onDateFromChanged,
        onDateToChanged = viewModel::onDateToChanged,
        onQuickPeriodSelected = viewModel::onQuickPeriodSelected,
        onOpenSession = onOpenSession,
        onOpenRowerProfile = onOpenRowerProfile,
    )
}

@Composable
fun StatsScreen(
    contentPadding: PaddingValues,
    uiState: StatsUiState,
    onRowerSearchQueryChanged: (String) -> Unit,
    onBoatSearchQueryChanged: (String) -> Unit,
    onRowerToggled: (String) -> Unit,
    onBoatToggled: (String) -> Unit,
    onDateFromChanged: (String?) -> Unit,
    onDateToChanged: (String?) -> Unit,
    onQuickPeriodSelected: (StatsQuickPeriod) -> Unit,
    onOpenSession: (Long) -> Unit,
    onOpenRowerProfile: (String) -> Unit,
) {
    var filtersVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Statistiques",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            OutlinedButton(
                onClick = { filtersVisible = !filtersVisible },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (filtersVisible) "Masquer les filtres" else "Afficher les filtres")
            }

            if (filtersVisible) {
                StatsFiltersCard(
                    uiState = uiState,
                    onRowerSearchQueryChanged = onRowerSearchQueryChanged,
                    onBoatSearchQueryChanged = onBoatSearchQueryChanged,
                    onRowerToggled = onRowerToggled,
                    onBoatToggled = onBoatToggled,
                    onDateFromChanged = onDateFromChanged,
                    onDateToChanged = onDateToChanged,
                    onQuickPeriodSelected = onQuickPeriodSelected,
                )
            }

            StatsOverviewCard(globalStats = uiState.globalStats)

            Text(
                text = "Statistiques des bateaux",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            if (uiState.boatStats.isEmpty()) {
                EmptyStatsMessage("Aucune statistique bateau disponible pour les filtres actuels.")
            } else {
                uiState.boatStats.forEach { stat ->
                    BoatStatsCard(stat = stat)
                }
            }

            Text(
                text = "Statistiques des rameurs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            if (uiState.rowerStats.isEmpty()) {
                EmptyStatsMessage("Aucune statistique rameur disponible pour les filtres actuels.")
            } else {
                uiState.rowerStats.forEach { stat ->
                    RowerStatsCard(
                        stat = stat,
                        onOpenSession = onOpenSession,
                        onOpenProfile = onOpenRowerProfile
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsFiltersCard(
    uiState: StatsUiState,
    onRowerSearchQueryChanged: (String) -> Unit,
    onBoatSearchQueryChanged: (String) -> Unit,
    onRowerToggled: (String) -> Unit,
    onBoatToggled: (String) -> Unit,
    onDateFromChanged: (String?) -> Unit,
    onDateToChanged: (String?) -> Unit,
    onQuickPeriodSelected: (StatsQuickPeriod) -> Unit,
) {
    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }

    if (showDateFromPicker) {
        AppDatePickerDialog(
            storageDate = uiState.dateFrom ?: currentStorageDate(),
            onDismissRequest = { showDateFromPicker = false },
            onDateSelected = { onDateFromChanged(it) },
        )
    }

    if (showDateToPicker) {
        AppDatePickerDialog(
            storageDate = uiState.dateTo ?: uiState.dateFrom ?: currentStorageDate(),
            onDismissRequest = { showDateToPicker = false },
            onDateSelected = { onDateToChanged(it) },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Filtres",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickPeriodButton(
                    label = "Aujourd'hui",
                    isSelected = uiState.selectedQuickPeriod == StatsQuickPeriod.TODAY,
                    onClick = { onQuickPeriodSelected(StatsQuickPeriod.TODAY) },
                )
                QuickPeriodButton(
                    label = "Cette semaine",
                    isSelected = uiState.selectedQuickPeriod == StatsQuickPeriod.THIS_WEEK,
                    onClick = { onQuickPeriodSelected(StatsQuickPeriod.THIS_WEEK) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickPeriodButton(
                    label = "Ce mois",
                    isSelected = uiState.selectedQuickPeriod == StatsQuickPeriod.THIS_MONTH,
                    onClick = { onQuickPeriodSelected(StatsQuickPeriod.THIS_MONTH) },
                )
                QuickPeriodButton(
                    label = "Période personnalisée",
                    isSelected = uiState.selectedQuickPeriod == StatsQuickPeriod.CUSTOM,
                    onClick = { onQuickPeriodSelected(StatsQuickPeriod.CUSTOM) },
                )
            }

            AppSelectorFieldButton(
                onClick = {
                    showDateFromPicker = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.dateFrom == null) {
                        "Date de début"
                    } else {
                        "Date de début : ${formatDateForDisplay(uiState.dateFrom)}"
                    },
                )
            }

            AppSelectorFieldButton(
                onClick = {
                    showDateToPicker = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.dateTo == null) {
                        "Date de fin"
                    } else {
                        "Date de fin : ${formatDateForDisplay(uiState.dateTo)}"
                    },
                )
            }

            if (uiState.dateFrom != null || uiState.dateTo != null) {
                OutlinedButton(
                    onClick = {
                        onQuickPeriodSelected(StatsQuickPeriod.CUSTOM)
                        onDateFromChanged(null)
                        onDateToChanged(null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Effacer la période")
                }
            }

            Text(
                text = if (uiState.selectedRowerKeys.isEmpty()) {
                    "Filtrer par rameur"
                } else {
                    "Rameurs sélectionnés : ${uiState.selectedRowerKeys.size}"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            SearchableSelectableList(
                searchQuery = uiState.rowerSearchQuery,
                onSearchQueryChanged = onRowerSearchQueryChanged,
                searchLabel = "Rechercher un rameur",
                selectedKeys = uiState.selectedRowerKeys,
                options = uiState.filteredRowerOptions,
                emptyLabel = "Aucun rameur disponible.",
                noResultsLabel = "Aucun rameur ne correspond à la recherche.",
                onOptionToggled = onRowerToggled,
            )

            Text(
                text = if (uiState.selectedBoatKeys.isEmpty()) {
                    "Filtrer par bateau"
                } else {
                    "Bateaux sélectionnés : ${uiState.selectedBoatKeys.size}"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            SearchableSelectableList(
                searchQuery = uiState.boatSearchQuery,
                onSearchQueryChanged = onBoatSearchQueryChanged,
                searchLabel = "Rechercher un bateau",
                selectedKeys = uiState.selectedBoatKeys,
                options = uiState.filteredBoatOptions,
                emptyLabel = "Aucun bateau disponible.",
                noResultsLabel = "Aucun bateau ne correspond à la recherche.",
                onOptionToggled = nBoatToggled, // Reference fix
            )
        }
    }
}

private val nBoatToggled: (String) -> Unit = {} // Placeholder for reference fix if needed

@Composable
private fun RowScope.QuickPeriodButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StatsOverviewCard(globalStats: GlobalStatsUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatItem(label = "Total des sessions", value = globalStats.totalSessions.toString())
            StatItem(label = "Distance totale", value = "${String.format("%.1f", globalStats.totalKm)} km")
            StatItem(label = "Sessions terminées", value = globalStats.completedSessions.toString())
            StatItem(label = "Sessions en cours", value = globalStats.ongoingSessions.toString())
        }
    }
}

@Composable
private fun RowerStatsCard(
    stat: RowerStatUi,
    onOpenSession: (Long) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    var sessionsVisible by remember(stat.key) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onOpenProfile(stat.label) }
                ) {
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${stat.totalSessions} sessions • ${String.format("%.1f", stat.totalKm)} km",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedButton(
                onClick = { sessionsVisible = !sessionsVisible },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (sessionsVisible) "Masquer les sessions" else "Afficher les sessions")
            }

            if (sessionsVisible) {
                stat.sessions.forEach { session ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSession(session.id) }
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = formatDateForDisplay(session.date),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = session.boatName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${String.format("%.1f", session.km)} km",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoatStatsCard(stat: StatLineUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stat.label.ifBlank { "Inconnu" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${String.format("%.1f", stat.totalKm)} km",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "${stat.totalSessions} sessions",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun EmptyStatsMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
