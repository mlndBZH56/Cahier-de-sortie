package com.aca56.cahiersortiecodex.feature.remarks.presentation

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
        contract = OpenMultipleDocuments(),
    ) { uris ->
        uris.forEach(viewModel::addPhoto)
        if (uris.isEmpty()) {
            viewModel.clearMessage()
        }
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
        onEditRepairUpdate = viewModel::startEditingRepairUpdate,
        onDeleteRepairUpdate = viewModel::deleteRepairUpdate,
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
    onRemovePhoto: (String?) -> Unit,
    onSaveRemark: () -> Unit,
    onDeleteRemark: (String) -> Unit,
    onStartRepairUpdate: (String) -> Unit,
    onStartRepairClosure: (String) -> Unit,
    onEditRepairUpdate: (Long) -> Unit,
    onDeleteRepairUpdate: (Long) -> Unit,
    onCancelRepairUpdate: () -> Unit,
    onRepairUpdateContentChanged: (String) -> Unit,
    onSaveRepairUpdate: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    var showFilterDatePicker by remember { mutableStateOf(false) }
    var showEditorDatePicker by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }
    var remarkPendingDeleteKey by remember { mutableStateOf<String?>(null) }
    var repairUpdatePendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var selectedRemarkForDetailsKey by remember { mutableStateOf<String?>(null) }
    var selectedRepairUpdateForDetailsId by remember { mutableStateOf<Long?>(null) }
    var filterBoatSearchQuery by remember { mutableStateOf("") }
    var editorBoatSearchQuery by remember { mutableStateOf("") }
    val editingRemark = uiState.editingRemark
    val selectedRemarkForDetails = uiState.remarks.firstOrNull { it.key == selectedRemarkForDetailsKey }
    val selectedRepairUpdateForDetails = uiState.repairUpdatesByRemarkId.values
        .flatten()
        .firstOrNull { it.id == selectedRepairUpdateForDetailsId }

    remarkPendingDeleteKey?.let { remarkKey ->
        DeleteConfirmationDialog(
            onConfirm = {
                onDeleteRemark(remarkKey)
                remarkPendingDeleteKey = null
            },
            onDismiss = { remarkPendingDeleteKey = null },
        )
    }

    repairUpdatePendingDeleteId?.let { updateId ->
        AlertDialog(
            onDismissRequest = { repairUpdatePendingDeleteId = null },
            title = { Text("Confirmer la suppression ?") },
            text = { Text("Cette action supprimera définitivement le suivi.") },
            dismissButton = {
                TextButton(onClick = { repairUpdatePendingDeleteId = null }) {
                    Text("Annuler")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRepairUpdate(updateId)
                        repairUpdatePendingDeleteId = null
                    },
                ) {
                    Text("Supprimer")
                }
            },
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

    if (showFiltersDialog) {
        AppModalDialog(
            title = "Filtres",
            onDismiss = { showFiltersDialog = false },
        ) {
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
                    onBoatFilterSelected(selectedKey.toLongOrNull())
                },
            )

            if (uiState.selectedBoatId != null) {
                OutlinedButton(
                    onClick = {
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
                    onClick = { showFilterDatePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = uiState.selectedDateFilter?.let { "Date : ${formatDateForDisplay(it)}" }
                            ?: "Toutes les dates",
                    )
                }
                OutlinedButton(
                    onClick = { onDateFilterSelected(null) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Effacer la date")
                }
            }

            Button(
                onClick = { showFiltersDialog = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fermer")
            }
        }
    }

    if (uiState.isEditorVisible) {
        AppModalDialog(
            title = if (uiState.isEditing) "Modifier la remarque" else "Ajouter une remarque",
            onDismiss = onCancelEditing,
        ) {
            RemarkEditorCard(
                uiState = uiState,
                editingRemark = editingRemark,
                editorBoatSearchQuery = editorBoatSearchQuery,
                onEditorBoatSearchQueryChanged = { editorBoatSearchQuery = it },
                onEditorDateClick = { showEditorDatePicker = true },
                onEditorContentChanged = onEditorContentChanged,
                onEditorStatusChanged = onEditorStatusChanged,
                onBoatForEditorSelected = onBoatForEditorSelected,
                onAddPhoto = onAddPhoto,
                onRemovePhoto = onRemovePhoto,
                onClearEditorBoat = {
                    onBoatForEditorSelected(null)
                    editorBoatSearchQuery = ""
                },
                onCancelEditing = onCancelEditing,
                onSaveRemark = onSaveRemark,
            )
        }
    }

    if (uiState.repairUpdateRemarkKey != null) {
        AppModalDialog(
            title = if (uiState.repairUpdateMode == RepairUpdateMode.CLOSE_REPAIR) {
                "Clôturer la réparation"
            } else if (uiState.editingRepairUpdateId != null) {
                "Modifier le suivi"
            } else {
                "Ajouter un suivi"
            },
            onDismiss = onCancelRepairUpdate,
        ) {
            RepairUpdateEditorCard(
                mode = uiState.repairUpdateMode,
                content = uiState.repairUpdateContentInput,
                photoPath = uiState.repairUpdatePhotoPath,
                isSaving = uiState.isSaving,
                onContentChanged = onRepairUpdateContentChanged,
                onAddPhoto = onAddPhoto,
                onRemovePhoto = { onRemovePhoto(null) },
                onCancel = onCancelRepairUpdate,
                onSave = onSaveRepairUpdate,
            )
        }
    }

    selectedRemarkForDetails?.let { remark ->
        RemarkDetailsDialog(
            remark = remark,
            repairUpdates = uiState.repairUpdatesByRemarkId[remark.id].orEmpty(),
            onDismiss = { selectedRemarkForDetailsKey = null },
            onEditRepairUpdate = { updateId ->
                selectedRemarkForDetailsKey = null
                onEditRepairUpdate(updateId)
            },
            onDeleteRepairUpdate = { updateId ->
                selectedRemarkForDetailsKey = null
                repairUpdatePendingDeleteId = updateId
            },
            onViewRepairUpdatePhotos = { updateId -> selectedRepairUpdateForDetailsId = updateId },
        )
    }

    selectedRepairUpdateForDetails?.let { update ->
        RepairUpdatePhotosDialog(
            update = update,
            onDismiss = { selectedRepairUpdateForDetailsId = null },
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

            OutlinedButton(
                onClick = { showFiltersDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ouvrir les filtres")
            }

            Button(
                onClick = {
                    if (uiState.isEditorVisible && !uiState.isEditing) onCancelEditing() else onStartAddingRemark()
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

                            if (remark.photoPaths.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { selectedRemarkForDetailsKey = remark.key },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Voir les photos")
                                }
                            }

                            uiState.repairUpdatesByRemarkId[remark.id].orEmpty().forEach { update ->
                                RepairUpdateCard(
                                    update = update,
                                    onEdit = { onEditRepairUpdate(update.id) },
                                    onDelete = { repairUpdatePendingDeleteId = update.id },
                                    onViewPhotos = { selectedRepairUpdateForDetailsId = update.id },
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onEditRemark(remark.key) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Modifier")
                                }
                                OutlinedButton(
                                    onClick = { remarkPendingDeleteKey = remark.key },
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
                                        onClick = { onStartRepairUpdate(remark.key) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Ajouter un suivi")
                                    }
                                    Button(
                                        onClick = { onStartRepairClosure(remark.key) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Marquer comme réparé")
                                    }
                                }
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
    onRemovePhoto: (String?) -> Unit,
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                ),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onAddPhoto,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Ajouter une photo")
                    }
                    if (uiState.editorPhotoPaths.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onRemovePhoto(uiState.editorPhotoPaths.last()) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Retirer la dernière photo")
                        }
                    }
                }
                if (uiState.editorPhotoPaths.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.editorPhotoPaths.forEachIndexed { index, path ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Photo ${index + 1}")
                                OutlinedButton(onClick = { onRemovePhoto(path) }) {
                                    Text("Retirer")
                                }
                            }
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
private fun RepairUpdateCard(
    update: RepairUpdateItemUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewPhotos: () -> Unit,
) {
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
            if (update.photoPath != null) {
                OutlinedButton(
                    onClick = onViewPhotos,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Voir les photos")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("Modifier")
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text("Supprimer")
                }
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                ),
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

@Composable
private fun RemarkDetailsDialog(
    remark: RemarkItemUi,
    repairUpdates: List<RepairUpdateItemUi>,
    onDismiss: () -> Unit,
    onEditRepairUpdate: (Long) -> Unit,
    onDeleteRepairUpdate: (Long) -> Unit,
    onViewRepairUpdatePhotos: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Détails de la remarque") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Date : ${formatDateForDisplay(remark.date)}")
                Text("Bateau : ${remark.boatName.ifBlank { "Aucun bateau" }}")
                Text("Statut : ${remark.status.displayLabel()}")
                Text(remark.content)

                if (remark.photoPaths.isNotEmpty()) {
                    Text(
                        text = "Photos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    remark.photoPaths.forEach { photoPath ->
                        RemarkPhotoPreview(photoPath = photoPath)
                    }
                }

                if (repairUpdates.isNotEmpty()) {
                    Text(
                        text = "Suivis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    repairUpdates.forEach { update ->
                        RepairUpdateCard(
                            update = update,
                            onEdit = { onEditRepairUpdate(update.id) },
                            onDelete = { onDeleteRepairUpdate(update.id) },
                            onViewPhotos = { onViewRepairUpdatePhotos(update.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
    )
}

@Composable
private fun RepairUpdatePhotosDialog(
    update: RepairUpdateItemUi,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photos du suivi") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(update.createdAt)
                Text(update.content)
                update.photoPath?.let { path ->
                    RemarkPhotoPreview(photoPath = path)
                } ?: Text("Aucune photo associée.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
    )
}

@Composable
private fun AppModalDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                content()
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
