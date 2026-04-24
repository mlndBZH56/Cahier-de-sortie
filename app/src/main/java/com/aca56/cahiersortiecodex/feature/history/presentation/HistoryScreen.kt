package com.aca56.cahiersortiecodex.feature.history.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.aca56.cahiersortiecodex.ui.components.AppSelectorFieldButton
import com.aca56.cahiersortiecodex.ui.components.AppDatePickerDialog
import com.aca56.cahiersortiecodex.ui.components.DeleteConfirmationDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableList
import com.aca56.cahiersortiecodex.ui.components.SearchableSingleSelectList
import com.aca56.cahiersortiecodex.ui.components.currentStorageDate
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay

@Composable
fun HistoryRoute(
    contentPadding: PaddingValues,
    initialBoatId: Long?,
    onOpenSession: (Long) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(
            sessionRepository = appContainer.sessionRepository,
            initialBoatId = initialBoatId,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    HistoryScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onRowerSearchChanged = viewModel::onRowerSearchChanged,
        onRowerToggled = viewModel::onRowerToggled,
        onBoatSelected = viewModel::onBoatSelected,
        onDateFromFilterChanged = viewModel::onDateFromFilterChanged,
        onDateToFilterChanged = viewModel::onDateToFilterChanged,
        onDestinationFilterChanged = viewModel::onDestinationFilterChanged,
        onOpenSession = onOpenSession,
        onClearFilters = viewModel::clearFilters,
        onDismissMessage = viewModel::clearMessage,
    )
}

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    uiState: HistoryUiState,
    onRowerSearchChanged: (String) -> Unit,
    onRowerToggled: (String) -> Unit,
    onBoatSelected: (Long?) -> Unit,
    onDateFromFilterChanged: (String) -> Unit,
    onDateToFilterChanged: (String) -> Unit,
    onDestinationFilterChanged: (String?) -> Unit,
    onOpenSession: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onDismissMessage: () -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Historique",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedButton(
                onClick = { filtersVisible = !filtersVisible },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (filtersVisible) "Masquer les filtres" else "Filtres")
            }

            if (filtersVisible) {
                HistoryFilters(
                    uiState = uiState,
                    onRowerSearchChanged = onRowerSearchChanged,
                    onRowerToggled = onRowerToggled,
                    onBoatSelected = onBoatSelected,
                    onDateFromFilterChanged = onDateFromFilterChanged,
                    onDateToFilterChanged = onDateToFilterChanged,
                    onDestinationFilterChanged = onDestinationFilterChanged,
                    onClearFilters = onClearFilters,
                )
            }

            if (uiState.filteredSessions.isEmpty()) {
                Text(
                    text = "Aucune session ne correspond aux filtres actuels.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.filteredSessions.forEach { session ->
                    HistorySessionCard(
                        session = session,
                        onOpenSession = { onOpenSession(session.id) },
                    )
                }
            }
        }
    }

    uiState.message?.let { message ->
        FeedbackDialog(
            message = message,
            type = uiState.messageType ?: FeedbackDialogType.ERROR,
            onDismiss = onDismissMessage,
        )
    }
}

@Composable
private fun HistoryFilters(
    uiState: HistoryUiState,
    onRowerSearchChanged: (String) -> Unit,
    onRowerToggled: (String) -> Unit,
    onBoatSelected: (Long?) -> Unit,
    onDateFromFilterChanged: (String) -> Unit,
    onDateToFilterChanged: (String) -> Unit,
    onDestinationFilterChanged: (String?) -> Unit,
    onClearFilters: () -> Unit,
) {
    var showDateFromPicker by remember { mutableStateOf(false) }
    var showDateToPicker by remember { mutableStateOf(false) }
    var boatSearchQuery by remember { mutableStateOf("") }
    var destinationMenuExpanded by remember { mutableStateOf(false) }

    if (showDateFromPicker) {
        AppDatePickerDialog(
            storageDate = uiState.dateFromFilter.ifBlank { uiState.allSessions.firstOrNull()?.date ?: currentStorageDate() },
            onDismissRequest = { showDateFromPicker = false },
            onDateSelected = onDateFromFilterChanged,
        )
    }

    if (showDateToPicker) {
        AppDatePickerDialog(
            storageDate = uiState.dateToFilter.ifBlank { uiState.allSessions.firstOrNull()?.date ?: currentStorageDate() },
            onDismissRequest = { showDateToPicker = false },
            onDateSelected = onDateToFilterChanged,
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 3.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Filtres",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = if (uiState.selectedRowers.isEmpty()) {
                    "Filtrer par rameur"
                } else {
                    "Rameurs sélectionnés : ${uiState.selectedRowers.size}"
                },
                style = MaterialTheme.typography.titleMedium,
            )

            SearchableSelectableList(
                searchQuery = uiState.rowerSearchQuery,
                onSearchQueryChanged = onRowerSearchChanged,
                searchLabel = "Rechercher des rameurs",
                selectedKeys = uiState.selectedRowers,
                options = uiState.filteredAvailableRowerOptions,
                emptyLabel = "Aucun rameur disponible.",
                noResultsLabel = "Aucun rameur ne correspond à la recherche.",
                onOptionToggled = onRowerToggled,
            )

            SearchableSingleSelectList(
                searchQuery = boatSearchQuery,
                onSearchQueryChanged = { boatSearchQuery = it },
                searchLabel = "Rechercher un bateau",
                selectedKey = uiState.selectedBoatId?.toString(),
                options = uiState.availableBoatOptions.filter { option ->
                    boatSearchQuery.isBlank() || option.label.contains(boatSearchQuery.trim(), ignoreCase = true)
                },
                emptyLabel = "Aucun bateau disponible.",
                noResultsLabel = "Aucun bateau ne correspond à la recherche.",
                onOptionSelected = { selectedKey ->
                    onBoatSelected(selectedKey.toLongOrNull())
                },
            )

            if (uiState.selectedBoatId != null) {
                OutlinedButton(
                    onClick = {
                        onBoatSelected(null)
                        boatSearchQuery = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Effacer le bateau")
                }
            }

            AppSelectorFieldButton(
                onClick = {
                    showDateFromPicker = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.dateFromFilter.isBlank()) {
                        "Date de début"
                    } else {
                        "Date de début : ${formatDateForDisplay(uiState.dateFromFilter)}"
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
                    if (uiState.dateToFilter.isBlank()) {
                        "Date de fin"
                    } else {
                        "Date de fin : ${formatDateForDisplay(uiState.dateToFilter)}"
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.dateFromFilter.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            onDateFromFilterChanged("")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Effacer le début")
                    }
                }
                if (uiState.dateToFilter.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            onDateToFilterChanged("")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Effacer la fin")
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                AppSelectorFieldButton(
                    onClick = {
                        destinationMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(uiState.selectedDestination ?: "Toutes les destinations")
                }

                DropdownMenu(
                    expanded = destinationMenuExpanded,
                    onDismissRequest = { destinationMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.92f),
                ) {
                    DropdownMenuItem(
                        text = { Text("Toutes les destinations") },
                        onClick = {
                            onDestinationFilterChanged(null)
                            destinationMenuExpanded = false
                        },
                    )
                    uiState.availableDestinationOptions.forEach { destination ->
                        DropdownMenuItem(
                            text = { Text(destination.label) },
                            onClick = {
                                onDestinationFilterChanged(destination.key)
                                destinationMenuExpanded = false
                            },
                        )
                    }
                }
            }

            Button(
                onClick = onClearFilters,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Effacer les filtres")
            }
        }
    }
}

@Composable
private fun HistorySessionCard(
    session: HistorySessionUi,
    onOpenSession: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSession)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatDateForDisplay(session.date),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = session.boatName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HistoryDetailRoute(
    contentPadding: PaddingValues,
    sessionId: Long,
    historyViewModel: HistoryViewModel,
    onEditSession: (Long) -> Unit,
    onCloseDetail: () -> Unit,
) {
    val uiState by historyViewModel.uiState.collectAsState()
    val session = uiState.allSessions.firstOrNull { it.id == sessionId }

    HistoryDetailScreen(
        contentPadding = contentPadding,
        isLoaded = uiState.isLoaded,
        session = session,
        onEditSession = onEditSession,
        onCloseDetail = onCloseDetail,
        onDeleteSession = historyViewModel::deleteSession,
    )
}

@Composable
fun HistoryDetailScreen(
    contentPadding: PaddingValues,
    isLoaded: Boolean,
    session: HistorySessionUi?,
    onEditSession: (Long) -> Unit,
    onCloseDetail: () -> Unit,
    onDeleteSession: (Long) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && session != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDeleteSession(session.id)
                showDeleteDialog = false
                onCloseDetail()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Détails de la session",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        if (!isLoaded) {
            Text(
                text = "Chargement de la session...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        if (session == null) {
            Text(
                text = "Session introuvable.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 3.dp,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Date : ${formatDateForDisplay(session.date)}")
                Text("Bateau : ${session.boatName}")
                Text("Statut : ${session.status}")
                Text("Heure de départ : ${session.startTime}")
                Text("Heure de fin : ${session.endTime.ifBlank { "Non définie" }}")
                Text("Destination : ${session.destination.ifBlank { "Non définie" }}")
                Text("Km : ${session.km.ifBlank { "Non défini" }}")
                Text("Remarques : ${session.remarks.ifBlank { "Aucune remarque" }}")

                Text(
                    text = "Rameurs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                if (session.participants.isEmpty()) {
                    Text(
                        text = "Aucun rameur lié à cette session.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    session.participants.forEach { participant ->
                        Text(
                            text = if (participant.isGuest) {
                                "${participant.name} (Invité)"
                            } else {
                                participant.name
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { onEditSession(session.id) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Modifier")
                }
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Supprimer")
                }
            }
        }
    }
}
