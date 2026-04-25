package com.aca56.cahiersortiecodex.feature.boats.presentation

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.data.export.DatabaseCsvExporter
import com.aca56.cahiersortiecodex.data.local.entity.BoatEntity
import com.aca56.cahiersortiecodex.data.local.entity.decodeRemarkPhotoPaths
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.ui.components.AppImageViewerDialog
import com.aca56.cahiersortiecodex.ui.components.DeleteConfirmationDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
import com.aca56.cahiersortiecodex.ui.components.PhotoSourceChooserDialog
import com.aca56.cahiersortiecodex.ui.components.formatDateForDisplay
import com.aca56.cahiersortiecodex.ui.components.rememberInteractionAwareValueChange

@Composable
fun BoatsRoute(
    contentPadding: PaddingValues,
    onOpenBoat: (Long?) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: BoatsViewModel = viewModel(
        factory = BoatsViewModel.factory(
            boatRepository = appContainer.boatRepository,
            remarkRepository = appContainer.remarkRepository,
            sessionRepository = appContainer.sessionRepository,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    BoatsScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onDismissMessage = viewModel::clearMessage,
        onOpenBoat = onOpenBoat,
    )
}

@Composable
fun BoatsScreen(
    contentPadding: PaddingValues,
    uiState: BoatsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onOpenBoat: (Long?) -> Unit,
) {
    val trackedOnSearchChanged = rememberInteractionAwareValueChange(onSearchQueryChanged)
    val isCompactScreen = LocalConfiguration.current.screenWidthDp < 600
    val horizontalPadding = if (isCompactScreen) 12.dp else 20.dp
    val verticalPadding = if (isCompactScreen) 12.dp else 18.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Bateaux",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = trackedOnSearchChanged,
                    label = { Text("Rechercher un bateau") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                    ),
                    singleLine = true,
                    maxLines = 1,
                )
            }
        }

        if (uiState.isLoading) {
            Text(
                text = "Chargement des bateaux...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (uiState.filteredBoats.isEmpty()) {
            Text(
                text = "Aucun bateau ne correspond à la recherche actuelle.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                uiState.filteredBoats.forEach { boat ->
                    BoatListCard(
                        boat = boat,
                        onClick = { onOpenBoat(boat.id) },
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
private fun BoatListCard(
    boat: BoatListItemUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = boat.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "État : ${boat.status.label()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BoatStatusBadge(status = boat.status)
        }
    }
}

@Composable
fun BoatDetailRoute(
    contentPadding: PaddingValues,
    boatId: Long?,
    onOpenSession: (Long) -> Unit,
    onOpenFullHistory: (Long) -> Unit,
    onOpenBoatRemarks: (Long) -> Unit,
    onAddBoatRemark: (Long) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as CahierSortieApplication).appContainer
    val viewModel: BoatDetailViewModel = viewModel(
        key = "boat_detail_${boatId ?: "new"}",
        factory = BoatDetailViewModel.factory(
            boatId = boatId,
            boatRepository = appContainer.boatRepository,
            boatPhotoRepository = appContainer.boatPhotoRepository,
            remarkRepository = appContainer.remarkRepository,
            repairUpdateRepository = appContainer.repairUpdateRepository,
            sessionRepository = appContainer.sessionRepository,
            boatPhotoStorage = appContainer.boatPhotoStorage,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()
    var showPhotoSourceChooser by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPhotos(uris)
        } else {
            viewModel.clearMessage()
        }
    }
    val photoCameraLauncher = rememberLauncherForActivityResult(
        contract = TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.addPhoto(bitmap)
        } else {
            viewModel.clearMessage()
        }
    }
    val boatSheetExportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null && uiState.boat.hasPersistentBoat) {
            DatabaseCsvExporter.exportSingleBoatSheet(
                contentResolver = context.contentResolver,
                uri = uri,
                boat = BoatEntity(
                    id = uiState.boat.id,
                    name = uiState.boat.name,
                    seatCount = uiState.boat.seatCount.toIntOrNull() ?: 0,
                    type = uiState.boat.type,
                    weightRange = uiState.boat.weightRangeDisplay.takeUnless { it == "Non défini" }.orEmpty(),
                    riggingType = uiState.boat.riggingTypeDisplay.takeUnless { it == "Non défini" }.orEmpty(),
                    year = uiState.boat.year.toIntOrNull(),
                    notes = uiState.boat.notes,
                ),
                remarks = uiState.boat.remarks,
                repairUpdates = uiState.boat.repairUpdatesByRemarkId.values.flatten(),
                boatPhotos = uiState.boat.photos.map { it.filePath },
                sessions = uiState.boat.allSessionDetails,
            )
            viewModel.clearMessage()
        } else {
            viewModel.clearMessage()
        }
    }

    if (showPhotoSourceChooser) {
        PhotoSourceChooserDialog(
            onDismiss = { showPhotoSourceChooser = false },
            onTakePhoto = {
                showPhotoSourceChooser = false
                photoCameraLauncher.launch(null)
            },
            onPickFromGallery = {
                showPhotoSourceChooser = false
                photoPickerLauncher.launch(arrayOf("image/*"))
            },
        )
    }

    BoatDetailScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onNameChanged = viewModel::onNameChanged,
        onSeatCountChanged = viewModel::onSeatCountChanged,
        onTypeChanged = viewModel::onTypeChanged,
        onWeightSingleChanged = viewModel::onWeightSingleChanged,
        onWeightMinChanged = viewModel::onWeightMinChanged,
        onWeightMaxChanged = viewModel::onWeightMaxChanged,
        onUseWeightRangeChanged = viewModel::onUseWeightRangeChanged,
        onRiggingCoupleChanged = viewModel::onRiggingCoupleChanged,
        onRiggingPointeChanged = viewModel::onRiggingPointeChanged,
        onYearChanged = viewModel::onYearChanged,
        onNotesChanged = viewModel::onNotesChanged,
        onStartEditing = viewModel::startEditing,
        onCancelEditing = viewModel::cancelEditing,
        onSaveBoat = viewModel::saveBoat,
        onDeletePhoto = viewModel::deletePhoto,
        onAddPhoto = { showPhotoSourceChooser = true },
        onOpenSession = onOpenSession,
        onOpenFullHistory = onOpenFullHistory,
        onOpenBoatRemarks = onOpenBoatRemarks,
        onAddBoatRemark = onAddBoatRemark,
        onExportBoatSheet = {
            boatSheetExportLauncher.launch(defaultBoatSheetExportFileName(uiState.boat.name))
        },
        onDismissMessage = viewModel::clearMessage,
    )
}

@Composable
fun BoatDetailScreen(
    contentPadding: PaddingValues,
    uiState: BoatDetailUiState,
    onNameChanged: (String) -> Unit,
    onSeatCountChanged: (String) -> Unit,
    onTypeChanged: (String) -> Unit,
    onWeightSingleChanged: (String) -> Unit,
    onWeightMinChanged: (String) -> Unit,
    onWeightMaxChanged: (String) -> Unit,
    onUseWeightRangeChanged: (Boolean) -> Unit,
    onRiggingCoupleChanged: (Boolean) -> Unit,
    onRiggingPointeChanged: (Boolean) -> Unit,
    onYearChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onSaveBoat: () -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onAddPhoto: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onOpenFullHistory: (Long) -> Unit,
    onOpenBoatRemarks: (Long) -> Unit,
    onAddBoatRemark: (Long) -> Unit,
    onExportBoatSheet: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val trackedOnNameChanged = rememberInteractionAwareValueChange(onNameChanged)
    val trackedOnSeatCountChanged = rememberInteractionAwareValueChange(onSeatCountChanged)
    val trackedOnYearChanged = rememberInteractionAwareValueChange(onYearChanged)
    val trackedOnNotesChanged = rememberInteractionAwareValueChange(onNotesChanged)
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
    var showPhotoManager by remember { mutableStateOf(false) }
    val isCompactScreen = LocalConfiguration.current.screenWidthDp < 600
    val horizontalPadding = if (isCompactScreen) 12.dp else 20.dp
    val verticalPadding = if (isCompactScreen) 12.dp else 18.dp

    selectedPhotoPath?.let { photoPath ->
        BoatPhotoViewerDialog(
            filePath = photoPath,
            onDismiss = { selectedPhotoPath = null },
        )
    }

    if (showPhotoManager) {
        BoatPhotoManagerDialog(
            photos = uiState.boat.photos,
            onDismiss = { showPhotoManager = false },
            onAddPhotos = onAddPhoto,
            onDeletePhoto = onDeletePhoto,
            onViewPhoto = { selectedPhotoPath = it },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = if (uiState.isNewBoat) "Nouveau bateau" else "Détails du bateau",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        if (uiState.isLoading) {
            Text(
                text = "Chargement du bateau...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            BoatSectionCard(
                title = "Informations générales",
                subtitle = "Renseignez les caractéristiques du bateau et enregistrez-les.",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Statut",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        BoatStatusBadge(status = uiState.boat.status)
                    }
                    if (uiState.isEditMode) {
                        OutlinedButton(
                            onClick = onCancelEditing,
                        ) { Text("Annuler") }
                    } else {
                        if (isCompactScreen) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onExportBoatSheet,
                                    enabled = uiState.boat.hasPersistentBoat,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Exporter cette fiche bateau") }
                                Button(
                                    onClick = onStartEditing,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Modifier le bateau") }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = onExportBoatSheet,
                                    enabled = uiState.boat.hasPersistentBoat,
                                ) { Text("Exporter cette fiche bateau") }
                                Button(
                                    onClick = onStartEditing,
                                ) { Text("Modifier le bateau") }
                            }
                        }
                    }
                }

                if (uiState.isEditMode) {
                    AppBoatTextField(
                        value = uiState.boat.name,
                        onValueChange = trackedOnNameChanged,
                        label = "Nom",
                    )
                    AppBoatTextField(
                        value = uiState.boat.seatCount,
                        onValueChange = trackedOnSeatCountChanged,
                        label = "Nombre de places",
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                            AppBoatSelectorButton(
                                label = "Type",
                                value = uiState.boat.type.ifBlank { "Choisir un type" },
                                onClick = { typeMenuExpanded = true },
                            )
                        DropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false },
                        ) {
                            BoatTypeOptions.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        onTypeChanged(type)
                                        typeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    WeightRangeEditor(
                        boat = uiState.boat,
                        onWeightMinChanged = onWeightMinChanged,
                        onWeightMaxChanged = onWeightMaxChanged,
                    )

                    RiggingTypeEditor(
                        coupleChecked = uiState.boat.riggingCouple,
                        pointeChecked = uiState.boat.riggingPointe,
                        onCoupleChanged = onRiggingCoupleChanged,
                        onPointeChanged = onRiggingPointeChanged,
                    )

                    AppBoatTextField(
                        value = uiState.boat.year,
                        onValueChange = trackedOnYearChanged,
                        label = "Année",
                    )
                    AppBoatTextField(
                        value = uiState.boat.notes,
                        onValueChange = trackedOnNotesChanged,
                        label = "Notes",
                    )

                    Button(
                        onClick = onSaveBoat,
                        enabled = uiState.canSave && !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (uiState.isSaving) "Enregistrement..." else "Enregistrer le bateau")
                    }
                } else {
                    BoatInfoSheet(
                        title = "Nom",
                        value = uiState.boat.name,
                    )
                    BoatInfoSheet(
                        title = "Nombre de places",
                        value = uiState.boat.seatCount,
                    )
                    BoatInfoSheet(
                        title = "Type",
                        value = uiState.boat.type.ifBlank { "Non défini" },
                    )
                    BoatInfoSheet(
                        title = "Plage de poids",
                        value = uiState.boat.weightRangeDisplay,
                    )
                    BoatInfoSheet(
                        title = "Type d’armement",
                        value = uiState.boat.riggingTypeDisplay,
                    )
                    BoatInfoSheet(
                        title = "Année",
                        value = uiState.boat.year.ifBlank { "Non définie" },
                    )
                    BoatInfoSheet(
                        title = "Notes",
                        value = uiState.boat.notes.ifBlank { "Aucune note" },
                    )
                }
            }

            BoatSectionCard(
                title = "Remarques et maintenance",
                subtitle = "Affiche les 3 dernières remarques liées à ce bateau, y compris la maintenance.",
            ) {
                if (!uiState.boat.hasPersistentBoat) {
                    Text(
                        text = "Enregistrez d'abord le bateau pour consulter ses remarques.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (uiState.boat.remarks.isEmpty()) {
                    Text(
                        text = "Aucune remarque liée à ce bateau.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onAddBoatRemark(uiState.boat.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ajouter une remarque")
                    }
                } else {
                    uiState.boat.remarks.take(3).forEach { remark ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (remark.status == RemarkStatus.REPAIR_NEEDED) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            },
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = formatDateForDisplay(remark.date),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(remark.content)
                                Text(
                                    text = when (remark.status) {
                                        RemarkStatus.NORMAL -> "Remarque normale"
                                        RemarkStatus.REPAIR_NEEDED -> "Réparation nécessaire"
                                        RemarkStatus.REPAIRED -> "Réparée"
                                    },
                                    color = when (remark.status) {
                                        RemarkStatus.NORMAL -> MaterialTheme.colorScheme.primary
                                        RemarkStatus.REPAIR_NEEDED -> MaterialTheme.colorScheme.error
                                        RemarkStatus.REPAIRED -> MaterialTheme.colorScheme.tertiary
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val photoCount = decodeRemarkPhotoPaths(remark.photoPath).size
                                if (photoCount > 0) {
                                    Text(
                                        text = "$photoCount photo(s) associée(s)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { onAddBoatRemark(uiState.boat.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ajouter une remarque")
                    }
                    OutlinedButton(
                        onClick = { onOpenBoatRemarks(uiState.boat.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Voir toutes les remarques")
                    }
                }
            }

            BoatSectionCard(
                title = "Photos",
                subtitle = "Affichez les photos du bateau en miniatures et gérez-les depuis une fenêtre dédiée.",
            ) {
                if (!uiState.boat.hasPersistentBoat) {
                    Text(
                        text = "Enregistrez d'abord le bateau pour ajouter des photos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedButton(
                        onClick = { showPhotoManager = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Modifier les photos")
                    }

                    if (uiState.boat.photos.isEmpty()) {
                        Text(
                            text = "Aucune photo enregistrée.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        BoatPhotoThumbnailGrid(
                            photos = uiState.boat.photos,
                            onPhotoClick = { selectedPhotoPath = it },
                        )
                    }
                }
            }

            BoatSectionCard(
                title = "Historique",
                subtitle = "Affiche uniquement les 2 dernières sessions de ce bateau.",
            ) {
                if (!uiState.boat.hasPersistentBoat) {
                    Text(
                        text = "Enregistrez d'abord le bateau pour consulter son historique.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (uiState.boat.recentSessions.isEmpty()) {
                    Text(
                        text = "Aucune session trouvée pour ce bateau.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    uiState.boat.recentSessions.forEach { session ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSession(session.id) }
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = formatDateForDisplay(session.date),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Rameurs : ${session.rowers.joinToString()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onOpenFullHistory(uiState.boat.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Voir l'historique complet")
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
private fun BoatSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                content()
            },
        )
    }
}

@Composable
private fun AppBoatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
        ),
        singleLine = true,
        maxLines = 1,
    )
}

@Composable
private fun AppBoatSelectorButton(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(value)
        }
    }
}

@Composable
private fun BoatInfoSheet(
    title: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun WeightRangeEditor(
    boat: BoatDetailUi,
    onWeightMinChanged: (String) -> Unit,
    onWeightMaxChanged: (String) -> Unit,
) {
    var minExpanded by remember { mutableStateOf(false) }
    var maxExpanded by remember { mutableStateOf(false) }
    val weightOptions = (30..120 step 5).map { it.toString() }

    Text("Poids", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        text = "Choisissez un minimum et un maximum par pas de 5 kg.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AppBoatSelectorButton(
                label = "Poids min",
                value = boat.weightMinValue.ifBlank { "Choisir" },
                onClick = { minExpanded = true },
            )
            DropdownMenu(
                expanded = minExpanded,
                onDismissRequest = { minExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Aucun") },
                    onClick = {
                        onWeightMinChanged("")
                        minExpanded = false
                    },
                )
                weightOptions.forEach { value ->
                    DropdownMenuItem(
                        text = { Text("$value kg") },
                        onClick = {
                            onWeightMinChanged(value)
                            minExpanded = false
                        },
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            AppBoatSelectorButton(
                label = "Poids max",
                value = boat.weightMaxValue.ifBlank { "Choisir" },
                onClick = { maxExpanded = true },
            )
            DropdownMenu(
                expanded = maxExpanded,
                onDismissRequest = { maxExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Aucun") },
                    onClick = {
                        onWeightMaxChanged("")
                        maxExpanded = false
                    },
                )
                weightOptions.forEach { value ->
                    DropdownMenuItem(
                        text = { Text("$value kg") },
                        onClick = {
                            onWeightMaxChanged(value)
                            maxExpanded = false
                        },
                    )
                }
            }
        }
    }
    if (
        boat.weightMinValue.toIntOrNull() != null &&
        boat.weightMaxValue.toIntOrNull() != null &&
        boat.weightMinValue.toInt() > boat.weightMaxValue.toInt()
    ) {
        Text(
            text = "Le poids minimum doit être inférieur ou égal au poids maximum.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RiggingTypeEditor(
    coupleChecked: Boolean,
    pointeChecked: Boolean,
    onCoupleChanged: (Boolean) -> Unit,
    onPointeChanged: (Boolean) -> Unit,
) {
    Text("Type d’armement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = coupleChecked, onCheckedChange = onCoupleChanged)
            Text("Couple")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = pointeChecked, onCheckedChange = onPointeChanged)
            Text("Pointe")
        }
    }
}

private fun defaultBoatSheetExportFileName(boatName: String): String {
    val sanitizedName = boatName
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "bateau" }
    return "fiche_${sanitizedName}.zip"
}

@Composable
private fun BoatStatusBadge(
    status: BoatStatusUi,
) {
    val backgroundColor = when (status) {
        BoatStatusUi.AVAILABLE -> MaterialTheme.colorScheme.secondaryContainer
        BoatStatusUi.IN_USE -> MaterialTheme.colorScheme.primaryContainer
        BoatStatusUi.IN_REPAIR -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (status) {
        BoatStatusUi.AVAILABLE -> MaterialTheme.colorScheme.onSecondaryContainer
        BoatStatusUi.IN_USE -> MaterialTheme.colorScheme.onPrimaryContainer
        BoatStatusUi.IN_REPAIR -> MaterialTheme.colorScheme.onErrorContainer
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(contentColor),
            )
            Text(
                text = status.label(),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BoatPhotoThumbnailGrid(
    photos: List<BoatPhotoUi>,
    onPhotoClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        photos.chunked(3).forEach { rowPhotos ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowPhotos.forEach { photo ->
                    BoatPhotoThumbnail(
                        filePath = photo.filePath,
                        modifier = Modifier.weight(1f),
                        onClick = { onPhotoClick(photo.filePath) },
                    )
                }
                repeat(3 - rowPhotos.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BoatPhotoThumbnail(
    filePath: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bitmap = remember(filePath) { BitmapFactory.decodeFile(filePath) }
    Surface(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Miniature du bateau",
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Photo indisponible",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BoatPhotoManagerDialog(
    photos: List<BoatPhotoUi>,
    onDismiss: () -> Unit,
    onAddPhotos: () -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onViewPhoto: (String) -> Unit,
) {
    var pendingDeletePhotoId by remember { mutableStateOf<Long?>(null) }

    pendingDeletePhotoId?.let { photoId ->
        DeleteConfirmationDialog(
            onConfirm = {
                onDeletePhoto(photoId)
                pendingDeletePhotoId = null
            },
            onDismiss = { pendingDeletePhotoId = null },
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Modifier les photos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(
                    onClick = onAddPhotos,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ajouter des photos")
                }
                if (photos.isEmpty()) {
                    Text(
                        text = "Aucune photo enregistrée.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    photos.forEach { photo ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                BoatPhotoThumbnail(
                                    filePath = photo.filePath,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onViewPhoto(photo.filePath) },
                                )
                                Text(
                                    text = "Ajoutée le ${photo.createdAt.take(10)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(
                                    onClick = { pendingDeletePhotoId = photo.id },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Supprimer la photo")
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Fermer")
                }
            }
        }
    }
}

@Composable
private fun BoatPhotoViewerDialog(
    filePath: String,
    onDismiss: () -> Unit,
) {
    AppImageViewerDialog(
        filePath = filePath,
        onDismiss = onDismiss,
    )
}

@Composable
private fun BoatPhotoPreview(
    filePath: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(filePath) { BitmapFactory.decodeFile(filePath) }

    if (bitmap != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Photo du bateau",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat()),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Impossible d'afficher cette photo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val BoatTypeOptions = listOf("1x", "2-", "2x", "2+", "4-", "4x", "4+", "4x+", "4Yx+", "4Yx-", "8x+", "8+")

private fun BoatStatusUi.label(): String {
    return when (this) {
        BoatStatusUi.AVAILABLE -> "Disponible"
        BoatStatusUi.IN_USE -> "En cours d'utilisation"
        BoatStatusUi.IN_REPAIR -> "En réparation"
    }
}
