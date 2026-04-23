package com.aca56.cahiersortiecodex.feature.newsession.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.data.local.entity.SessionStatus
import com.aca56.cahiersortiecodex.ui.components.AppSelectorFieldButton
import com.aca56.cahiersortiecodex.ui.components.AppDatePickerDialog
import com.aca56.cahiersortiecodex.ui.components.AppTimePickerDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.SearchableSelectableList
import com.aca56.cahiersortiecodex.ui.components.SearchableSingleSelectList
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay
import com.aca56.cahiersortiecodex.ui.components.rememberDoneKeyboardActions
import com.aca56.cahiersortiecodex.ui.components.rememberInteractionAwareValueChange
import com.aca56.cahiersortiecodex.ui.components.rememberDismissKeyboardAction

@Composable
fun NewSessionRoute(
    contentPadding: PaddingValues,
    onSessionSaved: (SessionStatus) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: NewSessionViewModel = viewModel(
        factory = NewSessionViewModel.factory(
            boatRepository = appContainer.boatRepository,
            destinationRepository = appContainer.destinationRepository,
            rowerRepository = appContainer.rowerRepository,
            sessionRepository = appContainer.sessionRepository,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    NewSessionScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        screenTitle = "Créer une session",
        onDateChanged = viewModel::onDateChanged,
        onBoatSelected = viewModel::onBoatSelected,
        onStartTimeChanged = viewModel::onStartTimeChanged,
        onEndTimeChanged = viewModel::onEndTimeChanged,
        onKmChanged = viewModel::onKmChanged,
        onRemarksChanged = viewModel::onRemarksChanged,
        onDestinationSelected = viewModel::onDestinationSelected,
        onCustomDestinationSelected = viewModel::onCustomDestinationSelected,
        onDestinationChanged = viewModel::onDestinationChanged,
        onRowerChecked = viewModel::onRowerChecked,
        onRowerSearchQueryChanged = viewModel::onRowerSearchQueryChanged,
        onGuestRowerNameChanged = viewModel::onGuestRowerNameChanged,
        onAddGuestRower = viewModel::addGuestRower,
        onRemoveGuestRower = viewModel::removeGuestRower,
        onSaveSession = viewModel::saveSession,
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
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: NewSessionViewModel = viewModel(
        key = "edit_session_$sessionId",
        factory = NewSessionViewModel.factory(
            boatRepository = appContainer.boatRepository,
            destinationRepository = appContainer.destinationRepository,
            rowerRepository = appContainer.rowerRepository,
            sessionRepository = appContainer.sessionRepository,
            sessionId = sessionId,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    NewSessionScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        screenTitle = "Modifier la session",
        onDateChanged = viewModel::onDateChanged,
        onBoatSelected = viewModel::onBoatSelected,
        onStartTimeChanged = viewModel::onStartTimeChanged,
        onEndTimeChanged = viewModel::onEndTimeChanged,
        onKmChanged = viewModel::onKmChanged,
        onRemarksChanged = viewModel::onRemarksChanged,
        onDestinationSelected = viewModel::onDestinationSelected,
        onCustomDestinationSelected = viewModel::onCustomDestinationSelected,
        onDestinationChanged = viewModel::onDestinationChanged,
        onRowerChecked = viewModel::onRowerChecked,
        onRowerSearchQueryChanged = viewModel::onRowerSearchQueryChanged,
        onGuestRowerNameChanged = viewModel::onGuestRowerNameChanged,
        onAddGuestRower = viewModel::addGuestRower,
        onRemoveGuestRower = viewModel::removeGuestRower,
        onSaveSession = viewModel::saveSession,
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
    onStartTimeChanged: (String) -> Unit,
    onEndTimeChanged: (String) -> Unit,
    onKmChanged: (String) -> Unit,
    onRemarksChanged: (String) -> Unit,
    onDestinationSelected: (Long?) -> Unit,
    onCustomDestinationSelected: () -> Unit,
    onDestinationChanged: (String) -> Unit,
    onRowerChecked: (Long, Boolean) -> Unit,
    onRowerSearchQueryChanged: (String) -> Unit,
    onGuestRowerNameChanged: (String) -> Unit,
    onAddGuestRower: () -> Unit,
    onRemoveGuestRower: (Long) -> Unit,
    onSaveSession: () -> Unit,
    onDismissFeedback: () -> Unit,
) {
    var destinationMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var boatSearchQuery by remember { mutableStateOf("") }
    val dismissKeyboard = rememberDismissKeyboardAction()
    val trackedOnKmChanged = rememberInteractionAwareValueChange(onKmChanged)
    val trackedOnRemarksChanged = rememberInteractionAwareValueChange(onRemarksChanged)
    val trackedOnDestinationChanged = rememberInteractionAwareValueChange(onDestinationChanged)
    val trackedOnGuestRowerNameChanged = rememberInteractionAwareValueChange(onGuestRowerNameChanged)
    val selectedBoatLabel = uiState.selectedBoat?.let { "${it.name} (${it.seatCount} places)" }
        ?: "Choisir un bateau"
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = screenTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            SectionCard(
                title = "Détails de la session",
                description = "Choisissez la date, les horaires et la destination de la session.",
            ) {
                FormGroupTitle("Date")
                AppSelectorFieldButton(
                    onClick = {
                        dismissKeyboard()
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Date : ${formatDateForDisplay(uiState.date)}")
                }

                FormGroupTitle("Heure de départ")
                SessionSelectionButton(
                    onClick = {
                        dismissKeyboard()
                        showTimePicker = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Heure de départ : ${uiState.startTime}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                FormGroupTitle("Heure de fin")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SessionSelectionButton(
                        onClick = {
                            dismissKeyboard()
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
                                dismissKeyboard()
                                onEndTimeChanged("")
                            },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text("Effacer l'heure de fin")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FormGroupTitle("Destination")
                Box(modifier = Modifier.fillMaxWidth()) {
                    SessionSelectionButton(
                        onClick = {
                            dismissKeyboard()
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
                                dismissKeyboard()
                                onDestinationSelected(null)
                                destinationMenuExpanded = false
                            },
                        )
                        uiState.availableDestinations.forEach { destination ->
                            DropdownMenuItem(
                                text = { Text(destination.name) },
                                onClick = {
                                    dismissKeyboard()
                                    onDestinationSelected(destination.id)
                                    destinationMenuExpanded = false
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Autre") },
                            onClick = {
                                dismissKeyboard()
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
                    label = { Text("Destination personnalisée") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                }
            }

            SectionCard(
                title = "Bateau et équipage",
                description = "Choisissez le bateau et les rameurs pour cette session.",
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
                    noResultsLabel = "Aucun bateau ne correspond à la recherche.",
                    onOptionSelected = { selectedKey ->
                        dismissKeyboard()
                        selectedKey.toLongOrNull()?.let(onBoatSelected)
                    },
                )

                if (uiState.availableBoats.isEmpty()) {
                    Text(
                        text = "Aucun bateau disponible pour le moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(12.dp))

                FormGroupTitle("Rameurs")
                Text(
                    text = buildString {
                        append("Sélectionnés : ${uiState.totalSelectedRowers}")
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
                    emptyLabel = "Aucun rameur enregistré pour le moment.",
                    noResultsLabel = "Aucun rameur ne correspond à la recherche.",
                    onOptionToggled = { optionKey ->
                        dismissKeyboard()
                        optionKey.toLongOrNull()?.let { rowerId ->
                            val checked = !uiState.selectedRowerIds.contains(rowerId)
                            onRowerChecked(rowerId, checked)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(12.dp))

                FormGroupTitle("Rameurs invités")

                SessionOutlinedTextField(
                    value = uiState.guestRowerName,
                    onValueChange = trackedOnGuestRowerNameChanged,
                    label = { Text("Nom complet du rameur invité") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Button(
                    onClick = {
                        dismissKeyboard()
                        onAddGuestRower()
                    },
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
                            OutlinedButton(
                                onClick = {
                                    dismissKeyboard()
                                    onRemoveGuestRower(guest.localId)
                                },
                            ) {
                                Text("Retirer")
                            }
                        }
                    }
                }
            }

            SectionCard(
                title = "Clôture de la session",
                description = "Ajoutez les informations de clôture si la session est terminée.",
            ) {
                Text(
                    text = if (uiState.derivedStatus.name == "COMPLETED") {
                        "Cette session sera enregistrée comme TERMINÉE."
                    } else {
                        "Cette session sera enregistrée comme EN COURS."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                FormGroupTitle("Km")
                SessionOutlinedTextField(
                    value = uiState.km,
                    onValueChange = trackedOnKmChanged,
                    label = { Text("Km") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormGroupTitle("Remarques")
                SessionOutlinedTextField(
                    value = uiState.remarks,
                    onValueChange = trackedOnRemarksChanged,
                    label = { Text("Remarques") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }

            Button(
                onClick = {
                    dismissKeyboard()
                    onSaveSession()
                },
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    (if (uiState.isSaving) {
                        "Enregistrement..."
                    } else if (uiState.isEditMode) {
                        "Enregistrer les modifications"
                    } else if (uiState.derivedStatus.name == "COMPLETED") {
                        "Enregistrer la session comme TERMINÉE"
                    } else {
                        "Enregistrer la session comme EN COURS"
                    }).toString(),
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
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val keyboardActions = rememberDoneKeyboardActions()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
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

