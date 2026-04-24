package com.aca56.cahiersortiecodex.feature.ongoingsessions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.ui.components.AppSelectorFieldButton
import com.aca56.cahiersortiecodex.ui.components.AppTimePickerDialog
import com.aca56.cahiersortiecodex.ui.components.DeleteConfirmationDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay
import com.aca56.cahiersortiecodex.ui.components.rememberInteractionAwareValueChange

@Composable
fun OngoingSessionsRoute(
    contentPadding: PaddingValues,
    onEditSession: (Long) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: OngoingSessionsViewModel = viewModel(
        factory = OngoingSessionsViewModel.factory(
            sessionRepository = appContainer.sessionRepository,
            destinationRepository = appContainer.destinationRepository,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    OngoingSessionsScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onOpenSession = viewModel::openSession,
        onFermerSession = viewModel::closeSessionDetails,
        onToggleBulkSelectionMode = viewModel::toggleBulkSelectionMode,
        onToggleSessionSelection = viewModel::toggleSessionSelection,
        onOpenBulkCompletionEditor = viewModel::openBulkCompletionEditor,
        onCloseBulkCompletionEditor = viewModel::closeBulkCompletionEditor,
        onEndTimeChanged = viewModel::onEndTimeChanged,
        onKmChanged = viewModel::onKmChanged,
        onRemarksChanged = viewModel::onRemarksChanged,
        onDestinationSelected = viewModel::onDestinationSelected,
        onCustomDestinationSelected = viewModel::onCustomDestinationSelected,
        onDestinationChanged = viewModel::onDestinationChanged,
        onCompleteSession = viewModel::completeOpenedSession,
        onCompleteSelectedSessions = viewModel::completeSelectedSessions,
        onDeleteSession = viewModel::deleteOpenedSession,
        onEditSession = onEditSession,
        onDismissFeedback = viewModel::clearFeedback,
    )
}

@Composable
fun OngoingSessionsScreen(
    contentPadding: PaddingValues,
    uiState: OngoingSessionsUiState,
    onOpenSession: (Long) -> Unit,
    onFermerSession: () -> Unit,
    onToggleBulkSelectionMode: () -> Unit,
    onToggleSessionSelection: (Long) -> Unit,
    onOpenBulkCompletionEditor: () -> Unit,
    onCloseBulkCompletionEditor: () -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onKmChanged: (String) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onDestinationSelected: (Long?) -> Unit,
    onCustomDestinationSelected: () -> Unit,
    onDestinationChanged: (String) -> Unit,
    onCompleteSession: () -> Unit,
    onCompleteSelectedSessions: () -> Unit,
    onDeleteSession: () -> Unit,
    onEditSession: (Long) -> Unit,
    onDismissFeedback: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDeleteSession()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        if (uiState.isBulkCompletionEditorOpen) {
            BulkCompletionEditorScreen(
                uiState = uiState,
                onCloseBulkCompletionEditor = onCloseBulkCompletionEditor,
                onEndTimeChanged = onEndTimeChanged,
                onKmChanged = onKmChanged,
                onRemarksChanged = onRemarksChanged,
                onDestinationSelected = onDestinationSelected,
                onCustomDestinationSelected = onCustomDestinationSelected,
                onDestinationChanged = onDestinationChanged,
                onCompleteSelectedSessions = onCompleteSelectedSessions,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Sorties en cours",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                OutlinedButton(
                    onClick = onToggleBulkSelectionMode,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (uiState.isBulkSelectionMode) {
                            "Annuler la sélection multiple"
                        } else {
                            "Sélectionner plusieurs sorties"
                        },
                    )
                }

                if (uiState.isBulkSelectionMode) {
                    BulkSelectionActionCard(
                        selectedCount = uiState.selectedSessionIds.size,
                        isSaving = uiState.isSaving,
                        onOpenBulkCompletionEditor = onOpenBulkCompletionEditor,
                    )
                }

                if (uiState.sessions.isEmpty()) {
                    Text(
                        text = "Aucune session en cours trouvée.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    uiState.sessions.forEach { session ->
                        OngoingSessionCard(
                            session = session,
                            uiState = uiState,
                            isOpened = uiState.openedSessionId == session.id,
                            isSelected = session.id in uiState.selectedSessionIds,
                            onOpenSession = { onOpenSession(session.id) },
                            onFermerSession = onFermerSession,
                            onToggleSelected = { onToggleSessionSelection(session.id) },
                            onEndTimeChanged = onEndTimeChanged,
                            onKmChanged = onKmChanged,
                            onRemarksChanged = onRemarksChanged,
                            onDestinationSelected = onDestinationSelected,
                            onCustomDestinationSelected = onCustomDestinationSelected,
                            onDestinationChanged = onDestinationChanged,
                            onCompleteSession = onCompleteSession,
                            onDeleteSession = { showDeleteDialog = true },
                            onEditSession = onEditSession,
                        )
                    }
                }
            }
        }
    }

    uiState.errorMessage?.let { message ->
        FeedbackDialog(
            message = message,
            type = FeedbackDialogType.ERROR,
            onDismiss = onDismissFeedback,
        )
    }

    uiState.successMessage?.let { message ->
        FeedbackDialog(
            message = message,
            type = FeedbackDialogType.SUCCESS,
            onDismiss = onDismissFeedback,
        )
    }
}

@Composable
private fun BulkSelectionActionCard(
    selectedCount: Int,
    isSaving: Boolean,
    onOpenBulkCompletionEditor: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Sélection multiple",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (selectedCount == 0) {
                    "Sélectionnez une ou plusieurs sessions ci-dessous."
                } else {
                    "$selectedCount sortie(s) sélectionnée(s)."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onOpenBulkCompletionEditor,
                enabled = !isSaving && selectedCount > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clôturer les sorties sélectionnées")
            }
        }
    }
}

@Composable
private fun BulkCompletionEditorScreen(
    uiState: OngoingSessionsUiState,
    onCloseBulkCompletionEditor: () -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onKmChanged: (String) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onDestinationSelected: (Long?) -> Unit,
    onCustomDestinationSelected: () -> Unit,
    onDestinationChanged: (String) -> Unit,
    onCompleteSelectedSessions: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Clôture des sorties sélectionnées",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedButton(
            onClick = onCloseBulkCompletionEditor,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Retour à la sélection")
        }

        Text(
            text = "${uiState.selectedSessionIds.size} sortie(s) recevront les mêmes valeurs de fin.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BulkCompletionCard(
            uiState = uiState,
            onEndTimeChanged = onEndTimeChanged,
            onKmChanged = onKmChanged,
            onRemarksChanged = onRemarksChanged,
            onDestinationSelected = onDestinationSelected,
            onCustomDestinationSelected = onCustomDestinationSelected,
            onDestinationChanged = onDestinationChanged,
            onCompleteSelectedSessions = onCompleteSelectedSessions,
        )
    }
}

@Composable
private fun OngoingSessionCard(
    session: OngoingSessionItemUi,
    uiState: OngoingSessionsUiState,
    isOpened: Boolean,
    isSelected: Boolean,
    onOpenSession: () -> Unit,
    onFermerSession: () -> Unit,
    onToggleSelected: () -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onKmChanged: (String) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onDestinationSelected: (Long?) -> Unit,
    onCustomDestinationSelected: () -> Unit,
    onDestinationChanged: (String) -> Unit,
    onCompleteSession: () -> Unit,
    onDeleteSession: () -> Unit,
    onEditSession: (Long) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (uiState.isBulkSelectionMode) {
                    Modifier.clickable { onToggleSelected() }
                } else {
                    Modifier.clickable { onOpenSession() }
                },
            ),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isBulkSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelected() },
                    )
                    Text(
                        text = "${formatDateForDisplay(session.date)} - ${session.boatName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Text(
                    text = "${formatDateForDisplay(session.date)} - ${session.boatName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text("Départ : ${session.startTime}")
            Text("Destination : ${session.destination.ifBlank { "Non définie" }}")
            Text(
                text = "Rameurs : " + session.rowerNames.ifEmpty { listOf("Aucun rameur") }.joinToString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (uiState.isBulkSelectionMode) {
                Text(
                    text = if (isSelected) "Sortie sélectionnée pour la clôture groupée." else "Touchez la case pour inclure cette sortie.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Touchez la carte pour clôturer rapidement cette session.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (!uiState.isBulkSelectionMode && isOpened) {
        CompleteSessionDialog(
            session = session,
            uiState = uiState,
            onDismiss = onFermerSession,
            onEndTimeChanged = onEndTimeChanged,
            onKmChanged = onKmChanged,
            onRemarksChanged = onRemarksChanged,
            onDestinationSelected = onDestinationSelected,
            onCustomDestinationSelected = onCustomDestinationSelected,
            onDestinationChanged = onDestinationChanged,
            onCompleteSession = onCompleteSession,
            onEditSession = {
                onFermerSession()
                onEditSession(session.id)
            },
            onDeleteSession = onDeleteSession,
        )
    }
}

@Composable
private fun BulkCompletionCard(
    uiState: OngoingSessionsUiState,
    onEndTimeChanged: (String) -> Unit,
    onKmChanged: (String) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onDestinationSelected: (Long?) -> Unit,
    onCustomDestinationSelected: () -> Unit,
    onDestinationChanged: (String) -> Unit,
    onCompleteSelectedSessions: () -> Unit,
) {
    val trackedOnKmChanged = rememberInteractionAwareValueChange(onKmChanged)
    val trackedOnRemarksChanged = rememberInteractionAwareValueChange(onRemarksChanged)
    val trackedOnDestinationChanged = rememberInteractionAwareValueChange(onDestinationChanged)
    var showEndTimePicker by remember { mutableStateOf(false) }
    var destinationMenuExpanded by remember { mutableStateOf(false) }
    val selectedDestinationLabel = when {
        uiState.isCustomDestination && uiState.destination.isNotBlank() -> "Autre : ${uiState.destination}"
        uiState.isCustomDestination -> "Autre"
        uiState.selectedDestinationId != null -> uiState.availableDestinations.firstOrNull { it.id == uiState.selectedDestinationId }?.name ?: "Choisir une destination"
        else -> "Aucune destination"
    }

    if (showEndTimePicker) {
        AppTimePickerDialog(
            title = "Choisir l'heure de fin",
            storageTime = uiState.endTime.ifBlank { "12:00" },
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = onEndTimeChanged,
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Définir les valeurs de clôture",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${uiState.selectedSessionIds.size} sortie(s) sélectionnée(s). Les mêmes valeurs seront appliquées à toutes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AppSelectorFieldButton(
                onClick = {
                    showEndTimePicker = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.endTime.isBlank()) {
                        "Heure de fin : non définie"
                    } else {
                        "Heure de fin : ${uiState.endTime}"
                    },
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                AppSelectorFieldButton(
                    onClick = {
                        destinationMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Destination : $selectedDestinationLabel")
                }

                DropdownMenu(
                    expanded = destinationMenuExpanded,
                    onDismissRequest = { destinationMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.92f),
                ) {
                    DropdownMenuItem(
                        text = { Text("Aucune destination") },
                        onClick = {
                            onDestinationSelected(null)
                            destinationMenuExpanded = false
                        },
                    )
                    uiState.availableDestinations.forEach { destination ->
                        DropdownMenuItem(
                            text = { Text(destination.name) },
                            onClick = {
                                onDestinationSelected(destination.id)
                                destinationMenuExpanded = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Autre") },
                        onClick = {
                            onCustomDestinationSelected()
                            destinationMenuExpanded = false
                        },
                    )
                }
            }

            if (uiState.isCustomDestination) {
                OutlinedTextField(
                    value = uiState.destination,
                    onValueChange = trackedOnDestinationChanged,
                    label = { Text("Destination personnalisée") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                    ),
                    singleLine = true,
                    maxLines = 1,
                )
            }

            OutlinedTextField(
                value = uiState.km,
                onValueChange = trackedOnKmChanged,
                label = { Text("Km") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                singleLine = true,
                maxLines = 1,
            )

            OutlinedTextField(
                value = uiState.remarks,
                onValueChange = trackedOnRemarksChanged,
                label = { Text("Remarques") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                ),
            )

            Button(
                onClick = onCompleteSelectedSessions,
                enabled = uiState.canBulkComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(if (uiState.isSaving) "Enregistrement..." else "Appliquer à toutes les sessions sélectionnées")
            }
        }
    }
}

@Composable
private fun CompleteSessionDialog(
    session: OngoingSessionItemUi,
    uiState: OngoingSessionsUiState,
    onDismiss: () -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onKmChanged: (String) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onDestinationSelected: (Long?) -> Unit,
    onCustomDestinationSelected: () -> Unit,
    onDestinationChanged: (String) -> Unit,
    onCompleteSession: () -> Unit,
    onEditSession: () -> Unit,
    onDeleteSession: () -> Unit,
) {
    val trackedOnKmChanged = rememberInteractionAwareValueChange(onKmChanged)
    val trackedOnRemarksChanged = rememberInteractionAwareValueChange(onRemarksChanged)
    val trackedOnDestinationChanged = rememberInteractionAwareValueChange(onDestinationChanged)
    var showEndTimePicker by remember { mutableStateOf(false) }
    var destinationMenuExpanded by remember { mutableStateOf(false) }
    val selectedDestinationLabel = when {
        uiState.isCustomDestination && uiState.destination.isNotBlank() -> "Autre : ${uiState.destination}"
        uiState.isCustomDestination -> "Autre"
        uiState.selectedDestinationId != null -> uiState.availableDestinations.firstOrNull { it.id == uiState.selectedDestinationId }?.name ?: "Choisir une destination"
        else -> "Aucune destination"
    }

    if (showEndTimePicker) {
        AppTimePickerDialog(
            title = "Choisir l'heure de fin",
            storageTime = uiState.endTime.ifBlank { session.startTime },
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = onEndTimeChanged,
        )
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clôturer la session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSelectorFieldButton(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (uiState.endTime.isBlank()) "Heure de fin : non définie" else "Heure de fin : ${uiState.endTime}",
                    )
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    AppSelectorFieldButton(
                        onClick = { destinationMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Destination : $selectedDestinationLabel")
                    }
                    DropdownMenu(
                        expanded = destinationMenuExpanded,
                        onDismissRequest = { destinationMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.92f),
                    ) {
                        DropdownMenuItem(
                            text = { Text("Aucune destination") },
                            onClick = {
                                onDestinationSelected(null)
                                destinationMenuExpanded = false
                            },
                        )
                        uiState.availableDestinations.forEach { destination ->
                            DropdownMenuItem(
                                text = { Text(destination.name) },
                                onClick = {
                                    onDestinationSelected(destination.id)
                                    destinationMenuExpanded = false
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Autre") },
                            onClick = {
                                onCustomDestinationSelected()
                                destinationMenuExpanded = false
                            },
                        )
                    }
                }
                if (uiState.isCustomDestination) {
                    OutlinedTextField(
                        value = uiState.destination,
                        onValueChange = trackedOnDestinationChanged,
                        label = { Text("Destination personnalisée") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        maxLines = 1,
                    )
                }
                OutlinedTextField(
                    value = uiState.km,
                    onValueChange = trackedOnKmChanged,
                    label = { Text("Km") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    maxLines = 1,
                )
                OutlinedTextField(
                    value = uiState.remarks,
                    onValueChange = trackedOnRemarksChanged,
                    label = { Text("Remarques") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
                OutlinedButton(
                    onClick = onEditSession,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Modifier la session complète")
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        onDeleteSession()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Supprimer la session")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        confirmButton = {
            Button(
                onClick = onCompleteSession,
                enabled = uiState.canComplete,
            ) {
                Text(if (uiState.isSaving) "Enregistrement..." else "Valider")
            }
        },
    )
}
