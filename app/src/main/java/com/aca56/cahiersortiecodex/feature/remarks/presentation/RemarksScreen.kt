package com.aca56.cahiersortiecodex.feature.remarks.presentation

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.ui.components.AppSelectorFieldButton
import com.aca56.cahiersortiecodex.ui.components.AppDatePickerDialog
import com.aca56.cahiersortiecodex.ui.components.DeleteConfirmationDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.SearchableSingleSelectList
import com.aca56.cahiersortiecodex.ui.components.currentStorageDate
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay
import com.aca56.cahiersortiecodex.ui.components.rememberInteractionAwareValueChange
import com.aca56.cahiersortiecodex.ui.components.rememberDismissKeyboardAction

@Composable
fun RemarksRoute(
    contentPadding: PaddingValues,
    initialBoatId: Long? = null,
    autoStartEditor: Boolean = false,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: RemarksViewModel = viewModel(
        factory = RemarksViewModel.factory(
            remarkRepository = appContainer.remarkRepository,
            repairUpdateRepository = appContainer.repairUpdateRepository,
            boatRepository = appContainer.boatRepository,
            sessionRepository = appContainer.sessionRepository,
            boatPhotoStorage = appContainer.boatPhotoStorage,
            initialBoatId = initialBoatId,
            autoStartEditor = autoStartEditor,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::addPhoto) ?: viewModel.clearMessage()
    }

    RemarksScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onBoatFilterSelected = viewModel::onBoatFilterSelected,
        onDateFilterSelected = viewModel::onDateFilterSelected,
        onStartAddingRemark = viewModel::startAddingRemark,
        onCancelEditing = viewModel::cancelEditing,
        onEditRemark = viewModel::startEditingRemark,
        onEditorDateChanged = viewModel::onEditorDateChanged,
        onEditorContentChanged = viewModel::onEditorContentChanged,
        onEditorStatusChanged = viewModel::onEditorStatusChanged,
        onBoatForEditorSelected = viewModel::onBoatForEditorSelected,
        onAddPhoto = { photoPickerLauncher.launch(arrayOf("image/*")) },
        onRemovePhoto = viewModel::removePhoto,
        onSaveRemark = viewModel::saveRemark,
        onDeleteRemark = viewModel::deleteRemark,
        onStartRepairUpdate = viewModel::startRepairUpdate,
        onStartRepairClosure = viewModel::startRepairClosure,
        onCancelRepairUpdate = viewModel::cancelRepairUpdate,
        onRepairUpdateContentChanged = viewModel::onRepairUpdateContentChanged,
        onSaveRepairUpdate = viewModel::saveRepairUpdate,
        onDismissMessage = viewModel::clearMessage,
    )
}

@Composable
fun RemarksScreen(
    contentPadding: PaddingValues,
    uiState: RemarksUiState,
    onBoatFilterSelected: (Long?) -> Unit,
    onDateFilterSelected: (String?) -> Unit,
    onStartAddingRemark: () -> Unit,
    onCancelEditing: () -> Unit,
    onEditRemark: (String) -> Unit,
    onEditorDateChanged: (String) -> Unit,
    onEditorContentChanged: (String) -> Unit,
    onEditorStatusChanged: (RemarkStatus) -> Unit,
    onBoatForEditorSelected: (Long?) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSaveRemark: () -> Unit,
    onDeleteRemark: (String) -> Unit,
    onStartRepairUpdate: (String) -> Unit,
    onStartRepairClosure: (String) -> Unit,
    onCancelRepairUpdate: () -> Unit,
    onRepairUpdateContentChanged: (String) -> Unit,
    onSaveRepairUpdate: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    var showFilterDatePicker by remember { mutableStateOf(false) }
    var showEditorDatePicker by remember { mutableStateOf(false) }
    var remarkPendingDeleteKey by remember { mutableStateOf<String?>(null) }
    var filterBoatSearchQuery by remember { mutableStateOf("") }
    var editorBoatSearchQuery by remember { mutableStateOf("") }
    val dismissKeyboard = rememberDismissKeyboardAction()
    val editingRemark = uiState.editingRemark

    remarkPendingDeleteKey?.let { remarkKey ->
        DeleteConfirmationDialog(
            onConfirm = {
                dismissKeyboard()
                onDeleteRemark(remarkKey)
                remarkPendingDeleteKey = null
            },
            onDismiss = { remarkPendingDeleteKey = null },
        )
    }

    if (showFilterDatePicker) {
        AppDatePickerDialog(
            storageDate = uiState.selectedDateFilter ?: currentStorageDate(),
            onDismissRequest = { showFilterDatePicker = false },
            onDateSelected = onDateFilterSelected,
        )
    }

    if (showEditorDatePicker) {
        AppDatePickerDialog(
            storageDate = uiState.editorDateInput,
            onDismissRequest = { showEditorDatePicker = false },
            onDateSelected = onEditorDateChanged,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Remarques",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Filtres",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    SearchableSingleSelectList(
                        searchQuery = filterBoatSearchQuery,
                        onSearchQueryChanged = { filterBoatSearchQuery = it },
                        searchLabel = "Rechercher un bateau",
                        selectedKey = uiState.selectedBoatId?.toString(),
                        options = uiState.availableBoatOptions.filter { option ->
                            filterBoatSearchQuery.isBlank() || option.label.contains(filterBoatSearchQuery.trim(), ignoreCase = true)
                        },
                        emptyLabel = "Aucun bateau disponible.",
                        noResultsLabel = "Aucun bateau ne correspond à la recherche.",
                        onOptionSelected = { selectedKey ->
                            dismissKeyboard()
                            onBoatFilterSelected(selectedKey.toLongOrNull())
                        },
                    )

                    if (uiState.selectedBoatId != null) {
                        OutlinedButton(
                            onClick = {
                                dismissKeyboard()
                                onBoatFilterSelected(null)
                                filterBoatSearchQuery = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Effacer le bateau")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppSelectorFieldButton(
                            onClick = {
                                dismissKeyboard()
                                showFilterDatePicker = true
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = uiState.selectedDateFilter?.let { "Date : ${formatDateForDisplay(it)}" }
                                    ?: "Toutes les dates",
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                dismissKeyboard()
                                onDateFilterSelected(null)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Effacer la date")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    dismissKeyboard()
                    if (uiState.isEditorVisible && !uiState.isEditing) {
                        onCancelEditing()
                    } else {
                        onStartAddingRemark()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (uiState.isEditorVisible && !uiState.isEditing) {
                        "Masquer l'ajout de remarque"
                    } else {
                        "Ajouter une remarque"
                    },
                )
            }

            if (uiState.isEditorVisible && !uiState.isEditing) {
                RemarkEditorCard(
                    uiState = uiState,
                    editingRemark = editingRemark,
                    editorBoatSearchQuery = editorBoatSearchQuery,
                    onEditorBoatSearchQueryChanged = { editorBoatSearchQuery = it },
                    onEditorDateClick = {
                        dismissKeyboard()
                        showEditorDatePicker = true
                    },
                    onEditorStatusChanged = onEditorStatusChanged,
                    onEditorContentChanged = onEditorContentChanged,
                    onBoatForEditorSelected = { boatId ->
                        dismissKeyboard()
                        onBoatForEditorSelected(boatId)
                    },
                    onAddPhoto = {
                        dismissKeyboard()
                        onAddPhoto()
                    },
                    onRemovePhoto = {
                        dismissKeyboard()
                        onRemovePhoto()
                    },
                    onClearEditorBoat = {
                        dismissKeyboard()
                        onBoatForEditorSelected(null)
                        editorBoatSearchQuery = ""
                    },
                    onCancelEditing = {
                        dismissKeyboard()
                        onCancelEditing()
                    },
                    onSaveRemark = {
                        dismissKeyboard()
                        onSaveRemark()
                    },
                )
            }

            if (uiState.filteredRemarks.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = "Aucune remarque ne correspond aux filtres actuels.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                uiState.filteredRemarks.forEach { remark ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = if (remark.status == RemarkStatus.REPAIR_NEEDED) 4.dp else 2.dp,
                        color = if (remark.status == RemarkStatus.REPAIR_NEEDED) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (remark.status == RemarkStatus.REPAIR_NEEDED) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = formatDateForDisplay(remark.date),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = remark.sourceLabel,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Text(
                                    text = remark.boatName.ifBlank { "Aucun bateau" },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Text(
                                text = remark.content,
                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Text(
                                text = remark.status.displayLabel(),
                                color = remark.statusColor(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )

                            remark.photoPath?.let { photoPath ->
                                RemarkPhotoPreview(photoPath = photoPath)
                            }

                            uiState.repairUpdatesByRemarkId[remark.id].orEmpty().forEach { update ->
                                RepairUpdateCard(update = update)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        dismissKeyboard()
                                        onEditRemark(remark.key)
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Modifier")
                                }
                                OutlinedButton(
                                    onClick = {
                                        dismissKeyboard()
                                        remarkPendingDeleteKey = remark.key
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Supprimer")
                                }
                            }

                            if (remark.source == RemarkSource.STANDALONE && remark.status == RemarkStatus.REPAIR_NEEDED) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            dismissKeyboard()
                                            onStartRepairUpdate(remark.key)
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Ajouter un suivi")
                                    }
                                    Button(
                                        onClick = {
                                            dismissKeyboard()
                                            onStartRepairClosure(remark.key)
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Marquer comme réparé")
                                    }
                                }
                            }

                            if (uiState.repairUpdateRemarkKey == remark.key) {
                                RepairUpdateEditorCard(
                                    mode = uiState.repairUpdateMode,
                                    content = uiState.repairUpdateContentInput,
                                    photoPath = uiState.repairUpdatePhotoPath,
                                    isSaving = uiState.isSaving,
                                    onContentChanged = onRepairUpdateContentChanged,
                                    onAddPhoto = {
                                        dismissKeyboard()
                                        onAddPhoto()
                                    },
                                    onRemovePhoto = {
                                        dismissKeyboard()
                                        onRemovePhoto()
                                    },
                                    onCancel = {
                                        dismissKeyboard()
                                        onCancelRepairUpdate()
                                    },
                                    onSave = {
                                        dismissKeyboard()
                                        onSaveRepairUpdate()
                                    },
                                )
                            }

                            if (uiState.isEditorVisible && uiState.editingRemarkKey == remark.key) {
                                RemarkEditorCard(
                                    uiState = uiState,
                                    editingRemark = editingRemark,
                                    editorBoatSearchQuery = editorBoatSearchQuery,
                                    onEditorBoatSearchQueryChanged = { editorBoatSearchQuery = it },
                                    onEditorDateClick = {
                                        dismissKeyboard()
                                        showEditorDatePicker = true
                                    },
                                    onEditorContentChanged = onEditorContentChanged,
                                    onEditorStatusChanged = onEditorStatusChanged,
                                    onBoatForEditorSelected = { boatId ->
                                        dismissKeyboard()
                                        onBoatForEditorSelected(boatId)
                                    },
                                    onAddPhoto = {
                                        dismissKeyboard()
                                        onAddPhoto()
                                    },
                                    onRemovePhoto = {
                                        dismissKeyboard()
                                        onRemovePhoto()
                                    },
                                    onClearEditorBoat = {
                                        dismissKeyboard()
                                        onBoatForEditorSelected(null)
                                        editorBoatSearchQuery = ""
                                    },
                                    onCancelEditing = {
                                        dismissKeyboard()
                                        onCancelEditing()
                                    },
                                    onSaveRemark = {
                                        dismissKeyboard()
                                        onSaveRemark()
                                    },
                                )
                            }
                        }
                    }
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
private fun RemarkEditorCard(
    uiState: RemarksUiState,
    editingRemark: RemarkItemUi?,
    editorBoatSearchQuery: String,
    onEditorBoatSearchQueryChanged: (String) -> Unit,
    onEditorDateClick: () -> Unit,
    onEditorContentChanged: (String) -> Unit,
    onEditorStatusChanged: (RemarkStatus) -> Unit,
    onBoatForEditorSelected: (Long?) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onClearEditorBoat: () -> Unit,
    onCancelEditing: () -> Unit,
    onSaveRemark: () -> Unit,
) {
    val trackedOnEditorContentChanged = rememberInteractionAwareValueChange(onEditorContentChanged)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (uiState.isEditing) "Modifier la remarque" else "Ajouter une remarque",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = if (editingRemark?.source == RemarkSource.SESSION) {
                    "Cette remarque est liée à une session, donc son bateau et sa date proviennent de cette session."
                } else {
                    "Utilisez ceci pour les réparations, incidents ou notes hors session."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (editingRemark?.source == RemarkSource.SESSION) {
                Text(
                    text = "Date : ${formatDateForDisplay(uiState.editorDateInput)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Bateau : ${editingRemark.boatName.ifBlank { "Aucun bateau" }}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                AppSelectorFieldButton(
                    onClick = onEditorDateClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Date : ${formatDateForDisplay(uiState.editorDateInput)}")
                }

                SearchableSingleSelectList(
                    searchQuery = editorBoatSearchQuery,
                    onSearchQueryChanged = onEditorBoatSearchQueryChanged,
                    searchLabel = "Rechercher un bateau",
                    selectedKey = uiState.selectedBoatForEditorId?.toString(),
                    options = uiState.availableBoatOptions.filter { option ->
                        editorBoatSearchQuery.isBlank() || option.label.contains(editorBoatSearchQuery.trim(), ignoreCase = true)
                    },
                    emptyLabel = "Aucun bateau disponible.",
                    noResultsLabel = "Aucun bateau ne correspond à la recherche.",
                    onOptionSelected = { selectedKey ->
                        onBoatForEditorSelected(selectedKey.toLongOrNull())
                    },
                )

                if (uiState.selectedBoatForEditorId != null) {
                    OutlinedButton(
                        onClick = onClearEditorBoat,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Effacer le bateau")
                    }
                }
            }

            OutlinedTextField(
                value = uiState.editorContentInput,
                onValueChange = trackedOnEditorContentChanged,
                label = { Text("Contenu") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            if (editingRemark?.source != RemarkSource.SESSION) {
                Text(
                    text = "Statut",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RemarkStatusButton(
                        label = "Normale",
                        selected = uiState.editorStatus == RemarkStatus.NORMAL,
                        onClick = { onEditorStatusChanged(RemarkStatus.NORMAL) },
                    )
                    RemarkStatusButton(
                        label = "Réparation nécessaire",
                        selected = uiState.editorStatus == RemarkStatus.REPAIR_NEEDED,
                        onClick = { onEditorStatusChanged(RemarkStatus.REPAIR_NEEDED) },
                    )
                    RemarkStatusButton(
                        label = "Réparée",
                        selected = uiState.editorStatus == RemarkStatus.REPAIRED,
                        onClick = { onEditorStatusChanged(RemarkStatus.REPAIRED) },
                    )
                }

                uiState.editorPhotoPath?.let { photoPath ->
                    RemarkPhotoPreview(photoPath = photoPath)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onAddPhoto,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (uiState.editorPhotoPath == null) "Ajouter une photo" else "Changer la photo")
                    }
                    if (uiState.editorPhotoPath != null) {
                        OutlinedButton(
                            onClick = onRemovePhoto,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Retirer la photo")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancelEditing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Annuler")
                }
                Button(
                    onClick = onSaveRemark,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (uiState.isSaving) "Enregistrement..." else "Enregistrer")
                }
            }
        }
    }
}

@Composable
private fun RowScope.RemarkStatusButton(
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

@Composable
private fun RemarkPhotoPreview(photoPath: String) {
    val bitmap = remember(photoPath) { BitmapFactory.decodeFile(photoPath) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Photo de la remarque",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.FillWidth,
        )
    }
}

@Composable
private fun RepairUpdateCard(update: RepairUpdateItemUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = update.createdAt,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = update.content,
                style = MaterialTheme.typography.bodyMedium,
            )
            update.photoPath?.let { photoPath ->
                RemarkPhotoPreview(photoPath = photoPath)
            }
        }
    }
}

@Composable
private fun RepairUpdateEditorCard(
    mode: RepairUpdateMode,
    content: String,
    photoPath: String?,
    isSaving: Boolean,
    onContentChanged: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val trackedOnContentChanged = rememberInteractionAwareValueChange(onContentChanged)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (mode == RepairUpdateMode.CLOSE_REPAIR) {
                    "Clôturer la réparation"
                } else {
                    "Ajouter un suivi"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = content,
                onValueChange = trackedOnContentChanged,
                label = {
                    Text(
                        if (mode == RepairUpdateMode.CLOSE_REPAIR) {
                            "Commentaire final"
                        } else {
                            "Commentaire de suivi"
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            photoPath?.let { path ->
                RemarkPhotoPreview(photoPath = path)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onAddPhoto,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (photoPath == null) "Ajouter une photo" else "Changer la photo")
                }
                if (photoPath != null) {
                    OutlinedButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Retirer la photo")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Annuler")
                }
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isSaving) "Enregistrement..." else "Valider")
                }
            }
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

@Composable
private fun RemarkStatusColor(status: RemarkStatus) = when (status) {
    RemarkStatus.NORMAL -> MaterialTheme.colorScheme.primary
    RemarkStatus.REPAIR_NEEDED -> MaterialTheme.colorScheme.error
    RemarkStatus.REPAIRED -> MaterialTheme.colorScheme.tertiary
}

@Composable
private fun RemarkItemUi.statusColor() = RemarkStatusColor(status)
