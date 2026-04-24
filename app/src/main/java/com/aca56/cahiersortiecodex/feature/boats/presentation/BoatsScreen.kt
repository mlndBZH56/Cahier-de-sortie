package com.aca56.cahiersortiecodex.feature.boats.presentation

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aca56.cahiersortiecodex.CahierSortieApplication
import com.aca56.cahiersortiecodex.data.local.entity.RemarkStatus
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialog
import com.aca56.cahiersortiecodex.ui.components.FeedbackDialogType
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 18.dp),
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
            sessionRepository = appContainer.sessionRepository,
            boatPhotoStorage = appContainer.boatPhotoStorage,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.addPhoto(uri)
        } else {
            viewModel.clearMessage()
        }
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
        onAddPhoto = {
            photoPickerLauncher.launch(arrayOf("image/*"))
        },
        onOpenSession = onOpenSession,
        onOpenFullHistory = onOpenFullHistory,
        onOpenBoatRemarks = onOpenBoatRemarks,
        onAddBoatRemark = onAddBoatRemark,
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
    onDismissMessage: () -> Unit,
) {
    val trackedOnNameChanged = rememberInteractionAwareValueChange(onNameChanged)
    val trackedOnSeatCountChanged = rememberInteractionAwareValueChange(onSeatCountChanged)
    val trackedOnYearChanged = rememberInteractionAwareValueChange(onYearChanged)
    val trackedOnNotesChanged = rememberInteractionAwareValueChange(onNotesChanged)
    var typeMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
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
                        Button(
                            onClick = onStartEditing,
                        ) { Text("Modifier le bateau") }
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
                        title = "Type de gréement",
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
                                remark.photoPath?.let { filePath ->
                                    BoatPhotoPreview(filePath)
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
                subtitle = "Ajoutez des photos locales ; elles sont compressées avant l'enregistrement.",
            ) {
                if (!uiState.boat.hasPersistentBoat) {
                    Text(
                        text = "Enregistrez d'abord le bateau pour ajouter des photos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Button(
                        onClick = onAddPhoto,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ajouter une photo")
                    }

                    if (uiState.boat.photos.isEmpty()) {
                        Text(
                            text = "Aucune photo enregistrée.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        uiState.boat.photos.forEach { photo ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    BoatPhotoPreview(photo.filePath)
                                    Text(
                                        text = "Ajoutée le ${photo.createdAt.take(10)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    OutlinedButton(
                                        onClick = { onDeletePhoto(photo.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Supprimer la photo")
                                    }
                                }
                            }
                        }
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
    Text("Type de gréement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
private fun BoatPhotoPreview(
    filePath: String,
) {
    val bitmap = remember(filePath) { BitmapFactory.decodeFile(filePath) }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Photo du bateau",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
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
        BoatStatusUi.IN_USE -> "Déjà utilisé"
        BoatStatusUi.IN_REPAIR -> "En réparation"
    }
}
