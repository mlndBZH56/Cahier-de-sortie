package com.aca56.cahiersortiecodex.feature.newsession.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialogDefaults
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
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.ui.components.AppDatePickerDialog
import com.aca56.cahiersortiecodex.ui.components.AppSelectorFieldButton
import com.aca56.cahiersortiecodex.ui.components.AppTimePickerDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableList
import com.aca56.cahiersortiecodex.ui.components.SearchableSingleSelectList
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay
import com.aca56.cahiersortiecodex.ui.components.rememberInteractionAwareValueChange

@Composable
fun NewSessionRoute(
    contentPadding: PaddingValues,
    onSessionSaved: (SessionStatus) -> Unit,
    onOpenBoatDetails: (Long) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: NewSessionViewModel = viewModel(
        factory = NewSessionViewModel.factory(
            boatRepository = appContainer.boatRepository,
            remarkRepository = appContainer.remarkRepository,
            destinationRepository = appContainer.destinationRepository,
            rowerRepository = appContainer.rowerRepository,
            sessionRepository = appContainer.sessionRepository,
            boatPhotoStorage = appContainer.boatPhotoStorage,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()
    val remarkPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenMultipleDocuments(),
    ) { uris ->
        viewModel.addSessionRemarkPhotos(uris)
    }

    NewSessionScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        screenTitle = "Nouvelle sortie",
        onDateChanged = viewModel::onDateChanged,
        onBoatSelected = viewModel::onBoatSelected,
        onForceBoatSelection = viewModel::forceBoatSelection,
        onDismissBoatConflict = viewModel::dismissBoatConflict,
        onStartTimeChanged = viewModel::onStartTimeChanged,
        onEndTimeChanged = viewModel::onEndTimeChanged,
        onKmChanged = viewModel::onKmChanged,
        onRemarksChanged = viewModel::onRemarksChanged,
        onSessionRemarkStatusChanged = viewModel::onSessionRemarkStatusChanged,
        onDestinationSelected = viewModel::onDestinationSelected,
        onCustomDestinationSelected = viewModel::onCustomDestinationSelected,
        onDestinationChanged = viewModel::onDestinationChanged,
        onRowerChecked = viewModel::onRowerChecked,
        onRowerSearchQueryChanged = viewModel::onRowerSearchQueryChanged,
        onGuestRowerNameChanged = viewModel::onGuestRowerNameChanged,
        onAddGuestRower = viewModel::addGuestRower,
        onRemoveGuestRower = viewModel::removeGuestRower,
        onToggleQuickMode = viewModel::toggleQuickMode,
        onResetForm = viewModel::resetForm,
        onApplySuggestedCrew = viewModel::applySuggestedCrew,
        onAddSessionRemarkPhotos = { remarkPhotoPickerLauncher.launch(arrayOf("image/*")) },
        onRemoveSessionRemarkPhoto = viewModel::removeSessionRemarkPhoto,
        onSaveSession = viewModel::saveSession,
        onOpenBoatDetails = onOpenBoatDetails,
        onDismissFeedback = {
            val savedSessionStatus = uiState.savedSessionStatus
            viewModel.clearFeedback()
            if (savedSessionStatus != null) {
                onSessionSaved(savedSessionStatus)
            }
        },
    )
}

@Composable
fun EditSessionRoute(
    contentPadding: PaddingValues,
    sessionId: Long,
    onSessionSaved: (SessionStatus) -> Unit,
    onOpenBoatDetails: (Long) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: NewSessionViewModel = viewModel(
        key = "edit_session_$sessionId",
        factory = NewSessionViewModel.factory(
            boatRepository = appContainer.boatRepository,
            remarkRepository = appContainer.remarkRepository,
            destinationRepository = appContainer.destinationRepository,
            rowerRepository = appContainer.rowerRepository,
            sessionRepository = appContainer.sessionRepository,
            boatPhotoStorage = appContainer.boatPhotoStorage,
            sessionId = sessionId,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()
    val remarkPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenMultipleDocuments(),
    ) { uris ->
        viewModel.addSessionRemarkPhotos(uris)
    }

    NewSessionScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        screenTitle = "Modifier la sortie",
        onDateChanged = viewModel::onDateChanged,
        onBoatSelected = viewModel::onBoatSelected,
        onForceBoatSelection = viewModel::forceBoatSelection,
        onDismissBoatConflict = viewModel::dismissBoatConflict,
        onStartTimeChanged = viewModel::onStartTimeChanged,
        onEndTimeChanged = viewModel::onEndTimeChanged,
        onKmChanged = viewModel::onKmChanged,
        onRemarksChanged = viewModel::onRemarksChanged,
        onSessionRemarkStatusChanged = viewModel::onSessionRemarkStatusChanged,
        onDestinationSelected = viewModel::onDestinationSelected,
        onCustomDestinationSelected = viewModel::onCustomDestinationSelected,
        onDestinationChanged = viewModel::onDestinationChanged,
        onRowerChecked = viewModel::onRowerChecked,
        onRowerSearchQueryChanged = viewModel::onRowerSearchQueryChanged,
        onGuestRowerNameChanged = viewModel::onGuestRowerNameChanged,
        onAddGuestRower = viewModel::addGuestRower,
        onRemoveGuestRower = viewModel::removeGuestRower,
        onToggleQuickMode = viewModel::toggleQuickMode,
        onResetForm = viewModel::resetForm,
        onApplySuggestedCrew = viewModel::applySuggestedCrew,
        onAddSessionRemarkPhotos = { remarkPhotoPickerLauncher.launch(arrayOf("image/*")) },
        onRemoveSessionRemarkPhoto = viewModel::removeSessionRemarkPhoto,
        onSaveSession = viewModel::saveSession,
        onOpenBoatDetails = onOpenBoatDetails,
        onDismissFeedback = {
            val savedSessionStatus = uiState.savedSessionStatus
            viewModel.clearFeedback()
            if (savedSessionStatus != null) {
                onSessionSaved(savedSessionStatus)
            }
        },
    )
}

@Composable
fun NewSessionScreen(
    contentPadding: PaddingValues,
    uiState: NewSessionUiState,
    screenTitle: String,
    onDateChanged: (String) -> Unit,
    onBoatSelected: (Long) -> Unit,
    onForceBoatSelection: () -> Unit,
    onDismissBoatConflict: () -> Unit,
    onStartTimeChanged: (String) -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onKmChanged: (String) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onSessionRemarkStatusChanged: (RemarkStatus) -> Unit,
    onDestinationSelected: (Long?) -> Unit,
    onCustomDestinationSelected: () -> Unit,
    onDestinationChanged: (String) -> Unit,
    onRowerChecked: (Long, Boolean) -> Unit,
    onRowerSearchQueryChanged: (String) -> Unit,
    onGuestRowerNameChanged: (String) -> Unit,
    onAddGuestRower: () -> Unit,
    onRemoveGuestRower: (Long) -> Unit,
    onToggleQuickMode: () -> Unit,
    onResetForm: () -> Unit,
    onApplySuggestedCrew: (Set<Long>) -> Unit,
    onAddSessionRemarkPhotos: () -> Unit,
    onRemoveSessionRemarkPhoto: (String) -> Unit,
    onSaveSession: () -> Unit,
    onOpenBoatDetails: (Long) -> Unit,
    onDismissFeedback: () -> Unit,
) {
    var destinationMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var boatSearchQuery by remember { mutableStateOf("") }
    var showRemarkStatusOptions by remember(uiState.sessionRemarkStatus) { mutableStateOf(uiState.sessionRemarkStatus != RemarkStatus.NORMAL) }
    val trackedOnKmChanged = rememberInteractionAwareValueChange(onKmChanged)
    val trackedOnRemarksChanged = rememberInteractionAwareValueChange(onRemarksChanged)
    val trackedOnDestinationChanged = rememberInteractionAwareValueChange(onDestinationChanged)
    val trackedOnGuestRowerNameChanged = rememberInteractionAwareValueChange(onGuestRowerNameChanged)
    val selectedBoatLabel = uiState.selectedBoat?.let {
        val status = uiState.boatStatuses[it.id]?.label()?.let { label -> " - $label" }.orEmpty()
        "${it.name} (${it.seatCount} places)$status"
    } ?: "Choisir un bateau"
    val selectedDestinationLabel = when {
        uiState.isCustomDestination && uiState.destination.isNotBlank() -> "Autre : ${uiState.destination}"
        uiState.selectedDestinationId != null -> uiState.availableDestinations.firstOrNull {
            it.id == uiState.selectedDestinationId
        }?.name ?: "Choisir une destination"
        else -> "Choisir une destination"
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            storageDate = uiState.date,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = onDateChanged,
        )
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            title = "Choisir l'heure de départ",
            storageTime = uiState.startTime,
            onDismissRequest = { showTimePicker = false },
            onTimeSelected = onStartTimeChanged,
        )
    }

    if (showEndTimePicker) {
        AppTimePickerDialog(
            title = "Choisir l'heure de fin",
            storageTime = uiState.endTime.ifBlank { uiState.startTime },
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = onEndTimeChanged,
        )
    }

    uiState.boatConflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = onDismissBoatConflict,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
            titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
            textContentColor = MaterialTheme.colorScheme.onErrorContainer,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            title = { Text(conflict.title) },
            text = { Text(conflict.description) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onDismissBoatConflict()
                        onOpenBoatDetails(conflict.boatId)
                    }) {
                        Text("Voir la fiche bateau")
                    }
                    TextButton(onClick = onForceBoatSelection) {
                        Text("Forcer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissBoatConflict) {
                    Text("Annuler")
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            HeaderRow(
                title = screenTitle,
                showReset = !uiState.isEditMode,
                onResetForm = onResetForm,
            )

            if (!uiState.isEditMode) {
                ModeToggleCard(
                    isQuickMode = uiState.isQuickMode,
                    onToggleQuickMode = onToggleQuickMode,
                )
            }

            if (!uiState.isQuickMode) {
                SectionCard(
                    title = "Informations de sortie",
                    description = "Renseignez la date, l'heure de départ et la destination.",
                ) {
                    FormGroupTitle("Date")
                    AppSelectorFieldButton(
                        onClick = {
                            showDatePicker = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Date : ${formatDateForDisplay(uiState.date)}")
                    }

                    FormGroupTitle("Heure de départ")
                    SessionSelectionButton(
                        onClick = {
                            showTimePicker = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Heure de départ : ${uiState.startTime}")
                    }

                    FormGroupTitle("Destination")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SessionSelectionButton(
                            onClick = {
                                destinationMenuExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(selectedDestinationLabel)
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
                    SessionOutlinedTextField(
                        value = uiState.destination,
                        onValueChange = trackedOnDestinationChanged,
                        label = { Text("Destination personnalisee") },
                        modifier = Modifier.fillMaxWidth(),
                        type = SessionFieldType.SIMPLE,
                    )
                    }
                }
            } else {
                QuickModeInfoCard()
            }

            SectionCard(
                    title = "Équipage",
                description = "Choisissez le bateau et les rameurs.",
            ) {
                FormGroupTitle("Bateau")
                Text(
                    text = selectedBoatLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SearchableSingleSelectList(
                    searchQuery = boatSearchQuery,
                    onSearchQueryChanged = { boatSearchQuery = it },
                    searchLabel = "Rechercher un bateau",
                    selectedKey = uiState.selectedBoatId?.toString(),
                    options = uiState.availableBoatOptions.filter { option ->
                        boatSearchQuery.isBlank() || option.label.contains(boatSearchQuery.trim(), ignoreCase = true)
                    },
                    emptyLabel = "Aucun bateau disponible pour le moment.",
                    noResultsLabel = "Aucun bateau ne correspond a la recherche.",
                    onOptionSelected = { selectedKey ->
                        selectedKey.toLongOrNull()?.let(onBoatSelected)
                    },
                )

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))

                if (uiState.suggestedCrew.isNotEmpty()) {
                    CompactSuggestionsSection(
                        suggestions = uiState.suggestedCrew,
                        onApplySuggestion = onApplySuggestedCrew,
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))
                }

                FormGroupTitle("Rameurs")
                Text(
                    text = buildString {
                        append("Selectionnes : ${uiState.totalSelectedRowers}")
                        uiState.selectedBoat?.let { append(" / ${it.seatCount} places") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.isSeatCountValid) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )

                SearchableSelectableList(
                    searchQuery = uiState.rowerSearchQuery,
                    onSearchQueryChanged = onRowerSearchQueryChanged,
                    searchLabel = "Rechercher des rameurs",
                    selectedKeys = uiState.selectedRowerIds.map { it.toString() }.toSet(),
                    options = uiState.filteredRowerOptions,
                    emptyLabel = "Aucun rameur enregistre pour le moment.",
                    noResultsLabel = "Aucun rameur ne correspond a la recherche.",
                    onOptionToggled = { optionKey ->
                        optionKey.toLongOrNull()?.let { rowerId ->
                            val checked = !uiState.selectedRowerIds.contains(rowerId)
                            onRowerChecked(rowerId, checked)
                        }
                    },
                )

                if (!uiState.isQuickMode) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))

                    FormGroupTitle("Rameurs invités")
                    SessionOutlinedTextField(
                        value = uiState.guestRowerName,
                        onValueChange = trackedOnGuestRowerNameChanged,
                        label = { Text("Nom complet du rameur invité") },
                        modifier = Modifier.fillMaxWidth(),
                        type = SessionFieldType.SIMPLE,
                    )

                    Button(
                        onClick = onAddGuestRower,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Ajouter un rameur invité")
                    }

                    if (uiState.guestRowers.isEmpty()) {
                        Text(
                            text = "Aucun rameur invité ajouté.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        uiState.guestRowers.forEach { guest ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(guest.fullName)
                                OutlinedButton(onClick = { onRemoveGuestRower(guest.localId) }) {
                                    Text("Retirer")
                                }
                            }
                        }
                    }
                }
            }

            if (!uiState.isQuickMode) {
                SectionCard(
                    title = "Clôture",
                    description = "Ajoutez les informations de fin si la sortie est terminée.",
                ) {
                    FormGroupTitle("Heure de fin")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SessionSelectionButton(
                            onClick = {
                                showEndTimePicker = true
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                if (uiState.endTime.isBlank()) {
                                    "Heure de fin : non définie"
                                } else {
                                    "Heure de fin : ${uiState.endTime}"
                                },
                            )
                        }

                        if (uiState.endTime.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    onEndTimeChanged("")
                                },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text("Effacer")
                            }
                        }
                    }

                    FormGroupTitle("Km")
                    SessionOutlinedTextField(
                        value = uiState.km,
                        onValueChange = trackedOnKmChanged,
                        label = { Text("Km") },
                        modifier = Modifier.fillMaxWidth(),
                        type = SessionFieldType.NUMERIC,
                    )

                    FormGroupTitle("Remarques")
                    SessionOutlinedTextField(
                        value = uiState.remarks,
                        onValueChange = trackedOnRemarksChanged,
                        label = { Text("Remarques") },
                        modifier = Modifier.fillMaxWidth(),
                        type = SessionFieldType.LONG_TEXT,
                    )

                    if (
                        uiState.remarks.isNotBlank() ||
                        uiState.sessionRemarkPhotoPaths.isNotEmpty() ||
                        uiState.sessionRemarkStatus != RemarkStatus.NORMAL
                    ) {
                        OutlinedButton(
                            onClick = { showRemarkStatusOptions = !showRemarkStatusOptions },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (uiState.sessionRemarkStatus == RemarkStatus.NORMAL && !showRemarkStatusOptions) {
                                    "Ajouter un statut"
                                } else {
                                    "Statut : ${uiState.sessionRemarkStatus.displayLabel()}"
                                },
                            )
                        }

                        OutlinedButton(
                            onClick = onAddSessionRemarkPhotos,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (uiState.sessionRemarkPhotoPaths.isEmpty()) {
                                    "Ajouter des photos"
                                } else {
                                    "Ajouter des photos (${uiState.sessionRemarkPhotoPaths.size})"
                                },
                            )
                        }

                        if (uiState.sessionRemarkPhotoPaths.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.sessionRemarkPhotoPaths.forEachIndexed { index, path ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Photo ${index + 1}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        OutlinedButton(onClick = { onRemoveSessionRemarkPhoto(path) }) {
                                            Text("Retirer")
                                        }
                                    }
                                }
                            }
                        }

                        if (showRemarkStatusOptions) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SessionRemarkStatusButton(
                                    label = "Remarque normale",
                                    selected = uiState.sessionRemarkStatus == RemarkStatus.NORMAL,
                                    onClick = {
                                        onSessionRemarkStatusChanged(RemarkStatus.NORMAL)
                                        showRemarkStatusOptions = false
                                    },
                                )
                                SessionRemarkStatusButton(
                                    label = "Réparation nécessaire",
                                    selected = uiState.sessionRemarkStatus == RemarkStatus.REPAIR_NEEDED,
                                    onClick = {
                                        onSessionRemarkStatusChanged(RemarkStatus.REPAIR_NEEDED)
                                        showRemarkStatusOptions = true
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onSaveSession,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    when {
                        uiState.isSaving -> "Enregistrement..."
                        uiState.isQuickMode && !uiState.isEditMode -> "Demarrer la sortie"
                        uiState.isEditMode -> "Enregistrer"
                        uiState.derivedStatus == SessionStatus.COMPLETED -> "Enregistrer la sortie"
                        else -> "Demarrer la sortie"
                    },
                )
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
private fun HeaderRow(
    title: String,
    showReset: Boolean,
    onResetForm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (showReset) {
            OutlinedButton(onClick = onResetForm) {
                Text("Réinitialiser")
            }
        }
    }
}

@Composable
private fun ModeToggleCard(
    isQuickMode: Boolean,
    onToggleQuickMode: () -> Unit,
) {
    SectionCard(
        title = "Mode de saisie",
        description = "Choisissez entre une saisie rapide ou une saisie complete.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val completeButton: @Composable (Modifier) -> Unit = { modifier ->
                if (isQuickMode) {
                    OutlinedButton(
                        onClick = onToggleQuickMode,
                        modifier = modifier,
                    ) {
                        Text("Mode complet")
                    }
                } else {
                    Button(
                        onClick = {},
                        modifier = modifier,
                        enabled = false,
                    ) {
                        Text("Mode complet")
                    }
                }
            }
            val quickButton: @Composable (Modifier) -> Unit = { modifier ->
                if (isQuickMode) {
                    Button(
                        onClick = {},
                        modifier = modifier,
                        enabled = false,
                    ) {
                        Text("Mode rapide")
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleQuickMode,
                        modifier = modifier,
                    ) {
                        Text("Mode rapide")
                    }
                }
            }

            completeButton(Modifier.weight(1f))
            quickButton(Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickModeInfoCard() {
    SectionCard(
        title = "Mode rapide",
        description = "Seuls le bateau et les rameurs sont affichés. L'heure de départ sera définie automatiquement au démarrage.",
    ) {
        Text(
            text = "Le démarrage de la sortie crée directement une sortie en cours.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactSuggestionsSection(
    suggestions: List<SuggestedCrewPatternUi>,
    onApplySuggestion: (Set<Long>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormGroupTitle("Suggestions d'équipage")
        Text(
            text = "Touchez une proposition pour remplir rapidement l'équipage.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        suggestions.take(5).forEach { suggestion ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onApplySuggestion(suggestion.rowerIds) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = suggestion.rowerNames.joinToString(", "),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${suggestion.occurrenceCount} sorties similaires",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "Ajouter",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                content()
            },
        )
    }
}

@Composable
private fun FormGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SessionOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    type: SessionFieldType = SessionFieldType.SIMPLE,
) {
    val keyboardOptions = when (type) {
        SessionFieldType.SIMPLE -> KeyboardOptions(keyboardType = KeyboardType.Text)
        SessionFieldType.NUMERIC -> KeyboardOptions(keyboardType = KeyboardType.Number)
        SessionFieldType.LONG_TEXT -> KeyboardOptions(keyboardType = KeyboardType.Text)
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        singleLine = type != SessionFieldType.LONG_TEXT,
        maxLines = if (type == SessionFieldType.LONG_TEXT) Int.MAX_VALUE else 1,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

private enum class SessionFieldType {
    SIMPLE,
    NUMERIC,
    LONG_TEXT,
}

@Composable
private fun SessionSelectionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    AppSelectorFieldButton(
        onClick = onClick,
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun RowScope.SessionRemarkStatusButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(label)
        }
    }
}

private fun RemarkStatus.displayLabel(): String {
    return when (this) {
        RemarkStatus.NORMAL -> "Remarque normale"
        RemarkStatus.REPAIR_NEEDED -> "Réparation nécessaire"
        RemarkStatus.REPAIRED -> "Réparée"
    }
}
